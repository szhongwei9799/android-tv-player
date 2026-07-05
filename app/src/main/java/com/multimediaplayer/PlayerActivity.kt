package com.multimediaplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.multimediaplayer.data.database.AppDatabase
import com.multimediaplayer.ui.screens.PlayerScreen
import com.multimediaplayer.ui.theme.MediaPlayerTheme

class PlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val playlistId = intent.getLongExtra("playlist_id", -1)
        
        setContent {
            MediaPlayerTheme {
                PlayerScreen(
                    playlistId = playlistId,
                    onBack = { finish() }
                )
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
