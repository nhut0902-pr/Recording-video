package com.example.data

import kotlinx.coroutines.flow.Flow

class MediaRepository(private val mediaDao: MediaDao) {
    val allMediaItems: Flow<List<MediaItem>> = mediaDao.getAllMediaItems()

    fun getMediaItemsByType(type: String): Flow<List<MediaItem>> =
        mediaDao.getMediaItemsByType(type)

    suspend fun insert(item: MediaItem): Long =
        mediaDao.insertMediaItem(item)

    suspend fun deleteById(id: Int) =
        mediaDao.deleteMediaItemById(id)

    suspend fun deleteAll() =
        mediaDao.deleteAllMediaItems()
}
