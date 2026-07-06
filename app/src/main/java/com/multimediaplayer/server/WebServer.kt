package com.multimediaplayer.server

import android.content.Context
import com.multimediaplayer.data.database.AppDatabase
import com.multimediaplayer.data.models.*
import com.multimediaplayer.utils.FileUtils
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

class WebServer(
    private val context: Context,
    port: Int
) : NanoHTTPD(port) {
    
    private val database = AppDatabase.getDatabase(context)
    private val apiRouter = ApiRouter(database, context)
    
    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method
        
        // CORS头
        val corsHeaders = mapOf(
            "Access-Control-Allow-Origin" to "*",
            "Access-Control-Allow-Methods" to "GET, POST, PUT, DELETE, OPTIONS",
            "Access-Control-Allow-Headers" to "Content-Type, Authorization"
        )
        
        // 处理OPTIONS请求
        if (method == Method.OPTIONS) {
            return newFixedLengthResponse(Response.Status.OK, "text/plain", "")
        }
        
        // API路由
        if (uri.startsWith("/api/")) {
            return apiRouter.handleRequest(session)
        }
        
        // 静态资源
        if (uri == "/" || uri == "/index.html") {
            return serveStaticFile("web/index.html", "text/html; charset=utf-8")
        }
        
        if (uri.startsWith("/web/")) {
            val filePath = uri.removePrefix("/web/")
            val mimeType = getMimeType(filePath)
            return serveStaticFile("web/$filePath", mimeType)
        }
        
        // 视频流
        if (uri.startsWith("/video/")) {
            return serveVideoStream(uri)
        }
        
        // 二维码
        if (uri == "/qr") {
            return serveQrCode()
        }
        
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")
    }
    
    private fun serveStaticFile(assetPath: String, contentType: String): Response {
        return try {
            val inputStream = context.assets.open(assetPath)
            if (contentType.startsWith("text/") || contentType.contains("javascript") || contentType.contains("json")) {
                val text = inputStream.bufferedReader(charset = Charsets.UTF_8).readText()
                inputStream.close()
                val response = newFixedLengthResponse(Response.Status.OK, contentType, text)
                addCorsHeaders(response)
                response
            } else {
                val response = newChunkedResponse(Response.Status.OK, contentType, inputStream)
                addCorsHeaders(response)
                response
            }
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain; charset=utf-8", "File not found")
        }
    }
    
    private fun serveVideoStream(uri: String): Response {
        return try {
            val path = uri.removePrefix("/video/")
            val file = File(path)
            
            if (!file.exists()) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found")
            }
            
            val inputStream = FileInputStream(file)
            val mimeType = FileUtils.getMimeType(file.name) ?: "video/mp4"
            
            val response = newChunkedResponse(Response.Status.OK, mimeType, inputStream)
            addCorsHeaders(response)
            response
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error serving video")
        }
    }
    
    private fun serveQrCode(): Response {
        return try {
            val qrBitmap = com.multimediaplayer.utils.QrCodeGenerator.generateQrCode(
                com.multimediaplayer.utils.NetworkUtils.getServerUrl(context)
            )
            
            val outputStream = java.io.ByteArrayOutputStream()
            qrBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
            val inputStream = java.io.ByteArrayInputStream(outputStream.toByteArray())
            
            val response = newChunkedResponse(Response.Status.OK, "image/png", inputStream)
            addCorsHeaders(response)
            response
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error generating QR code")
        }
    }
    
    private fun addCorsHeaders(response: Response): Response {
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
        return response
    }
    
    private fun getMimeType(filePath: String): String {
        val extension = filePath.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "html" -> "text/html; charset=utf-8"
            "css" -> "text/css; charset=utf-8"
            "js" -> "application/javascript; charset=utf-8"
            "json" -> "application/json; charset=utf-8"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "svg" -> "image/svg+xml"
            "ico" -> "image/x-icon"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            else -> "application/octet-stream"
        }
    }
}
