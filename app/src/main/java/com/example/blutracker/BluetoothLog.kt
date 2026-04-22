package com.example.blutracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bluetooth_logs")
data class BluetoothLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deviceName: String,
    val action: String,
    val timestamp: Long,
    val latitude: Double? = null,
    val longitude: Double? = null
)