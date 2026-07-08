package com.multimediaplayer.player.datasource

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import jcifs.context.BaseContext
import jcifs.smb.SmbFile
import java.io.InputStream

class SmbDataSource : BaseDataSource(false) {

    private var smbFile: SmbFile? = null
    private var inputStream: InputStream? = null
    private var currentUri: Uri? = null

    @Throws(java.io.IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        currentUri = dataSpec.uri
        val uriString = dataSpec.uri.toString()
        transferInitializing(dataSpec)
        smbFile = SmbFile(uriString, BaseContext())
        inputStream = smbFile!!.inputStream
        val length = smbFile!!.length()
        transferStarted(dataSpec)
        return if (length > 0) length else C.LENGTH_UNSET
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
        try { smbFile?.close() } catch (_: Exception) {}
        inputStream = null
        smbFile = null
    }

    class Factory : DataSource.Factory {
        override fun createDataSource() = SmbDataSource()
    }
}
