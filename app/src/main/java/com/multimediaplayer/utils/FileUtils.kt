package com.multimediaplayer.utils

import android.content.Context
import android.os.Environment
import android.webkit.MimeTypeMap
import com.multimediaplayer.data.models.MediaType
import java.io.File

object FileUtils {
    private val VIDEO_EXTENSIONS = listOf("mp4", "mkv", "avi", "flv", "ts", "mov", "webm")
    private val AUDIO_EXTENSIONS = listOf("mp3", "wav", "aac", "flac", "ogg", "m4a")
    private val IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg")
    private val PPT_EXTENSIONS = listOf("ppt", "pptx")
    private val PDF_EXTENSIONS = listOf("pdf")

    fun getMediaType(fileName: String): MediaType? {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when {
            VIDEO_EXTENSIONS.contains(extension) -> MediaType.VIDEO
            AUDIO_EXTENSIONS.contains(extension) -> MediaType.AUDIO
            IMAGE_EXTENSIONS.contains(extension) -> MediaType.IMAGE
            PPT_EXTENSIONS.contains(extension) -> MediaType.PPT
            PDF_EXTENSIONS.contains(extension) -> MediaType.PDF
            else -> null
        }
    }

    fun getMimeType(fileName: String): String? {
        val extension = fileName.substringAfterLast('.', "")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    fun getMediaDirectory(context: Context): File {
        val dir = File(context.filesDir, "media")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getThumbnailDirectory(context: Context): File {
        val dir = File(context.filesDir, "thumbnails")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }

    fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
            minutes > 0 -> String.format("%d:%02d", minutes, seconds % 60)
            else -> String.format("0:%02d", seconds)
        }
    }
}
