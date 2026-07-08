package com.multimediaplayer.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object AppLogger {
    private const val TAG = "MediaPlayer"
    private var logDir: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var logFile: File? = null

    fun init(context: Context) {
        logDir = File(context.filesDir, "logs")
        logDir?.mkdirs()
        rollLogFile()
    }

    private fun rollLogFile() {
        val date = fileDateFormat.format(Date())
        logFile = File(logDir, "app-$date.log")
    }

    private fun writeLog(level: String, tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val line = "[$timestamp][$level][$tag] $message"
        Log.d(TAG, line)
        try {
            val file = logFile
            if (file != null) {
                if (fileDateFormat.format(Date()) != file.name.removePrefix("app-").removeSuffix(".log")) {
                    rollLogFile()
                }
                file.appendText("$line\n")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log file", e)
        }
    }

    fun i(tag: String, message: String) = writeLog("INFO", tag, message)
    fun w(tag: String, message: String) = writeLog("WARN", tag, message)
    fun e(tag: String, message: String, error: Throwable? = null) {
        val msg = if (error != null) "$message: ${error.message}" else message
        writeLog("ERROR", tag, msg)
    }

    fun getLogFiles(): List<File> {
        return logDir?.listFiles()?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
    }

    fun clearOldLogs(daysToKeep: Int = 7) {
        val cutoff = System.currentTimeMillis() - daysToKeep * 24 * 60 * 60 * 1000L
        logDir?.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) file.delete()
        }
    }
}
