package com.multimediaplayer.data.database

import androidx.room.*
import com.multimediaplayer.data.models.ScheduledTask
import com.multimediaplayer.data.models.DisplaySettings
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    // 定时任务
    @Query("SELECT * FROM scheduled_tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<ScheduledTask>>

    @Query("SELECT * FROM scheduled_tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): ScheduledTask?

    @Query("SELECT * FROM scheduled_tasks WHERE isEnabled = 1")
    fun getEnabledTasks(): Flow<List<ScheduledTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ScheduledTask): Long

    @Update
    suspend fun updateTask(task: ScheduledTask)

    @Delete
    suspend fun deleteTask(task: ScheduledTask)

    @Query("DELETE FROM scheduled_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("UPDATE scheduled_tasks SET isEnabled = :enabled WHERE id = :id")
    suspend fun setTaskEnabled(id: Long, enabled: Boolean)

    @Query("SELECT COUNT(*) FROM scheduled_tasks")
    fun getTaskCount(): Flow<Int>

    // 显示设置
    @Query("SELECT * FROM display_settings WHERE id = 1")
    suspend fun getDisplaySettings(): DisplaySettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDisplaySettings(settings: DisplaySettings)

    @Update
    suspend fun updateDisplaySettings(settings: DisplaySettings)
}
