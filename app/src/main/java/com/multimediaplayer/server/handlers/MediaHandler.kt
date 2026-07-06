package com.multimediaplayer.server.handlers

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.multimediaplayer.data.database.AppDatabase
import com.multimediaplayer.data.models.Media
import com.multimediaplayer.data.models.MediaSource
import com.multimediaplayer.data.models.MediaTagCrossRef
import com.multimediaplayer.data.models.MediaType
import com.multimediaplayer.data.models.Tag
import com.multimediaplayer.utils.FileUtils
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File

class MediaHandler(
    private val database: AppDatabase,
    private val context: Context
) {
    private val gson = Gson()
    
    fun getMediaList(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val queryParams = session.parms
        val type = queryParams["type"]
        val query = queryParams["query"]
        val tagId = queryParams["tagId"]
        
        val result = runBlocking {
            var mediaList = database.mediaDao().getAllMedia().first()
            
            if (!type.isNullOrBlank()) {
                val mediaType = try {
                    MediaType.valueOf(type.uppercase())
                } catch (e: Exception) {
                    null
                }
                if (mediaType != null) {
                    mediaList = mediaList.filter { it.type == mediaType }
                }
            }
            
            if (!tagId.isNullOrBlank()) {
                val id = tagId.toLongOrNull()
                if (id != null) {
                    val taggedIds = database.tagDao().getMediaByTagId(id).first().map { it.id }.toSet()
                    mediaList = mediaList.filter { it.id in taggedIds }
                }
            }
            
            if (!query.isNullOrBlank()) {
                val q = query.lowercase()
                mediaList = mediaList.filter { it.name.lowercase().contains(q) }
            }
            
            mediaList
        }
        
        return successResponse(result)
    }
    
    fun getMedia(id: Long): NanoHTTPD.Response {
        val media = runBlocking { database.mediaDao().getMediaById(id) }
            ?: return errorResponse("Media not found", NanoHTTPD.Response.Status.NOT_FOUND)
        return successResponse(media)
    }
    
    fun addMedia(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val body = parseBody(session)
        val media = try {
            gson.fromJson(body, Media::class.java)
        } catch (e: Exception) {
            return errorResponse("Invalid media data")
        }
        
        val id = runBlocking {
            database.mediaDao().insertMedia(media)
        }
        
        assignUnclassifiedTag(id)
        
        return successResponse(mapOf("id" to id))
    }
    
    fun updateMedia(id: Long, session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val body = parseBody(session)
        val mediaUpdate = try {
            gson.fromJson(body, Media::class.java)
        } catch (e: Exception) {
            return errorResponse("Invalid media data")
        }
        
        val existingMedia = runBlocking { database.mediaDao().getMediaById(id) }
            ?: return errorResponse("Media not found", NanoHTTPD.Response.Status.NOT_FOUND)
        
        val updatedMedia = existingMedia.copy(
            name = mediaUpdate.name,
            updatedAt = System.currentTimeMillis()
        )
        
        runBlocking { database.mediaDao().updateMedia(updatedMedia) }
        return successResponse(updatedMedia)
    }
    
    fun deleteMedia(id: Long): NanoHTTPD.Response {
        val media = runBlocking { database.mediaDao().getMediaById(id) }
            ?: return errorResponse("Media not found", NanoHTTPD.Response.Status.NOT_FOUND)
        
        runBlocking {
            database.mediaDao().deleteMedia(media)
            database.tagDao().deleteAllMediaTags(id)
        }
        
        // 删除文件
        if (media.source == MediaSource.LOCAL) {
            try {
                File(media.path).delete()
            } catch (e: Exception) {
                // 忽略文件删除错误
            }
        }
        
        return successResponse()
    }
    
    fun uploadMedia(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        return try {
            val contentType = session.headers["content-type"] ?: ""
            val fileName: String
            val targetFile: File
            
            if (contentType.contains("octet-stream")) {
                // 直接二进制上传 - 从输入流读取，避免parseBody的内存映射限制
                fileName = java.net.URLDecoder.decode(
                    session.headers["x-filename"] ?: "upload", "UTF-8"
                )
                val mediaType = FileUtils.getMediaType(fileName)
                    ?: return errorResponse("Unsupported file type")
                val contentLength = session.headers["content-length"]?.toLongOrNull() ?: 0L
                val inputStream = session.getInputStream()
                val mediaDir = FileUtils.getMediaDirectory(context)
                targetFile = File(mediaDir, "${System.currentTimeMillis()}_$fileName")
                targetFile.outputStream().use { out ->
                    val buffer = ByteArray(8192)
                    var totalBytesRead = 0L
                    while (totalBytesRead < contentLength) {
                        val toRead = minOf(buffer.size.toLong(), contentLength - totalBytesRead).toInt()
                        val bytesRead = inputStream.read(buffer, 0, toRead)
                        if (bytesRead < 0) break
                        out.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                    }
                }
            } else {
                // 传统multipart上传（兼容旧客户端）
                val files = HashMap<String, String>()
                session.parseBody(files)
                val tempFilePath = files["file"]
                    ?: return errorResponse("No file uploaded")
                val tempFile = File(tempFilePath)
                if (!tempFile.exists()) {
                    return errorResponse("File not found")
                }
                fileName = session.headers["x-filename"] ?:
                    session.parms["filename"] ?:
                    tempFile.name
                val mediaType = FileUtils.getMediaType(fileName)
                    ?: return errorResponse("Unsupported file type")
                val mediaDir = FileUtils.getMediaDirectory(context)
                targetFile = File(mediaDir, "${System.currentTimeMillis()}_$fileName")
                tempFile.copyTo(targetFile, overwrite = true)
            }
            
            val media = Media(
                name = fileName,
                type = FileUtils.getMediaType(fileName)!!,
                source = MediaSource.LOCAL,
                path = targetFile.absolutePath,
                fileSize = targetFile.length()
            )
            
            val id = runBlocking {
                database.mediaDao().insertMedia(media)
            }
            
            assignUnclassifiedTag(id)
            
            successResponse(mapOf("id" to id, "name" to fileName))
        } catch (e: Exception) {
            errorResponse("Upload failed: ${e.message}")
        }
    }
    
    fun getMediaAudioOverlay(mediaId: Long): NanoHTTPD.Response {
        // TODO: 实现获取附加音频设置
        return successResponse(null)
    }
    
    fun setMediaAudioOverlay(mediaId: Long, session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        // TODO: 实现设置附加音频
        return successResponse()
    }
    
    private fun assignUnclassifiedTag(mediaId: Long) {
        runBlocking {
            var unclassifiedTag = database.tagDao().getTagByName("未分类")
            if (unclassifiedTag == null) {
                val id = database.tagDao().insertTag(Tag(name = "未分类", color = "#999999"))
                unclassifiedTag = database.tagDao().getTagById(id)
            }
            if (unclassifiedTag != null) {
                database.tagDao().insertMediaTagCrossRef(MediaTagCrossRef(mediaId, unclassifiedTag.id))
            }
        }
    }
    
    private fun parseBody(session: NanoHTTPD.IHTTPSession): String {
        return try {
            val contentLength = session.headers["content-length"]?.toLongOrNull() ?: 0L
            if (contentLength > 0) {
                val inputStream = session.getInputStream()
                val bytes = ByteArray(contentLength.toInt())
                var total = 0
                while (total < contentLength) {
                    val bytesRead = inputStream.read(bytes, total, bytes.size - total)
                    if (bytesRead < 0) break
                    total += bytesRead
                }
                String(bytes, 0, total, Charsets.UTF_8)
            } else {
                ""
            }
        } catch (e: Exception) {
            val body = HashMap<String, String>()
            session.parseBody(body)
            body["postData"] ?: body["content"] ?: ""
        }
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
            "application/json; charset=utf-8",
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
        return NanoHTTPD.newFixedLengthResponse(status, "application/json; charset=utf-8", json.toString())
    }
}
