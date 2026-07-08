package com.multimediaplayer.server.handlers

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.multimediaplayer.data.database.AppDatabase
import com.multimediaplayer.data.models.ScheduledTask
import com.multimediaplayer.data.models.TaskType
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class TaskHandler(
    private val database: AppDatabase
) {
    private val gson = Gson()
    
    fun getTaskList(): NanoHTTPD.Response {
        val tasks = runBlocking { database.taskDao().getAllTasks().first() }
        return successResponse(tasks)
    }
    
    fun createTask(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val body = parseBody(session)
        val data = try {
            gson.fromJson(body, JsonObject::class.java)
        } catch (e: Exception) {
            return errorResponse("Invalid task data")
        }
        
        val name = data.get("name")?.asString
            ?: return errorResponse("name is required")
        
        val type = try {
            TaskType.valueOf(data.get("type")?.asString ?: "PLAY")
        } catch (e: Exception) {
            return errorResponse("Invalid task type")
        }
        
        val task = ScheduledTask(
            name = name,
            type = type,
            playlistId = data.get("playlistId")?.asLong,
            cronExpression = data.get("cronExpression")?.asString,
            timeOfDay = data.get("timeOfDay")?.asString,
            daysOfWeek = data.get("daysOfWeek")?.asString,
            startDate = data.get("startDate")?.asLong,
            endDate = data.get("endDate")?.asLong,
            isEnabled = data.get("isEnabled")?.asBoolean ?: true
        )
        
        val id = runBlocking { database.taskDao().insertTask(task) }
        return successResponse(mapOf("id" to id))
    }
    
    fun updateTask(id: Long, session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val body = parseBody(session)
        val data = try {
            gson.fromJson(body, JsonObject::class.java)
        } catch (e: Exception) {
            return errorResponse("Invalid task data")
        }
        
        val existingTask = runBlocking { database.taskDao().getTaskById(id) }
            ?: return errorResponse("Task not found", NanoHTTPD.Response.Status.NOT_FOUND)
        
        val updatedTask = existingTask.copy(
            name = data.get("name")?.asString ?: existingTask.name,
            type = try {
                TaskType.valueOf(data.get("type")?.asString ?: existingTask.type.name)
            } catch (e: Exception) {
                existingTask.type
            },
            playlistId = data.get("playlistId")?.asLong ?: existingTask.playlistId,
            cronExpression = data.get("cronExpression")?.asString ?: existingTask.cronExpression,
            timeOfDay = data.get("timeOfDay")?.asString ?: existingTask.timeOfDay,
            daysOfWeek = data.get("daysOfWeek")?.asString ?: existingTask.daysOfWeek,
            startDate = data.get("startDate")?.asLong ?: existingTask.startDate,
            endDate = data.get("endDate")?.asLong ?: existingTask.endDate,
            isEnabled = data.get("isEnabled")?.asBoolean ?: existingTask.isEnabled
        )
        
        runBlocking { database.taskDao().updateTask(updatedTask) }
        return successResponse(updatedTask)
    }
    
    fun deleteTask(id: Long): NanoHTTPD.Response {
        val task = runBlocking { database.taskDao().getTaskById(id) }
            ?: return errorResponse("Task not found", NanoHTTPD.Response.Status.NOT_FOUND)
        
        runBlocking { database.taskDao().deleteTask(task) }
        return successResponse()
    }
    
    fun toggleTask(id: Long): NanoHTTPD.Response {
        val task = runBlocking { database.taskDao().getTaskById(id) }
            ?: return errorResponse("Task not found", NanoHTTPD.Response.Status.NOT_FOUND)
        
        runBlocking { database.taskDao().setTaskEnabled(id, !task.isEnabled) }
        return successResponse(mapOf("isEnabled" to !task.isEnabled))
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
}
