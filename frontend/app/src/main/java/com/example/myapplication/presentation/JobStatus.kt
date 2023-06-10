package com.example.myapplication.presentation

enum class JobStatus {
    /**
     * Statuses that indicate the job's progress along its handling
     */

    // First status
    JOB_BEGIN,

    IMAGE_RECEIVED,

    // Upload req status
    URL_FOR_UPLOAD_FETCHED,
    ERROR_URL_FETCH,

    // Upload image status
    IMAGE_UPLOADED,
    ERROR_IMAGE_UPLOAD,

    // Process text status
    STILL_PROCESSING_TEXT,
    PROCESSING_TEXT_FAILURE,

    // Process audio status
    STILL_PROCESSING_AUDIO,
    PROCESSING_AUDIO_FAILURE,
    AUDIO_FAILURE_LANGUAGE_NOT_SUPPORTED,
    AUDIO_FAILURE_TEXT_TOO_SHORT,


    GET_FAILURE,
    FINISH;


}