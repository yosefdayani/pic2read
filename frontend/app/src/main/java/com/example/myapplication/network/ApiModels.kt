package com.example.myapplication.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


sealed class ProcessedDataResults {
    /**
     * class to be stored inside Result object returned from the model's getProcessedData
     */
    data class Created(
        val status: StatusFromApiCall
    ) : ProcessedDataResults()

    data class TextProcessing(
        val status: StatusFromApiCall
    ) : ProcessedDataResults()

    data class TextFailed(
        val status: StatusFromApiCall
    ) : ProcessedDataResults()

    data class AudioProcessing(
        val status: StatusFromApiCall,
        val processedText: String
    ) : ProcessedDataResults()

    data class AudioFailed(
        val status: StatusFromApiCall,
        val processedText: String
    ) : ProcessedDataResults()

    data class Success(
        val status: StatusFromApiCall,
        val processedText: String,
        val urlAudio: String
    ) : ProcessedDataResults()
}

/**
 * class to be stored inside Result object returned from the model's uploadReq
 */
data class UploadReq(
    val jobId: String,
    val url: String
)

// responses
@JsonClass(generateAdapter = true)
data class UploadReqResponse(
    @Json(name = "jobId")
    val jobId: String,
    @Json(name = "url")
    val url: String
)

@JsonClass(generateAdapter = true)
data class UploadReqBody(
    @Json(name = "fileName")
    val fileName: String,
    @Json(name = "gender")
    val gender: Boolean
)

@JsonClass(generateAdapter = true)
data class GetProcessedDataResponse(
    @Json(name = "status")
    val status: StatusFromApiCall?,
    @Json(name = "processedText")
    val processedText: String?,
    @Json(name = "urlAudio")
    val urlAudio: String?
)

@JsonClass(generateAdapter = false)
enum class StatusFromApiCall {
    /**
     * Statuses returned from the server
     */
    @Json(name = "created")
    Created,

    @Json(name = "text_processing")
    TextProcessing,

    @Json(name = "text_failed")
    TextFailed,

    @Json(name = "audio_processing")
    AudioProcessing,

    @Json(name = "audio_failed")
    AudioFailed,

    @Json(name = "language_not_supported")
    LanguageNotSupported,

    @Json(name = "text_too_short")
    TextTooShort,

    @Json(name = "success")
    Success;
}