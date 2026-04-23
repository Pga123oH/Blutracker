package com.example.blutracker.rssi

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BleSignalScanner(private val context: Context) {

    private val bluetoothAdapter =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private val _rssiState = MutableStateFlow<Int?>(null)
    val rssiState: StateFlow<Int?> = _rssiState

    private val _statusState = MutableStateFlow(ScanStatus.IDLE)
    val statusState: StateFlow<ScanStatus> = _statusState

    private var targetMac: String? = null

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (result.device.address == targetMac) {
                _rssiState.value = result.rssi
            }
        }
        override fun onScanFailed(errorCode: Int) {
            _statusState.value = ScanStatus.ERROR
        }
    }

    fun startScanning(macAddress: String) {
        targetMac = macAddress

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _statusState.value = ScanStatus.BLUETOOTH_OFF
            return
        }

        if (!hasBleScanPermission()) {
            _statusState.value = ScanStatus.MISSING_PERMISSION
            return
        }

        val filters = listOf(
            ScanFilter.Builder().setDeviceAddress(macAddress).build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        try {
            @Suppress("MissingPermission")
            bluetoothAdapter.bluetoothLeScanner?.startScan(filters, settings, leScanCallback)
            _statusState.value = ScanStatus.SCANNING
        } catch (e: SecurityException) {
            _statusState.value = ScanStatus.MISSING_PERMISSION
        }
    }

    fun stopScanning() {
        if (!hasBleScanPermission()) {
            _statusState.value = ScanStatus.IDLE
            _rssiState.value = null
            return
        }
        try {
            @Suppress("MissingPermission")
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(leScanCallback)
        } catch (e: SecurityException) {

        }
        _statusState.value = ScanStatus.IDLE
        _rssiState.value = null
    }

    private fun hasBleScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}

enum class ScanStatus {
    IDLE,
    SCANNING,
    BLUETOOTH_OFF,
    MISSING_PERMISSION,
    ERROR
}
