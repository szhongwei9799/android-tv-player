package com.multimediaplayer.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransitionType {
    NONE,           // 无转场
    FADE,           // 淡入淡出
    SLIDE_LEFT,     // 左滑入
    SLIDE_RIGHT,    // 右滑入
    SLIDE_UP,       // 上滑入
    SLIDE_DOWN,     // 下滑入
    ZOOM_IN,        // 放大进入
    ZOOM_OUT,       // 缩小进入
    WIPE_LEFT,      // 左擦除
    WIPE_RIGHT,     // 右擦除
    DISSOLVE,       // 溶解
    BLUR,           // 模糊过渡
    RANDOM          // 随机选择
}

enum class PlayMode {
    SEQUENTIAL,     // 顺序播放
    RANDOM,         // 随机播放（每次随机选下一首）
    SHUFFLE         // 洗牌播放（打乱顺序后顺序播放）
}

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "默认播放列表",
    val description: String? = null,
    val transitionEffect: TransitionType = TransitionType.FADE,
    val defaultInterval: Int = 10,
    val tagPlayMode: PlayMode = PlayMode.SEQUENTIAL,
    val tagLoopCount: Int = -1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tags",
    primaryKeys = ["playlistId", "tagId"]
)
data class PlaylistTag(
    val playlistId: Long,
    val tagId: Long,
    val sortOrder: Int = 0,
    val playMode: PlayMode = PlayMode.SEQUENTIAL,
    val loopCount: Int = -1
)
