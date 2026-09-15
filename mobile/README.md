# CareerPulse Mobile Container (Capacitor for iOS & Android)

This directory contains the mobile packaging container for **CareerPulse**, built with **Ionic Capacitor**. It wraps the responsive CareerPulse web application into native iOS (`.ipa` / Xcode) and Android (`.apk`, `.aab` / Android Studio) applications.

## How It Works
- **Remote Webview Architecture**: Points directly to the live CareerPulse server. Any updates deployed to the backend or web application are immediately available in the mobile app without requiring app store resubmissions.
- **Native Device Features**: Includes native status bar styling, custom splash screen, network connectivity monitoring, and safe-area notch handling.
- **Hardware Back Button**: Integrates with Android's native back button to dismiss open modal sheets before exiting.

## Prerequisites
- Node.js 18+
- For Android: Android Studio with Android SDK 34+
- For iOS: macOS with Xcode 15+ and CocoaPods

## Quick Start

1. Install Capacitor dependencies:
   ```bash
   cd mobile
   npm install
   ```

2. Add native platforms:
   ```bash
   # Add Android Studio project
   npx cap add android

   # Add iOS Xcode project (macOS only)
   npx cap add ios
   ```

3. Sync configuration and plugins:
   ```bash
   npx cap sync
   ```

4. Launch in Android Studio or Xcode:
   ```bash
   # Open Android Studio
   npx cap open android

   # Open Xcode
   npx cap open ios
   ```

## Development URL Configuration
In `capacitor.config.json`, the `server.url` property controls what the native container loads:
- **Android Emulator**: Use `http://10.0.2.2:8080` (points to your host machine's localhost).
- **Physical Device**: Use your local machine's LAN IP (e.g. `http://192.168.1.100:8080`).
- **Production**: Point to your deployed server URL (e.g. `https://careerpulse.app`).
