#  BluTracker
A simple Android app that remembers where you left your Bluetooth devices. 


##  Features
* **Instant Logging:** Drops a GPS pin the exact second a Bluetooth device disconnects.
* **Notifications:** Sends a quick alert to your phone so you realize you left your device behind before you get too far.
* **Offline:** Everything is saved locally on your phone using a Room database. There is no cloud sync and no accounts.
* **Home Screen Widget:** widget so you can turn background tracking on and off.
* * **Live Signal Tracker:** Uses Bluetooth RSSI (Signal Strength) to estimate how far away you are from a device. 
* **Haptic Beep Engine:** The closer you get, the faster the phone beeps and vibrates. It’s like a high-tech game of "Hot or Cold" for your headphones.

##  How It Works
1. Give the app the required location and notification permissions.
2. Connect your Bluetooth headphones or speaker normally.
3. When they disconnect, the app's background receiver triggers, grabs a single GPS coordinate, and saves it to the database.
4. Open the app whenever you need to check the map and find your stuff!
5. Once you are on the apps, click the trackker button to locate your devices.

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
