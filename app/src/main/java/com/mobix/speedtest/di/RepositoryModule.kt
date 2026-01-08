package com.mobix.speedtest.di

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideRepository(dao: HistoryDao): SpeedTestRepository =
        SpeedTestRepositoryImpl(dao)
}