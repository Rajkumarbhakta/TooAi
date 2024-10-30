package com.rkbapps.tooai.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rkbapps.tooai.db.entity.QrScan
import com.rkbapps.tooai.repository.BarcodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BarcodeViewModel @Inject constructor(private val repository: BarcodeRepository) :
    ViewModel() {

    val qrScanList: StateFlow<List<QrScan>> = repository.qrScanList


    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getAllQrScans()
        }
    }

    fun addNewQrScan(qrScan: QrScan) {
        viewModelScope.async(Dispatchers.IO) {
            repository.addNewQrScan(qrScan)
        }.let {

        }
    }

    fun deleteQrScan(qrScan: QrScan) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteQrScan(qrScan)
        }
    }
}