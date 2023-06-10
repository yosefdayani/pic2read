package com.example.myapplication.presentation

import com.example.myapplication.network.*
import com.example.myapplication.network.StatusFromApiCall.*

import okhttp3.RequestBody

private const val MSG_JOB_ID_NOT_EXISTS = "JobId does not exist"

class ResultModel(
    private val processDataApi: ProcessDataApi
) : ResultModelInterface {

    override suspend fun uploadReq(fileName: String, preference: Boolean): Result<UploadReq> {
        /**
         * Calls POST to retrieve uploading presigned URL
         */
        val body = UploadReqBody(fileName, preference)
        val response = processDataApi.uploadReq(body)
        return when {
            response.isSuccessful && response.body() != null -> {
                val uploadReqResponse = response.body()!!
                Result.success(
                    UploadReq(
                        jobId = uploadReqResponse.jobId,
                        url = uploadReqResponse.url
                    )
                )
            }
            else -> Result.failure(ApiError())
        }
    }


    override suspend fun uploadImage(url: String, body: RequestBody): Result<Unit> {
        /**
         * Calls for GET method to upload image to bucket
         */
        val response = processDataApi.uploadImage(url, body)
        return when {
            response.isSuccessful -> Result.success(Unit)
            else -> Result.failure(ApiError())
        }
    }


    override suspend fun getProcessedData(jobId: String): Result<ProcessedDataResults> {
        /**
         * Calls for GET method to get processed text and audioUrl from server
         */
        val response = processDataApi.getProcessedDataUrl(jobId = jobId)
        return when {
            response.isSuccessful && response.body() != null -> {
                val body: GetProcessedDataResponse = response.body()!!
                when(body.status){
                    TextProcessing ->
                        Result.success(ProcessedDataResults.TextProcessing(status = body.status))
                    AudioProcessing ->
                        Result.success(ProcessedDataResults.AudioProcessing(status = body.status, processedText = body.processedText!!))
                    Success ->
                        Result.success(ProcessedDataResults.Success(status = body.status, processedText = body.processedText!!, urlAudio = body.urlAudio!!))
                    AudioFailed ->
                        Result.success(ProcessedDataResults.AudioFailed(status = body.status, processedText = body.processedText!!))
                    LanguageNotSupported ->
                        Result.success(ProcessedDataResults.AudioFailed(status = body.status, processedText = body.processedText!!))
                    TextTooShort ->
                        Result.success(ProcessedDataResults.AudioFailed(status = body.status, processedText = body.processedText!!))
                    TextFailed ->
                        Result.success(ProcessedDataResults.TextFailed(status = body.status))
                    else -> Result.success(ProcessedDataResults.Created(status = body.status!!))
                }
            }
            else -> {
                when {
                    response.code() == 404 -> Result.failure(ApiError(MSG_JOB_ID_NOT_EXISTS))
                    else -> Result.failure(ApiError(MSG_REQ_ERROR))
                }
            }
        }
    }
}

private const val MSG_REQ_ERROR = "Request Error"

class ApiError(error: String = MSG_REQ_ERROR) : Exception(error)
