package com.multimediaplayer.player.datasource

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.InputStream
import java.net.URI

class FtpDataSource : BaseDataSource(false) {

    private var ftpClient: FTPClient? = null
    private var inputStream: InputStream? = null
    private var currentUri: Uri? = null

    @Throws(java.io.IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        currentUri = dataSpec.uri
        val uriString = currentUri.toString()
        transferInitializing(dataSpec)

        val parsed = URI(uriString)
        val host = parsed.host
        val port = if (parsed.port > 0) parsed.port else 21
        val userInfo = parsed.userInfo
        val username: String
        val password: String
        if (userInfo != null && userInfo.contains(":")) {
            val parts = userInfo.split(":", limit = 2)
            username = parts[0]
            password = if (parts.size > 1) parts[1] else ""
        } else {
            username = "anonymous"
            password = "anonymous@"
        }
        val path = parsed.path ?: "/"

        val client = FTPClient()
        client.connect(host, port)
        client.login(username, password)
        client.enterLocalPassiveMode()
        client.setFileType(FTP.BINARY_FILE_TYPE)
        ftpClient = client

        inputStream = client.retrieveFileStream(path)
        if (inputStream == null) {
            throw java.io.IOException("Failed to retrieve file: $path")
        }

        transferStarted(dataSpec)
        return C.LENGTH_UNSET.toLong()
    }

    @Throws(java.io.IOException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val read = inputStream?.read(buffer, offset, length) ?: -1
        if (read > 0) bytesTransferred(read)
        return read
    }

    override fun getUri(): Uri? = currentUri

    @Throws(java.io.IOException::class)
    override fun close() {
        try { inputStream?.close() } catch (_: Exception) {}
        try {
            ftpClient?.logout()
            ftpClient?.disconnect()
        } catch (_: Exception) {}
        inputStream = null
        ftpClient = null
    }

    class Factory : DataSource.Factory {
        override fun createDataSource() = FtpDataSource()
    }
}
