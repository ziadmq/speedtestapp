package com.mobix.speedtest.di

import com.mobix.speedtest.data.repository.SpeedTestRepositoryImpl
import com.mobix.speedtest.domain.repository.SpeedTestRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideRepository(): SpeedTestRepository =
        SpeedTestRepositoryImpl() // تم إزالة الـ dao نهائياً
}