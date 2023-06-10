package com.example.myapplication.presentation

import com.example.myapplication.network.ProcessedDataResults
import com.example.myapplication.network.UploadReq
import okhttp3.RequestBody

interface ResultModelInterface {
    /**
     * Calls POST to retrieve uploading presigned URL
     */
    suspend fun uploadReq(fileName: String, preference: Boolean): Result<UploadReq>
    /**
     * Calls for GET method to upload image to bucket
     */
    suspend fun uploadImage(url: String, body: RequestBody): Result<Unit>
    /**
     * Calls for GET method to get processed text and audioUrl from server
     */
    suspend fun getProcessedData(jobId: String): Result<ProcessedDataResults>
}