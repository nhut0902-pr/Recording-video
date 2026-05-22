package com.example.data.tiktok

import retrofit2.http.GET
import retrofit2.http.Query

interface TikTokApiService {
    @GET("api/")
    suspend fun getTikTokVideo(@Query("url") url: String): TikWmResponse
}
