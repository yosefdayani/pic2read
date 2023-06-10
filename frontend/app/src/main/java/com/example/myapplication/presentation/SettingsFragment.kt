package com.example.myapplication.presentation

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.example.myapplication.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}