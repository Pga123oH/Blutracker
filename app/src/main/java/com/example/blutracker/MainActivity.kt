package com.example.blutracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.blutracker.rssi.RssiTrackerScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

object Routes {
    const val DEVICES = "devices"
    const val TRACKER = "tracker"
}

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* results handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }

        setContent {
            val professionalDarkBlueScheme = darkColorScheme(
                primary = Color(0xFF1E88E5),
                background = Color(0xFF0F172A),
                surface = Color(0xFF1E293B),
                onPrimary = Color.White,
                onBackground = Color(0xFFF1F5F9),
                onSurface = Color(0xFFF1F5F9)
            )

            MaterialTheme(colorScheme = professionalDarkBlueScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BluTrackerApp()
                }
            }
        }
    }
}

@Composable
fun BluTrackerApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BluTrackerBottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DEVICES,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.DEVICES) {
                DevicesTab()
            }
            composable(Routes.TRACKER) {
                RssiTrackerScreen()
            }
        }
    }
}

@Composable
fun BluTrackerBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(
            selected = currentRoute == Routes.DEVICES,
            onClick = {
                navController.navigate(Routes.DEVICES) {
                    popUpTo(Routes.DEVICES) { inclusive = false }
                    launchSingleTop = true
                }
            },
            icon = { Text("📱", style = MaterialTheme.typography.titleMedium) },
            label = { Text("Devices") }
        )
        NavigationBarItem(
            selected = currentRoute == Routes.TRACKER,
            onClick = {
                navController.navigate(Routes.TRACKER) {
                    popUpTo(Routes.DEVICES) { inclusive = false }
                    launchSingleTop = true
                }
            },
            icon = { Text("📡", style = MaterialTheme.typography.titleMedium) },
            label = { Text("Tracker") }
        )
    }
}

@Composable
fun DevicesTab() {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val logList by db.bluetoothLogDao().getAllLogs().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    LogDashboard(
        logs = logList,
        onDeleteDevice = { deviceName ->
            coroutineScope.launch(Dispatchers.IO) {
                db.bluetoothLogDao().deleteDevice(deviceName)
            }
        }
    )
}
