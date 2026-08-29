# Gloo Wall Tapper

Production-ready Android application that provides advanced macro automation for games featuring a gloo wall button. Uses Accessibility Services and WindowManager overlays to deliver seamless, background-optimized tap automation.

## Features

- **Seamless Onboarding**: Automatically checks and redirects to SYSTEM_ALERT_WINDOW and Accessibility Service settings.
- **Main UI**: Clean, modern screen with a large ON/OFF toggle switch.
- **Customization Settings**: Change colors and resize both floating bubbles (Button 1 and Button 2), plus adjust tap interval.
- **Floating Window Logic**: Two draggable overlay bubbles spawned via WindowManager.
- **Button 1 (The Tapper)**: Single quick tap triggers one accessibility tap; press-and-hold triggers rapid continuous taps at the exact coordinate.
- **Button 2 (The Disabler)**: While physically held, completely disables Button 1's continuous tapping; releasing instantly restores it.
- **Boot persistence**: Optionally restarts the service after device reboot.

## Project Structure

```
app/
  src/main/
    java/com/gloowalltapper/
      SplashActivity.kt          - Onboarding
      MainActivity.kt            - Main UI, toggle, customization
      AccessibilityService.kt    - Android Accessibility Service for gesture dispatch
      FloatingWindowService.kt   - WindowManager overlay bubbles
      TapEngine.kt               - Core tap automation logic (single + continuous)
      BootReceiver.kt            - Restores service on boot
    res/
      layout/
        activity_splash.xml
        activity_main.xml
      values/
        strings.xml, colors.xml, styles.xml, dimens.xml
      drawable/
        toggle_selector.xml, bubble_selector.xml, ic_launcher.xml
      xml/
        accessibility_service_config.xml
        backup_rules.xml, data_extraction_rules.xml
    AndroidManifest.xml
    proguard-rules.pro
```

## Tech Stack

- Kotlin + Coroutines
- AndroidX (AppCompat, Material 3, ConstraintLayout)
- WindowManager TYPE_APPLICATION_OVERLAY
- AccessibilityService dispatchGesture

## Permissions

- `SYSTEM_ALERT_WINDOW` - Draw floating bubbles over other apps
- `BIND_ACCESSIBILITY_SERVICE` - Simulate taps via accessibility gestures
- `POST_NOTIFICATIONS` - Foreground service notification
- `FOREGROUND_SERVICE` - Keep overlay service alive

## Building

Requires Android SDK 34 and Gradle 8.2+.

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

## Note

This app requires the user to manually enable Display over other apps and Accessibility Service in Android Settings. The app automatically redirects to the correct settings pages.
