package com.multimediaplayer.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.multimediaplayer.player.datasource.ProtocolDataSourceFactory
import com.multimediaplayer.data.models.Media
import com.multimediaplayer.data.models.MediaType
import com.multimediaplayer.utils.PdfRendererHelper
import com.multimediaplayer.utils.PptRendererHelper
import androidx.compose.material3.CircularProgressIndicator
import java.io.File

@Composable
fun MediaRenderer(
    media: Media,
    isPlaying: Boolean,
    onVideoComplete: () -> Unit,
    pageCommand: Int = 0,
    onPageCommandConsumed: () -> Unit = {},
    imageInterval: Int = 5,
    pptInterval: Int = 10,
    pdfInterval: Int = 10,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    when (media.type) {
        MediaType.VIDEO, MediaType.STREAM -> {
            VideoPlayer(
                media = media,
                isPlaying = isPlaying,
                onVideoComplete = onVideoComplete,
                modifier = modifier
            )
        }
        MediaType.IMAGE -> {
            ImagePlayer(
                media = media,
                onVideoComplete = onVideoComplete,
                isPlaying = isPlaying,
                imageInterval = imageInterval,
                modifier = modifier
            )
        }
        MediaType.PDF -> {
            PdfPlayer(
                media = media,
                isPlaying = isPlaying,
                onVideoComplete = onVideoComplete,
                pageCommand = pageCommand,
                onPageCommandConsumed = onPageCommandConsumed,
                pdfInterval = pdfInterval,
                modifier = modifier
            )
        }
        MediaType.PPT -> {
            PptPlayer(
                media = media,
                isPlaying = isPlaying,
                onVideoComplete = onVideoComplete,
                pageCommand = pageCommand,
                onPageCommandConsumed = onPageCommandConsumed,
                pptInterval = pptInterval,
                modifier = modifier
            )
        }
        MediaType.AUDIO -> {
            AudioPlayer(
                media = media,
                isPlaying = isPlaying,
                modifier = modifier
            )
        }
    }
}

@Composable
fun VideoPlayer(
    media: Media,
    isPlaying: Boolean,
    onVideoComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dataSourceFactory = remember { ProtocolDataSourceFactory(context) }
    val rtspSourceFactory = remember { RtspMediaSource.Factory() }
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory))
            .build()
    }

    LaunchedEffect(media.path) {
        val mediaSource = if (media.path.startsWith("rtsp://", ignoreCase = true) || media.path.startsWith("rtsps://", ignoreCase = true)) {
            rtspSourceFactory.createMediaSource(MediaItem.fromUri(Uri.parse(media.path)))
        } else if (media.source == com.multimediaplayer.data.models.MediaSource.LOCAL) {
            val defaultMediaSourceFactory = DefaultMediaSourceFactory(context)
                .setDataSourceFactory(dataSourceFactory)
            defaultMediaSourceFactory.createMediaSource(MediaItem.fromUri(Uri.fromFile(File(media.path))))
        } else {
            val defaultMediaSourceFactory = DefaultMediaSourceFactory(context)
                .setDataSourceFactory(dataSourceFactory)
            defaultMediaSourceFactory.createMediaSource(MediaItem.fromUri(Uri.parse(media.path)))
        }

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                    onVideoComplete()
                }
            }
        })
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
            }
        },
        modifier = modifier
    )
}

@Composable
fun ImagePlayer(
    media: Media,
    onVideoComplete: () -> Unit = {},
    isPlaying: Boolean = true,
    imageInterval: Int = 5,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (media.source == com.multimediaplayer.data.models.MediaSource.LOCAL) {
        AsyncImage(
            model = File(media.path),
            contentDescription = media.name,
            contentScale = ContentScale.Fit,
            modifier = modifier.background(Color.Black)
        )
    } else {
        AsyncImage(
            model = media.path,
            contentDescription = media.name,
            contentScale = ContentScale.Fit,
            modifier = modifier.background(Color.Black)
        )
    }
}

