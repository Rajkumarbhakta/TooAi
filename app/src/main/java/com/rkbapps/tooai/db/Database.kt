package com.rkbapps.tooai.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rkbapps.tooai.db.dao.ChatDao
import com.rkbapps.tooai.db.dao.DocumentScansDao
import com.rkbapps.tooai.db.dao.LlmModelDao
import com.rkbapps.tooai.db.dao.QrScanDao
import com.rkbapps.tooai.db.dao.RecognizedTextDao
import com.rkbapps.tooai.db.entity.ChatMessage
import com.rkbapps.tooai.db.entity.ChatSession
import com.rkbapps.tooai.db.entity.DocumentScans
import com.rkbapps.tooai.db.entity.LlmModel
import com.rkbapps.tooai.db.entity.QrScan
import com.rkbapps.tooai.db.entity.RecognizedText

@Database(entities = [QrScan::class, RecognizedText::class, DocumentScans::class, LlmModel::class, ChatSession::class, ChatMessage::class], version = 3, exportSchema = false)
abstract class Database : RoomDatabase() {
    abstract fun qrScanDao(): QrScanDao
    abstract fun recognizedTextDao(): RecognizedTextDao
    
    abstract fun documentScanDao(): DocumentScansDao

    abstract fun llmModelDao(): LlmModelDao

    abstract fun chatDao(): ChatDao


    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `DocumentScans` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `path` TEXT NOT NULL, `title` TEXT NOT NULL, `timeMillis` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `LlmModel` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `displayName` TEXT NOT NULL, `sizeInBytes` INTEGER NOT NULL, `path` TEXT NOT NULL, `fileLocation` TEXT NOT NULL, `maxTokens` INTEGER NOT NULL, `topK` INTEGER NOT NULL, `topP` REAL NOT NULL, `temperature` REAL NOT NULL)")
            }
        }
        
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `chat_sessions` (`id` TEXT NOT NULL, `modelId` INTEGER NOT NULL, `title` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `chat_messages` (`id` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `sender` TEXT NOT NULL, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `timeToFirstToken` REAL, `prefillSpeed` REAL, `decodeSpeed` REAL, `totalLatency` REAL, `isError` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`sessionId`) REFERENCES `chat_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            }
        }
    }
}
