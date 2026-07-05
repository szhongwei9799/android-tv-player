package com.multimediaplayer.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: String = "#999999",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "media_tag_cross_ref",
    primaryKeys = ["mediaId", "tagId"]
)
data class MediaTagCrossRef(
    val mediaId: Long,
    val tagId: Long
)
