package com.vidlynk.app.ui.screens.home

import androidx.lifecycle.ViewModel
import com.vidlynk.app.data.local.WatchHistoryEntity
import com.vidlynk.app.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: VideoRepository
) : ViewModel() {

    val historyFlow: Flow<List<WatchHistoryEntity>> = repository.getWatchHistory()

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
