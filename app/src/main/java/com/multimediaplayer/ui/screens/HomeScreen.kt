package com.multimediaplayer.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.multimediaplayer.data.database.AppDatabase
import com.multimediaplayer.utils.NetworkUtils
import com.multimediaplayer.utils.QrCodeGenerator
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartPlay: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    
    var serverUrl by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var hasPlaylists by remember { mutableStateOf(false) }
    var defaultPlaylistId by remember { mutableStateOf<Long?>(null) }
    
    // 获取服务器URL和二维码
    LaunchedEffect(Unit) {
        serverUrl = NetworkUtils.getServerUrl(context)
        qrBitmap = QrCodeGenerator.generateQrCode(serverUrl)
        
        // 检查是否有播放列表
        val playlistDao = database.playlistDao()
        val defaultPlaylist = playlistDao.getDefaultPlaylist()
        if (defaultPlaylist != null) {
            hasPlaylists = true
            defaultPlaylistId = defaultPlaylist.id
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        if (!hasPlaylists) {
            // 无播放列表时显示二维码
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 标题
                Text(
                    text = "多媒体展示系统",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // 二维码
                qrBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "管理后台二维码",
                        modifier = Modifier.size(300.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // 提示文字
                Text(
                    text = "请扫码管理媒体内容",
                    fontSize = 24.sp,
                    color = Color(0xFFB0B0B0)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 服务器地址
                Text(
                    text = "后台地址: $serverUrl",
                    fontSize = 20.sp,
                    color = Color(0xFF1976D2)
                )
            }
        } else {
            // 有播放列表时显示播放选项
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "多媒体展示系统",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // 播放按钮
                Button(
                    onClick = {
                        defaultPlaylistId?.let { onStartPlay(it) }
                    },
                    modifier = Modifier
                        .width(300.dp)
                        .height(80.dp)
                ) {
                    Text(
                        text = "开始播放",
                        fontSize = 24.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 二维码（小尺寸）
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    qrBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "管理后台二维码",
                            modifier = Modifier.size(120.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(24.dp))
                    
                    Column {
                        Text(
                            text = "管理后台",
                            fontSize = 18.sp,
                            color = Color(0xFFB0B0B0)
                        )
                        Text(
                            text = serverUrl,
                            fontSize = 16.sp,
                            color = Color(0xFF1976D2)
                        )
                    }
                }
            }
        }
    }
}
