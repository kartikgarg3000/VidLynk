package com.vidlynk.app.ui.screens.player

import com.vidlynk.app.data.model.VideoDto

sealed interface PlayerUiState {
    data object Loading : PlayerUiState

    /**
     * Video metadata resolved — waiting to show pre-roll interstitial ad.
     * Playback has NOT started yet.
     */
    data class PreRollAd(
        val video: VideoDto,
        val initialPositionMs: Long
    ) : PlayerUiState

    /**
     * Pre-roll ad shown (or skipped if unavailable) — video is ready to play.
     */
    data class Success(
        val video: VideoDto,
        val initialPositionMs: Long
    ) : PlayerUiState

    data class Error(val message: String) : PlayerUiState
}
