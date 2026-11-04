package com.vidlynk.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey
    val slug: String,
    val videoId: String,
    val title: String,
    val description: String?,
    val videoUrl: String,
    val thumbnailUrl: String?,
    val durationSeconds: Int,
    val lastPositionMs: Long = 0L,
    val watchedAtTimestamp: Long = System.currentTimeMillis()
)
