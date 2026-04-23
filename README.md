#  BluTracker
A simple Android app that remembers where you left your Bluetooth devices. 


##  Features
* **Instant Logging:** Drops a GPS pin the exact second a Bluetooth device disconnects.
* **Notifications:** Sends a quick alert to your phone so you realize you left your device behind before you get too far.
* **Offline:** Everything is saved locally on your phone using a Room database. There is no cloud sync and no accounts.
* **Home Screen Widget:** widget so you can turn background tracking on and off.
*  **Live Signal Tracker:** Uses Bluetooth RSSI (Signal Strength) to estimate how far away you are from a device. 
* **Haptic Beep Engine:** The closer you get, the faster the phone beeps and vibrates. It’s like a high-tech game of "Hot or Cold" for your headphones.

##  How It Works
1. Give the app the required location and notification permissions.
2. Connect your Bluetooth headphones or speaker normally.
3. When they disconnect, the app's background receiver triggers, grabs a single GPS coordinate, and saves it to the database.
4. Open the app whenever you need to check the map and find your stuff!
5. Once you are on the apps, click the trackker button to locate your devices.

## How to Find Your Device
1. Open the app to see your device's last location
2. Hold your phone at chest level (portrait orientation)
3. *Your body blocks Bluetooth signals from one side* — use this as a natural 180° reflector
4. Slowly spin in place
5. The app emits *haptic feedback and beeping* that intensifies as you face the direction of your device
6. The strongest signal tells you which way to go

##  Requirements for Developers
* **Android Studio:** Panda 4 (2025.3.4) or newer
* **Kotlin:** 1.9.0+
* **Minimum SDK:** API 24 (Android 7.0)
* **Target SDK:** API 34 (Android 14)
* **Dependencies:** Jetpack Compose, Room SQLite, Google Play Services Location
* **Jetpack Compose** 
* **Vibrator Manager API** 
* **SoundPool API** 

##  A Note on Privacy
Because the app needs to track where your headphones disconnect while your phone is asleep in your pocket, it requires the **Background Location** permission. However, the app doesn't  request internet permissions. Your location data never leaves your device.

## Device Compatibility

BluTracker works best with devices that consistently report signal strength (RSSI):

*Works well:*
- Bluetooth headphones (AirPods, Sony, JBL, etc.)
- Wireless speakers

*Limited support:*
- Smart watches (BLE devices)
- Some fitness trackers with privacy features
- Devices using BLE privacy mode

*Workaround:* These devices still log their last known location when they disconnect, 
So you can see where you left them — just don't use the direction-finding feature.

## FAQ

*Q: Does BluTracker use my location all the time?*
A: No. It only grabs your GPS location when a Bluetooth device disconnects. 
   Otherwise it's idle. This keeps battery usage minimal.

*Q: Why does it need background location permission?*
A: Because when you walk away from your headphones, your phone is likely locked 
   in your pocket. The app needs permission to run in the background and capture 
   that GPS location while your screen is off.

*Q: How accurate is the GPS?*
A: Typically 5-20 meters in open areas. Indoors or in cities with tall buildings, 
   accuracy drops to 20-100m. It's good enough to know "which room" or "which 
   building," not pixel-perfect.

*Q: Does my data go to a server?*
A: No. Everything stays on your phone in a local SQLite database. This is why 
   there's no cloud sync or account needed.

*Q: Can I export my location history?*
A: Not yet. You can see all saved locations in the app's map view.

*Q: Does it work with all Bluetooth devices?*
A: Yes, anything that properly signals disconnection (headphones, speakers, 
   smartwatches, etc.). It listens for the system Bluetooth disconnect event.

*Q: Can I track other people's devices?*
A: No, only devices paired with your phone. The app has no network capability.
