package com.example.aiphishingmodel.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

data class MessageRequest(
    val app: String,
    val title: String,
    val body: String
)

data class PredictionResponse(
    val status: String,
    val prediction: String,
    val confidence: Double,
    val message: String
)

interface ApiService {

    @Headers("x-api-key: mysecretkey")

    @POST("/notify")

    fun sendMessage(
        @Body request: MessageRequest
    ): Call<PredictionResponse>

}