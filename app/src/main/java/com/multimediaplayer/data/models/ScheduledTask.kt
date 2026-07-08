package com.multimediaplayer.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskType {
    PLAY,           // 播放指定播放列表
    STOP,           // 停止播放
    POWER_OFF,      // 关机（需系统权限）
    SWITCH_SOURCE   // 切换信号源
}

@Entity(tableName = "scheduled_tasks")
data class ScheduledTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: TaskType,
    val playlistId: Long? = null,
    val endTime: String? = null,  // "HH:mm" - 播放结束时间, null=播放至媒体自然结束
    val cronExpression: String? = null,
    val timeOfDay: String? = null,
    val daysOfWeek: String? = null,  // 存储为逗号分隔字符串 "1,2,3,4,5"
    val startDate: Long? = null,
    val endDate: Long? = null,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "display_settings")
data class DisplaySettings(
    @PrimaryKey
    val id: Long = 1,
    val imageInterval: Int = 5,       // 图片显示间隔（秒）
    val pptInterval: Int = 10,        // PPT每页间隔（秒）
    val pdfInterval: Int = 10,        // PDF每页间隔（秒）
    val transitionDuration: Int = 500 // 转场时长（毫秒）
)

enum class VideoEndAction {
    NEXT,           // 播放下一个
    LOOP,           // 循环播放
    PAUSE           // 暂停
}
