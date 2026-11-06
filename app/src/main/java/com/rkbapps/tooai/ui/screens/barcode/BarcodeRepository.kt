package com.rkbapps.tooai.ui.screens.barcode

import com.rkbapps.tooai.db.dao.QrScanDao
import com.rkbapps.tooai.db.entity.QrScan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import javax.inject.Inject

class BarcodeRepository @Inject constructor(
    private val qrScanDao: QrScanDao,
) {

    private val _qrScanList = MutableStateFlow<List<QrScan>>(emptyList())
    val qrScanList: StateFlow<List<QrScan>> = _qrScanList

    // Returns the new row's id, or null when the insert failed.
    suspend fun addNewQrScan(qrScan: QrScan): Long? {
        return try {
            qrScanDao.newQrScan(qrScan)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteQrScan(qrScan: QrScan): Boolean {
        return try {
            qrScanDao.deleteQrScan(qrScan)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getAllQrScans() {
        try {
            val qrScans = qrScanDao.getAllQrScan()
            _qrScanList.emitAll(qrScans)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}