package com.rkbapps.tools.di

import android.content.Context
import androidx.room.Room
import com.rkbapps.tools.db.Database
import com.rkbapps.tools.db.QrScanDao
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
    fun provideRoomDatabase(@ApplicationContext context: Context): Database {
        return Room.databaseBuilder(
            context.applicationContext,
            Database::class.java,
            "app_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideQrScanDao(database: Database): QrScanDao {
        return database.qrScanDao()
    }

}