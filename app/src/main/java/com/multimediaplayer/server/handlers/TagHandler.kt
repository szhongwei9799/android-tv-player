package com.multimediaplayer.server.handlers

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.multimediaplayer.data.database.AppDatabase
import com.multimediaplayer.data.models.MediaTagCrossRef
import com.multimediaplayer.data.models.Tag
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking

class TagHandler(
    private val database: AppDatabase
) {
    private val gson = Gson()
    
    fun getTagList(): NanoHTTPD.Response {
        val tags = runBlocking { database.tagDao().getAllTags() }
        
        // 为每个标签添加媒体数量
        val tagsWithCount = runBlocking {
            tags.map { tag ->
                val count = database.tagDao().getTagMediaCount(tag.id)
                TagWithCount(tag.id, tag.name, tag.color, count)
            }
        }
        
        return successResponse(tagsWithCount)
    }
    
    fun getMediaTags(mediaId: Long): NanoHTTPD.Response {
        val tags = runBlocking { database.tagDao().getTagsForMedia(mediaId) }
        return successResponse(tags)
    }
    
    fun createTag(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val body = parseBody(session)
        val tag = try {
            gson.fromJson(body, Tag::class.java)
        } catch (e: Exception) {
            return errorResponse("Invalid tag data")
        }
        
        // 检查标签名是否已存在
        val existingTag = runBlocking { database.tagDao().getTagByName(tag.name) }
        if (existingTag != null) {
            return errorResponse("Tag name already exists")
        }
        
        val id = runBlocking { database.tagDao().insertTag(tag) }
        return successResponse(mapOf("id" to id))
    }
    
    fun updateTag(id: Long, session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val body = parseBody(session)
        val tagUpdate = try {
            gson.fromJson(body, Tag::class.java)
        } catch (e: Exception) {
            return errorResponse("Invalid tag data")
        }
        
        val existingTag = runBlocking { database.tagDao().getTagById(id) }
            ?: return errorResponse("Tag not found", NanoHTTPD.Response.Status.NOT_FOUND)
        
        // 检查标签名是否已存在
        if (tagUpdate.name != existingTag.name) {
            val duplicateTag = runBlocking { database.tagDao().getTagByName(tagUpdate.name) }
            if (duplicateTag != null) {
                return errorResponse("Tag name already exists")
            }
        }
        
        val updatedTag = existingTag.copy(
            name = tagUpdate.name,
            color = tagUpdate.color
        )
        
        runBlocking { database.tagDao().updateTag(updatedTag) }
        return successResponse(updatedTag)
    }
    
    fun deleteTag(id: Long): NanoHTTPD.Response {
        val tag = runBlocking { database.tagDao().getTagById(id) }
            ?: return errorResponse("Tag not found", NanoHTTPD.Response.Status.NOT_FOUND)
        
        // 不允许删除"未分类"标签
        if (tag.name == "未分类") {
            return errorResponse("Cannot delete default tag")
        }
        
        runBlocking {
            // 删除标签，但保留媒体
            database.tagDao().deleteTag(tag)
        }
        
        return successResponse()
    }
    
    fun addTagToMedia(mediaId: Long, session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val body = parseBody(session)
        val data = try {
            gson.fromJson(body, JsonObject::class.java)
        } catch (e: Exception) {
            return errorResponse("Invalid request data")
        }
        
        val tagId = data.get("tagId")?.asLong
            ?: return errorResponse("tagId is required")
        
        val media = runBlocking { database.mediaDao().getMediaById(mediaId) }
            ?: return errorResponse("Media not found", NanoHTTPD.Response.Status.NOT_FOUND)
        
        val tag = runBlocking { database.tagDao().getTagById(tagId) }
            ?: return errorResponse("Tag not found", NanoHTTPD.Response.Status.NOT_FOUND)
        
        runBlocking {
            database.tagDao().insertMediaTagCrossRef(MediaTagCrossRef(mediaId, tagId))
        }
        
        return successResponse()
    }
    
    fun removeTagFromMedia(mediaId: Long, tagId: Long): NanoHTTPD.Response {
        runBlocking {
            database.tagDao().deleteMediaTagCrossRef(MediaTagCrossRef(mediaId, tagId))
        }
        return successResponse()
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
    
    data class TagWithCount(
        val id: Long,
        val name: String,
        val color: String,
        val mediaCount: Int
    )
}
