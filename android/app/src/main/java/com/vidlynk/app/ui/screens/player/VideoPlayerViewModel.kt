package com.vidlynk.app.ui.screens.player

import android.app.Activity
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidlynk.app.core.ads.AdMobManager
import com.vidlynk.app.data.model.VideoDto
import com.vidlynk.app.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    private val repository: VideoRepository,
    private val adMobManager: AdMobManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val videoSlug: String? = savedStateHandle.get<String>("slug")

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        videoSlug?.let { loadVideo(it) }
    }

    fun preloadAds(context: Context) {
        // Pre-load both pre-roll and exit interstitial ads simultaneously
        adMobManager.loadInterstitialAd(context)
    }

    fun loadVideo(slug: String) {
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            val savedPos = repository.getSavedPosition(slug)
            val result = repository.getVideoBySlug(slug)

            result.onSuccess { video ->
                // Move to PreRollAd state — wait for ad before playing
                _uiState.value = PlayerUiState.PreRollAd(
                    video = video,
                    initialPositionMs = savedPos
                )
            }.onFailure { error ->
                _uiState.value = PlayerUiState.Error(
                    message = error.localizedMessage ?: "Failed to load stream"
                )
            }
        }
    }

    /**
     * Called when the PreRollAd composable is ready to show the interstitial.
     * Shows the ad if loaded, then immediately transitions to Success (playback) state.
     */
    fun showPreRollAd(activity: Activity, video: VideoDto, initialPositionMs: Long) {
        adMobManager.showInterstitialAd(activity) {
            // After ad dismissed or if no ad ready → start video
            _uiState.value = PlayerUiState.Success(
                video = video,
                initialPositionMs = initialPositionMs
            )
            // Pre-load next ad (exit interstitial) in background
            adMobManager.loadInterstitialAd(activity)
        }
    }

    fun saveProgress(positionMs: Long) {
        val slug = videoSlug ?: return
        viewModelScope.launch {
            repository.updatePlaybackPosition(slug, positionMs)
        }
    }

    fun showExitAd(activity: Activity, onFinished: () -> Unit) {
        adMobManager.showInterstitialAd(activity, onFinished)
    }
}
