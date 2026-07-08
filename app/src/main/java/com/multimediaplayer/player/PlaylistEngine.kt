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
    
    private val _currentTag = MutableStateFlow<PlaylistTag?>(null)
    val currentTag: StateFlow<PlaylistTag?> = _currentTag.asStateFlow()
    
    private val _tags = MutableStateFlow<List<PlaylistTag>>(emptyList())
    
    private val _mediaList = MutableStateFlow<List<Media>>(emptyList())
    val mediaList: StateFlow<List<Media>> = _mediaList.asStateFlow()
    
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentMedia = MutableStateFlow<Media?>(null)
    val currentMedia: StateFlow<Media?> = _currentMedia.asStateFlow()
    
    private var currentTagIndex = 0
    private var shuffleOrder = mutableListOf<Int>()
    private var loopCompleted = 0
    private var tagLoopCompleted = 0
    
    suspend fun loadPlaylist(playlistId: Long) {
        val playlist = database.playlistDao().getPlaylistById(playlistId)
        _currentPlaylist.value = playlist
        
        val tags = database.playlistDao().getPlaylistTags(playlistId)
        _tags.value = tags
        
        if (tags.isEmpty()) {
            _mediaList.value = emptyList()
            _currentMedia.value = null
            return
        }
        
        currentTagIndex = 0
        loadTagMedia(tags[0])
    }
    
    private suspend fun loadTagMedia(tag: PlaylistTag) {
        _currentTag.value = tag
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
        val tag = _currentTag.value ?: return false
        val media = _mediaList.value
        if (media.isEmpty()) return false
        
        val nextIdx = getNextIndex()
        
        if (nextIdx < media.size) {
            _currentIndex.value = nextIdx
            _currentMedia.value = media[nextIdx]
            return true
        }
        
        // 当前标签播完，前进到下一个标签
        return advanceToNextTag()
    }
    
    private fun advanceToNextTag(): Boolean {
        val tags = _tags.value
        val playlist = _currentPlaylist.value ?: return false
        if (tags.isEmpty()) return false
        
        val isLastTag = currentTagIndex >= tags.size - 1
        
        if (isLastTag) {
            tagLoopCompleted++
            if (playlist.tagLoopCount != -1 && tagLoopCompleted >= playlist.tagLoopCount) {
                return false  // 所有标签循环结束
            }
            currentTagIndex = 0
        } else {
            currentTagIndex++
        }
        
        runBlocking { loadTagMedia(tags[currentTagIndex]) }
        return true
    }
    
    private fun getNextIndex(): Int {
        val tag = _currentTag.value ?: return _currentIndex.value + 1
        val size = _mediaList.value.size
        if (size == 0) return Int.MAX_VALUE
        
        val isLast = when (tag.playMode) {
            PlayMode.SHUFFLE -> shuffleOrder.indexOf(_currentIndex.value) >= size - 1
            else -> _currentIndex.value >= size - 1
        }
        
        if (isLast) {
            loopCompleted++
            if (tag.loopCount != -1 && loopCompleted >= tag.loopCount) {
                return Int.MAX_VALUE  // 当前标签循环结束，触发下一个标签
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
            MediaType.PPT -> defaultInterval * 1.5.toInt()
            MediaType.PDF -> defaultInterval * 1.5.toInt()
            else -> defaultInterval
        }
    }
}
