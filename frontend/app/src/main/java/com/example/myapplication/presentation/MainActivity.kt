package com.example.myapplication.presentation

import android.content.pm.ActivityInfo
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.example.myapplication.R
import androidx.navigation.fragment.NavHostFragment

class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setNavigationGraph()
        // allows completely full screen
        val attrib = window.attributes
        attrib.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        // lock portrait orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    private fun setNavigationGraph() {
        val navController =
            (supportFragmentManager.findFragmentById(R.id.main_nav_host) as NavHostFragment).navController
        val graph = navController.navInflater.inflate(R.navigation.navigation)
        navController.graph = graph
    }
}