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
