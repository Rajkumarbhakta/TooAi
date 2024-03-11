package com.rkbapps.tools.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.rkbapps.tools.db.entity.QrScan
import kotlinx.coroutines.flow.Flow

@Dao
interface QrScanDao {

    @Insert
    suspend fun newQrScan(qrScan: QrScan): Long

    @Delete
    suspend fun deleteQrScan(qrScan: QrScan)

    @Update
    suspend fun updateQrScan(qrScan: QrScan)

    @Query("select * from qrscan")
    fun getAllQrScan(): Flow<List<QrScan>>

    @Query("select * from qrscan where id=:id")
    suspend fun getQrScanById(id: kotlin.Long): QrScan


}