package com.multimediaplayer.data.database

import androidx.room.*
import com.multimediaplayer.data.models.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): Playlist?

    @Query("SELECT * FROM playlists WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultPlaylist(): Playlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: Long)

    // 播放列表项目
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistItem(item: PlaylistItem)

    @Delete
    suspend fun deletePlaylistItem(item: PlaylistItem)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND mediaId = :mediaId")
    suspend fun deletePlaylistItemById(playlistId: Long, mediaId: Long)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun deleteAllPlaylistItems(playlistId: Long)

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY sortOrder")
    suspend fun getPlaylistItems(playlistId: Long): List<PlaylistItem>

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY sortOrder")
    fun getPlaylistItemsFlow(playlistId: Long): Flow<List<PlaylistItem>>

    @Query("""
        SELECT m.* FROM media m 
        INNER JOIN playlist_items pi ON m.id = pi.mediaId 
        WHERE pi.playlistId = :playlistId 
        ORDER BY pi.sortOrder
    """)
    fun getPlaylistMedia(playlistId: Long): Flow<List<Media>>

    // 播放列表-标签关联
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistTag(playlistTag: PlaylistTag)

    @Delete
    suspend fun deletePlaylistTag(playlistTag: PlaylistTag)

    @Query("DELETE FROM playlist_tags WHERE playlistId = :playlistId")
    suspend fun deleteAllPlaylistTags(playlistId: Long)

    @Query("SELECT * FROM playlist_tags WHERE playlistId = :playlistId")
    suspend fun getPlaylistTags(playlistId: Long): List<PlaylistTag>

    @Transaction
    suspend fun setPlaylistTags(playlistId: Long, tagIds: List<Long>) {
        deleteAllPlaylistTags(playlistId)
        tagIds.forEach { tagId ->
            insertPlaylistTag(PlaylistTag(playlistId, tagId))
        }
    }

    @Query("SELECT COUNT(*) FROM playlists")
    fun getPlaylistCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun getPlaylistItemCount(playlistId: Long): Int
}
