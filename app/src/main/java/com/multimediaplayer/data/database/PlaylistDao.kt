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

    @Transaction
    suspend fun ensureDefaultPlaylist(): Playlist {
        var playlist = getDefaultPlaylist()
        if (playlist == null) {
            val id = insertPlaylist(Playlist())
            playlist = getPlaylistById(id)
        }
        return playlist!!
    }

    // === PlaylistItem ===
    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY sortOrder")
    suspend fun getPlaylistItems(playlistId: Long): List<PlaylistItem>

    @Query("SELECT * FROM playlist_items WHERE id = :id")
    suspend fun getPlaylistItemById(id: Long): PlaylistItem?

    @Insert
    suspend fun insertPlaylistItem(item: PlaylistItem): Long

    @Update
    suspend fun updatePlaylistItem(item: PlaylistItem)

    @Delete
    suspend fun deletePlaylistItem(item: PlaylistItem)

    // === PlaylistItemTag ===
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistItemTag(tag: PlaylistItemTag)

    @Query("DELETE FROM playlist_item_tags WHERE itemId = :itemId AND tagId = :tagId")
    suspend fun deletePlaylistItemTag(itemId: Long, tagId: Long)

    @Query("DELETE FROM playlist_item_tags WHERE tagId = :tagId")
    suspend fun deleteAllPlaylistItemTagsByTagId(tagId: Long)

    @Query("DELETE FROM playlist_item_tags WHERE itemId = :itemId")
    suspend fun deleteAllPlaylistItemTags(itemId: Long)

    @Query("SELECT * FROM playlist_item_tags WHERE itemId = :itemId ORDER BY sortOrder")
    suspend fun getPlaylistItemTags(itemId: Long): List<PlaylistItemTag>

    @Query("""
        SELECT t.*, pit.sortOrder, pit.playMode, pit.loopCount 
        FROM tags t 
        INNER JOIN playlist_item_tags pit ON t.id = pit.tagId 
        WHERE pit.itemId = :itemId 
        ORDER BY pit.sortOrder
    """)
    fun getItemTagsWithSettings(itemId: Long): Flow<List<ItemTagWithSettings>>

    @Transaction
    suspend fun replaceItemTags(itemId: Long, tags: List<PlaylistItemTag>) {
        deleteAllPlaylistItemTags(itemId)
        tags.forEach { insertPlaylistItemTag(it) }
    }
}

data class ItemTagWithSettings(
    val id: Long,
    val name: String,
    val color: String,
    val createdAt: Long,
    val sortOrder: Int,
    val playMode: String,
    val loopCount: Int
)