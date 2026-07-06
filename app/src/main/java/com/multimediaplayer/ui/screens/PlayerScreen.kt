package com.multimediaplayer.ui.screens

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
    
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(playlistId) {
        playlist = database.playlistDao().getPlaylistById(playlistId)
        
        when (playlist?.type) {
            PlaylistType.MANUAL -> {
                database.playlistDao().getPlaylistMedia(playlistId).collect { media ->
                    mediaList = media
                }
            }
            PlaylistType.TAG_BASED -> {
                val playlistTags = database.playlistDao().getPlaylistTags(playlistId)
                val tagIds = playlistTags.map { it.tagId }
                if (tagIds.isNotEmpty()) {
                    database.tagDao().getMediaByTagIds(tagIds).collect { media ->
                        mediaList = media
                    }
                }
            }
            else -> {}
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
                    if (currentIndex < mediaList.size - 1) {
                        currentIndex++
                    } else if (playlist?.isDefault == true) {
                        currentIndex = 0
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
