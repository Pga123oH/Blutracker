package com.example.blutracker

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BluetoothReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {


        val prefs = context.getSharedPreferences("BluTrackerPrefs", Context.MODE_PRIVATE)
        val isTracking = prefs.getBoolean("isTrackingEnabled", true)

        if (!isTracking) {
            Log.d("BluetoothLog", "Widget is OFF. Ignoring Bluetooth event.")
            return
        }
        // ----------------------------------

        val action = intent.action
        val pendingResult = goAsync()

        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

        val hasBluetoothPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }

        val deviceName = if (hasBluetoothPermission) device?.name ?: "Unknown Device" else "Unknown Device"
        val currentTime = System.currentTimeMillis()

        if (action == BluetoothDevice.ACTION_ACL_CONNECTED) {
            Log.d("BluetoothLog", "Device Connected: $deviceName")
            saveToDatabase(context, deviceName, "Connected", currentTime, null, null, pendingResult)
        }
        else if (action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
            Log.d("BluetoothLog", "Device Disconnected: $deviceName")

            val hasLocationPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasLocationPermission) {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        val lat = location?.latitude
                        val lon = location?.longitude
                        saveToDatabase(context, deviceName, "Disconnected", currentTime, lat, lon, pendingResult)
                        sendNotification(context, deviceName) // Trigger the alarm!
                    }
                    .addOnFailureListener {
                        saveToDatabase(context, deviceName, "Disconnected", currentTime, null, null, pendingResult)
                        sendNotification(context, deviceName) // Trigger the alarm even if GPS fails!
                    }
            } else {
                saveToDatabase(context, deviceName, "Disconnected", currentTime, null, null, pendingResult)
                sendNotification(context, deviceName)
            }
        }
    }

    private fun saveToDatabase(context: Context, deviceName: String, action: String, time: Long, lat: Double?, lon: Double?, pendingResult: PendingResult) {
        val newLog = BluetoothLog(deviceName = deviceName, action = action, timestamp = time, latitude = lat, longitude = lon)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.getDatabase(context).bluetoothLogDao().insertLog(newLog)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun sendNotification(context: Context, deviceName: String) {
        val channelId = "bluetooth_alerts"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Bluetooth Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Device Disconnected!")
            .setContentText("Your $deviceName was left behind. Location saved.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}