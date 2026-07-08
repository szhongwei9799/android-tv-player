package com.multimediaplayer

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.multimediaplayer.ui.screens.PlayerScreen
import com.multimediaplayer.ui.theme.MediaPlayerTheme

class PlayerActivity : ComponentActivity() {
    var keyEventHandler: ((KeyEvent) -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val playlistId = intent.getLongExtra("playlist_id", -1)
        val durationMinutes = if (intent.hasExtra("duration_minutes")) intent.getIntExtra("duration_minutes", -1) else -1

        setContent {
            MediaPlayerTheme {
                PlayerScreen(
                    playlistId = playlistId,
                    durationMinutes = if (durationMinutes > 0) durationMinutes else null,
                    onBack = { finish() }
                )
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (keyEventHandler?.invoke(event) == true) return true
        return super.dispatchKeyEvent(event)
    }

    override fun onBackPressed() {
        finish()
    }
}
