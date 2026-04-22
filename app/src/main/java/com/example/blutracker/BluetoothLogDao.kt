package com.example.blutracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow // NEW: Imports the Live Feed tool

@Dao
interface BluetoothLogDao {

    @Insert
    fun insertLog(log: BluetoothLog)


    @Query("SELECT * FROM bluetooth_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<BluetoothLog>>
}