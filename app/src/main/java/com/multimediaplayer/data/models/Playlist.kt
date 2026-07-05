package com.multimediaplayer.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PlaylistType {
    MANUAL,     // 手动添加媒体
    TAG_BASED   // 基于标签动态生成
}

enum class SortOrder {
    MANUAL,     // 手动排序
    NAME_ASC,   // 名称升序
    NAME_DESC,  // 名称降序
    DATE_ASC,   // 创建时间升序
    DATE_DESC,  // 创建时间降序
    RANDOM      // 随机
}

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

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val type: PlaylistType = PlaylistType.MANUAL,
    val sortOrder: SortOrder = SortOrder.MANUAL,
    val transitionEffect: TransitionType = TransitionType.FADE,
    val defaultInterval: Int = 10,  // 默认显示间隔（秒）
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_items",
    primaryKeys = ["playlistId", "mediaId"]
)
data class PlaylistItem(
    val playlistId: Long,
    val mediaId: Long,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "playlist_tags",
    primaryKeys = ["playlistId", "tagId"]
)
data class PlaylistTag(
    val playlistId: Long,
    val tagId: Long
)
