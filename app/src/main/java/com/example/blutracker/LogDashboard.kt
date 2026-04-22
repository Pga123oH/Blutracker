package com.example.blutracker

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogDashboard(logs: List<BluetoothLog>) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())


    val latestLogs = logs.groupBy { it.deviceName }.map { it.value.first() }

    if (latestLogs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No devices disconnected yet!\n\nTurn off a connected Bluetooth device to record your first location.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(32.dp)
            )
        }
        return
    }

    LazyColumn(modifier = Modifier.padding(16.dp)) {

        items(latestLogs) { log ->
            Card(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = log.deviceName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(text = "Status: ${log.action}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Time: ${dateFormat.format(Date(log.timestamp))}", style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(12.dp))

                    if (log.latitude != null && log.longitude != null) {
                        Button(
                            onClick = {

                                val mapsUrl = "https://www.google.com/maps/search/?api=1&query=${log.latitude},${log.longitude}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open in Google Maps")
                        }
                    } else {
                        Text(
                            text = "No GPS Data Available for this event.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}