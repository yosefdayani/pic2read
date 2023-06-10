package com.example.myapplication.presentation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ImageView
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R

/**
 * Fragment of the main screen, the fragment sends the source of the image to the ResultFragment.
 */
class MainScreenFragment : Fragment(R.layout.fragment_main_screen) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //set icons onClickListeners
        requireView().findViewById<ImageView>(R.id.upload).setOnClickListener { galleryButton() }
        requireView().findViewById<ImageView>(R.id.camera).setOnClickListener { cameraButton() }
        requireView().findViewById<ImageView>(R.id.settings).setOnClickListener { settingsButton() }
    }

    /**
     * Camera button's onClickListener, navigates to ResultFragment with SOURCE_CAMERA
     */
    private fun cameraButton() {
        val action = MainScreenFragmentDirections
            .actionFragmentMainScreenToFragmentResult(ResultFragment.SOURCE_CAMERA)
        findNavController().navigate(action)
    }

    /**
     * Gallery button's onClickListener, navigates to ResultFragment with SOURCE_GALLERY
     */
    private fun galleryButton() {
        val action = MainScreenFragmentDirections
            .actionFragmentMainScreenToFragmentResult(ResultFragment.SOURCE_GALLERY)
        findNavController().navigate(action)
    }

    /**
     * Settings button's onClickListener, navigates to SettingsFragment
     */
    private fun settingsButton() {
        val action = MainScreenFragmentDirections
            .actionFragmentMainScreenToFragmentSettings()
        findNavController().navigate(action)
    }
}