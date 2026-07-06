package com.multimediaplayer.player

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.multimediaplayer.data.models.TransitionType

@Composable
fun TransitionEffect(
    transitionType: TransitionType,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val transition = updateTransition(targetState = isVisible, label = "transition")
    
    when (transitionType) {
        TransitionType.NONE -> {
            if (isVisible) {
                content()
            }
        }
        
        TransitionType.FADE -> {
            val alpha by transition.animateFloat(
                transitionSpec = {
                    tween(durationMillis = 500, easing = FastOutSlowInEasing)
                },
                label = "fade"
            ) { visible ->
                if (visible) 1f else 0f
            }
            
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(),
                modifier = modifier
            ) {
                content()
            }
        }
        
        TransitionType.SLIDE_LEFT -> {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(500)
                ) + fadeIn(),
                exit = slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(500)
                ) + fadeOut(),
                modifier = modifier
            ) {
                content()
            }
        }
        
        TransitionType.SLIDE_RIGHT -> {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(500)
                ) + fadeIn(),
                exit = slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(500)
                ) + fadeOut(),
                modifier = modifier
            ) {
                content()
            }
        }
        
        TransitionType.SLIDE_UP -> {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(500)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(500)
                ) + fadeOut(),
                modifier = modifier
            ) {
                content()
            }
        }
        
        TransitionType.SLIDE_DOWN -> {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(500)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(500)
                ) + fadeOut(),
                modifier = modifier
            ) {
                content()
            }
        }
        
        TransitionType.ZOOM_IN -> {
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(
                    initialScale = 0.5f,
                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                ) + fadeIn(),
                exit = scaleOut(
                    targetScale = 2f,
                    animationSpec = tween(500)
                ) + fadeOut(),
                modifier = modifier
            ) {
                content()
            }
        }
        
        TransitionType.ZOOM_OUT -> {
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(
                    initialScale = 2f,
                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                ) + fadeIn(),
                exit = scaleOut(
                    targetScale = 0.5f,
                    animationSpec = tween(500)
                ) + fadeOut(),
                modifier = modifier
            ) {
                content()
            }
        }
        
        TransitionType.WIPE_LEFT -> {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(500)
                ),
                exit = slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(500)
                ),
                modifier = modifier
            ) {
                content()
            }
        }
        
        TransitionType.WIPE_RIGHT -> {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(500)
                ),
                exit = slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(500)
                ),
                modifier = modifier
            ) {
                content()
            }
        }
        
        TransitionType.DISSOLVE -> {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(1000)),
                exit = fadeOut(animationSpec = tween(1000)),
                modifier = modifier
            ) {
                content()
            }
        }
        
        TransitionType.BLUR -> {
            // 模糊效果需要自定义实现
            // 这里简化为淡入淡出
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(500)),
                exit = fadeOut(animationSpec = tween(500)),
                modifier = modifier
            ) {
                content()
            }
        }
        
        TransitionType.RANDOM -> {
            // 随机效果由调用方处理
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn() + slideInHorizontally(),
                exit = fadeOut() + slideOutHorizontally(),
                modifier = modifier
            ) {
                content()
            }
        }
    }
}
