package com.multimediaplayer.player

import android.content.Context
import com.multimediaplayer.data.database.AppDatabase
import com.multimediaplayer.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class PlaylistEngine(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)

    private val _currentPlaylist = MutableStateFlow<Playlist?>(null)
    val currentPlaylist: StateFlow<Playlist?> = _currentPlaylist.asStateFlow()

    private val _currentItem = MutableStateFlow<PlaylistItem?>(null)
    val currentItem: StateFlow<PlaylistItem?> = _currentItem.asStateFlow()

    private val _items = MutableStateFlow<List<PlaylistItem>>(emptyList())

    private val _currentItemTag = MutableStateFlow<PlaylistItemTag?>(null)
    val currentItemTag: StateFlow<PlaylistItemTag?> = _currentItemTag.asStateFlow()

    private val _mediaList = MutableStateFlow<List<Media>>(emptyList())
    val mediaList: StateFlow<List<Media>> = _mediaList.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentMedia = MutableStateFlow<Media?>(null)
    val currentMedia: StateFlow<Media?> = _currentMedia.asStateFlow()

    private var currentItemIndex = 0
    private var currentTagIndex = 0
    private var shuffleOrder = mutableListOf<Int>()
    private var loopCompleted = 0
    private var tagLoopCompleted = 0
    private var itemLoopCompleted = 0

    suspend fun loadPlaylist(playlistId: Long) {
        val playlist = database.playlistDao().getPlaylistById(playlistId)
        _currentPlaylist.value = playlist

        val items = database.playlistDao().getPlaylistItems(playlistId)
        _items.value = items

        if (items.isEmpty()) {
            _mediaList.value = emptyList()
            _currentMedia.value = null
            return
        }

        currentItemIndex = 0
        currentTagIndex = 0
        loadItemTag(items[0])
    }

    private suspend fun loadItemTag(item: PlaylistItem) {
        _currentItem.value = item
        val tags = database.playlistDao().getPlaylistItemTags(item.id)
        if (tags.isEmpty()) {
            _mediaList.value = emptyList()
            _currentMedia.value = null
            return
        }
        currentTagIndex = 0
        loadTagMedia(tags[0])
    }

    private suspend fun loadTagMedia(tag: PlaylistItemTag) {
        _currentItemTag.value = tag
        val media = database.tagDao().getMediaByTagId(tag.tagId).first()
        _mediaList.value = media
        _currentIndex.value = 0
        _currentMedia.value = media.firstOrNull()
        loopCompleted = 0
        buildShuffleOrder()
    }

    private fun buildShuffleOrder() {
        val size = _mediaList.value.size
        shuffleOrder = (0 until size).toMutableList()
        shuffleOrder.shuffle()
    }

    fun next(): Boolean {
        val tag = _currentItemTag.value ?: return false
        val media = _mediaList.value
        if (media.isEmpty()) return false

        val nextIdx = getNextIndex()

        if (nextIdx < media.size) {
            _currentIndex.value = nextIdx
            _currentMedia.value = media[nextIdx]
            return true
        }

        return advanceToNextTag()
    }

    private fun advanceToNextTag(): Boolean {
        val items = _items.value
        val item = _currentItem.value ?: return false
        val currentTag = _currentItemTag.value ?: return advanceToNextItem()
        val tags = runBlocking { database.playlistDao().getPlaylistItemTags(item.id) }
        if (tags.isEmpty()) return advanceToNextItem()

        val isLastTag = currentTagIndex >= tags.size - 1

        if (isLastTag) {
            tagLoopCompleted++
            if (currentTag.loopCount != 0 && tagLoopCompleted >= currentTag.loopCount) {
                return advanceToNextItem()
            }
            currentTagIndex = 0
        } else {
            currentTagIndex++
        }

        runBlocking { loadTagMedia(tags[currentTagIndex]) }
        return true
    }

    private fun advanceToNextItem(): Boolean {
        val items = _items.value
        val playlist = _currentPlaylist.value ?: return false
        if (items.isEmpty()) return false

        val isLastItem = currentItemIndex >= items.size - 1

        if (isLastItem) {
            itemLoopCompleted++
            if (playlist.itemLoopCount != 0 && itemLoopCompleted >= playlist.itemLoopCount) {
                return false
            }
            currentItemIndex = 0
        } else {
            currentItemIndex++
        }

        runBlocking { loadItemTag(items[currentItemIndex]) }
        return true
    }

    private fun getNextIndex(): Int {
        val tag = _currentItemTag.value ?: return _currentIndex.value + 1
        val size = _mediaList.value.size
        if (size == 0) return Int.MAX_VALUE

        val isLast = when (tag.playMode) {
            PlayMode.SHUFFLE -> shuffleOrder.indexOf(_currentIndex.value) >= size - 1
            else -> _currentIndex.value >= size - 1
        }

        if (isLast) {
            loopCompleted++
            if (tag.loopCount != 0 && loopCompleted >= tag.loopCount) {
                return Int.MAX_VALUE
            }
        }

        return when (tag.playMode) {
            PlayMode.RANDOM -> {
                var next = _currentIndex.value
                while (next == _currentIndex.value && size > 1) {
                    next = (0 until size).random()
                }
                next
            }
            PlayMode.SHUFFLE -> {
                val currentShuffleIdx = shuffleOrder.indexOf(_currentIndex.value)
                if (currentShuffleIdx < size - 1) shuffleOrder[currentShuffleIdx + 1]
                else 0
            }
            else -> {
                if (_currentIndex.value < size - 1) _currentIndex.value + 1
                else 0
            }
        }
    }

    fun previous() {
        val idx = _currentIndex.value
        val media = _mediaList.value
        if (idx > 0 && media.isNotEmpty()) {
            _currentIndex.value = idx - 1
            _currentMedia.value = media[idx - 1]
        }
    }

    fun seekTo(index: Int) {
        val media = _mediaList.value
        if (index in media.indices) {
            _currentIndex.value = index
            _currentMedia.value = media[index]
        }
    }

    fun stop() {
        _isPlaying.value = false
        _currentMedia.value = null
    }

    fun getCurrentTransitionType(): TransitionType {
        val playlist = _currentPlaylist.value ?: return TransitionType.FADE
        return when (playlist.transitionEffect) {
            TransitionType.RANDOM -> {
                val effects = TransitionType.values().filter {
                    it != TransitionType.RANDOM && it != TransitionType.NONE
                }
                effects.random()
            }
            else -> playlist.transitionEffect
        }
    }

    fun getDisplayInterval(media: Media): Int {
        val playlist = _currentPlaylist.value
        val defaultInterval = playlist?.defaultInterval ?: 10

        return when (media.type) {
            MediaType.IMAGE -> defaultInterval
            MediaType.PPT -> (defaultInterval * 1.5).toInt()
            MediaType.PDF -> (defaultInterval * 1.5).toInt()
            else -> defaultInterval
        }
    }
}