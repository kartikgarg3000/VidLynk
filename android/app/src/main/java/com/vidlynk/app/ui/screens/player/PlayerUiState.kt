package com.vidlynk.app.ui.screens.player

import com.vidlynk.app.data.model.VideoDto

sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data class Success(
        val video: VideoDto,
        val initialPositionMs: Long
    ) : PlayerUiState
    data class Error(val message: String) : PlayerUiState
}
