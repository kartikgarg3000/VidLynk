package com.vidlynk.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY watchedAtTimestamp DESC LIMIT 50")
    fun getAllHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE slug = :slug LIMIT 1")
    suspend fun getHistoryBySlug(slug: String): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHistory(entity: WatchHistoryEntity)

    @Query("UPDATE watch_history SET lastPositionMs = :positionMs, watchedAtTimestamp = :now WHERE slug = :slug")
    suspend fun updatePlaybackPosition(slug: String, positionMs: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM watch_history WHERE slug = :slug")
    suspend fun deleteHistory(slug: String)
}
