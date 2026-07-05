package com.multimediaplayer.server.handlers

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.multimediaplayer.data.database.AppDatabase
import com.multimediaplayer.utils.NetworkUtils
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class SystemHandler(
    private val database: AppDatabase,
    private val context: Context
) {
    private val gson = Gson()
    
    fun getSystemInfo(): NanoHTTPD.Response {
        val info = JsonObject().apply {
            addProperty("deviceName", android.os.Build.DEVICE)
            addProperty("manufacturer", android.os.Build.MANUFACTURER)
            addProperty("model", android.os.Build.MODEL)
            addProperty("sdkVersion", android.os.Build.VERSION.SDK_INT)
            addProperty("ipAddress", NetworkUtils.getDeviceIpAddress(context))
            addProperty("serverPort", 8080)
            
            val mediaCount = runBlocking { database.mediaDao().getMediaCount() }
            addProperty("mediaCount", mediaCount)
            
            val playlistCount = runBlocking { database.playlistDao().getPlaylistCount() }
            addProperty("playlistCount", playlistCount)
            
            val taskCount = runBlocking { database.taskDao().getTaskCount() }
            addProperty("taskCount", taskCount)
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            addProperty("startTime", dateFormat.format(Date()))
        }
        
        return successResponse(info)
    }
    
    fun getQrCode(): NanoHTTPD.Response {
        return try {
            val url = NetworkUtils.getServerUrl(context)
            val qrBitmap = com.multimediaplayer.utils.QrCodeGenerator.generateQrCode(url)
            
            val outputStream = ByteArrayOutputStream()
            qrBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
            val inputStream = ByteArrayInputStream(outputStream.toByteArray())
            
            val response = NanoHTTPD.newChunkedResponse(
                NanoHTTPD.Response.Status.OK,
                "image/png",
                inputStream
            )
            
            response
        } catch (e: Exception) {
            errorResponse("Error generating QR code")
        }
    }
    
    fun getConfig(): NanoHTTPD.Response {
        val config = runBlocking { database.taskDao().getDisplaySettings() }
        return successResponse(config)
    }
    
    fun updateConfig(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val body = parseBody(session)
        val data = try {
            gson.fromJson(body, JsonObject::class.java)
        } catch (e: Exception) {
            return errorResponse("Invalid config data")
        }
        
        val currentConfig = runBlocking { database.taskDao().getDisplaySettings() }
            ?: com.multimediaplayer.data.models.DisplaySettings()
        
        val updatedConfig = currentConfig.copy(
            imageInterval = data.get("imageInterval")?.asInt ?: currentConfig.imageInterval,
            pptInterval = data.get("pptInterval")?.asInt ?: currentConfig.pptInterval,
            pdfInterval = data.get("pdfInterval")?.asInt ?: currentConfig.pdfInterval,
            transitionDuration = data.get("transitionDuration")?.asInt ?: currentConfig.transitionDuration
        )
        
        runBlocking { database.taskDao().updateDisplaySettings(updatedConfig) }
        return successResponse(updatedConfig)
    }
    
    fun getCurrentDisplay(): NanoHTTPD.Response {
        // TODO: 返回当前播放状态
        val state = JsonObject().apply {
            addProperty("isPlaying", false)
            addProperty("currentMediaId", null)
            addProperty("playlistId", null)
        }
        return successResponse(state)
    }
    
    fun controlDisplay(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val body = parseBody(session)
        val data = try {
            gson.fromJson(body, JsonObject::class.java)
        } catch (e: Exception) {
            return errorResponse("Invalid control data")
        }
        
        val action = data.get("action")?.asString
            ?: return errorResponse("action is required")
        
        // TODO: 实现播放控制
        return successResponse(mapOf("action" to action, "status" to "accepted"))
    }
    
    private fun parseBody(session: NanoHTTPD.IHTTPSession): String {
        val body = HashMap<String, String>()
        session.parseBody(body)
        return body["postData"] ?: body["content"] ?: ""
    }
    
    private fun successResponse(data: Any? = null): NanoHTTPD.Response {
        val json = JsonObject().apply {
            addProperty("success", true)
            if (data != null) {
                add("data", gson.toJsonTree(data))
            }
        }
        return NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK,
            "application/json",
            json.toString()
        )
    }
    
    private fun errorResponse(
        message: String,
        status: NanoHTTPD.Response.Status = NanoHTTPD.Response.Status.BAD_REQUEST
    ): NanoHTTPD.Response {
        val json = JsonObject().apply {
            addProperty("error", message)
        }
        return NanoHTTPD.newFixedLengthResponse(status, "application/json", json.toString())
    }
}
