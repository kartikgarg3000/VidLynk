package com.vidlynk.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WatchHistoryEntity::class], version = 1, exportSchema = false)
abstract class VidLynkDatabase : RoomDatabase() {
    abstract fun watchHistoryDao(): WatchHistoryDao
}
