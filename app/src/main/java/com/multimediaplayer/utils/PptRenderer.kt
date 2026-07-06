package com.multimediaplayer.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import org.apache.poi.hslf.usermodel.HSLFSlide
import org.apache.poi.hslf.usermodel.HSLFSlideShow
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFSlide
import java.io.File
import java.io.FileOutputStream

object PptRendererHelper {
    
    data class PptSlideInfo(
        val slideIndex: Int,
        val width: Int,
        val height: Int
    )
    
    fun getSlideCount(context: Context, pptPath: String): Int {
        return try {
            val file = File(pptPath)
            when {
                pptPath.endsWith(".ppt", ignoreCase = true) -> {
                    val fis = file.inputStream()
                    val slideshow = HSLFSlideShow(fis)
                    val count = slideshow.slides.size
                    fis.close()
                    count
                }
                pptPath.endsWith(".pptx", ignoreCase = true) -> {
                    val fis = file.inputStream()
                    val slideshow = XMLSlideShow(fis)
                    val count = slideshow.slides.size
                    fis.close()
                    count
                }
                else -> 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
    
    fun renderPptSlide(
        context: Context,
        pptPath: String,
        slideIndex: Int,
        maxWidth: Int = 1920,
        maxHeight: Int = 1080
    ): Bitmap? {
        return try {
            val file = File(pptPath)
            when {
                pptPath.endsWith(".ppt", ignoreCase = true) -> {
                    renderHslfSlide(file, slideIndex, maxWidth, maxHeight)
                }
                pptPath.endsWith(".pptx", ignoreCase = true) -> {
                    renderXslfSlide(file, slideIndex, maxWidth, maxHeight)
                }
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun renderHslfSlide(
        file: File,
        slideIndex: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Bitmap? {
        val fis = file.inputStream()
        val slideshow = HSLFSlideShow(fis)
        
        if (slideIndex >= slideshow.slides.size) {
            fis.close()
            return null
        }
        
        val slide = slideshow.slides[slideIndex] as HSLFSlide
        val pageWidth = 960.0
        val pageHeight = 540.0
        
        // 创建位图
        val width = minOf(pageWidth.toInt(), maxWidth)
        val height = minOf(pageHeight.toInt(), maxHeight)
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 绘制白色背景
        canvas.drawColor(Color.WHITE)
        
        // 绘制幻灯片内容（简化版本，只绘制文本）
        drawSlideContent(canvas, slide, width.toFloat(), height.toFloat())
        
        fis.close()
        return bitmap
    }
    
    private fun renderXslfSlide(
        file: File,
        slideIndex: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Bitmap? {
        val fis = file.inputStream()
        val slideshow = XMLSlideShow(fis)
        
        if (slideIndex >= slideshow.slides.size) {
            fis.close()
            return null
        }
        
        val slide = slideshow.slides[slideIndex] as XSLFSlide
        val pageWidth = 960.0
        val pageHeight = 540.0
        
        // 创建位图
        val width = minOf(pageWidth.toInt(), maxWidth)
        val height = minOf(pageHeight.toInt(), maxHeight)
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 绘制白色背景
        canvas.drawColor(Color.WHITE)
        
        // 绘制幻灯片内容（简化版本）
        drawXslfSlideContent(canvas, slide, width.toFloat(), height.toFloat())
        
        fis.close()
        return bitmap
    }
    
    private fun drawSlideContent(canvas: Canvas, slide: HSLFSlide, width: Float, height: Float) {
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            isAntiAlias = true
        }
        
        var yPos = 50f
        slide.shapes.forEach { shape ->
            if (shape is org.apache.poi.hslf.usermodel.HSLFTextShape) {
                val text = shape.text
                if (text.isNotBlank()) {
                    canvas.drawText(text, 50f, yPos, paint)
                    yPos += 40f
                }
            }
        }
    }
    
    private fun drawXslfSlideContent(canvas: Canvas, slide: XSLFSlide, width: Float, height: Float) {
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            isAntiAlias = true
        }
        
        var yPos = 50f
        slide.shapes.forEach { shape ->
            if (shape is org.apache.poi.xslf.usermodel.XSLFTextShape) {
                val text = shape.text
                if (text.isNotBlank()) {
                    canvas.drawText(text, 50f, yPos, paint)
                    yPos += 40f
                }
            }
        }
    }
}
