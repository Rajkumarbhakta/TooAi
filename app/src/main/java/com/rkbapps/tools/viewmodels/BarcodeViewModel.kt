package com.rkbapps.tools.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rkbapps.tools.db.QrScan
import com.rkbapps.tools.repository.BarcodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BarcodeViewModel @Inject constructor(private val repository: BarcodeRepository):ViewModel() {

    val qrScanList: StateFlow<List<QrScan>> = repository.qrScanList

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getAllQrScans()
        }
    }

    fun addNewQrScan(qrScan: QrScan){
        viewModelScope.async (Dispatchers.IO) {
            repository.addNewQrScan(qrScan)
        }.let {

        }
    }
}