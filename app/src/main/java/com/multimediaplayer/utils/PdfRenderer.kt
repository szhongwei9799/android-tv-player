package com.multimediaplayer.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

object PdfRendererHelper {
    
    data class PdfPageInfo(
        val pageIndex: Int,
        val width: Int,
        val height: Int
    )
    
    fun getPdfPageCount(context: Context, pdfPath: String): Int {
        return try {
            val file = if (pdfPath.startsWith("content://")) {
                File(context.cacheDir, "temp.pdf")
            } else {
                File(pdfPath)
            }
            
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(descriptor)
            val count = renderer.pageCount
            renderer.close()
            descriptor.close()
            count
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
    
    fun renderPdfPage(
        context: Context,
        pdfPath: String,
        pageIndex: Int,
        maxWidth: Int = 1920,
        maxHeight: Int = 1080
    ): Bitmap? {
        return try {
            val file = if (pdfPath.startsWith("content://")) {
                File(context.cacheDir, "temp.pdf")
            } else {
                File(pdfPath)
            }
            
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(descriptor)
            
            if (pageIndex >= renderer.pageCount) {
                renderer.close()
                descriptor.close()
                return null
            }
            
            val page = renderer.openPage(pageIndex)
            
            // 计算缩放比例
            val scale = minOf(
                maxWidth.toFloat() / page.width,
                maxHeight.toFloat() / page.height,
                2.0f
            )
            
            val width = (page.width * scale).toInt()
            val height = (page.height * scale).toInt()
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            
            page.close()
            renderer.close()
            descriptor.close()
            
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
