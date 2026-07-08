package com.multimediaplayer.player.datasource

import android.content.Context
import android.net.Uri
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.rtmp.RtmpDataSource

class ProtocolDataSourceFactory(private val context: Context) : DataSource.Factory {

    private val defaultFactory = DefaultDataSource.Factory(context)
    private val smbFactory = SmbDataSource.Factory()
    private val ftpFactory = FtpDataSource.Factory()
    private val rtmpFactory = RtmpDataSource.Factory()

    override fun createDataSource(): DataSource {
        return ProtocolSelectorDataSource(
            defaultFactory.createDataSource(),
            smbFactory.createDataSource(),
            ftpFactory.createDataSource(),
            rtmpFactory.createDataSource()
        )
    }

    private class ProtocolSelectorDataSource(
        private val defaultSource: DataSource,
        private val smbSource: DataSource,
        private val ftpSource: DataSource,
        private val rtmpSource: DataSource
    ) : DataSource {

        private var activeSource: DataSource? = null

        override fun addTransferListener(listener: TransferListener) {
            defaultSource.addTransferListener(listener)
            smbSource.addTransferListener(listener)
            ftpSource.addTransferListener(listener)
            rtmpSource.addTransferListener(listener)
        }

        override fun open(dataSpec: DataSpec): Long {
            activeSource = selectSource(dataSpec.uri.scheme)
            return activeSource!!.open(dataSpec)
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            return activeSource?.read(buffer, offset, length) ?: -1
        }

        override fun getUri(): Uri? = activeSource?.uri

        override fun close() {
            activeSource?.close()
            activeSource = null
        }

        private fun selectSource(scheme: String?): DataSource {
            return when (scheme?.lowercase()) {
                "smb" -> smbSource
                "ftp", "ftps" -> ftpSource
                "rtmp", "rtmps" -> rtmpSource
                else -> defaultSource
            }
        }
    }
}
