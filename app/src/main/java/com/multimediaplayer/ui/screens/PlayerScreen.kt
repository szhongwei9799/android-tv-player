package com.multimediaplayer.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multimediaplayer.PlayerActivity
import com.multimediaplayer.data.database.AppDatabase
import com.multimediaplayer.data.models.*
import com.multimediaplayer.ui.components.MediaRenderer

const val ACTION_STOP_PLAYBACK = "com.multimediaplayer.STOP_PLAYBACK"

private data class ChannelItem(
    val playlistName: String,
    val mediaIndex: Int,
    val media: Media,
    val isHeader: Boolean = false
)

@Composable
fun PlayerScreen(
    playlistId: Long,
    endTimeMillis: Long? = null,
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

    var showChannelList by remember { mutableStateOf(false) }
    var channelItems by remember { mutableStateOf<List<ChannelItem>>(emptyList()) }
    var channelSelectedIndex by remember { mutableIntStateOf(0) }
    var pageCommand by remember { mutableIntStateOf(0) }

    LaunchedEffect(playlistId) {
        playlist = database.playlistDao().getPlaylistById(playlistId)
        if (playlist != null) {
            val items = database.playlistDao().getPlaylistItems(playlistId)
            val tagIds = items.flatMap { item ->
                runBlocking { database.playlistDao().getPlaylistItemTags(item.id) }
            }.map { it.tagId }
            if (tagIds.isNotEmpty()) {
                database.tagDao().getMediaByTagIds(tagIds).collect { media ->
                    mediaList = media
                    shuffleOrder = (media.indices).toList().shuffled()
                }
            }
        }
    }

    // 定时任务结束时间：每5秒检查一次，到点自动返回
    LaunchedEffect(playlistId, endTimeMillis) {
        if (endTimeMillis != null) {
            while (System.currentTimeMillis() < endTimeMillis) {
                kotlinx.coroutines.delay(5_000L)
            }
            onBack()
        }
    }

    fun buildChannelItems() {
        val items = mutableListOf<ChannelItem>()
        val pl = playlist
        if (pl != null && mediaList.isNotEmpty()) {
            items.add(ChannelItem(pl.name, -1, Media(name = "", type = MediaType.IMAGE, source = MediaSource.LOCAL, path = ""), isHeader = true))
            for (i in mediaList.indices) {
                items.add(ChannelItem(pl.name, i, mediaList[i]))
            }
        }
        channelItems = items
    }

    fun rebuildChannelItems() {
        buildChannelItems()
        channelSelectedIndex = channelItems.indexOfFirst { it.mediaIndex == currentIndex && !it.isHeader }
            .coerceAtLeast(1)
    }

    LaunchedEffect(mediaList) {
        if (mediaList.isNotEmpty() && channelItems.isEmpty()) {
            buildChannelItems()
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
        val size = mediaList.size
        if (size <= 1) return 0

        if (shuffleOrder.isNotEmpty() && shuffleOrder.size == size) {
            val idx = shuffleOrder.indexOf(currentIndex)
            if (idx < size - 1) return shuffleOrder[idx + 1]
        }
        return if (currentIndex < size - 1) currentIndex + 1 else 0
    }

    fun shouldContinue(): Boolean {
        if (endTimeMillis != null) return true
        val size = mediaList.size
        if (size <= 1) return false
        val isLast = currentIndex >= size - 1
        if (!isLast) return true
        loopCompleted++
        return loopCompleted < 1
    }

    fun handleKeyEvent(event: KeyEvent): Boolean = if (event.action == KeyEvent.ACTION_DOWN) {
        when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    if (showChannelList) {
                        val selected = channelItems.getOrNull(channelSelectedIndex)
                        if (selected != null && !selected.isHeader) {
                            currentIndex = selected.mediaIndex
                            pageCommand = 0
                            showChannelList = false
                        }
                    } else {
                        showChannelList = true
                        rebuildChannelItems()
                    }
                    true
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (showChannelList) {
                        var newIdx = channelSelectedIndex - 1
                        while (newIdx >= 0 && channelItems.getOrNull(newIdx)?.isHeader == true) newIdx--
                        if (newIdx >= 0) channelSelectedIndex = newIdx
                    }
                    true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (showChannelList) {
                        var newIdx = channelSelectedIndex + 1
                        while (newIdx < channelItems.size && channelItems.getOrNull(newIdx)?.isHeader == true) newIdx++
                        if (newIdx < channelItems.size) channelSelectedIndex = newIdx
                    }
                    true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (!showChannelList && mediaList.isNotEmpty()) {
                        val media = mediaList[currentIndex]
                        if (media.type == MediaType.PDF || media.type == MediaType.PPT) {
                            pageCommand = -1
                        } else if (currentIndex > 0) {
                            currentIndex--
                        }
                    }
                    true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (!showChannelList && mediaList.isNotEmpty()) {
                        val media = mediaList[currentIndex]
                        if (media.type == MediaType.PDF || media.type == MediaType.PPT) {
                            pageCommand = 1
                        } else if (currentIndex < mediaList.size - 1) {
                            currentIndex++
                        }
                    }
                    true
                }
                KeyEvent.KEYCODE_BACK -> {
                    if (showChannelList) {
                        showChannelList = false
                    } else {
                        onBack()
                    }
                    true
                }
                else -> false
            }
        } else false

    val activity = LocalContext.current as? PlayerActivity
    DisposableEffect(Unit) {
        activity?.keyEventHandler = { event -> handleKeyEvent(event) }
        onDispose { activity?.keyEventHandler = null }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
                pageCommand = pageCommand,
                onPageCommandConsumed = { pageCommand = 0 },
                modifier = Modifier.fillMaxSize()
            )

            if (showChannelList) {
                ChannelListOverlay(
                    items = channelItems,
                    selectedIndex = channelSelectedIndex,
                    currentMediaIndex = currentIndex,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

}

@Composable
private fun ChannelListOverlay(
    items: List<ChannelItem>,
    selectedIndex: Int,
    currentMediaIndex: Int,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scrollToIndex = selectedIndex.coerceAtLeast(0)

    LaunchedEffect(scrollToIndex) {
        if (scrollToIndex > 0) {
            listState.animateScrollToItem(scrollToIndex)
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xCC000000))
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 80.dp, bottom = 80.dp)
                .padding(horizontal = 60.dp),
            verticalArrangement = Arrangement.Center
        ) {
            itemsIndexed(items) { index, item ->
                if (item.isHeader) {
                    Text(
                        text = item.playlistName,
                        color = Color(0xFF4DABF7),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                    )
                } else {
                    val isSelected = index == selectedIndex
                    val isCurrent = item.mediaIndex == currentMediaIndex

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) Color(0x664DABF7)
                                else if (isCurrent) Color(0x33FFFFFF)
                                else Color.Transparent
                            )
                            .padding(vertical = 10.dp, horizontal = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mediaTypeIcon(item.media.type),
                                fontSize = 14.sp,
                                color = Color.White,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = item.media.name,
                                color = if (isSelected) Color.White else Color(0xCCFFFFFF),
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (isCurrent) {
                                Text(
                                    text = "▶ 播放中",
                                    color = Color(0xFF4DABF7),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun mediaTypeIcon(type: MediaType): String {
    return when (type) {
        MediaType.VIDEO -> "🎬"
        MediaType.STREAM -> "📹"
        MediaType.IMAGE -> "🖼️"
        MediaType.PPT -> "📊"
        MediaType.PDF -> "📄"
        MediaType.AUDIO -> "🎵"
    }
}
