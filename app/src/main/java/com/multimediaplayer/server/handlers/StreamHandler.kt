package com.multimediaplayer.server.handlers

import android.content.Context
import com.multimediaplayer.data.database.AppDatabase
import com.multimediaplayer.data.models.MediaSource
import com.multimediaplayer.utils.FileUtils
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileInputStream

class StreamHandler(
    private val database: AppDatabase,
    private val context: Context
) {
    
    fun streamMedia(mediaId: Long): NanoHTTPD.Response {
        val media = runBlocking { database.mediaDao().getMediaById(mediaId) }
            ?: return errorResponse("Media not found", NanoHTTPD.Response.Status.NOT_FOUND)
        
        return when (media.source) {
            MediaSource.LOCAL -> streamLocalFile(media.path)
            MediaSource.NETWORK -> streamNetworkUrl(media.path)
        }
    }
    
    private fun streamLocalFile(path: String): NanoHTTPD.Response {
        return try {
            val file = File(path)
            if (!file.exists()) {
                return errorResponse("File not found", NanoHTTPD.Response.Status.NOT_FOUND)
            }
            
            val mimeType = FileUtils.getMimeType(file.name) ?: "application/octet-stream"
            val inputStream = FileInputStream(file)
            
            val response = NanoHTTPD.newChunkedResponse(
                NanoHTTPD.Response.Status.OK,
                mimeType,
                inputStream
            )
            
            // 添加Range支持
            response.addHeader("Accept-Ranges", "bytes")
            response.addHeader("Content-Disposition", "inline; filename=\"${file.name}\"")
            
            response
        } catch (e: Exception) {
            errorResponse("Error streaming file: ${e.message}")
        }
    }
    
    private fun streamNetworkUrl(url: String): NanoHTTPD.Response {
        return try {
            val javaUrl = java.net.URL(url)
            val connection = javaUrl.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 30000
            
            val inputStream = connection.inputStream
            val contentType = connection.contentType ?: "application/octet-stream"
            
            val response = NanoHTTPD.newChunkedResponse(
                NanoHTTPD.Response.Status.OK,
                contentType,
                inputStream
            )
            
            response.addHeader("Accept-Ranges", "bytes")
            
            response
        } catch (e: Exception) {
            errorResponse("Error streaming URL: ${e.message}")
        }
    }
    
    private fun errorResponse(
        message: String,
        status: NanoHTTPD.Response.Status = NanoHTTPD.Response.Status.BAD_REQUEST
    ): NanoHTTPD.Response {
        return NanoHTTPD.newFixedLengthResponse(status, "text/plain", message)
    }
}
