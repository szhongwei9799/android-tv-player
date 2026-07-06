package com.multimediaplayer.server.handlers

import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.multimediaplayer.data.database.AppDatabase
import com.multimediaplayer.data.models.*
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class PlaylistHandler(
    private val database: AppDatabase,
    private val context: Context
) {
    private val gson = Gson()
    
    fun getPlaylistList(): NanoHTTPD.Response {
        val playlists: List<Playlist> = runBlocking { database.playlistDao().getAllPlaylists().first() }
        
        val playlistsWithCount = runBlocking {
            playlists.map { playlist ->
                val count = if (playlist.type == PlaylistType.TAG_BASED) {
                    val tagIds = database.playlistDao().getPlaylistTags(playlist.id).map { it.tagId }
                    if (tagIds.isEmpty()) 0
                    else database.tagDao().getMediaByTagIds(tagIds).first().size
                } else {
                    database.playlistDao().getPlaylistItemCount(playlist.id)
                }
                PlaylistWithCount(
                    playlist.id,
                    playlist.name,
                    playlist.description,
                    playlist.type,
                    playlist.transitionEffect,
                    count
                )
            }
        }
        
        return successResponse(playlistsWithCount)
    }
    
    fun getPlaylist(id: Long): NanoHTTPD.Response {
        val playlist = runBlocking { database.playlistDao().getPlaylistById(id) }
            ?: return errorResponse("Playlist not found", NanoHTTPD.Response.Status.NOT_FOUND)
        return successResponse(playlist)
    }
    
    fun createPlaylist(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val body = parseBody(session)
        val data = try {
            gson.fromJson(body, JsonObject::class.java)
        } catch (e: Exception) {
            return errorResponse("Invalid playlist data")
        }
        
        val name = data.get("name")?.asString
            ?: return errorResponse("name is required")
        
        val playlist = Playlist(
            name = name,
            description = data.get("description")?.asString,
            type = try {
                PlaylistType.valueOf(data.get("type")?.asString ?: "MANUAL")
            } catch (e: Exception) {
                PlaylistType.MANUAL
            },
            sortOrder = try {
                SortOrder.valueOf(data.get("sortOrder")?.asString ?: "MANUAL")
            } catch (e: Exception) {
                SortOrder.MANUAL
            },
            transitionEffect = try {
                TransitionType.valueOf(data.get("transitionEffect")?.asString ?: "FADE")
            } catch (e: Exception) {
                TransitionType.FADE
            },
            defaultInterval = data.get("defaultInterval")?.asInt ?: 10,
            isDefault = data.get("isDefault")?.asBoolean ?: false
        )
        
        val id = runBlocking { database.playlistDao().insertPlaylist(playlist) }
        
        // 添加标签关联（如果是TAG_BASED类型）
        if (playlist.type == PlaylistType.TAG_BASED) {
            val tagIds = data.getAsJsonArray("tagIds")?.map { it.asLong } ?: emptyList()
            runBlocking { database.playlistDao().setPlaylistTags(id, tagIds) }
        }
        
        return successResponse(mapOf("id" to id))
    }
    
    fun updatePlaylist(id: Long, session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val body = parseBody(session)
        val data = try {
            gson.fromJson(body, JsonObject::class.java)
        } catch (e: Exception) {
            return errorResponse("Invalid playlist data")
        }
        
        val existingPlaylist = runBlocking { database.playlistDao().getPlaylistById(id) }
            ?: return errorResponse("Playlist not found", NanoHTTPD.Response.Status.NOT_FOUND)
        
        val updatedPlaylist = existingPlaylist.copy(
            name = data.get("name")?.asString ?: existingPlaylist.name,
            description = data.get("description")?.asString ?: existingPlaylist.description,
            type = try {
                PlaylistType.valueOf(data.get("type")?.asString ?: existingPlaylist.type.name)
            } catch (e: Exception) {
                existingPlaylist.type
            },
            sortOrder = try {
                SortOrder.valueOf(data.get("sortOrder")?.asString ?: existingPlaylist.sortOrder.name)
            } catch (e: Exception) {
                existingPlaylist.sortOrder
            },
            transitionEffect = try {
                TransitionType.valueOf(data.get("transitionEffect")?.asString ?: existingPlaylist.transitionEffect.name)
            } catch (e: Exception) {
                existingPlaylist.transitionEffect
            },
            defaultInterval = data.get("defaultInterval")?.asInt ?: existingPlaylist.defaultInterval,
            isDefault = data.get("isDefault")?.asBoolean ?: existingPlaylist.isDefault,
            updatedAt = System.currentTimeMillis()
        )
        
        runBlocking { database.playlistDao().updatePlaylist(updatedPlaylist) }
        
        // 更新标签关联
        if (data.has("tagIds")) {
            val tagIds = data.getAsJsonArray("tagIds")?.map { it.asLong } ?: emptyList()
            runBlocking { database.playlistDao().setPlaylistTags(id, tagIds) }
        }
        
        return successResponse(updatedPlaylist)
    }
    
    fun deletePlaylist(id: Long): NanoHTTPD.Response {
        val playlist = runBlocking { database.playlistDao().getPlaylistById(id) }
            ?: return errorResponse("Playlist not found", NanoHTTPD.Response.Status.NOT_FOUND)
        
        runBlocking {
            database.playlistDao().deletePlaylist(playlist)
            // 注意：只删除播放列表，不删除媒体
            database.playlistDao().deleteAllPlaylistItems(id)
            database.playlistDao().deleteAllPlaylistTags(id)
        }
        
        return successResponse()
    }
    
    fun getPlaylistItems(playlistId: Long): NanoHTTPD.Response {
        val playlist = runBlocking { database.playlistDao().getPlaylistById(playlistId) }
            ?: return errorResponse("Playlist not found", NanoHTTPD.Response.Status.NOT_FOUND)
        
        val mediaList = runBlocking {
            if (playlist.type == PlaylistType.TAG_BASED) {
                val tagIds = database.playlistDao().getPlaylistTags(playlistId).map { it.tagId }
                if (tagIds.isEmpty()) emptyList()
                else database.tagDao().getMediaByTagIds(tagIds).first()
            } else {
                val items = database.playlistDao().getPlaylistItems(playlistId)
                items.mapNotNull { item ->
                    database.mediaDao().getMediaById(item.mediaId)
                }
            }
        }
        
        return successResponse(mediaList)
    }
    
    fun addItemToPlaylist(playlistId: Long, session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val body = parseBody(session)
        val data = try {
            gson.fromJson(body, JsonObject::class.java)
        } catch (e: Exception) {
            return errorResponse("Invalid request data")
        }
        
        val mediaId = data.get("mediaId")?.asLong
            ?: return errorResponse("mediaId is required")
        
        val playlist = runBlocking { database.playlistDao().getPlaylistById(playlistId) }
            ?: return errorResponse("Playlist not found", NanoHTTPD.Response.Status.NOT_FOUND)
        
        val media = runBlocking { database.mediaDao().getMediaById(mediaId) }
            ?: return errorResponse("Media not found", NanoHTTPD.Response.Status.NOT_FOUND)
        
        val currentItems = runBlocking { database.playlistDao().getPlaylistItems(playlistId) }
        val maxOrder = currentItems.maxOfOrNull { it.sortOrder } ?: 0
        
        val item = PlaylistItem(
            playlistId = playlistId,
            mediaId = mediaId,
            sortOrder = data.get("sortOrder")?.asInt ?: (maxOrder + 1)
        )
        
        runBlocking { database.playlistDao().insertPlaylistItem(item) }
        
        return successResponse()
    }
    
    fun removeItemFromPlaylist(playlistId: Long, mediaId: Long): NanoHTTPD.Response {
        runBlocking {
            database.playlistDao().deletePlaylistItemById(playlistId, mediaId)
        }
        return successResponse()
    }
    
    fun playPlaylist(id: Long): NanoHTTPD.Response {
        val playlist = runBlocking { database.playlistDao().getPlaylistById(id) }
            ?: return errorResponse("Playlist not found", NanoHTTPD.Response.Status.NOT_FOUND)
        
        try {
            val intent = Intent("com.multimediaplayer.PLAY").apply {
                putExtra("playlist_id", playlist.id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            return errorResponse("Failed to start playback: ${e.message}")
        }
        
        return successResponse(mapOf(
            "playlistId" to playlist.id,
            "name" to playlist.name,
            "message" to "Playback started"
        ))
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
    
    data class PlaylistWithCount(
        val id: Long,
        val name: String,
        val description: String?,
        val type: PlaylistType,
        val transitionEffect: TransitionType,
        val mediaCount: Int
    )
}
