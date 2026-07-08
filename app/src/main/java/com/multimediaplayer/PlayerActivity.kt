package com.multimediaplayer

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.multimediaplayer.ui.screens.PlayerScreen
import com.multimediaplayer.ui.theme.MediaPlayerTheme
import java.util.*

class PlayerActivity : ComponentActivity() {
    var keyEventHandler: ((KeyEvent) -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val playlistId = intent.getLongExtra("playlist_id", -1)
        val endTime = intent.getStringExtra("end_time")
        val endTimeMillis = endTime?.let { parseEndTime(it) }

        setContent {
            MediaPlayerTheme {
                PlayerScreen(
                    playlistId = playlistId,
                    endTimeMillis = endTimeMillis,
                    onBack = { finish() }
                )
            }
        }
    }

    private fun parseEndTime(time: String): Long {
        val parts = time.split(":")
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, parts.getOrNull(0)?.toIntOrNull() ?: 0)
        cal.set(Calendar.MINUTE, parts.getOrNull(1)?.toIntOrNull() ?: 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.timeInMillis
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (keyEventHandler?.invoke(event) == true) return true
        return super.dispatchKeyEvent(event)
    }

    override fun onBackPressed() {
        finish()
    }
}
