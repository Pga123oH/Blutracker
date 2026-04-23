package com.example.blutracker.rssi

import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.blutracker.AppDatabase
import com.example.blutracker.BluetoothLog
import kotlin.math.pow

@Composable
fun RssiTrackerScreen(
    viewModel: RssiTrackerViewModel = viewModel()
) {
    val context = LocalContext.current
    val rssi by viewModel.rssi.collectAsStateWithLifecycle()
    val status by viewModel.scanStatus.collectAsStateWithLifecycle()
    val activeDevice by viewModel.activeDeviceName.collectAsStateWithLifecycle()

    val db = remember { AppDatabase.getDatabase(context) }
    val logs by db.bluetoothLogDao().getAllLogs().collectAsState(initial = emptyList())

    val uniqueDevices = remember(logs) {
        logs.sortedByDescending { it.timestamp }.distinctBy { it.deviceName }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.pauseTracking() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Signal Tracker",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (status == ScanStatus.SCANNING || status == ScanStatus.ERROR) {
            ActiveTrackerPanel(
                deviceName = activeDevice ?: "Unknown Device",
                rssi = rssi,
                status = status,
                onStop = { viewModel.pauseTracking() }
            )
        }

        if (status == ScanStatus.BLUETOOTH_OFF || status == ScanStatus.MISSING_PERMISSION) {
            StatusBanner(status = status)
        }

        Text(
            text = if (uniqueDevices.isEmpty()) "No saved devices yet."
            else "Tap a device to start tracking:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uniqueDevices, key = { it.deviceName }) { log ->
                DeviceTrackCard(
                    log = log,
                    isActive = activeDevice == log.deviceName && status == ScanStatus.SCANNING,
                    onTrack = { mac -> viewModel.startTracking(mac, log.deviceName) }
                )
            }
        }
    }
}

@Composable
private fun ActiveTrackerPanel(
    deviceName: String,
    rssi: Int?,
    status: ScanStatus,
    onStop: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = deviceName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            SignalBarMeter(rssi = rssi)

            Text(
                text = rssi?.let { "$it dBm" } ?: "-- dBm",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = rssi?.let { distanceLabel(it) } ?: "Searching…",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = rssi?.let { beepHint(it) } ?: "🔍 Looking for device…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            OutlinedButton(
                onClick = onStop,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("■ Stop Tracking")
            }
        }
    }
}

@Composable
private fun DeviceTrackCard(
    log: BluetoothLog,
    isActive: Boolean,
    onTrack: (String) -> Unit
) {
    val context = LocalContext.current
    val macAddress = remember(log.deviceName) {
        resolveMacFromName(context, log.deviceName) ?: log.deviceName
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isActive) 6.dp else 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.deviceName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isActive) "● Tracking now"
                    else "Last seen: ${formatTimestamp(log.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isActive) Color(0xFF4CAF50)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (!isActive) {
                Button(
                    onClick = { onTrack(macAddress) },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Track")
                }
            }
        }
    }
}

@Composable
private fun SignalBarMeter(rssi: Int?) {
    val totalBars = 8
    val filledBars = rssi?.let { r ->
        val clamped = r.coerceIn(-100, -40)
        ((clamped + 100).toFloat() / 60f * totalBars).toInt().coerceIn(0, totalBars)
    } ?: 0

    val barColor by animateColorAsState(
        targetValue = when {
            filledBars >= 6 -> Color(0xFF4CAF50)
            filledBars >= 3 -> Color(0xFFFFC107)
            else            -> Color(0xFFF44336)
        },
        animationSpec = tween(300),
        label = "barColor"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(60.dp)
    ) {
        for (i in 1..totalBars) {
            val heightFraction by animateFloatAsState(
                targetValue = if (i <= filledBars) 1f else 0.2f,
                animationSpec = tween(200),
                label = "bar$i"
            )
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .fillMaxHeight(heightFraction)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(
                        if (i <= filledBars) barColor
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}

@Composable
private fun StatusBanner(status: ScanStatus) {
    val (text, color) = when (status) {
        ScanStatus.BLUETOOTH_OFF      -> "Bluetooth is turned OFF" to MaterialTheme.colorScheme.error
        ScanStatus.MISSING_PERMISSION -> "Missing Bluetooth Scan permission" to MaterialTheme.colorScheme.error
        else                          -> return
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            color = color,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun rssiToDistance(rssi: Int, txPower: Int = -59, n: Double = 2.5): Double =
    10.0.pow((txPower - rssi).toDouble() / (10.0 * n))

private fun distanceLabel(rssi: Int): String {
    val d = rssiToDistance(rssi)
    return when {
        d < 1.0  -> "< 1 m away"
        d < 10.0 -> "≈ %.1f m away".format(d)
        else     -> "≈ %.0f m away".format(d)
    }
}

private fun beepHint(rssi: Int): String {
    val d = rssiToDistance(rssi)
    return when {
        d < 1.5  -> " very close!"
        d < 4.0  -> "getting warmer"
        d < 8.0  -> " medium range"
        else     -> " device is far"
    }
}

private fun formatTimestamp(ts: Long): String {
    val sdf = java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(ts))
}

private fun resolveMacFromName(context: Context, deviceName: String): String? {
    return try {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        @Suppress("MissingPermission")
        manager.adapter?.bondedDevices?.firstOrNull { it.name == deviceName }?.address
    } catch (e: SecurityException) {
        null
    }
}
