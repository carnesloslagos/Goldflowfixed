# GoldFlow Android

Android prototype for Binance XAUUSDT order-flow monitoring.

## Included
- Binance Futures public WebSocket
- XAUUSDT aggregated trades
- Delta
- CVD
- Depth 20 order book
- Bid/ask volume balance
- CVD chart
- Basic LONG / SHORT / ESPERAR display

## GitHub build
Upload the CONTENTS of this folder to the ROOT of a GitHub repository.

Then:
1. Open the repository's Actions tab.
2. Select "Build Android APK".
3. Click "Run workflow".
4. Open the completed run.
5. Download the artifact "GoldFlow-debug-apk".
6. Extract it and install `app-debug.apk` on Android.

GitHub Actions installs Gradle 8.9 automatically, so this GitHub build does not require a Gradle wrapper.

## Android Studio
Open the folder in Android Studio and use Build -> Build APK(s).

This prototype does not execute trades.
