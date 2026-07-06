package com.multimediaplayer.server.handlers

import android.content.Context
import android.net.Uri
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
            when {
                !type.isNullOrBlank() -> {
                    val mediaType = try {
                        MediaType.valueOf(type.uppercase())
                    } catch (e: Exception) {
                        null
                    }
                    if (mediaType != null) {
                        database.mediaDao().getMediaByType(mediaType).first()
                    } else {
                        database.mediaDao().getAllMedia().first()
                    }
                }
                !query.isNullOrBlank() -> {
                    database.mediaDao().searchMedia(query).first()
                }
                !tagId.isNullOrBlank() -> {
                    val id = tagId.toLongOrNull()
                    if (id != null) {
                        database.tagDao().getMediaByTagId(id).first()
                    } else {
                        database.mediaDao().getAllMedia().first()
                    }
                }
                else -> {
                    database.mediaDao().getAllMedia().first()
                }
            }
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
            val files = HashMap<String, String>()
            session.parseBody(files)
            
            val tempFilePath = files["file"] ?: files["content"] ?: files["postData"]
                ?: return errorResponse("No file uploaded")
            
            val tempFile = File(tempFilePath)
            if (!tempFile.exists()) {
                return errorResponse("File not found")
            }
            
            // 获取原始文件名
            val fileName = session.headers["x-filename"] ?: 
                session.parms["filename"] ?: 
                tempFile.name
            
            // 确定媒体类型
            val mediaType = FileUtils.getMediaType(fileName)
                ?: return errorResponse("Unsupported file type")
            
            // 移动文件到媒体目录
            val mediaDir = FileUtils.getMediaDirectory(context)
            val targetFile = File(mediaDir, "${System.currentTimeMillis()}_$fileName")
            tempFile.copyTo(targetFile, overwrite = true)
            
            // 创建媒体记录
            val media = Media(
                name = fileName,
                type = mediaType,
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
