package com.multimediaplayer.server.handlers

import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.multimediaplayer.data.database.AppDatabase
import com.multimediaplayer.data.database.TagWithSettings
import com.multimediaplayer.data.models.*
import com.multimediaplayer.utils.AppLogger
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class PlaylistHandler(
    private val database: AppDatabase,
    private val context: Context
) {
    private val gson = Gson()
    
    fun getDefaultPlaylist(): NanoHTTPD.Response {
        val playlist = runBlocking { database.playlistDao().ensureDefaultPlaylist() }
        val tags = runBlocking { database.playlistDao().getPlaylistTags(playlist.id) }
        
        val tagDetails = runBlocking {
            tags.map { pt ->
                val tag = database.tagDao().getTagById(pt.tagId)
                val count = if (tag != null) database.tagDao().getTagMediaCount(pt.tagId) else 0
                PlaylistTagResponse(
                    tagId = pt.tagId,
                    name = tag?.name ?: "未知",
                    color = tag?.color ?: "#999999",
                    sortOrder = pt.sortOrder,
                    playMode = pt.playMode,
                    loopCount = pt.loopCount,
                    mediaCount = count
                )
            }
        }
        
        return successResponse(mapOf(
            "playlist" to playlist,
            "tags" to tagDetails
        ))
    }
    
    fun updatePlaylistSettings(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val body = parseBody(session)
        val data = try {
            gson.fromJson(body, JsonObject::class.java)
        } catch (e: Exception) {
            return errorResponse("Invalid data")
        }
        
        val playlist = runBlocking { database.playlistDao().ensureDefaultPlaylist() }
        
        val updated = playlist.copy(
            name = data.get("name")?.asString ?: playlist.name,
            description = data.get("description")?.asString ?: playlist.description,
            transitionEffect = try {
                TransitionType.valueOf(data.get("transitionEffect")?.asString ?: playlist.transitionEffect.name)
            } catch (e: Exception) {
                playlist.transitionEffect
            },
            defaultInterval = data.get("defaultInterval")?.asInt ?: playlist.defaultInterval,
            tagPlayMode = try {
                PlayMode.valueOf(data.get("tagPlayMode")?.asString ?: playlist.tagPlayMode.name)
            } catch (e: Exception) {
                playlist.tagPlayMode
            },
            tagLoopCount = data.get("tagLoopCount")?.asInt ?: playlist.tagLoopCount,
            updatedAt = System.currentTimeMillis()
        )
        
        runBlocking { database.playlistDao().updatePlaylist(updated) }
        AppLogger.i("PlaylistHandler", "Updated playlist settings")
        return successResponse(updated)
    }
    
    fun addTagToPlaylist(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val body = parseBody(session)
        val data = try {
            gson.fromJson(body, JsonObject::class.java)
        } catch (e: Exception) {
            return errorResponse("Invalid data")
        }
        
        val tagId = data.get("tagId")?.asLong
            ?: return errorResponse("tagId is required")
        
        val playlist = runBlocking { database.playlistDao().ensureDefaultPlaylist() }
        val tags = runBlocking { database.playlistDao().getPlaylistTags(playlist.id) }
        val maxOrder = tags.maxOfOrNull { it.sortOrder } ?: -1
        
        val pt = PlaylistTag(
            playlistId = playlist.id,
            tagId = tagId,
            sortOrder = maxOrder + 1,
            playMode = try {
                PlayMode.valueOf(data.get("playMode")?.asString ?: "SEQUENTIAL")
            } catch (e: Exception) {
                PlayMode.SEQUENTIAL
            },
            loopCount = data.get("loopCount")?.asInt ?: -1
        )
        
        runBlocking { database.playlistDao().insertPlaylistTag(pt) }
        AppLogger.i("PlaylistHandler", "Added tag #$tagId to default playlist")
        return successResponse()
    }
    
    fun updatePlaylistTag(tagId: Long, session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val body = parseBody(session)
        val data = try {
            gson.fromJson(body, JsonObject::class.java)
        } catch (e: Exception) {
            return errorResponse("Invalid data")
        }
        
        val playlist = runBlocking { database.playlistDao().ensureDefaultPlaylist() }
        val tags = runBlocking { database.playlistDao().getPlaylistTags(playlist.id) }
        val existing = tags.find { it.tagId == tagId }
            ?: return errorResponse("Tag not found in playlist", NanoHTTPD.Response.Status.NOT_FOUND)
        
        val updated = existing.copy(
            sortOrder = data.get("sortOrder")?.asInt ?: existing.sortOrder,
            playMode = try {
                PlayMode.valueOf(data.get("playMode")?.asString ?: existing.playMode.name)
            } catch (e: Exception) {
                existing.playMode
            },
            loopCount = data.get("loopCount")?.asInt ?: existing.loopCount
        )
        
        runBlocking {
            database.playlistDao().deletePlaylistTagById(playlist.id, tagId)
            database.playlistDao().insertPlaylistTag(updated)
        }
        AppLogger.i("PlaylistHandler", "Updated tag #$tagId in playlist")
        return successResponse()
    }
    
    fun removeTagFromPlaylist(tagId: Long): NanoHTTPD.Response {
        val playlist = runBlocking { database.playlistDao().ensureDefaultPlaylist() }
        runBlocking { database.playlistDao().deletePlaylistTagById(playlist.id, tagId) }
        AppLogger.i("PlaylistHandler", "Removed tag #$tagId from default playlist")
        return successResponse()
    }
    
    fun getPlaylistMedia(playlistId: Long): NanoHTTPD.Response {
        val tags = runBlocking { database.playlistDao().getPlaylistTags(playlistId) }
        val tagIds = tags.map { it.tagId }
        
        val tagMedia = tags.map { pt ->
            val mediaList = runBlocking {
                database.tagDao().getMediaByTagId(pt.tagId).first()
            }
            mapOf(
                "tagId" to pt.tagId,
                "playMode" to pt.playMode,
                "loopCount" to pt.loopCount,
                "media" to mediaList
            )
        }
        
        return successResponse(tagMedia)
    }
    
    fun playPlaylist(id: Long): NanoHTTPD.Response {
        val playlist = runBlocking {
            if (id == -1L) database.playlistDao().ensureDefaultPlaylist()
            else database.playlistDao().getPlaylistById(id)
                ?: return@runBlocking null
        } ?: return errorResponse("Playlist not found", NanoHTTPD.Response.Status.NOT_FOUND)
        
        try {
            val intent = Intent("com.multimediaplayer.PLAY").apply {
                putExtra("playlist_id", playlist.id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            return errorResponse("Failed to start playback: ${e.message}")
        }
        
        AppLogger.i("PlaylistHandler", "Started playback of playlist #${playlist.id}")
        return successResponse(mapOf(
            "playlistId" to playlist.id,
            "message" to "Playback started"
        ))
    }
    
    fun stopPlaylist(): NanoHTTPD.Response {
        try {
            val intent = Intent(com.multimediaplayer.ui.screens.ACTION_STOP_PLAYBACK)
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            return errorResponse("Failed to stop playback: ${e.message}")
        }
        AppLogger.i("PlaylistHandler", "Stopped playback")
        return successResponse(mapOf("message" to "Playback stopped"))
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
            } else ""
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
    
    data class PlaylistTagResponse(
        val tagId: Long,
        val name: String,
        val color: String,
        val sortOrder: Int,
        val playMode: PlayMode,
        val loopCount: Int,
        val mediaCount: Int
    )
}
