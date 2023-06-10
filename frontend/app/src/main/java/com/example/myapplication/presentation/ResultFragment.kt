package com.example.myapplication.presentation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import com.example.myapplication.R
import com.example.myapplication.network.ProcessDataApi
import java.io.File
import com.example.myapplication.presentation.JobStatus.*

/**
 * Fragment of the result screen, the fragment gets a picture from the user and the viewModel processes it.
 * The fragment uses the devices Gallery and Camera to get a picture, and then by observing the
 * viewModels live data, updates in a textView the detected text, and activates a button which plays
 * a voice reading the text.
 */
class ResultFragment : Fragment(R.layout.fragment_result) {

    private lateinit var resultViewModel: ResultViewModel

    //declaration of UI elements to be initialized in onViewCreated,
    //and to be accessed from various places in the fragment
    private lateinit var text: TextView
    private lateinit var playButton: ImageView
    private lateinit var homeButton: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var scrollView: ScrollView
    private var mediaPlayer = MediaPlayer()
    private var mediaPrepared = false //Indicator that mediaPlayer is prepared
    private var maleVoice = false  // default voice is female

    private val args: ResultFragmentArgs by navArgs()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resultViewModel = ViewModelProvider(
            this,
            ResultViewModel.Factory(
                RequestBodyBuilder(requireContext().applicationContext.contentResolver),
                ResultModel(
                    ProcessDataApi.instance
                )
            )
        ).get()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //The fragment wasn't destroyed
        if (savedInstanceState == null) {
            when (args.source) {
                SOURCE_CAMERA -> photoFromCamera()
                SOURCE_GALLERY -> photoFromGallery()
            }
        }
        findViews(view)
        setPreferences()
        setOnClicks()
        setOnCallBack()
        setupObserve()
    }

    /**
     * Sets up the observer, invoking viewModel functions to process an image.
     */
    private fun setupObserve() {
        resultViewModel.data.observe(viewLifecycleOwner) {
            when (it.status) {
                JOB_BEGIN -> {}
                IMAGE_RECEIVED -> {
                    launchToast(MSG_PLEASE_WAIT)
                    resultViewModel.uploadReq(maleVoice)
                }
                URL_FOR_UPLOAD_FETCHED -> resultViewModel.uploadImage()
                ERROR_URL_FETCH, ERROR_IMAGE_UPLOAD,
                PROCESSING_TEXT_FAILURE, GET_FAILURE -> launchToast(MSG_ERROR)
                IMAGE_UPLOADED -> resultViewModel.getProcessedData()
                STILL_PROCESSING_TEXT -> resultViewModel.getProcessedData()
                STILL_PROCESSING_AUDIO -> {
                    showViews(it)
                    resultViewModel.getProcessedData()
                }
                AUDIO_FAILURE_LANGUAGE_NOT_SUPPORTED -> {
                    showViews(it)
                    launchToast(MSG_LANGUAGE_NOT_SUPPORTED)
                }
                AUDIO_FAILURE_TEXT_TOO_SHORT -> {
                    launchToast(MSG_TEXT_TOO_SHORT)
                    showViews(it)
                }
                PROCESSING_AUDIO_FAILURE -> {
                    launchToast(MSG_ERROR)
                    showViews(it)
                }
                FINISH -> {
                    showViews(it)
                    prepareMediaPlayer()
                }
            }
        }
    }

    /**
     * Implements a callBack when back is pressed, which stops the audio if currently playing,
     * and returns to the MainScreenFragment by popping from the BackStack.
     */
    private fun setOnCallBack() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (mediaPrepared && mediaPlayer.isPlaying) {
                mediaPlayer.release()
            }
            findNavController().popBackStack()
        }
    }

    /**
     * Gets the users preferences and changes the views/variables accordingly.
     */
    private fun setPreferences() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext()).all
        if (preferences.isNotEmpty()) {  // check whether preferences fragment was created
            text.textSize = (preferences["font_size"] as String).toFloat()
            text.typeface = Typeface.create(preferences["font"] as String, Typeface.NORMAL)
            maleVoice = preferences["gender"] as Boolean
        }
    }

    /**
     * Initializes the view variables of the fragment
     */
    private fun findViews(view: View) {
        text = view.findViewById(R.id.text)
        playButton = view.findViewById(R.id.play)
        progressBar = view.findViewById(R.id.progressBar)
        scrollView = view.findViewById(R.id.scrollView)
        homeButton = view.findViewById(R.id.homeBtn)
    }

    /**
     * Sets the onClickListeners of the icons in the ResultFragment
     */
    private fun setOnClicks() {
        //if the mediaPlayer is prepared, starts and pauses depending on the current state.
        playButton.setOnClickListener {
            if (mediaPrepared) {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                    playButton.setImageResource(R.drawable.play)
                } else {
                    mediaPlayer.start()
                    playButton.setImageResource(R.drawable.pause)
                }
            }
        }
        //the home button pops the backStack and releases the mediaPlayer if its playing.
        homeButton.setOnClickListener() {
            if (mediaPrepared && mediaPlayer.isPlaying) {
                mediaPlayer.release()
            }
            findNavController().popBackStack()
        }
    }

    /**
     * Turns on the visibility of the views and removes the progressBar.
     */
    private fun showViews(ui: UIModel) {
        progressBar.visibility = View.GONE
        scrollView.visibility = View.VISIBLE
        homeButton.visibility = View.VISIBLE
        playButton.visibility = View.VISIBLE
        text.text = ui.processedText
    }

    /**
     * Displays toast with message
     */
    private fun launchToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    }

    /**
     * Prepares mediaPlayer with liveData's audioUrl.
     */
    private fun prepareMediaPlayer() {
        val audioUrl = resultViewModel.data.value?.audioUrl
        mediaPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(audioUrl)
            mediaPlayer.prepareAsync()
        } catch (e: Exception) {
            launchToast(MSG_MEDIA_ERROR)
        }
        //when prepared, changes the opacity of the icon and turns on mediaPrepared flag.
        mediaPlayer.setOnPreparedListener {
            playButton.alpha = 1F
            mediaPrepared = true
        }
        //when audio ends, changes resource
        mediaPlayer.setOnCompletionListener {
            playButton.setImageResource(R.drawable.play)
        }
    }

    /**
     * Gets camera permission and uses registerForActivityResult to get photo from Camera.
     */
    private fun photoFromCamera() {
        var uri = Uri.EMPTY //initial value, will be overwritten
        //TakePicture() writes the photo from the camera into uri variable.
        val resultLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { result ->
                if (result) {
                    resultViewModel.insertImage(uri)
                } else {
                    findNavController().popBackStack()
                }
            }

        //initializes an uri with a temp file and calls resultLauncher to write photo into uri.
        fun launchCameraIntent() {
            val photoFile = File.createTempFile(
                "IMG_", ".jpg",
                requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            )
            uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                photoFile
            )
            resultLauncher.launch(uri)
        }
        //asks for camera permissions, calls launchCameraIntent only if permissions are granted.
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchCameraIntent()
            } else {
                launchToast(MSG_PERMISSIONS_MISSING)
                findNavController().popBackStack()
            }
        }.launch(Manifest.permission.CAMERA)
    }

    /**
     * Uses registerForActivityResult to get photo from Gallery.
     */
    private fun photoFromGallery() {
        //Creates an activityResult launcher which gets an image from the devices Gallery.
        //if succeeds, inserts the image to the viewModel, if fails, returns to mainScreenFragment.
        val resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let {
                        resultViewModel.insertImage(it)
                    }
                } else {
                    findNavController().popBackStack()
                }
            }

        fun launchGalleyIntent() {
            val pickIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            resultLauncher.launch(pickIntent)
        }
        launchGalleyIntent()
    }

    companion object {
        const val SOURCE_CAMERA = "Camera"
        const val SOURCE_GALLERY = "Gallery"
        private const val MSG_LANGUAGE_NOT_SUPPORTED =
            "Supported audio languages: English, Spanish, French, Portuguese, Italian or German"
        private const val MSG_TEXT_TOO_SHORT =
            "Text under 20 characters cannot be processed to audio"
        private const val MSG_PLEASE_WAIT = "Please wait, this might take a while"
        private const val MSG_ERROR = "Error occurred, try again later"
        private const val MSG_MEDIA_ERROR = "Media Playing Error"
        private const val MSG_PERMISSIONS_MISSING = "Missing needed permissions to continue"
    }
}

