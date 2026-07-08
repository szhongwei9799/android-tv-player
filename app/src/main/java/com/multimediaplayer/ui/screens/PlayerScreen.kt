package com.multimediaplayer.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import com.multimediaplayer.data.database.AppDatabase
import com.multimediaplayer.data.models.*
import com.multimediaplayer.ui.components.MediaRenderer

const val ACTION_STOP_PLAYBACK = "com.multimediaplayer.STOP_PLAYBACK"

@Composable
fun PlayerScreen(
    playlistId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }

    var playlist by remember { mutableStateOf<Playlist?>(null) }
    var mediaList by remember { mutableStateOf<List<Media>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var shuffleOrder by remember { mutableStateOf(listOf<Int>()) }
    var loopCompleted by remember { mutableIntStateOf(0) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(playlistId) {
        playlist = database.playlistDao().getPlaylistById(playlistId)

        when (playlist?.type) {
            PlaylistType.MANUAL -> {
                database.playlistDao().getPlaylistMedia(playlistId).collect { media ->
                    mediaList = media
                    shuffleOrder = (media.indices).toList().shuffled()
                }
            }
            PlaylistType.TAG_BASED -> {
                val playlistTags = database.playlistDao().getPlaylistTags(playlistId)
                val tagIds = playlistTags.map { it.tagId }
                if (tagIds.isNotEmpty()) {
                    database.tagDao().getMediaByTagIds(tagIds).collect { media ->
                        mediaList = media
                        shuffleOrder = (media.indices).toList().shuffled()
                    }
                }
            }
            else -> {}
        }
    }

    val stopReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == ACTION_STOP_PLAYBACK) {
                    onBack()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        val filter = IntentFilter(ACTION_STOP_PLAYBACK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(stopReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(stopReceiver, filter)
        }
        onDispose { context.unregisterReceiver(stopReceiver) }
    }

    fun getNextIndex(): Int {
        val pl = playlist ?: return currentIndex + 1
        val size = mediaList.size
        if (size <= 1) return 0
        return when (pl.playMode) {
            PlayMode.RANDOM -> {
                var next = currentIndex
                while (next == currentIndex && size > 1) next = (0 until size).random()
                next
            }
            PlayMode.SHUFFLE -> {
                val idx = shuffleOrder.indexOf(currentIndex)
                if (idx < size - 1) shuffleOrder[idx + 1] else shuffleOrder.firstOrNull() ?: 0
            }
            else -> {
                if (currentIndex < size - 1) currentIndex + 1 else 0
            }
        }
    }

    fun shouldContinue(): Boolean {
        val pl = playlist ?: return true
        val size = mediaList.size
        if (size <= 1) return false
        val isLast = when (pl.playMode) {
            PlayMode.SHUFFLE -> shuffleOrder.indexOf(currentIndex) >= size - 1
            else -> currentIndex >= size - 1
        }
        if (!isLast) return true
        loopCompleted++
        return when {
            pl.loopCount == -1 -> true
            loopCompleted < pl.loopCount -> true
            else -> false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionCenter, Key.Enter -> {
                            if (mediaList.isNotEmpty()) {
                                val media = mediaList[currentIndex]
                                if (media.type == MediaType.VIDEO || media.type == MediaType.STREAM) {
                                    isPaused = !isPaused
                                }
                            }
                            true
                        }
                        Key.DirectionLeft -> {
                            if (mediaList.isNotEmpty()) {
                                val media = mediaList[currentIndex]
                                if (media.type != MediaType.VIDEO && media.type != MediaType.STREAM) {
                                    if (currentIndex > 0) currentIndex--
                                }
                            }
                            true
                        }
                        Key.DirectionRight -> {
                            if (mediaList.isNotEmpty()) {
                                val media = mediaList[currentIndex]
                                if (media.type != MediaType.VIDEO && media.type != MediaType.STREAM) {
                                    if (currentIndex < mediaList.size - 1) currentIndex++
                                }
                            }
                            true
                        }
                        Key.Back -> {
                            onBack()
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        if (mediaList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val media = mediaList[currentIndex]

            MediaRenderer(
                media = media,
                isPlaying = !isPaused,
                onVideoComplete = {
                    if (shouldContinue()) {
                        currentIndex = getNextIndex()
                    } else {
                        isPaused = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
