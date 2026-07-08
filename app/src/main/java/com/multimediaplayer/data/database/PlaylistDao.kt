package com.multimediaplayer.data.database

import androidx.room.*
import com.multimediaplayer.data.models.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC LIMIT 1")
    suspend fun getDefaultPlaylist(): Playlist?

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): Playlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    // 播放列表-标签关联
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistTag(playlistTag: PlaylistTag)

    @Delete
    suspend fun deletePlaylistTag(playlistTag: PlaylistTag)

    @Query("DELETE FROM playlist_tags WHERE playlistId = :playlistId AND tagId = :tagId")
    suspend fun deletePlaylistTagById(playlistId: Long, tagId: Long)

    @Query("DELETE FROM playlist_tags WHERE playlistId = :playlistId")
    suspend fun deleteAllPlaylistTags(playlistId: Long)

    @Query("DELETE FROM playlist_tags WHERE tagId = :tagId")
    suspend fun deleteAllPlaylistTagsByTagId(tagId: Long)

    @Query("SELECT * FROM playlist_tags WHERE playlistId = :playlistId ORDER BY sortOrder")
    suspend fun getPlaylistTags(playlistId: Long): List<PlaylistTag>

    @Query("""
        SELECT t.*, pt.sortOrder, pt.playMode, pt.loopCount 
        FROM tags t 
        INNER JOIN playlist_tags pt ON t.id = pt.tagId 
        WHERE pt.playlistId = :playlistId 
        ORDER BY pt.sortOrder
    """)
    fun getPlaylistTagsWithSettings(playlistId: Long): Flow<List<TagWithSettings>>

    @Transaction
    suspend fun ensureDefaultPlaylist(): Playlist {
        var playlist = getDefaultPlaylist()
        if (playlist == null) {
            val id = insertPlaylist(Playlist())
            playlist = getPlaylistById(id)
        }
        return playlist!!
    }
}

data class TagWithSettings(
    val id: Long,
    val name: String,
    val color: String,
    val createdAt: Long,
    val sortOrder: Int,
    val playMode: String,
    val loopCount: Int
)
