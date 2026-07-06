package com.multimediaplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.multimediaplayer.data.database.AppDatabase
import com.multimediaplayer.server.WebService
import com.multimediaplayer.ui.screens.HomeScreen
import com.multimediaplayer.ui.theme.MediaPlayerTheme
import com.multimediaplayer.utils.NetworkUtils

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启动Web服务
        startWebService()
        
        setContent {
            MediaPlayerTheme {
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
