package com.multimediaplayer.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.multimediaplayer.data.database.AppDatabase
import com.multimediaplayer.data.models.ScheduledTask
import kotlinx.coroutines.runBlocking
import java.util.*

class TaskScheduler(private val context: Context) {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val database = AppDatabase.getDatabase(context)
    
    fun scheduleAllTasks() {
        runBlocking {
            val tasks = database.taskDao().getAllTasks()
            tasks.collect { taskList ->
                taskList.filter { it.isEnabled }.forEach { task ->
                    scheduleTask(task)
                }
            }
        }
    }
    
    fun scheduleTask(task: ScheduledTask) {
        val intent = Intent(context, TaskReceiver::class.java).apply {
            action = "com.multimediaplayer.TASK_ACTION"
            putExtra("task_id", task.id)
            putExtra("task_type", task.type.name)
            putExtra("playlist_id", task.playlistId)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val calendar = Calendar.getInstance()
        
        // 解析时间
        val timeParts = task.timeOfDay?.split(":")
        if (timeParts != null && timeParts.size == 2) {
            calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toIntOrNull() ?: 0)
            calendar.set(Calendar.MINUTE, timeParts[1].toIntOrNull() ?: 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }
        
        // 如果时间已过，设置为明天
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        // 检查日期范围
        task.startDate?.let { startDate ->
            if (calendar.timeInMillis < startDate) {
                calendar.timeInMillis = startDate
            }
        }
        
        task.endDate?.let { endDate ->
            if (calendar.timeInMillis > endDate) {
                return // 任务已过期
            }
        }
        
        // 检查星期几
        task.daysOfWeek?.let { daysOfWeek ->
            val days = daysOfWeek.split(",").map { it.trim().toIntOrNull() ?: 0 }
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            
            // Calendar: 1=Sunday, 2=Monday, ..., 7=Saturday
            // 我们使用: 1=Monday, ..., 7=Sunday
            val adjustedDayOfWeek = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
            
            if (days.isNotEmpty() && !days.contains(adjustedDayOfWeek)) {
                // 找到下一个符合条件的日期
                for (i in 1..7) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    val nextDay = calendar.get(Calendar.DAY_OF_WEEK)
                    val adjustedNextDay = if (nextDay == Calendar.SUNDAY) 7 else nextDay - 1
                    if (days.contains(adjustedNextDay)) {
                        break
                    }
                }
            }
        }
        
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // 没有精确闹钟权限，使用非精确闹钟
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
    
    fun cancelTask(taskId: Long) {
        val intent = Intent(context, TaskReceiver::class.java).apply {
            action = "com.multimediaplayer.TASK_ACTION"
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
    }
    
    fun cancelAllTasks() {
        runBlocking {
            val tasks = database.taskDao().getAllTasks()
            tasks.collect { taskList ->
                taskList.forEach { task ->
                    cancelTask(task.id)
                }
            }
        }
    }
}
