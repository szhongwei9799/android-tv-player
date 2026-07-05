package com.multimediaplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.multimediaplayer.data.models.Media
import com.multimediaplayer.data.models.MediaType

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerControls(
    media: Media,
    currentIndex: Int,
    totalItems: Int,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xCC000000))
            .padding(16.dp)
    ) {
        // 媒体信息
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = media.name,
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = "${currentIndex + 1} / $totalItems",
                color = Color(0xFFB0B0B0),
                fontSize = 14.sp
            )
        }
        
        // 控制按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按钮
            ControlButton(
                icon = Icons.Default.ArrowBack,
                onClick = onBack,
                contentDescription = "返回"
            )
            
            Spacer(modifier = Modifier.width(24.dp))
            
            // 上一个
            ControlButton(
                icon = Icons.Default.SkipPrevious,
                onClick = onPrevious,
                contentDescription = "上一个",
                enabled = currentIndex > 0
            )
            
            Spacer(modifier = Modifier.width(24.dp))
            
            // 播放/暂停
            ControlButton(
                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                onClick = onPlayPause,
                contentDescription = if (isPlaying) "暂停" else "播放",
                size = 64
            )
            
            Spacer(modifier = Modifier.width(24.dp))
            
            // 下一个
            ControlButton(
                icon = Icons.Default.SkipNext,
                onClick = onNext,
                contentDescription = "下一个",
                enabled = currentIndex < totalItems - 1
            )
        }
        
        // 媒体类型指示器
        if (media.type == MediaType.PDF || media.type == MediaType.PPT) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "按左/右键翻页",
                color = Color(0xFFB0B0B0),
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun ControlButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    enabled: Boolean = true,
    size: Int = 48
) {
    IconButton(
        onClick = onClick,
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) Color.White else Color(0xFF666666),
            modifier = Modifier.size(size.dp)
        )
    }
}
