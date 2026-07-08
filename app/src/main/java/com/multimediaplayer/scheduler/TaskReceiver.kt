package com.multimediaplayer.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.multimediaplayer.data.database.AppDatabase
import com.multimediaplayer.data.models.TaskType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.multimediaplayer.TASK_ACTION") {
            return
        }
        
        val taskId = intent.getLongExtra("task_id", -1)
        val taskType = intent.getStringExtra("task_type")
        val playlistId = intent.getLongExtra("playlist_id", -1)
        val endTime = intent.getStringExtra("end_time")
        
        if (taskId == -1L || taskType == null) {
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            
            // 执行任务
            when (taskType) {
                TaskType.PLAY.name -> {
                    // 先停止当前播放
                    val stopIntent = Intent("com.multimediaplayer.STOP")
                    context.sendBroadcast(stopIntent)

                    val playIntent = Intent("com.multimediaplayer.PLAY").apply {
                        putExtra("playlist_id", playlistId)
                        putExtra("end_time", endTime)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(playIntent)
                }
                TaskType.STOP.name -> {
                    val stopIntent = Intent("com.multimediaplayer.STOP")
                    context.sendBroadcast(stopIntent)
                }
                TaskType.POWER_OFF.name -> {
                    // 关机（需要系统权限）
                    try {
                        Runtime.getRuntime().exec(arrayOf("su", "-c", "reboot -p"))
                    } catch (e: Exception) {
                        // 尝试普通关机
                        try {
                            Runtime.getRuntime().exec(arrayOf("reboot", "-p"))
                        } catch (e2: Exception) {
                            e2.printStackTrace()
                        }
                    }
                }
                TaskType.SWITCH_SOURCE.name -> {
                    // 切换信号源
                    // TODO: 实现信号源切换
                }
            }
        }
    }
}
