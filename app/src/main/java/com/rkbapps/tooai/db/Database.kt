package com.rkbapps.tooai.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rkbapps.tooai.db.dao.QrScanDao
import com.rkbapps.tooai.db.dao.RecognizedTextDao
import com.rkbapps.tooai.db.entity.QrScan
import com.rkbapps.tooai.db.entity.RecognizedText

@Database(entities = [QrScan::class, RecognizedText::class], version = 1)
abstract class Database : RoomDatabase() {
    abstract fun qrScanDao(): QrScanDao
    abstract fun recognizedTextDao(): RecognizedTextDao
}