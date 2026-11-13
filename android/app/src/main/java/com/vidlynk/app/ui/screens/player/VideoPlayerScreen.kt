package com.vidlynk.app.ui.screens.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.vidlynk.app.ui.screens.player.components.DoubleTapSeekIndicator
import com.vidlynk.app.ui.screens.player.components.PlaybackSpeedBottomSheet
import com.vidlynk.app.ui.screens.player.components.PlayerControlsOverlay
import com.vidlynk.app.ui.screens.player.components.SeekDirection
import com.vidlynk.app.ui.screens.player.components.SeekFeedback
import com.vidlynk.app.ui.theme.DeepBlack
import com.vidlynk.app.ui.theme.ElectricPurple
import com.vidlynk.app.ui.theme.NeonCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    exoPlayer: ExoPlayer,
    viewModel: VideoPlayerViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Player state
    var isBuffering by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isEnded by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var bufferedPositionMs by remember { mutableLongStateOf(0L) }

    // UX enhancements state
    var areControlsVisible by remember { mutableStateOf(true) }
    var controlsTimeoutKey by remember { mutableIntStateOf(0) }
    var isSpeedSheetVisible by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isZoomMode by remember { mutableStateOf(false) }
    var seekFeedback by remember { mutableStateOf<SeekFeedback?>(null) }

    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    // Pre-load interstitial ad as soon as screen opens
    LaunchedEffect(Unit) {
        viewModel.preloadAds(context)
    }

    // Pre-Roll Ad Handler
    LaunchedEffect(uiState) {
        if (uiState is PlayerUiState.PreRollAd) {
            val state = uiState as PlayerUiState.PreRollAd
            if (activity != null) {
                viewModel.showPreRollAd(activity, state.video, state.initialPositionMs)
            } else {
                viewModel.showPreRollAd(context as Activity, state.video, state.initialPositionMs)
            }
        }
    }

    // Auto-hide controls timer (3.5 seconds)
    LaunchedEffect(areControlsVisible, isPlaying, controlsTimeoutKey) {
        if (areControlsVisible && isPlaying && !isSpeedSheetVisible) {
            delay(3500)
            areControlsVisible = false
        }
    }

    // Auto-clear double tap seek feedback after 750ms
    LaunchedEffect(seekFeedback) {
        if (seekFeedback != null) {
            delay(750)
            seekFeedback = null
        }
    }

    // Periodic position tracking coroutine
    LaunchedEffect(uiState, isPlaying) {
        while (isActive) {
            currentPositionMs = exoPlayer.currentPosition
            durationMs = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
            bufferedPositionMs = exoPlayer.bufferedPosition
            delay(500)
        }
    }

    // System Bar insets management based on orientation
    LaunchedEffect(isLandscape) {
        if (activity != null) {
            val window = activity.window
            val insetsController = WindowCompat.getInsetsController(window, view)
            if (isLandscape) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Clean restore on screen exit
    DisposableEffect(Unit) {
        onDispose {
            if (activity != null) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                val window = activity.window
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Intercept back button: exit landscape first if rotated, otherwise show exit ad
    BackHandler {
        if (isLandscape && activity != null) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            viewModel.saveProgress(exoPlayer.currentPosition)
            exoPlayer.pause()
            if (activity != null) {
                viewModel.showExitAd(activity) { onNavigateBack() }
            } else {
                onNavigateBack()
            }
        }
    }

    // Sync ExoPlayer with Activity Lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.saveProgress(exoPlayer.currentPosition)
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (exoPlayer.playbackState != Player.STATE_ENDED) {
                        exoPlayer.play()
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    viewModel.saveProgress(exoPlayer.currentPosition)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Listen to ExoPlayer playback events
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                isEnded = playbackState == Player.STATE_ENDED
                if (isEnded) {
                    areControlsVisible = true
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        when (val state = uiState) {

            // ── Loading ─────────────────────────────────────────────────────
            is PlayerUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NeonCyan)
                        Spacer(Modifier.height(16.dp))
                        Text("Fetching stream…", color = Color.White)
                    }
                }
            }

            // ── Pre-Roll Ad Loading ──────────────────────────────────────────
            is PlayerUiState.PreRollAd -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        CircularProgressIndicator(color = ElectricPurple)
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = "⚡ VidLynk",
                            color = NeonCyan,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Loading your stream…",
                            color = Color.White,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = state.video.title,
                            color = Color.Gray,
                            fontSize = 13.sp,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ── Error ───────────────────────────────────────────────────────
            is PlayerUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { onNavigateBack() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = DeepBlack)
                            Spacer(Modifier.width(8.dp))
                            Text("Go Back / Retry", color = DeepBlack)
                        }
                    }
                }
            }

            // ── Success: Interactive Video Player ───────────────────────────
            is PlayerUiState.Success -> {
                // Initialize media item once
                LaunchedEffect(state.video.videoUrl) {
                    val mediaItem = MediaItem.fromUri(state.video.videoUrl)
                    exoPlayer.setMediaItem(mediaItem)
                    if (state.initialPositionMs > 0) {
                        exoPlayer.seekTo(state.initialPositionMs)
                    }
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                }

                // Update resize mode on player view when toggled
                LaunchedEffect(isZoomMode) {
                    playerViewRef?.resizeMode = if (isZoomMode) {
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    } else {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // ExoPlayer viewport (useController = false so Compose handles all controls)
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                resizeMode = if (isZoomMode) {
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                } else {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                                }
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                playerViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Touch gesture layer: Single tap toggles controls, Double tap seeks ±10s
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        areControlsVisible = !areControlsVisible
                                        controlsTimeoutKey++
                                    },
                                    onDoubleTap = { offset ->
                                        val width = size.width
                                        if (offset.x < width * 0.45f) {
                                            // Left double tap: Rewind 10 seconds
                                            val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                            exoPlayer.seekTo(newPos)
                                            currentPositionMs = newPos
                                            seekFeedback = SeekFeedback(SeekDirection.LEFT, 10)
                                        } else if (offset.x > width * 0.55f) {
                                            // Right double tap: Forward 10 seconds
                                            val dur = if (exoPlayer.duration > 0) exoPlayer.duration else Long.MAX_VALUE
                                            val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(dur)
                                            exoPlayer.seekTo(newPos)
                                            currentPositionMs = newPos
                                            seekFeedback = SeekFeedback(SeekDirection.RIGHT, 10)
                                        }
                                        controlsTimeoutKey++
                                    }
                                )
                            }
                    )

                    // Double-tap Seek visual indicator (Ripple icon + text)
                    DoubleTapSeekIndicator(seekFeedback = seekFeedback)

                    // Buffering Spinner
                    if (isBuffering) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // Compose-driven Player Controls Overlay
                    PlayerControlsOverlay(
                        isVisible = areControlsVisible,
                        isPlaying = isPlaying,
                        isEnded = isEnded,
                        title = state.video.title,
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                        playbackSpeed = playbackSpeed,
                        isZoomMode = isZoomMode,
                        isLandscape = isLandscape,
                        onPlayPauseToggle = {
                            if (isEnded) {
                                exoPlayer.seekTo(0)
                                exoPlayer.play()
                            } else if (isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                            controlsTimeoutKey++
                        },
                        onSeekTo = { newPosition ->
                            exoPlayer.seekTo(newPosition)
                            currentPositionMs = newPosition
                            controlsTimeoutKey++
                        },
                        onSeekBack10 = {
                            val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                            currentPositionMs = newPos
                            seekFeedback = SeekFeedback(SeekDirection.LEFT, 10)
                            controlsTimeoutKey++
                        },
                        onSeekForward10 = {
                            val dur = if (exoPlayer.duration > 0) exoPlayer.duration else Long.MAX_VALUE
                            val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(dur)
                            exoPlayer.seekTo(newPos)
                            currentPositionMs = newPos
                            seekFeedback = SeekFeedback(SeekDirection.RIGHT, 10)
                            controlsTimeoutKey++
                        },
                        onSpeedClick = {
                            isSpeedSheetVisible = true
                        },
                        onAspectRatioToggle = {
                            isZoomMode = !isZoomMode
                            controlsTimeoutKey++
                        },
                        onFullscreenToggle = {
                            if (activity != null) {
                                activity.requestedOrientation = if (isLandscape) {
                                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                }
                            }
                            controlsTimeoutKey++
                        },
                        onBackClick = {
                            if (isLandscape && activity != null) {
                                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            } else {
                                viewModel.saveProgress(exoPlayer.currentPosition)
                                exoPlayer.pause()
                                if (activity != null) {
                                    viewModel.showExitAd(activity) { onNavigateBack() }
                                } else {
                                    onNavigateBack()
                                }
                            }
                        }
                    )

                    // Playback Speed Selector BottomSheet
                    if (isSpeedSheetVisible) {
                        PlaybackSpeedBottomSheet(
                            currentSpeed = playbackSpeed,
                            onSpeedSelected = { speed ->
                                playbackSpeed = speed
                                exoPlayer.setPlaybackSpeed(speed)
                                isSpeedSheetVisible = false
                                controlsTimeoutKey++
                            },
                            onDismissRequest = {
                                isSpeedSheetVisible = false
                                controlsTimeoutKey++
                            }
                        )
                    }
                }
            }
        }
    }
}
