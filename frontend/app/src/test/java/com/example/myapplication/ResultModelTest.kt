package com.example.myapplication

import com.example.myapplication.network.ProcessDataApi
import com.example.myapplication.network.ProcessedDataResults.*
import com.example.myapplication.network.StatusFromApiCall
import com.example.myapplication.presentation.ResultModel
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.*


@OptIn(ExperimentalCoroutinesApi::class)
class ResultModelTest {
    private val model = ResultModel(ProcessDataApi.instance)

    /**
     * Calls the uploadReq method and asserts that the response is not empty.
     */
    @Test
    fun uploadReq_uploadReqResponseIsValid() = runTest {
        val result = model.uploadReq(FILE_NAME, true).onSuccess {
            Truth.assertThat(it.jobId).isNotNull()
            Truth.assertThat(it.url).isNotNull()
        }
        Truth.assertThat(result.isSuccess).isTrue()
    }

    /**
     * Upon a successful uploadReq call(assumed working) creates a RequestBody of an image
     * and checks that uploadImage is successful.
     */
    @Test
    fun uploadImage_uploadImageResponseIsValid() = runTest {
        model.uploadReq(FILE_NAME, true).onSuccess {
            val mediaType = "image/jpeg".toMediaTypeOrNull()
            val file = File(FILE_PATH)
            val body = file.asRequestBody(mediaType)
            val result = model.uploadImage(it.url, body)
            Truth.assertThat(result.isSuccess).isTrue()
        }.onFailure {
            Truth.assertThat(false).isTrue()
        }
    }

    /**
     * Upon a successful uploadReq and uploadImage calls(assumed working) starts periodically calling
     * getProcessedData and handles all cases of response until Success.
     */
    @Test
    fun getProcessedData_getProcessedDataResponseIsValid() = runTest {
        model.uploadReq(FILE_NAME, true).onSuccess {
            val jobId = it.jobId
            val mediaType = "image/jpeg".toMediaTypeOrNull()
            val file = File(FILE_PATH)
            val requestFile = file.asRequestBody(mediaType)
            model.uploadImage(it.url, requestFile).onSuccess {
                var stop = false //when Success is reached turns on flag to stop iterations
                for (i in 0..20) { //sets timeout to 10 seconds (sleep of 500ms)
                    if (stop) break //checks if Success was reached
                    model.getProcessedData(jobId)
                        .onSuccess { processedDataUrlResults ->
                            when (processedDataUrlResults) {
                                is Created -> {}
                                is TextProcessing -> {
                                    Truth.assertThat(processedDataUrlResults.status)
                                        .isEqualTo(StatusFromApiCall.TextProcessing)
                                }
                                is TextFailed -> {
                                    Truth.assertThat(processedDataUrlResults.status)
                                        .isEqualTo(StatusFromApiCall.TextFailed)
                                }
                                is AudioProcessing -> {
                                    Truth.assertThat(processedDataUrlResults.status)
                                        .isEqualTo(StatusFromApiCall.AudioProcessing)
                                    Truth.assertThat(processedDataUrlResults.processedText)
                                        .isNotEmpty()
                                }
                                is AudioFailed -> {
                                    Truth.assertThat(processedDataUrlResults.status)
                                        .isEqualTo(StatusFromApiCall.AudioFailed)
                                    Truth.assertThat(processedDataUrlResults.processedText)
                                        .isNotEmpty()
                                }
                                is Success -> {
                                    Truth.assertThat(processedDataUrlResults.status)
                                        .isEqualTo(StatusFromApiCall.Success)
                                    Truth.assertThat(processedDataUrlResults.processedText)
                                        .isNotEmpty()
                                    Truth.assertThat(processedDataUrlResults.urlAudio)
                                        .isNotEmpty()
                                    stop = true
                                }
                            }
                        }
                        .onFailure {
                            Truth.assertThat(true).isFalse()
                        }
                    Thread.sleep(500)  // test every status in time interval
                }
            }
        }
    }


    companion object {
        private const val FILE_PATH =
            "src/test/java/com/example/myapplication/data/english_text_gallery.jpg"
        private const val FILE_NAME = "model_test_pic.jpg"
    }
}