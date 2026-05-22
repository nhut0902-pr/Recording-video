package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val filePath: String,
    val type: String, // "RECORDING", "TIKTOK", "PHOTOBOOTH", "EDITED"
    val durationText: String = "", // e.g. "00:12" or "Photo"
    val timestamp: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0,
    val thumbnailPath: String? = null,
    val width: Int = 1920,
    val height: Int = 1080,
    val creatorName: String? = null, // TikTok username, or Photo booth style
    val sourceUrl: String? = null // TikTok original link
)
