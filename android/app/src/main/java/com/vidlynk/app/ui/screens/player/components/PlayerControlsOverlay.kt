package com.vidlynk.app.ui.screens.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidlynk.app.ui.theme.DeepBlack
import com.vidlynk.app.ui.theme.NeonCyan

@Composable
fun PlayerControlsOverlay(
    isVisible: Boolean,
    isPlaying: Boolean,
    isEnded: Boolean,
    title: String,
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    playbackSpeed: Float,
    isZoomMode: Boolean,
    isLandscape: Boolean,
    onPlayPauseToggle: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBack10: () -> Unit,
    onSeekForward10: () -> Unit,
    onSpeedClick: () -> Unit,
    onAspectRatioToggle: () -> Unit,
    onFullscreenToggle: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isUserScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
        ) {
            // Top Bar Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    // Aspect Ratio (Fit / Zoom) toggle
                    IconButton(onClick = onAspectRatioToggle) {
                        Icon(
                            imageVector = if (isZoomMode) Icons.Default.FitScreen else Icons.Default.CropFree,
                            contentDescription = if (isZoomMode) "Fit to screen" else "Zoom to fill",
                            tint = if (isZoomMode) NeonCyan else Color.White
                        )
                    }

                    // Playback speed pill
                    Surface(
                        onClick = onSpeedClick,
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                    ) {
                        Text(
                            text = if (playbackSpeed == 1.0f) "1x" else "${playbackSpeed}x",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Center Play / Pause / Replay + 10s buttons
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(36.dp)
            ) {
                // Seek Back 10s
                IconButton(
                    onClick = onSeekBack10,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Seek back 10s",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Play / Pause / Replay
                IconButton(
                    onClick = onPlayPauseToggle,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(NeonCyan)
                ) {
                    Icon(
                        imageVector = when {
                            isEnded -> Icons.Default.Replay
                            isPlaying -> Icons.Default.Pause
                            else -> Icons.Default.PlayArrow
                        },
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = DeepBlack,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Seek Forward 10s
                IconButton(
                    onClick = onSeekForward10,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Seek forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Bottom Bar Overlay: Scrubber Timeline, Times, Fullscreen toggle
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Seekbar Slider
                val displayPosition = if (isUserScrubbing) scrubPositionMs else currentPositionMs
                val maxRange = if (durationMs > 0) durationMs.toFloat() else 1f
                val sliderValue = displayPosition.toFloat().coerceIn(0f, maxRange)

                Slider(
                    value = sliderValue,
                    onValueChange = { newValue ->
                        isUserScrubbing = true
                        scrubPositionMs = newValue.toLong()
                    },
                    onValueChangeFinished = {
                        isUserScrubbing = false
                        onSeekTo(scrubPositionMs)
                    },
                    valueRange = 0f..maxRange,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                )

                // Time labels + Fullscreen button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatTime(displayPosition),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = " / ",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                        Text(
                            text = formatTime(durationMs),
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                    }

                    // Fullscreen / Landscape toggle
                    IconButton(
                        onClick = onFullscreenToggle,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isLandscape) "Exit fullscreen" else "Fullscreen",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

