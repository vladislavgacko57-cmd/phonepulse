package com.phonepulse.core.database.di

import android.content.Context
import androidx.room.Room
import com.phonepulse.core.database.PhonePulseDatabase
import com.phonepulse.core.database.dao.DiagnosticDao
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
    fun provideDatabase(@ApplicationContext context: Context): PhonePulseDatabase =
        Room.databaseBuilder(context, PhonePulseDatabase::class.java, "phonepulse.db")
            .build()

    @Provides
    fun provideDao(db: PhonePulseDatabase): DiagnosticDao = db.diagnosticDao()
}
