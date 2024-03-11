package com.rkbapps.tools.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rkbapps.tools.db.dao.QrScanDao
import com.rkbapps.tools.db.dao.RecognizedTextDao
import com.rkbapps.tools.db.entity.QrScan
import com.rkbapps.tools.db.entity.RecognizedText

@Database(entities = [QrScan::class, RecognizedText::class], version = 1)
abstract class Database :RoomDatabase(){
    abstract fun qrScanDao(): QrScanDao
    abstract fun recognizedTextDao(): RecognizedTextDao
}