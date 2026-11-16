package com.vidlynk.app.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidlynk.app.core.ads.AdMobManager
import com.vidlynk.app.data.local.WatchHistoryEntity
import com.vidlynk.app.data.model.VideoDto
import com.vidlynk.app.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface IncomingVideoState {
    data object None : IncomingVideoState
    data class Loading(val slug: String) : IncomingVideoState
    data class Success(val video: VideoDto) : IncomingVideoState
    data class Error(val slug: String, val message: String) : IncomingVideoState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: VideoRepository,
    private val adMobManager: AdMobManager
) : ViewModel() {

    val historyFlow: Flow<List<WatchHistoryEntity>> = repository.getWatchHistory()

    private val _incomingVideoState = MutableStateFlow<IncomingVideoState>(IncomingVideoState.None)
    val incomingVideoState: StateFlow<IncomingVideoState> = _incomingVideoState.asStateFlow()

    fun preloadAds(context: Context) {
        adMobManager.loadInterstitialAd(context)
    }

    fun handleIncomingSlug(slug: String?) {
        val cleanSlug = slug?.trim()
        if (cleanSlug.isNullOrEmpty()) {
            _incomingVideoState.value = IncomingVideoState.None
            return
        }

        // Avoid re-fetching if already loaded for the same slug
        val current = _incomingVideoState.value
        if (current is IncomingVideoState.Success && current.video.slug == cleanSlug) {
            return
        }

        viewModelScope.launch {
            _incomingVideoState.value = IncomingVideoState.Loading(cleanSlug)
            val result = repository.getVideoBySlug(cleanSlug)
            result.onSuccess { video ->
                _incomingVideoState.value = IncomingVideoState.Success(video)
            }.onFailure { err ->
                _incomingVideoState.value = IncomingVideoState.Error(
                    slug = cleanSlug,
                    message = err.localizedMessage ?: "Stream info unavailable"
                )
            }
        }
    }

    fun clearIncomingVideo() {
        _incomingVideoState.value = IncomingVideoState.None
    }

    fun extractSlug(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""

        // Matches https://vidlynk.app/watch/{slug} or vidlynk://watch/{slug} or plain slug
        return if (trimmed.contains("/")) {
            trimmed.substringAfterLast("/")
        } else {
            trimmed
        }
    }
}
