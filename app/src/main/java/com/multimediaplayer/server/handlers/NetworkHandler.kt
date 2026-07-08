package com.multimediaplayer.server.handlers

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.multimediaplayer.data.database.AppDatabase
import com.multimediaplayer.data.models.Media
import com.multimediaplayer.data.models.MediaSource
import com.multimediaplayer.data.models.MediaTagCrossRef
import com.multimediaplayer.data.models.MediaType
import com.multimediaplayer.utils.FileUtils
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

class NetworkHandler(
    private val database: AppDatabase,
    private val context: Context
) {
    private val gson = Gson()

    fun browseSmb(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val path = session.parms["path"] ?: "smb://"
        val username = session.parms["username"] ?: ""
        val password = session.parms["password"] ?: ""

        return try {
            val smbUrl = if (username.isNotBlank()) {
                "smb://${username}:${password}@${path.removePrefix("smb://")}"
            } else path

            val files = mutableListOf<Map<String, Any>>()
            val smbFile = jcifs.smb.SmbFile(smbUrl, jcifs.context.BaseContext())
            for (f in smbFile.listFiles()) {
                files.add(mapOf(
                    "name" to f.name,
                    "path" to f.path,
                    "isDirectory" to f.isDirectory,
                    "length" to f.length(),
                    "lastModified" to f.lastModified()
                ))
            }

            successResponse(files.sortedWith(
                compareByDescending<Map<String, Any>> { it["isDirectory"] as Boolean }
                    .thenBy { it["name"] as String }
            ))
        } catch (e: Exception) {
            errorResponse("Failed to browse SMB: ${e.message}")
        }
    }

    fun browseFtp(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val path = session.parms["path"] ?: "/"
        val host = session.parms["host"] ?: return errorResponse("host is required")
        val port = session.parms["port"]?.toIntOrNull() ?: 21
        val username = session.parms["username"] ?: "anonymous"
        val password = session.parms["password"] ?: "anonymous@"

        val client = org.apache.commons.net.ftp.FTPClient()
        return try {
            client.connect(host, port)
            client.login(username, password)
            client.enterLocalPassiveMode()

            val files = mutableListOf<Map<String, Any>>()
            val ftpFiles = client.listFiles(path)
            for (f in ftpFiles) {
                files.add(mapOf(
                    "name" to f.name,
                    "path" to "${path.trimEnd('/')}/${f.name}",
                    "isDirectory" to f.isDirectory,
                    "length" to f.size
                ))
            }

            client.logout()
            successResponse(files.sortedWith(
                compareByDescending<Map<String, Any>> { it["isDirectory"] as Boolean }
                    .thenBy { it["name"] as String }
            ))
        } catch (e: Exception) {
            errorResponse("Failed to browse FTP: ${e.message}")
        } finally {
            try { client.disconnect() } catch (_: Exception) {}
        }
    }

    fun browseWebdav(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val url = session.parms["url"] ?: return errorResponse("url is required")
        val username = session.parms["username"] ?: ""
        val password = session.parms["password"] ?: ""

        return try {
            val body = """<?xml version="1.0" encoding="utf-8"?>
<propfind xmlns="DAV:"><prop>
<displayname/>
<resourcetype/>
<getcontentlength/>
</prop></propfind>""".trimIndent()

            val httpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val requestBuilder = Request.Builder()
                .url(url)
                .method("PROPFIND", body.toRequestBody("application/xml; charset=utf-8".toMediaTypeOrNull()))
                .header("Depth", "1")

            if (username.isNotBlank()) {
                requestBuilder.header("Authorization", Credentials.basic(username, password))
            }

            val response = httpClient.newCall(requestBuilder.build()).execute()
            val xml = response.body?.string() ?: ""
            val files = parseWebdavResponse(xml, url)

            successResponse(files)
        } catch (e: Exception) {
            errorResponse("Failed to browse WebDAV: ${e.message}")
        }
    }

    private fun parseWebdavResponse(xml: String, baseUrl: String): List<Map<String, Any>> {
        val files = mutableListOf<Map<String, Any>>()
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc: Document = builder.parse(ByteArrayInputStream(xml.toByteArray()))
            val responses = doc.getElementsByTagNameNS("DAV:", "response")

            for (i in 0 until responses.length) {
                val resp = responses.item(i)
                val href = getChildTextContent(resp, "DAV:href") ?: continue
                if (href.isEmpty()) continue

                val hrefStr = href.trimEnd('/')
                val name = hrefStr.substringAfterLast("/")
                if (name.isEmpty() || href == baseUrl.trimEnd('/') + "/") continue

                val isCollection = hasChildWithTag(resp, "DAV:propstat", "DAV:prop", "DAV:resourcetype", "DAV:collection")
                val contentLength = getChildTextContentDeep(resp, "DAV:propstat", "DAV:prop", "DAV:getcontentlength")?.toLongOrNull() ?: 0L

                val fullUrl = if (href.startsWith("http")) href else {
                    "${baseUrl.trimEnd('/')}/${name}"
                }

                files.add(mapOf(
                    "name" to name,
                    "path" to fullUrl,
                    "isDirectory" to isCollection,
                    "length" to contentLength
                ))
            }
        } catch (_: Exception) {}
        return files
    }

    private fun getChildTextContent(parent: org.w3c.dom.Node, tagName: String): String? {
        val children = parent.childNodes
        for (j in 0 until children.length) {
            val child = children.item(j)
            if (child.nodeName == tagName || child.localName == tagName.substringAfter(":")) {
                return getNodeText(child)
            }
        }
        return null
    }

    private fun getChildTextContentDeep(parent: org.w3c.dom.Node, vararg tags: String): String? {
        var current = parent
        for (tag in tags) {
            val children = current.childNodes
            var found: org.w3c.dom.Node? = null
            for (j in 0 until children.length) {
                val child = children.item(j)
                if (child.nodeName == tag || child.localName == tag.substringAfter(":")) {
                    found = child
                    break
                }
            }
            if (found == null) return null
            current = found
        }
        return getNodeText(current)
    }

    private fun hasChildWithTag(parent: org.w3c.dom.Node, vararg tags: String): Boolean {
        var current = parent
        for (tag in tags) {
            val children = current.childNodes
            var found = false
            for (j in 0 until children.length) {
                val child = children.item(j)
                if (child.nodeName == tag || child.localName == tag.substringAfter(":")) {
                    current = child
                    found = true
                    break
                }
            }
            if (!found) return false
        }
        return true
    }

    private fun getNodeText(node: org.w3c.dom.Node): String {
        val childNodes = node.childNodes
        val sb = StringBuilder()
        for (i in 0 until childNodes.length) {
            val child = childNodes.item(i)
            if (child.nodeType == org.w3c.dom.Node.TEXT_NODE || child.nodeType == org.w3c.dom.Node.CDATA_SECTION_NODE) {
                sb.append(child.nodeValue ?: "")
            }
        }
        return sb.toString()
    }

    fun importNetworkFile(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val body = parseBody(session)
        val data = try {
            gson.fromJson(body, JsonObject::class.java)
        } catch (e: Exception) {
            return errorResponse("Invalid request data")
        }

        val url = data.get("url")?.asString ?: return errorResponse("url is required")
        val name = data.get("name")?.asString ?: url.substringAfterLast("/").substringBefore("?")
        val typeStr = data.get("type")?.asString

        val mediaType = if (typeStr != null) {
            try { MediaType.valueOf(typeStr) } catch (_: Exception) { FileUtils.getMediaType(name) ?: MediaType.VIDEO }
        } else {
            FileUtils.getMediaType(name) ?: MediaType.VIDEO
        }

        val media = Media(
            name = name,
            type = mediaType,
            source = MediaSource.NETWORK,
            path = url,
            fileSize = data.get("fileSize")?.asLong ?: 0
        )

        val id = runBlocking { database.mediaDao().insertMedia(media) }

        if (data.has("tagIds")) {
            val tagIds = data.getAsJsonArray("tagIds")?.map { it.asLong } ?: emptyList()
            for (tagId in tagIds) {
                runBlocking {
                    database.tagDao().insertMediaTagCrossRef(MediaTagCrossRef(id, tagId))
                }
            }
        }

        return successResponse(mapOf("id" to id))
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
            if (data != null) add("data", gson.toJsonTree(data))
        }
        return NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK, "application/json; charset=utf-8", json.toString()
        )
    }

    private fun errorResponse(message: String, status: NanoHTTPD.Response.Status = NanoHTTPD.Response.Status.BAD_REQUEST): NanoHTTPD.Response {
        return NanoHTTPD.newFixedLengthResponse(status, "application/json; charset=utf-8",
            JsonObject().apply { addProperty("error", message) }.toString())
    }
}
