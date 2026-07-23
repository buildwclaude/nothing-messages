package com.buildwclaude.messages.di

import android.content.Context
import androidx.room.Room
import com.buildwclaude.messages.data.db.AppDatabase
import com.buildwclaude.messages.data.db.BlockedNumberDao
import com.buildwclaude.messages.data.db.ScheduledMessageDao
import com.buildwclaude.messages.data.db.ThreadSettingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "messages.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun scheduledDao(db: AppDatabase): ScheduledMessageDao = db.scheduledMessageDao()

    @Provides
    fun threadSettingDao(db: AppDatabase): ThreadSettingDao = db.threadSettingDao()

    @Provides
    fun blockedDao(db: AppDatabase): BlockedNumberDao = db.blockedNumberDao()
}
