package com.vidlynk.app.core.di

import com.vidlynk.app.data.repository.VideoRepository
import com.vidlynk.app.data.repository.VideoRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindVideoRepository(
        repositoryImpl: VideoRepositoryImpl
    ): VideoRepository
}
