# HomeApps release build

Release signing is configured entirely through environment variables so private keys and passwords never enter the repository.

```bash
export HOMEAPPS_KEYSTORE=/absolute/path/to/homeapps.jks
export HOMEAPPS_KEYSTORE_PASSWORD='...'
export HOMEAPPS_KEY_ALIAS='homeapps'
export HOMEAPPS_KEY_PASSWORD='...'
./gradlew bundleRelease
```

The signed bundle is written to `app/build/outputs/bundle/release/`.

Without these variables, debug builds continue to work normally and the release build remains unsigned.

## Safe device testing

Instrumentation tests use the isolated `com.punishdave.homeapps.uitest` package. This prevents
`connectedAndroidTest` from clearing the settings belonging to the everyday HomeApps installation.

Before installing an APK manually, verify the currently installed version and update in place:

```bash
adb shell dumpsys package com.punishdave.homeapps | grep -E 'versionCode|versionName'
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Do not uninstall the package to resolve a signing mismatch without first exporting an encrypted
settings backup from Settings. Uninstalling removes both DataStore files and the Android Keystore key.
