package com.vidlynk.app.ui.screens.player

import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.vidlynk.app.core.ads.BannerAdView
import com.vidlynk.app.ui.theme.DeepBlack
import com.vidlynk.app.ui.theme.NeonCyan

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    exoPlayer: ExoPlayer,
    viewModel: VideoPlayerViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    var isBuffering by remember { mutableStateOf(false) }

    // Preload interstitial ads
    LaunchedEffect(Unit) {
        viewModel.preloadAds(context)
    }

    // Intercept back button to show AdMob interstitial before exit
    BackHandler {
        viewModel.saveProgress(exoPlayer.currentPosition)
        exoPlayer.pause()
        if (activity != null) {
            viewModel.showExitAd(activity) {
                onNavigateBack()
            }
        } else {
            onNavigateBack()
        }
    }

    // Sync lifecycle events to pause player and save playback position
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.saveProgress(exoPlayer.currentPosition)
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    // Do not auto-play if ended
                    if (exoPlayer.playbackState != Player.STATE_ENDED) {
                        exoPlayer.play()
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    viewModel.saveProgress(exoPlayer.currentPosition)
                    exoPlayer.release()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Listen to player buffering state
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        when (val state = uiState) {
            is PlayerUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NeonCyan)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Connecting to stream…", color = Color.White)
                    }
                }
            }

            is PlayerUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onNavigateBack() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = DeepBlack)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Go Back / Retry", color = DeepBlack)
                        }
                    }
                }
            }

            is PlayerUiState.Success -> {
                // Initialize ExoPlayer media item once
                LaunchedEffect(state.video.videoUrl) {
                    val mediaItem = MediaItem.fromUri(state.video.videoUrl)
                    exoPlayer.setMediaItem(mediaItem)
                    if (state.initialPositionMs > 0) {
                        exoPlayer.seekTo(state.initialPositionMs)
                    }
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Video player viewport
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = true
                                    setShowNextButton(false)
                                    setShowPreviousButton(false)
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Top Gradient Overlay with Title and Back Arrow
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(onClick = {
                                    viewModel.saveProgress(exoPlayer.currentPosition)
                                    exoPlayer.pause()
                                    if (activity != null) {
                                        viewModel.showExitAd(activity) { onNavigateBack() }
                                    } else {
                                        onNavigateBack()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = state.video.title,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        // Buffering Overlay
                        AnimatedVisibility(
                            visible = isBuffering,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            CircularProgressIndicator(color = NeonCyan)
                        }
                    }

                    // Bottom AdMob Monetization Banner
                    BannerAdView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeepBlack)
                    )
                }
            }
        }
    }
}
