package com.rkbapps.tooai.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rkbapps.tooai.db.dao.DocumentScansDao
import com.rkbapps.tooai.db.dao.LlmModelDao
import com.rkbapps.tooai.db.dao.QrScanDao
import com.rkbapps.tooai.db.dao.RecognizedTextDao
import com.rkbapps.tooai.db.entity.DocumentScans
import com.rkbapps.tooai.db.entity.LlmModel
import com.rkbapps.tooai.db.entity.QrScan
import com.rkbapps.tooai.db.entity.RecognizedText

@Database(entities = [QrScan::class, RecognizedText::class, DocumentScans::class, LlmModel::class], version = 2, exportSchema = false)
abstract class Database : RoomDatabase() {
    abstract fun qrScanDao(): QrScanDao
    abstract fun recognizedTextDao(): RecognizedTextDao
    
    abstract fun documentScanDao(): DocumentScansDao

    abstract fun llmModelDao(): LlmModelDao


    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `DocumentScans` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `path` TEXT NOT NULL, `title` TEXT NOT NULL, `timeMillis` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `LlmModel` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `displayName` TEXT NOT NULL, `sizeInBytes` INTEGER NOT NULL, `path` TEXT NOT NULL, `fileLocation` TEXT NOT NULL, `maxTokens` INTEGER NOT NULL, `topK` INTEGER NOT NULL, `topP` REAL NOT NULL, `temperature` REAL NOT NULL)")
            }
        }
    }
}