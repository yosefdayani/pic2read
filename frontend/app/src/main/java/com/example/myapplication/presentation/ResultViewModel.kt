package com.example.myapplication.presentation

import android.net.Uri
import androidx.lifecycle.*
import com.example.myapplication.network.ProcessedDataResults
import com.example.myapplication.network.StatusFromApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

class ResultViewModel(
    private val requestBodyBuilder: RequestBodyBuilder,
    private val model: ResultModelInterface,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO  // So we could use test dispatcher
): ViewModel() {

    private val _data = MutableLiveData<UIModel>()
    val data : LiveData<UIModel> = _data
    private var uiModel = UIModel()

    fun insertImage(uri : Uri){
        /**
         * posts the chosen uri to data
         */
        uiModel.image = uri
        uiModel.status = JobStatus.IMAGE_RECEIVED
        _data.postValue(uiModel)
    }

    fun uploadReq(preference: Boolean) {
        /**
         * Calls for model's method to request upload url, if succeeded updates and posts uiModel
         */
        viewModelScope.launch(dispatcher) {
            // random file name to enable simultaneous requests
            val fileName = Random.nextInt(0, 10000).toString() + FILE_NAME
            model.uploadReq(fileName, preference)
                .onSuccess { uploadRequest ->
                    uiModel.uploadUrl = uploadRequest.url
                    uiModel.jobId = uploadRequest.jobId
                    uiModel.status = JobStatus.URL_FOR_UPLOAD_FETCHED
                    _data.postValue(uiModel)
                }
                .onFailure {
                    uiModel.status = JobStatus.ERROR_URL_FETCH
                    _data.postValue(uiModel)
                }
        }
    }

    fun uploadImage() {
        /**
         * Calls for model's method to upload image and updates uiModel's status accordingly
         */
        viewModelScope.launch(dispatcher) {
            val requestBody = requestBodyBuilder.build(uiModel.image)
            model.uploadImage(uiModel.uploadUrl, requestBody)
                .onSuccess {
                    uiModel.status = JobStatus.IMAGE_UPLOADED
                    _data.postValue(uiModel)
                }
                .onFailure {
                    uiModel.status = JobStatus.ERROR_IMAGE_UPLOAD
                    _data.postValue(uiModel)
                }
        }
    }


    fun getProcessedData() {
        /**
         * Calls for model's method to get processed text and audioUrl, updated and posts the
         * uiModel according to the status returned from the server, and updates the JobStatus
         */
        viewModelScope.launch(dispatcher) {
            model.getProcessedData(uiModel.jobId)
                .onSuccess { processedDataResult ->
                    when (processedDataResult) {
                        is ProcessedDataResults.Created -> {
                            _data.postValue(uiModel)
                        }
                        is ProcessedDataResults.TextProcessing -> {
                            uiModel.status = JobStatus.STILL_PROCESSING_TEXT
                            _data.postValue(uiModel)
                        }
                        is ProcessedDataResults.TextFailed -> {
                            uiModel.status = JobStatus.PROCESSING_TEXT_FAILURE
                            _data.postValue(uiModel)
                        }
                        is ProcessedDataResults.AudioProcessing -> {
                            uiModel.processedText = processedDataResult.processedText
                            uiModel.status = JobStatus.STILL_PROCESSING_AUDIO
                            _data.postValue(uiModel)
                        }

                        is ProcessedDataResults.AudioFailed -> {
                            uiModel.processedText = processedDataResult.processedText
                            uiModel.status = JobStatus.PROCESSING_AUDIO_FAILURE
                            if(processedDataResult.status == StatusFromApiCall.LanguageNotSupported )
                                uiModel.status = JobStatus.AUDIO_FAILURE_LANGUAGE_NOT_SUPPORTED
                            if(processedDataResult.status == StatusFromApiCall.TextTooShort )
                                uiModel.status = JobStatus.AUDIO_FAILURE_TEXT_TOO_SHORT
                            _data.postValue(uiModel)
                        }

                        is ProcessedDataResults.Success -> {
                            uiModel.processedText = processedDataResult.processedText
                            uiModel.audioUrl = processedDataResult.urlAudio
                            uiModel.status = JobStatus.FINISH
                            _data.postValue(uiModel)
                        }
                    }
                }
                .onFailure { uiModel.status = JobStatus.GET_FAILURE
                    _data.postValue(uiModel) }
        }
    }

    class Factory(
        var bodyBuilder: RequestBodyBuilder,
        var resultModel: ResultModelInterface
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ResultViewModel(bodyBuilder, resultModel) as T
        }
    }
    companion object{
        const val FILE_NAME = "pic.jpg"
    }
}
