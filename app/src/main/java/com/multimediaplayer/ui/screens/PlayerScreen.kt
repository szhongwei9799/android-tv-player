package com.multimediaplayer.ui.screens

import android.view.KeyEvent
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
import androidx.tv.material3.*
import com.multimediaplayer.data.database.AppDatabase
import com.multimediaplayer.data.models.*
import com.multimediaplayer.player.PlaylistEngine
import com.multimediaplayer.ui.components.MediaRenderer
import com.multimediaplayer.ui.components.PlayerControls
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerScreen(
    playlistId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    
    var playlist by remember { mutableStateOf<Playlist?>(null) }
    var mediaList by remember { mutableStateOf<List<Media>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var isPaused by remember { mutableStateOf(false) }
    
    val focusRequester = remember { FocusRequester() }
    
    // 加载播放列表
    LaunchedEffect(playlistId) {
        playlist = database.playlistDao().getPlaylistById(playlistId)
        
        // 根据播放列表类型获取媒体
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
    
    // 自动隐藏控制栏
    LaunchedEffect(showControls, isPaused) {
        if (showControls && isPaused) {
            kotlinx.coroutines.delay(3000)
            if (!isPaused) {
                showControls = false
            }
        }
    }
    
    // 处理遥控器按键
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
                                } else {
                                    showControls = !showControls
                                }
                            }
                            true
                        }
                        Key.DirectionLeft -> {
                            if (mediaList.isNotEmpty()) {
                                val media = mediaList[currentIndex]
                                if (media.type == MediaType.VIDEO || media.type == MediaType.STREAM) {
                                    // 快退由播放器组件处理
                                } else if (currentIndex > 0) {
                                    currentIndex--
                                }
                            }
                            true
                        }
                        Key.DirectionRight -> {
                            if (mediaList.isNotEmpty()) {
                                val media = mediaList[currentIndex]
                                if (media.type == MediaType.VIDEO || media.type == MediaType.STREAM) {
                                    // 快进由播放器组件处理
                                } else if (currentIndex < mediaList.size - 1) {
                                    currentIndex++
                                }
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            // 音量增加由系统处理
                            true
                        }
                        Key.DirectionDown -> {
                            // 音量减少由系统处理
                            true
                        }
                        Key.Back -> {
                            onBack()
                            true
                        }
                        Key.Menu -> {
                            showControls = true
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        if (mediaList.isEmpty()) {
            // 加载中
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // 媒体渲染
            val media = mediaList[currentIndex]
            
            MediaRenderer(
                media = media,
                isPlaying = isPlaying && !isPaused,
                onVideoComplete = {
                    // 播放完成，切换到下一个
                    if (currentIndex < mediaList.size - 1) {
                        currentIndex++
                    } else if (playlist?.isDefault == true) {
                        // 循环播放
                        currentIndex = 0
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // 播放控制
            if (showControls) {
                PlayerControls(
                    media = media,
                    currentIndex = currentIndex,
                    totalItems = mediaList.size,
                    isPlaying = !isPaused,
                    onPlayPause = { isPaused = !isPaused },
                    onPrevious = {
                        if (currentIndex > 0) {
                            currentIndex--
                        }
                    },
                    onNext = {
                        if (currentIndex < mediaList.size - 1) {
                            currentIndex++
                        }
                    },
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
