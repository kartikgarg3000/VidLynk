package com.vidlynk.app.data.repository

import com.vidlynk.app.data.local.WatchHistoryDao
import com.vidlynk.app.data.local.WatchHistoryEntity
import com.vidlynk.app.data.model.VideoDto
import com.vidlynk.app.data.remote.VidLynkApi
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepositoryImpl @Inject constructor(
    private val api: VidLynkApi,
    private val watchHistoryDao: WatchHistoryDao
) : VideoRepository {

    override suspend fun getVideoBySlug(slug: String): Result<VideoDto> {
        return try {
            val response = api.getVideoBySlug(slug)
            val video = response.data
            if (response.success && video != null) {
                // Cache into local database
                saveToHistory(video)
                Result.success(video)
            } else {
                // Try to fallback to offline local cache if exists
                val cached = watchHistoryDao.getHistoryBySlug(slug)
                if (cached != null) {
                    Result.success(cached.toVideoDto())
                } else {
                    Result.failure(Exception(response.message ?: "Video not found"))
                }
            }
        } catch (e: Exception) {
            // Check local fallback
            val cached = watchHistoryDao.getHistoryBySlug(slug)
            if (cached != null) {
                Result.success(cached.toVideoDto())
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun saveToHistory(video: VideoDto, startPositionMs: Long) {
        val existing = watchHistoryDao.getHistoryBySlug(video.slug)
        val entity = WatchHistoryEntity(
            slug = video.slug,
            videoId = video.id,
            title = video.title,
            description = video.description,
            videoUrl = video.videoUrl,
            thumbnailUrl = video.thumbnailUrl,
            durationSeconds = video.duration,
            lastPositionMs = existing?.lastPositionMs ?: startPositionMs,
            watchedAtTimestamp = System.currentTimeMillis()
        )
        watchHistoryDao.insertOrUpdateHistory(entity)
    }

    override suspend fun updatePlaybackPosition(slug: String, positionMs: Long) {
        watchHistoryDao.updatePlaybackPosition(slug, positionMs)
    }

    override suspend fun getSavedPosition(slug: String): Long {
        return watchHistoryDao.getHistoryBySlug(slug)?.lastPositionMs ?: 0L
    }

    override fun getWatchHistory(): Flow<List<WatchHistoryEntity>> {
        return watchHistoryDao.getAllHistory()
    }

    private fun WatchHistoryEntity.toVideoDto(): VideoDto {
        return VideoDto(
            id = this.videoId,
            slug = this.slug,
            title = this.title,
            description = this.description,
            duration = this.durationSeconds,
            thumbnailUrl = this.thumbnailUrl,
            videoUrl = this.videoUrl,
            mimeType = "video/mp4",
            resolution = "1080p",
            viewsCount = 0,
            createdAt = "",
            shareUrl = null
        )
    }
}
