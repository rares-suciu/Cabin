# Cabin

A native Android offline read-it-later app.

## Building Locally
1. Clone the repository.
2. Ensure you have Android Studio or Gradle installed.
3. Run `./gradlew assembleDebug` to build the app.

## Downloading and Sideloading
1. Go to the [Releases page](https://github.com/rares-suciu/Cabin/releases) on GitHub.
2. Download the `app-debug.apk` file from the latest release.
3. Transfer the APK to your Android device.
4. On your device, open a file manager, tap the APK, and select "Install". Ensure you have allowed installation from unknown sources in your device settings.

## Adding to Feedly
1. Open an article in Feedly.
2. Tap the Share button.
3. Select "Cabin" from the share sheet.
4. The article will be saved for offline reading.

## Verifying Offline Functionality
1. Share an article from Feedly to Cabin.
2. Wait for the article to download (a toast message will appear, and you can check the "Flight Readiness" screen for status).
3. Enable Airplane Mode on your device.
4. Open the Cabin app.
5. Tap the downloaded article. You should be able to read the full text and view images without an internet connection.
6. Use the search bar to find articles offline.

## Adding a Real Signing Key
To build a signed release APK, you will need a valid keystore.
1. Generate a keystore using Android Studio or `keytool`.
2. Update `app/build.gradle.kts` to configure the `signingConfigs` and `buildTypes` for release.

## Known Limitations
* Paywalls and heavily JS-rendered pages may not extract correctly.
* No sync or account support.
* Some complex layouts might render differently offline.
