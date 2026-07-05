package com.multimediaplayer.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MediaType {
    VIDEO, AUDIO, IMAGE, PPT, PDF, STREAM
}

enum class MediaSource {
    LOCAL, NETWORK
}

@Entity(tableName = "media")
data class Media(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: MediaType,
    val source: MediaSource,
    val path: String,
    val duration: Long? = null,
    val fileSize: Long = 0,
    val width: Int? = null,
    val height: Int? = null,
    val thumbnail: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
