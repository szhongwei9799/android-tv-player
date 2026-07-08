package com.multimediaplayer.player

import android.content.Context
import com.multimediaplayer.data.database.AppDatabase
import com.multimediaplayer.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking

class PlaylistEngine(private val context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    
    private val _currentPlaylist = MutableStateFlow<Playlist?>(null)
    val currentPlaylist: StateFlow<Playlist?> = _currentPlaylist.asStateFlow()
    
    private val _mediaList = MutableStateFlow<List<Media>>(emptyList())
    val mediaList: StateFlow<List<Media>> = _mediaList.asStateFlow()
    
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentMedia = MutableStateFlow<Media?>(null)
    val currentMedia: StateFlow<Media?> = _currentMedia.asStateFlow()
    
    private var shuffleOrder = mutableListOf<Int>()
    private var loopCompleted = 0
    
    suspend fun loadPlaylist(playlistId: Long) {
        val playlist = database.playlistDao().getPlaylistById(playlistId)
        _currentPlaylist.value = playlist
        
        val media = when (playlist?.type) {
            PlaylistType.MANUAL -> {
                loadManualPlaylist(playlistId)
            }
            PlaylistType.TAG_BASED -> {
                loadTagBasedPlaylist(playlistId)
            }
            else -> emptyList()
        }
        
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
    
    private suspend fun loadManualPlaylist(playlistId: Long): List<Media> {
        val items = database.playlistDao().getPlaylistItems(playlistId)
        return items.mapNotNull { item ->
            database.mediaDao().getMediaById(item.mediaId)
        }
    }
    
    private suspend fun loadTagBasedPlaylist(playlistId: Long): List<Media> {
        val playlistTags = database.playlistDao().getPlaylistTags(playlistId)
        val tagIds = playlistTags.map { it.tagId }
        
        if (tagIds.isEmpty()) return emptyList()
        
        return database.tagDao().getMediaByTagIds(tagIds).let { flow ->
            var mediaList = emptyList<Media>()
            flow.collect { media ->
                mediaList = media
            }
            mediaList
        }
    }
    
    fun getPlaybackIndex(mediaIndex: Int): Int {
        val playlist = _currentPlaylist.value ?: return mediaIndex
        return when (playlist.playMode) {
            PlayMode.SHUFFLE -> {
                if (mediaIndex < shuffleOrder.size) shuffleOrder[mediaIndex]
                else mediaIndex
            }
            else -> mediaIndex
        }
    }
    
    fun getNextIndex(): Int {
        val playlist = _currentPlaylist.value ?: return _currentIndex.value + 1
        val size = _mediaList.value.size
        if (size == 0) return 0
        
        return when (playlist.playMode) {
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
    
    fun shouldLoop(): Boolean {
        val playlist = _currentPlaylist.value ?: return true
        val size = _mediaList.value.size
        if (size <= 1) return false
        val isLastItem = (_currentIndex.value >= size - 1) ||
            (playlist.playMode == PlayMode.SHUFFLE && shuffleOrder.indexOf(_currentIndex.value) >= size - 1)
        if (!isLastItem) return true
        
        loopCompleted++
        return when {
            playlist.loopCount == -1 -> true
            loopCompleted < playlist.loopCount -> true
            else -> false
        }
    }
    
    fun next() {
        if (_mediaList.value.isEmpty()) return
        val nextIdx = getNextIndex()
        _currentIndex.value = nextIdx
        _currentMedia.value = _mediaList.value.getOrNull(nextIdx)
    }
    
    fun previous() {
        val currentIdx = _currentIndex.value
        val media = _mediaList.value
        
        if (currentIdx > 0) {
            _currentIndex.value = currentIdx - 1
            _currentMedia.value = media[currentIdx - 1]
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
