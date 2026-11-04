package com.vidlynk.app.core.di

import android.content.Context
import androidx.room.Room
import com.vidlynk.app.data.local.VidLynkDatabase
import com.vidlynk.app.data.local.WatchHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VidLynkDatabase {
        return Room.databaseBuilder(
            context,
            VidLynkDatabase::class.java,
            "vidlynk.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideWatchHistoryDao(database: VidLynkDatabase): WatchHistoryDao {
        return database.watchHistoryDao()
    }
}
