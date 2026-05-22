package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items ORDER BY timestamp DESC")
    fun getAllMediaItems(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE type = :type ORDER BY timestamp DESC")
    fun getMediaItemsByType(type: String): Flow<List<MediaItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItem(item: MediaItem): Long

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteMediaItemById(id: Int)

    @Query("DELETE FROM media_items")
    suspend fun deleteAllMediaItems()
}
