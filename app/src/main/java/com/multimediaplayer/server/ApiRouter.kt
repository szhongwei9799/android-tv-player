package com.multimediaplayer.server

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.multimediaplayer.data.database.AppDatabase
import com.multimediaplayer.data.models.*
import com.multimediaplayer.server.handlers.*
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking

class ApiRouter(
    private val database: AppDatabase,
    private val context: Context
) {
    private val gson = Gson()
    private val mediaHandler = MediaHandler(database, context)
    private val tagHandler = TagHandler(database)
    private val playlistHandler = PlaylistHandler(database, context)
    private val taskHandler = TaskHandler(database)
    private val streamHandler = StreamHandler(database, context)
    private val systemHandler = SystemHandler(database, context)
    
    fun handleRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val uri = session.uri
        val method = session.method
        
        // 路由匹配（body 由各 handler 自行解析，NanoHTTPD 的 parseBody 只能调一次）
        return when {
            // 媒体API
            uri == "/api/media" && method == NanoHTTPD.Method.GET -> {
                mediaHandler.getMediaList(session)
            }
            uri == "/api/media" && method == NanoHTTPD.Method.POST -> {
                mediaHandler.addMedia(session)
            }
            uri.matches(Regex("^/api/media/\\d+$")) && method == NanoHTTPD.Method.GET -> {
                val id = uri.split("/").last().toLong()
                mediaHandler.getMedia(id)
            }
            uri.matches(Regex("^/api/media/\\d+$")) && method == NanoHTTPD.Method.PUT -> {
                val id = uri.split("/").last().toLong()
                mediaHandler.updateMedia(id, session)
            }
            uri.matches(Regex("^/api/media/\\d+$")) && method == NanoHTTPD.Method.DELETE -> {
                val id = uri.split("/").last().toLong()
                mediaHandler.deleteMedia(id)
            }
            uri == "/api/media/upload" && method == NanoHTTPD.Method.POST -> {
                mediaHandler.uploadMedia(session)
            }
            uri.matches(Regex("^/api/media/\\d+/stream$")) && method == NanoHTTPD.Method.GET -> {
                val id = uri.split("/")[3].toLong()
                streamHandler.streamMedia(id)
            }
            uri.matches(Regex("^/api/media/\\d+/tags$")) && method == NanoHTTPD.Method.GET -> {
                val id = uri.split("/")[3].toLong()
                tagHandler.getMediaTags(id)
            }
            uri.matches(Regex("^/api/media/\\d+/tags$")) && method == NanoHTTPD.Method.POST -> {
                val id = uri.split("/")[3].toLong()
                tagHandler.addTagToMedia(id, session)
            }
            uri.matches(Regex("^/api/media/\\d+/tags/\\d+$")) && method == NanoHTTPD.Method.DELETE -> {
                val mediaId = uri.split("/")[3].toLong()
                val tagId = uri.split("/")[5].toLong()
                tagHandler.removeTagFromMedia(mediaId, tagId)
            }
            uri.matches(Regex("^/api/media/\\d+/audio$")) && method == NanoHTTPD.Method.GET -> {
                val id = uri.split("/")[3].toLong()
                mediaHandler.getMediaAudioOverlay(id)
            }
            uri.matches(Regex("^/api/media/\\d+/audio$")) && method == NanoHTTPD.Method.POST -> {
                val id = uri.split("/")[3].toLong()
                mediaHandler.setMediaAudioOverlay(id, session)
            }
            
            // 标签API
            uri == "/api/tags" && method == NanoHTTPD.Method.GET -> {
                tagHandler.getTagList()
            }
            uri == "/api/tags" && method == NanoHTTPD.Method.POST -> {
                tagHandler.createTag(session)
            }
            uri.matches(Regex("^/api/tags/\\d+$")) && method == NanoHTTPD.Method.PUT -> {
                val id = uri.split("/").last().toLong()
                tagHandler.updateTag(id, session)
            }
            uri.matches(Regex("^/api/tags/\\d+$")) && method == NanoHTTPD.Method.DELETE -> {
                val id = uri.split("/").last().toLong()
                tagHandler.deleteTag(id)
            }
            
            // 播放列表API
            uri == "/api/playlists" && method == NanoHTTPD.Method.GET -> {
                playlistHandler.getPlaylistList()
            }
            uri == "/api/playlists" && method == NanoHTTPD.Method.POST -> {
                playlistHandler.createPlaylist(session)
            }
            uri.matches(Regex("^/api/playlists/\\d+$")) && method == NanoHTTPD.Method.GET -> {
                val id = uri.split("/").last().toLong()
                playlistHandler.getPlaylist(id)
            }
            uri.matches(Regex("^/api/playlists/\\d+$")) && method == NanoHTTPD.Method.PUT -> {
                val id = uri.split("/").last().toLong()
                playlistHandler.updatePlaylist(id, session)
            }
            uri.matches(Regex("^/api/playlists/\\d+$")) && method == NanoHTTPD.Method.DELETE -> {
                val id = uri.split("/").last().toLong()
                playlistHandler.deletePlaylist(id)
            }
            uri.matches(Regex("^/api/playlists/\\d+/items$")) && method == NanoHTTPD.Method.GET -> {
                val id = uri.split("/")[3].toLong()
                playlistHandler.getPlaylistItems(id)
            }
            uri.matches(Regex("^/api/playlists/\\d+/items$")) && method == NanoHTTPD.Method.POST -> {
                val id = uri.split("/")[3].toLong()
                playlistHandler.addItemToPlaylist(id, session)
            }
            uri.matches(Regex("^/api/playlists/\\d+/items/\\d+$")) && method == NanoHTTPD.Method.DELETE -> {
                val playlistId = uri.split("/")[3].toLong()
                val mediaId = uri.split("/")[5].toLong()
                playlistHandler.removeItemFromPlaylist(playlistId, mediaId)
            }
            uri.matches(Regex("^/api/playlists/\\d+/play$")) && method == NanoHTTPD.Method.POST -> {
                val id = uri.split("/")[3].toLong()
                playlistHandler.playPlaylist(id)
            }
            
            // 定时任务API
            uri == "/api/tasks" && method == NanoHTTPD.Method.GET -> {
                taskHandler.getTaskList()
            }
            uri == "/api/tasks" && method == NanoHTTPD.Method.POST -> {
                taskHandler.createTask(session)
            }
            uri.matches(Regex("^/api/tasks/\\d+$")) && method == NanoHTTPD.Method.PUT -> {
                val id = uri.split("/").last().toLong()
                taskHandler.updateTask(id, session)
            }
            uri.matches(Regex("^/api/tasks/\\d+$")) && method == NanoHTTPD.Method.DELETE -> {
                val id = uri.split("/").last().toLong()
                taskHandler.deleteTask(id)
            }
            uri.matches(Regex("^/api/tasks/\\d+/toggle$")) && method == NanoHTTPD.Method.PUT -> {
                val id = uri.split("/")[3].toLong()
                taskHandler.toggleTask(id)
            }
            
            // 系统API
            uri == "/api/system/info" && method == NanoHTTPD.Method.GET -> {
                systemHandler.getSystemInfo()
            }
            uri == "/api/system/qr" && method == NanoHTTPD.Method.GET -> {
                systemHandler.getQrCode()
            }
            uri == "/api/config" && method == NanoHTTPD.Method.GET -> {
                systemHandler.getConfig()
            }
            uri == "/api/config" && method == NanoHTTPD.Method.PUT -> {
                systemHandler.updateConfig(session)
            }
            uri == "/api/display/current" && method == NanoHTTPD.Method.GET -> {
                systemHandler.getCurrentDisplay()
            }
            uri == "/api/display/control" && method == NanoHTTPD.Method.POST -> {
                systemHandler.controlDisplay(session)
            }
            
            else -> errorResponse("Not Found", NanoHTTPD.Response.Status.NOT_FOUND)
        }
    }
    
    fun errorResponse(
        message: String,
        status: NanoHTTPD.Response.Status = NanoHTTPD.Response.Status.BAD_REQUEST
    ): NanoHTTPD.Response {
        val json = JsonObject().apply {
            addProperty("error", message)
        }
        return NanoHTTPD.newFixedLengthResponse(status, "application/json; charset=utf-8", json.toString())
    }
    
    fun successResponse(data: Any? = null): NanoHTTPD.Response {
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
}
