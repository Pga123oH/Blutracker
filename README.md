#  BluTracker
A simple Android app that remembers where you left your Bluetooth devices. 

If your headphones disconnect, run out of battery, or you walk out of range, BluTracker quietly wakes up in the background, grabs your current GPS location, and saves it. That way, you always know exactly where you last had them.

##  Features
* **Instant Logging:** Drops a GPS pin the exact second a Bluetooth device disconnects.
* **Notifications:** Sends a quick alert to your phone so you realize you left your device behind before you get too far.
* **100% Offline & Private:** Everything is saved locally on your phone using a Room database. There is no cloud sync, no accounts, and no weird tracking. 
* **Simple UI:** A clean, dark-mode dashboard that shows your devices and gives you a 1-tap button to open their last known location in Google Maps.
* **Home Screen Widget:** A handy toggle widget so you can turn background tracking off when you're just hanging out at home.

##  How It Works
1. Give the app the required location and notification permissions.
2. Connect your Bluetooth headphones or speaker normally.
3. When they disconnect, the app's background receiver triggers, grabs a single GPS coordinate, and saves it to the database.
4. Open the app whenever you need to check the map and find your stuff!

##  Requirements for Developers
* **Android Studio:** Panda 4 (2025.3.4) or newer
* **Kotlin:** 1.9.0+
* **Minimum SDK:** API 24 (Android 7.0)
* **Target SDK:** API 34 (Android 14)
* **Dependencies:** Jetpack Compose, Room SQLite, Google Play Services Location

##  A Note on Privacy
Because the app needs to track where your headphones disconnect while your phone is asleep in your pocket, it requires the **Background Location** permission. However, the app doesn't even request internet permissions. Your location data never leaves your device.
