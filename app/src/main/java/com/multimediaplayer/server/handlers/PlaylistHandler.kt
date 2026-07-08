package com.multimediaplayer.server.handlers

import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.multimediaplayer.data.database.AppDatabase
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
        val items = runBlocking { database.playlistDao().getPlaylistItems(playlist.id) }

        val itemsWithTags = runBlocking {
            items.map { item ->
                val tags = database.playlistDao().getPlaylistItemTags(item.id)
                val tagDetails = tags.map { pt ->
                    val tag = database.tagDao().getTagById(pt.tagId)
                    val count = if (tag != null) database.tagDao().getTagMediaCount(pt.tagId) else 0
                    ItemTagResponse(
                        tagId = pt.tagId,
                        name = tag?.name ?: "未知",
                        color = tag?.color ?: "#999999",
                        sortOrder = pt.sortOrder,
                        playMode = pt.playMode,
                        loopCount = pt.loopCount,
                        mediaCount = count
                    )
                }
                ItemResponse(
                    id = item.id,
                    name = item.name,
                    sortOrder = item.sortOrder,
                    tags = tagDetails
                )
            }
        }

        return successResponse(mapOf(
            "playlist" to mapOf(
                "id" to playlist.id,
                "transitionEffect" to playlist.transitionEffect,
                "defaultInterval" to playlist.defaultInterval,
                "itemPlayMode" to playlist.itemPlayMode,
                "itemLoopCount" to playlist.itemLoopCount
            ),
            "items" to itemsWithTags
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
            transitionEffect = try {
                TransitionType.valueOf(data.get("transitionEffect")?.asString ?: playlist.transitionEffect.name)
            } catch (e: Exception) {
                playlist.transitionEffect
            },
            defaultInterval = data.get("defaultInterval")?.asInt ?: playlist.defaultInterval,
            itemPlayMode = try {
                PlayMode.valueOf(data.get("itemPlayMode")?.asString ?: playlist.itemPlayMode.name)
            } catch (e: Exception) {
                playlist.itemPlayMode
            },
            itemLoopCount = data.get("itemLoopCount")?.asInt ?: playlist.itemLoopCount,
            updatedAt = System.currentTimeMillis()
        )

        runBlocking { database.playlistDao().updatePlaylist(updated) }
        AppLogger.i("PlaylistHandler", "Updated playlist settings")
        return successResponse(updated)
    }

    fun createPlaylistItem(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val body = parseBody(session)
        val data = try {
            gson.fromJson(body, JsonObject::class.java)
        } catch (e: Exception) {
            return errorResponse("Invalid data")
        }

        val name = data.get("name")?.asString
            ?: return errorResponse("name is required")

        val playlist = runBlocking { database.playlistDao().ensureDefaultPlaylist() }
        val items = runBlocking { database.playlistDao().getPlaylistItems(playlist.id) }
        val maxOrder = items.maxOfOrNull { it.sortOrder } ?: -1

        val item = PlaylistItem(
            playlistId = playlist.id,
            name = name,
            sortOrder = maxOrder + 1
        )
        val itemId = runBlocking { database.playlistDao().insertPlaylistItem(item) }

        // 添加选中的标签
        val tagIds = data.get("tagIds")?.asJsonArray
        if (tagIds != null) {
            val tags = tagIds.mapNotNull { it?.asLong }
            runBlocking {
                tags.forEachIndexed { idx, tagId ->
                    database.playlistDao().insertPlaylistItemTag(
                        PlaylistItemTag(
                            itemId = itemId,
                            tagId = tagId,
                            sortOrder = idx
                        )
                    )
                }
            }
        }

        AppLogger.i("PlaylistHandler", "Created playlist item #$itemId '$name'")
        return successResponse(mapOf("id" to itemId))
    }

    fun updatePlaylistItem(itemId: Long, session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val body = parseBody(session)
        val data = try {
            gson.fromJson(body, JsonObject::class.java)
        } catch (e: Exception) {
            return errorResponse("Invalid data")
        }

        val existing = runBlocking { database.playlistDao().getPlaylistItemById(itemId) }
            ?: return errorResponse("Item not found", NanoHTTPD.Response.Status.NOT_FOUND)

        val updated = existing.copy(
            name = data.get("name")?.asString ?: existing.name,
            sortOrder = data.get("sortOrder")?.asInt ?: existing.sortOrder
        )

        runBlocking { database.playlistDao().updatePlaylistItem(updated) }
        AppLogger.i("PlaylistHandler", "Updated playlist item #$itemId")
        return successResponse()
    }

    fun deletePlaylistItem(itemId: Long): NanoHTTPD.Response {
        val item = runBlocking { database.playlistDao().getPlaylistItemById(itemId) }
            ?: return errorResponse("Item not found", NanoHTTPD.Response.Status.NOT_FOUND)

        runBlocking {
            database.playlistDao().deleteAllPlaylistItemTags(itemId)
            database.playlistDao().deletePlaylistItem(item)
        }
        AppLogger.i("PlaylistHandler", "Deleted playlist item #$itemId")
        return successResponse()
    }

    fun replaceItemTags(itemId: Long, session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val body = parseBody(session)
        val data = try {
            gson.fromJson(body, JsonObject::class.java)
        } catch (e: Exception) {
            return errorResponse("Invalid data")
        }

        val existing = runBlocking { database.playlistDao().getPlaylistItemById(itemId) }
            ?: return errorResponse("Item not found", NanoHTTPD.Response.Status.NOT_FOUND)

        val tagsArray = data.get("tags")?.asJsonArray
            ?: return errorResponse("tags is required")

        val tags = tagsArray.mapNotNull { elem ->
            val obj = elem?.asJsonObject ?: return@mapNotNull null
            PlaylistItemTag(
                itemId = itemId,
                tagId = obj.get("tagId")?.asLong ?: return@mapNotNull null,
                sortOrder = obj.get("sortOrder")?.asInt ?: 0,
                playMode = try {
                    PlayMode.valueOf(obj.get("playMode")?.asString ?: "SEQUENTIAL")
                } catch (e: Exception) {
                    PlayMode.SEQUENTIAL
                },
                loopCount = obj.get("loopCount")?.asInt ?: -1
            )
        }

        runBlocking { database.playlistDao().replaceItemTags(itemId, tags) }
        AppLogger.i("PlaylistHandler", "Replaced tags for playlist item #$itemId")
        return successResponse()
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

    data class ItemTagResponse(
        val tagId: Long,
        val name: String,
        val color: String,
        val sortOrder: Int,
        val playMode: PlayMode,
        val loopCount: Int,
        val mediaCount: Int
    )

    data class ItemResponse(
        val id: Long,
        val name: String,
        val sortOrder: Int,
        val tags: List<ItemTagResponse>
    )
}