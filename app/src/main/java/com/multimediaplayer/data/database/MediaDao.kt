package com.multimediaplayer.data.database

import androidx.room.*
import com.multimediaplayer.data.models.Media
import com.multimediaplayer.data.models.MediaType
import com.multimediaplayer.data.models.MediaSource
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media ORDER BY createdAt DESC")
    fun getAllMedia(): Flow<List<Media>>

    @Query("SELECT * FROM media WHERE type = :type ORDER BY createdAt DESC")
    fun getMediaByType(type: MediaType): Flow<List<Media>>

    @Query("SELECT * FROM media WHERE id = :id")
    suspend fun getMediaById(id: Long): Media?

    @Query("SELECT * FROM media WHERE name LIKE '%' || :query || '%'")
    fun searchMedia(query: String): Flow<List<Media>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: Media): Long

    @Update
    suspend fun updateMedia(media: Media)

    @Delete
    suspend fun deleteMedia(media: Media)

    @Query("DELETE FROM media WHERE id = :id")
    suspend fun deleteMediaById(id: Long)

    @Query("SELECT COUNT(*) FROM media")
    fun getMediaCount(): Flow<Int>

    @Query("SELECT * FROM media WHERE source = :source ORDER BY createdAt DESC")
    fun getMediaBySource(source: MediaSource): Flow<List<Media>>
}
