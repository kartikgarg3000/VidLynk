package com.vidlynk.app.ui.screens.home

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vidlynk.app.core.ads.BannerAdView
import com.vidlynk.app.data.local.WatchHistoryEntity
import com.vidlynk.app.data.model.VideoDto
import com.vidlynk.app.ui.theme.DarkSurfaceElevated
import com.vidlynk.app.ui.theme.DeepBlack
import com.vidlynk.app.ui.theme.ElectricPurple
import com.vidlynk.app.ui.theme.NeonCyan
import com.vidlynk.app.util.ShareUtils

@Composable
fun HomeScreen(
    incomingSlug: String? = null,
    onVideoSelected: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val historyList by viewModel.historyFlow.collectAsState(initial = emptyList())
    val incomingVideoState by viewModel.incomingVideoState.collectAsState()
    var inputUrl by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Preload ads immediately and handle incoming deep-link video
    LaunchedEffect(incomingSlug) {
        viewModel.preloadAds(context)
        viewModel.handleIncomingSlug(incomingSlug)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .statusBarsPadding()
    ) {
        // Scrollable Content Column
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 100.dp) // Room for floating ad at bottom
        ) {
            // App Header: VidLynk
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Vid",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Lynk",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonCyan
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NeonCyan.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "⚡ Instant Stream",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // ── Incoming Video Card (Triggered by Telegram / Web deep link) ─────────
            item {
                when (val state = incomingVideoState) {
                    is IncomingVideoState.Success -> {
                        IncomingVideoHeroCard(
                            video = state.video,
                            onOpenClick = { onVideoSelected(state.video.slug) },
                            onDismiss = { viewModel.clearIncomingVideo() }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    is IncomingVideoState.Loading -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Ready to stream...",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { onVideoSelected(state.slug) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DeepBlack)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("OPEN VIDEO NOW", color = DeepBlack, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    is IncomingVideoState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, Color.Red.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text("Video Ready to Play", color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Slug: ${state.slug}", color = Color.Gray, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { onVideoSelected(state.slug) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DeepBlack)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("OPEN VIDEO NOW", color = DeepBlack, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    IncomingVideoState.None -> {
                        // Manual Link Input Bar (Optional helper if user copied link manually)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                OutlinedTextField(
                                    value = inputUrl,
                                    onValueChange = { inputUrl = it },
                                    placeholder = { Text("Paste VidLynk link or slug…", color = Color.Gray) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonCyan,
                                        unfocusedBorderColor = Color.DarkGray,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        val slug = viewModel.extractSlug(inputUrl)
                                        if (slug.isNotEmpty()) {
                                            onVideoSelected(slug)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(46.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DeepBlack)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Play Stream", color = DeepBlack, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // ── Recently Watched Section Header ──────────────────────────────
            item {
                Text(
                    text = "Recently Watched Streams",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // ── History List ──────────────────────────────────────────────────
            if (historyList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No streams watched yet.\nTap any VidLynk link in Telegram or WhatsApp to start instant streaming!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(historyList, key = { it.slug }) { item ->
                    WatchHistoryCard(
                        item = item,
                        onClick = { onVideoSelected(item.slug) },
                        onShare = {
                            val shareUrl = "https://vidlynk-production.up.railway.app/watch/${item.slug}"
                            ShareUtils.shareVideoPost(
                                context = context,
                                title = item.title,
                                slug = item.slug,
                                shareUrl = shareUrl,
                                thumbnailUrl = item.thumbnailUrl,
                                coroutineScope = coroutineScope
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // ── Floating Banner Ad on Home Page ───────────────────────────────────
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .navigationBarsPadding(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated.copy(alpha = 0.96f)),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ADVERTISEMENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                BannerAdView()
            }
        }
    }
}

/**
 * High-Impact Hero Card rendered when a link is opened from Telegram / WhatsApp.
 * Captures user attention with the video title, status badge, and giant "Open Video" button.
 */
@Composable
fun IncomingVideoHeroCard(
    video: VideoDto,
    onOpenClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, NeonCyan),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonCyan.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = "⚡ VIDEO READY TO STREAM",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = video.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                lineHeight = 24.sp
            )

            if (!video.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = video.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Giant Prominent Action Button
            Button(
                onClick = onOpenClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = DeepBlack,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OPEN & WATCH VIDEO",
                    color = DeepBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun WatchHistoryCard(
    item: WatchHistoryEntity,
    onClick: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(NeonCyan.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NeonCyan)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = "Resume at: ${formatDuration(item.lastPositionMs)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
        IconButton(onClick = onShare) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share Post",
                tint = NeonCyan
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = millis / (1000 * 60 * 60)
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
