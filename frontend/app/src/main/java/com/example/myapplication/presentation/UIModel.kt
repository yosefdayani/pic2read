package com.example.myapplication.presentation

import android.net.Uri

data class UIModel(
    /**
     * UIModel class for storing the job's data, this will be posted by the VM to LiveData
     */
    var image: Uri = Uri.EMPTY,
    var processedText: String = "",
    var uploadUrl: String = "",
    var jobId: String = "",
    var audioUrl: String = "",
    var status: JobStatus = JobStatus.JOB_BEGIN
)
