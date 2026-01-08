package com.mobix.speedtest.di

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context) =
        Room.databaseBuilder(context, AppDatabase::class.java, "mobix_db").build()

    @Provides
    fun provideDao(db: AppDatabase) = db.historyDao()
}