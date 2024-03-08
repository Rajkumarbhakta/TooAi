package com.rkbapps.tools.repository

import android.content.Context
import com.rkbapps.tools.db.QrScan
import com.rkbapps.tools.db.QrScanDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class BarcodeRepository @Inject constructor(
    private val qrScanDao: QrScanDao,
    @ApplicationContext context: Context
) {

    private val _qrScanList = MutableStateFlow<List<QrScan>>(emptyList())
    val qrScanList: StateFlow<List<QrScan>> = _qrScanList

    suspend fun addNewQrScan(qrScan: QrScan): Boolean {
        return try {
            val id = qrScanDao.newQrScan(qrScan)
            val new =qrScanDao.getQrScanById(id)
            val tempList: MutableList<QrScan> = ArrayList()
            tempList.addAll(_qrScanList.value)
            tempList.add(new)
            _qrScanList.emit(tempList)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getAllQrScans() {
        try {
            val qrScans = qrScanDao.getAllQrScan()
            _qrScanList.emit(qrScans)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}