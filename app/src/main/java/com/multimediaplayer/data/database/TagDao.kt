package com.multimediaplayer.data.database

import androidx.room.*
import com.multimediaplayer.data.models.Tag
import com.multimediaplayer.data.models.MediaTagCrossRef
import com.multimediaplayer.data.models.Media
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getTagById(id: Long): Tag?

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): Tag?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag): Long

    @Update
    suspend fun updateTag(tag: Tag)

    @Delete
    suspend fun deleteTag(tag: Tag)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTagById(id: Long)

    // 媒体-标签关联
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMediaTagCrossRef(crossRef: MediaTagCrossRef)

    @Delete
    suspend fun deleteMediaTagCrossRef(crossRef: MediaTagCrossRef)

    @Query("DELETE FROM media_tag_cross_ref WHERE mediaId = :mediaId")
    suspend fun deleteAllMediaTags(mediaId: Long)

    @Query("DELETE FROM media_tag_cross_ref WHERE tagId = :tagId")
    suspend fun deleteAllMediaTagsForTag(tagId: Long)

    @Query("SELECT * FROM media_tag_cross_ref WHERE mediaId = :mediaId")
    suspend fun getMediaTagRefs(mediaId: Long): List<MediaTagCrossRef>

    @Query("SELECT * FROM media_tag_cross_ref WHERE tagId = :tagId")
    suspend fun getTagMediaRefs(tagId: Long): List<MediaTagCrossRef>

    @Query("""
        SELECT m.* FROM media m 
        INNER JOIN media_tag_cross_ref mt ON m.id = mt.mediaId 
        WHERE mt.tagId = :tagId 
        ORDER BY m.createdAt DESC
    """)
    fun getMediaByTagId(tagId: Long): Flow<List<Media>>

    @Query("""
        SELECT DISTINCT m.* FROM media m 
        INNER JOIN media_tag_cross_ref mt ON m.id = mt.mediaId 
        WHERE mt.tagId IN (:tagIds) 
        ORDER BY m.createdAt DESC
    """)
    fun getMediaByTagIds(tagIds: List<Long>): Flow<List<Media>>

    @Query("""
        SELECT t.* FROM tags t 
        INNER JOIN media_tag_cross_ref mt ON t.id = mt.tagId 
        WHERE mt.mediaId = :mediaId
    """)
    fun getTagsForMedia(mediaId: Long): Flow<List<Tag>>

    @Query("SELECT COUNT(*) FROM media_tag_cross_ref WHERE mediaId = :mediaId")
    suspend fun getMediaTagCount(mediaId: Long): Int

    @Query("SELECT COUNT(*) FROM media_tag_cross_ref WHERE tagId = :tagId")
    suspend fun getTagMediaCount(tagId: Long): Int

    @Transaction
    suspend fun setMediaTags(mediaId: Long, tagIds: List<Long>) {
        deleteAllMediaTags(mediaId)
        tagIds.forEach { tagId ->
            insertMediaTagCrossRef(MediaTagCrossRef(mediaId, tagId))
        }
    }
}