@Composable
fun PdfPlayer(
    media: Media,
    isPlaying: Boolean,
    onVideoComplete: () -> Unit,
    pageCommand: Int = 0,
    onPageCommandConsumed: () -> Unit = {},
    pdfInterval: Int = 10,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentPage by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(0) }
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(media.path) {
        pageCount = PdfRendererHelper.getPdfPageCount(context, media.path)
        currentPage = 0
        if (pageCount > 0) {
            bitmap = PdfRendererHelper.renderPdfPage(context, media.path, 0)
        }
    }

    LaunchedEffect(pageCommand) {
        if (pageCommand != 0 && pageCount > 0) {
            val newPage = (currentPage + pageCommand).coerceIn(0, pageCount - 1)
            if (newPage != currentPage) {
                currentPage = newPage
                bitmap = PdfRendererHelper.renderPdfPage(context, media.path, currentPage)
            }
            onPageCommandConsumed()
        }
    }

    LaunchedEffect(isPlaying, currentPage) {
        if (isPlaying && pageCount > 0) {
            kotlinx.coroutines.delay(pdfInterval * 1000L)
            if (currentPage < pageCount - 1) {
                currentPage++
                bitmap = PdfRendererHelper.renderPdfPage(context, media.path, currentPage)
            } else if (currentPage == pageCount - 1) {
                onVideoComplete()
            }
        }
    }

    LaunchedEffect(currentPage) {
        if (pageCount > 0 && currentPage in 0 until pageCount) {
            bitmap = PdfRendererHelper.renderPdfPage(context, media.path, currentPage)
        }
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "PDF Page ${currentPage + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } ?: run {
            CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
fun PptPlayer(
    media: Media,
    isPlaying: Boolean,
    onVideoComplete: () -> Unit,
    pageCommand: Int = 0,
    onPageCommandConsumed: () -> Unit = {},
    pptInterval: Int = 10,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentSlide by remember { mutableIntStateOf(0) }
    var slideCount by remember { mutableIntStateOf(0) }
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(media.path) {
        slideCount = PptRendererHelper.getSlideCount(context, media.path)
        currentSlide = 0
        if (slideCount > 0) {
            bitmap = PptRendererHelper.renderPptSlide(context, media.path, 0)
        }
    }

    LaunchedEffect(pageCommand) {
        if (pageCommand != 0 && slideCount > 0) {
            val newSlide = (currentSlide + pageCommand).coerceIn(0, slideCount - 1)
            if (newSlide != currentSlide) {
                currentSlide = newSlide
                bitmap = PptRendererHelper.renderPptSlide(context, media.path, currentSlide)
            }
            onPageCommandConsumed()
        }
    }

    LaunchedEffect(isPlaying, currentSlide) {
        if (isPlaying && slideCount > 0) {
            kotlinx.coroutines.delay(pptInterval * 1000L)
            if (currentSlide < slideCount - 1) {
                currentSlide++
                bitmap = PptRendererHelper.renderPptSlide(context, media.path, currentSlide)
            } else if (currentSlide == slideCount - 1) {
                onVideoComplete()
            }
        }
    }

    LaunchedEffect(currentSlide) {
        if (slideCount > 0 && currentSlide in 0 until slideCount) {
            bitmap = PptRendererHelper.renderPptSlide(context, media.path, currentSlide)
        }
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "PPT Slide ${currentSlide + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } ?: run {
            CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
fun AudioPlayer(
    media: Media,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dataSourceFactory = remember { ProtocolDataSourceFactory(context) }
    val rtspSourceFactory = remember { RtspMediaSource.Factory() }
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory))
            .build()
    }

    LaunchedEffect(media.path) {
        val mediaSource = if (media.path.startsWith("rtsp://", ignoreCase = true) || media.path.startsWith("rtsps://", ignoreCase = true)) {
            rtspSourceFactory.createMediaSource(MediaItem.fromUri(Uri.parse(media.path)))
        } else if (media.source == com.multimediaplayer.data.models.MediaSource.LOCAL) {
            val defaultMediaSourceFactory = DefaultMediaSourceFactory(context)
                .setDataSourceFactory(dataSourceFactory)
            defaultMediaSourceFactory.createMediaSource(MediaItem.fromUri(Uri.fromFile(File(media.path))))
        } else {
            val defaultMediaSourceFactory = DefaultMediaSourceFactory(context)
                .setDataSourceFactory(dataSourceFactory)
            defaultMediaSourceFactory.createMediaSource(MediaItem.fromUri(Uri.parse(media.path)))
        }

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
    }
}
