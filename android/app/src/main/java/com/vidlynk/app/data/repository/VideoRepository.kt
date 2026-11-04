package com.vidlynk.app.data.repository

import com.vidlynk.app.data.local.WatchHistoryEntity
import com.vidlynk.app.data.model.VideoDto
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    suspend fun getVideoBySlug(slug: String): Result<VideoDto>
    suspend fun saveToHistory(video: VideoDto, startPositionMs: Long = 0L)
    suspend fun updatePlaybackPosition(slug: String, positionMs: Long)
    suspend fun getSavedPosition(slug: String): Long
    fun getWatchHistory(): Flow<List<WatchHistoryEntity>>
}
