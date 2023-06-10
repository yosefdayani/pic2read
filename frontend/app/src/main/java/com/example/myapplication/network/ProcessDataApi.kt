package com.example.myapplication.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

interface ProcessDataApi {
    /**
     * API calls
     */

    @POST("/")
    suspend fun uploadReq(
        @Body body: UploadReqBody
    ): Response<UploadReqResponse>

    @PUT
    suspend fun uploadImage(
        @Url url: String,
        @Body body: RequestBody
    ) : Response<Unit>

    @GET
    suspend fun getProcessedDataUrl(
        @Url url: String = BASE_PROCESSED_DATA_URL,
        @Query("jobId") jobId: String
    ) : Response<GetProcessedDataResponse>


    companion object{
        private const val BASE_UPLOAD_URL    = "https://stry426ebosd3go67en4u4yhti0imieg.lambda-url.us-west-2.on.aws/"
        const val BASE_PROCESSED_DATA_URL = "https://lngubcqomwqe7qqbdubopx4jhi0euytz.lambda-url.us-west-2.on.aws/"

        val instance: ProcessDataApi by lazy {
            val retrofit: Retrofit = createRetrofit()
            retrofit.create(ProcessDataApi::class.java)
        }

        private fun createRetrofit(): Retrofit {

            // Create converter
            val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

            // Create logger
            val logging = HttpLoggingInterceptor()
            logging.setLevel(HttpLoggingInterceptor.Level.BODY)

            // Create client
            val httpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            // Build Retrofit
            return Retrofit.Builder()
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .baseUrl(BASE_UPLOAD_URL)
                .client(httpClient)
                .build()
        }
    }
}
