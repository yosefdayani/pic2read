package com.example.myapplication

import android.app.Application
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.example.myapplication.network.ProcessedDataResults
import com.example.myapplication.network.StatusFromApiCall.*
import com.example.myapplication.network.UploadReq
import com.example.myapplication.presentation.JobStatus.*
import com.example.myapplication.presentation.RequestBodyBuilder
import com.example.myapplication.presentation.ResultModelInterface
import com.example.myapplication.presentation.ResultViewModel
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.RequestBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ResultViewModelTest {

    private lateinit var viewModel: ResultViewModel
    private val context = ApplicationProvider.getApplicationContext() as Application
    private lateinit var model: FakeResultModel
    private var uri = Uri.fromFile(File(FILE_PATH))

    @get:Rule
    val instantLiveData = InstantTaskExecutorRule()

    // init viewModel and model before every test case
    @Before
    fun setup() {
        val contentResolver = RequestBodyBuilder(context.applicationContext.contentResolver)
        model = FakeResultModel()
        viewModel = ResultViewModel(contentResolver, model, UnconfinedTestDispatcher())
    }

    /**
     * Calls insertImage and asserts that the image and status in the liveData were updated.
     */
    @Test
    fun insertImage_imageUpdated() {
        viewModel.insertImage(uri)
        Truth.assertThat(viewModel.data.value!!.image).isNotEqualTo(Uri.EMPTY)
        Truth.assertThat(viewModel.data.value!!.status).isEqualTo(IMAGE_RECEIVED)
    }

    /**
     * Calls uploadReq and asserts that the uploadUrl and status in the liveData were updated upon a
     * successful model call
     */
    @Test
    fun uploadReq_modelSuccess_uploadUrlExists() {
        viewModel.uploadReq(true)
        Truth.assertThat(viewModel.data.value!!.uploadUrl).isNotEmpty()
        Truth.assertThat(viewModel.data.value!!.status).isEqualTo(URL_FOR_UPLOAD_FETCHED)
    }

    /**
     * Calls uploadReq and asserts that the status in the liveData was updated upon an
     * unsuccessful model call and no uploadUrl was given.
     */
    @Test
    fun uploadReq_modelFailure_uploadUrlEmpty() {
        model.isGood = false
        viewModel.uploadReq(true)
        Truth.assertThat(viewModel.data.value!!.uploadUrl).isEmpty()
        Truth.assertThat(viewModel.data.value!!.status).isEqualTo(ERROR_URL_FETCH)
    }

    /**
     * Assuming uploadReq is valid,
     * Calls uploadImage and asserts that the uploadUrl and status in the liveData were updated upon a
     * successful model call
     */
    @Test
    fun uploadImage_modelSuccess_statusSuccess() {
        viewModel.uploadReq(true)
        viewModel.insertImage(uri)
        viewModel.uploadImage()
        Truth.assertThat(viewModel.data.value!!.status).isEqualTo(IMAGE_UPLOADED)
    }
    /**
     * Assuming uploadReq is valid,
     * Calls uploadImage and asserts that the status in the liveData was updated upon an
     * unsuccessful model call
     */
    @Test
    fun uploadImage_modelFailure_statusFailure() {
        viewModel.uploadReq(true)
        viewModel.insertImage(uri)
        model.isGood = false
        viewModel.uploadImage()
        Truth.assertThat(viewModel.data.value!!.status).isEqualTo(ERROR_IMAGE_UPLOAD)
    }

    /**
     * Assuming uploadReq and uploadImage are valid,
     * Calls getProcessedData and since our FakeResultModel returns all cases one after the other,
     * we check the handling of each case in turn.
     * asserts that the status and processedText/audioUrl (if relevant) were updated in the liveData
     * upon a successful model call.
     */
    @Test
    fun getProcessedData_modelSuccess_uiModelUpdated() {
        viewModel.uploadReq(true)
        viewModel.insertImage(uri)
        viewModel.uploadImage()
        // in Created state
        viewModel.getProcessedData()
        Truth.assertThat(viewModel.data.value!!.status).isEqualTo(IMAGE_UPLOADED)
        viewModel.getProcessedData()
        // in TextProcessing state
        Truth.assertThat(viewModel.data.value!!.status).isEqualTo(STILL_PROCESSING_TEXT)
        viewModel.getProcessedData()
        // in TextFailed state
        Truth.assertThat(viewModel.data.value!!.status).isEqualTo(PROCESSING_TEXT_FAILURE)
        viewModel.getProcessedData()
        // in AudioProcessing state
        Truth.assertThat(viewModel.data.value!!.status).isEqualTo(STILL_PROCESSING_AUDIO)
        Truth.assertThat(viewModel.data.value!!.processedText).isNotEmpty()
        viewModel.getProcessedData()
        // in AudioFailed state
        Truth.assertThat(viewModel.data.value!!.status).isEqualTo(PROCESSING_AUDIO_FAILURE)
        Truth.assertThat(viewModel.data.value!!.processedText).isNotEmpty()
        viewModel.getProcessedData()
        Truth.assertThat(viewModel.data.value!!.status).isEqualTo(AUDIO_FAILURE_TEXT_TOO_SHORT)
        Truth.assertThat(viewModel.data.value!!.processedText).isNotEmpty()
        viewModel.getProcessedData()
        Truth.assertThat(viewModel.data.value!!.status)
            .isEqualTo(AUDIO_FAILURE_LANGUAGE_NOT_SUPPORTED)
        Truth.assertThat(viewModel.data.value!!.processedText).isNotEmpty()
        viewModel.getProcessedData()
        // in Success state
        Truth.assertThat(viewModel.data.value!!.status).isEqualTo(FINISH)
        Truth.assertThat(viewModel.data.value!!.processedText).isNotEmpty()
        Truth.assertThat(viewModel.data.value!!.audioUrl).isNotEmpty()
    }

    /**
     * Assuming uploadReq and uploadImage are valid,
     * Calls getProcessedData and asserts that the status in the liveData was updated upon an
     * unsuccessful model call
     */
    @Test
    fun getProcessedData_modelFailure_uiModelUpdated() {
        viewModel.uploadReq(true)
        viewModel.insertImage(uri)
        viewModel.uploadImage()
        model.isGood = false
        // http GET failure
        viewModel.getProcessedData()
        Truth.assertThat(viewModel.data.value!!.status).isEqualTo(GET_FAILURE)
    }


    companion object {
        // a list of all statuses for the FakeResultModel to iterate over
        private val statuses = listOf(
            Created,
            TextProcessing,
            TextFailed,
            AudioProcessing,
            AudioFailed,
            TextTooShort,
            LanguageNotSupported,
            Success
        )
        //constants
        private const val JOB_ID = "fake_job_id"
        private const val UPLOAD_URL = "fake_upload_url"
        private const val AUDIO_URL = "fake_audio_url"
        private const val PROCESSED_TEXT = "fake_text"
        private const val FILE_PATH =
            "src/test/java/com/example/myapplication/data/english_text_gallery.jpg"

        //Exception class to return upon failure
        class ApiError(error: String) : Exception(error)

        //Model class which implements the ResultModelInterface
        class FakeResultModel : ResultModelInterface {
            var isGood = true //will the api calls succeed
            var numOfGetCalls = 0 //counts the amount of times the getProcessedData function was called

            /**
             * if the model succeeds returns jobId and url with fixed values, else returns Failure.
             */
            override suspend fun uploadReq(fileName: String, preference: Boolean): Result<UploadReq> {
                if (isGood) {
                    return Result.success(UploadReq(jobId = JOB_ID, url = UPLOAD_URL))
                }
                return Result.failure(ApiError("error"))
            }

            /**
             * if the model succeeds returns Success, else returns Failure.
             */
            override suspend fun uploadImage(url: String, body: RequestBody): Result<Unit> {
                if (isGood) {
                    return Result.success(Unit)
                }
                return Result.failure(ApiError("error"))
            }

            /**
             * if the model fails returns Failure, else:
             * iterates over all statuses and returns the Result according to the values the viewModel
             * expects to receive.
             */
            override suspend fun getProcessedData(jobId: String): Result<ProcessedDataResults> {
                return if (isGood) {
                    when (val currStatus = statuses[numOfGetCalls++ % 8]) {
                        Created -> Result.success(ProcessedDataResults.Created(status = currStatus))
                        TextProcessing ->
                            Result.success(ProcessedDataResults.TextProcessing(status = currStatus))
                        AudioProcessing ->
                            Result.success(
                                ProcessedDataResults.AudioProcessing(
                                    status = currStatus,
                                    processedText = PROCESSED_TEXT
                                )
                            )
                        Success ->
                            Result.success(
                                ProcessedDataResults.Success(
                                    status = currStatus,
                                    processedText = PROCESSED_TEXT,
                                    urlAudio = AUDIO_URL
                                )
                            )
                        AudioFailed ->
                            Result.success(
                                ProcessedDataResults.AudioFailed(
                                    status = currStatus,
                                    processedText = PROCESSED_TEXT
                                )
                            )
                        LanguageNotSupported ->
                            Result.success(
                                ProcessedDataResults.AudioFailed(
                                    status = currStatus,
                                    processedText = PROCESSED_TEXT
                                )
                            )
                        TextTooShort ->
                            Result.success(
                                ProcessedDataResults.AudioFailed(
                                    status = currStatus,
                                    processedText = PROCESSED_TEXT
                                )
                            )
                        TextFailed -> Result.success(ProcessedDataResults.TextFailed(status = currStatus))
                    }
                } else {
                    Result.failure(ApiError("Error: can't get processed image url"))  // TODO
                }
            }
        }
    }
}