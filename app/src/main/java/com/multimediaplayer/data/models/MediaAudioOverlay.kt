package com.multimediaplayer.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AudioMode {
    ORIGINAL,       // 原声
    OVERRIDE,       // 覆盖（静音原声）
    MIX             // 混合
}

@Entity(tableName = "media_audio_overlay")
data class MediaAudioOverlay(
    @PrimaryKey
    val mediaId: Long,                   // 关联的视频ID
    val audioId: Long,                   // 附加音频ID
    val audioMode: AudioMode = AudioMode.OVERRIDE,
    val volume: Float = 1.0f,            // 音频音量 (0.0-1.0)
    val syncOffset: Long = 0,            // 同步偏移（毫秒）
    val isLoop: Boolean = true           // 是否循环
)
