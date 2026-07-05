package com.multimediaplayer.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.multimediaplayer.server.WebService

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            // 启动Web服务
            val serviceIntent = Intent(context, WebService::class.java)
            context.startForegroundService(serviceIntent)
            
            // 重新调度所有任务
            val scheduler = TaskScheduler(context)
            scheduler.scheduleAllTasks()
        }
    }
}
