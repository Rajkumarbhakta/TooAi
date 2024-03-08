package com.rkbapps.tools.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [QrScan::class], version = 1)
abstract class Database :RoomDatabase(){
    abstract fun qrScanDao():QrScanDao
}