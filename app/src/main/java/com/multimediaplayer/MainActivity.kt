package com.multimediaplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.multimediaplayer.data.database.AppDatabase
import com.multimediaplayer.server.WebService
import com.multimediaplayer.ui.screens.HomeScreen
import com.multimediaplayer.ui.theme.MediaPlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启动Web服务
        startWebService()
        
        setContent {
            MediaPlayerTheme {
                val database = remember { AppDatabase.getDatabase(this@MainActivity) }

                LaunchedEffect(Unit) {
                    val defaultPlaylist = database.playlistDao().getDefaultPlaylist()
                    if (defaultPlaylist != null) {
                        val intent = Intent(this@MainActivity, PlayerActivity::class.java).apply {
                            putExtra("playlist_id", defaultPlaylist.id)
                        }
                        startActivity(intent)
                    }
                }

                HomeScreen(
                    onStartPlay = { playlistId ->
                        val intent = Intent(this, PlayerActivity::class.java).apply {
                            putExtra("playlist_id", playlistId)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }

    private fun startWebService() {
        val serviceIntent = Intent(this, WebService::class.java)
        startForegroundService(serviceIntent)
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }
}
