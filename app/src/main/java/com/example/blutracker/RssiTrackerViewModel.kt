package com.example.blutracker.rssi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch



class RssiTrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner     = BleSignalScanner(application)
    private val beepEngine  = RssiBeepEngine(application)


    val rssi: StateFlow<Int?> = scanner.rssiState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)


    val scanStatus: StateFlow<ScanStatus> = scanner.statusState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScanStatus.IDLE)


    private val _activeDeviceName = MutableStateFlow<String?>(null)
    val activeDeviceName: StateFlow<String?> = _activeDeviceName

    // Forward every RSSI update to the beep engine automatically
    init {
        viewModelScope.launch {
            scanner.rssiState.collect { rssi ->
                rssi?.let { beepEngine.updateRssi(it) }
            }
        }
    }


    fun startTracking(macAddress: String, deviceName: String = macAddress) {
        _activeDeviceName.value = deviceName
        scanner.startScanning(macAddress)
        beepEngine.start()
    }


    fun pauseTracking() {
        scanner.stopScanning()
        beepEngine.stop()
        _activeDeviceName.value = null
    }

    override fun onCleared() {
        super.onCleared()
        scanner.stopScanning()
        beepEngine.stop()
    }
}
