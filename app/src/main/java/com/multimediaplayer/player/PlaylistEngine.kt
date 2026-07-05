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
    
    fun play() {
        if (_mediaList.value.isNotEmpty()) {
            _isPlaying.value = true
        }
    }
    
    fun pause() {
        _isPlaying.value = false
    }
    
    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }
    
    fun next() {
        val currentIdx = _currentIndex.value
        val media = _mediaList.value
        
        if (currentIdx < media.size - 1) {
            _currentIndex.value = currentIdx + 1
            _currentMedia.value = media[currentIdx + 1]
        } else if (_currentPlaylist.value?.isDefault == true) {
            // 循环播放
            _currentIndex.value = 0
            _currentMedia.value = media.firstOrNull()
        }
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
                // 随机选择一个转场效果
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
