package com.example.data.tiktok

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TikWmResponse(
    val code: Int = 0,
    val msg: String = "",
    val data: TikWmData? = null
)

@JsonClass(generateAdapter = true)
data class TikWmData(
    val id: String? = null,
    val title: String? = null,
    val play: String? = null, // Video URL without watermark
    val wmplay: String? = null, // Video URL with watermark
    val cover: String? = null, // Thumbnail cover
    val duration: Int? = null,
    val author: TikWmAuthor? = null
)

@JsonClass(generateAdapter = true)
data class TikWmAuthor(
    val id: String? = null,
    val unique_id: String? = null, // Username
    val nickname: String? = null,
    val avatar: String? = null
)
