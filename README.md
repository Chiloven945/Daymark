# Daymark

A local-first countdown, count-up, and anniversary app for Android, built with Jetpack Compose and
Material 3 Expressive.

## Status

M0–M5 are implemented:

- Home, Events, Calendar, Settings
- Event detail and editor
- Room + DataStore
- Hilt + MVVM + StateFlow
- Recurrence-aware date engine
- AlarmManager reminders and notifications
- JSON export/import

Widgets and the Nothing OS theme layer are planned for M6/M7.

## Build

Requirements:

- JDK 21
- Android SDK 37
- Android Studio with AGP 9 support

```bash
./gradlew test :app:assembleDebug
```

The debug APK is at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Package

```text
top.chiloven.daymark
```

## Documentation

- [Roadmap](docs/ROADMAP.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Nothing OS 5 UI research](docs/nothing-os-5-ui-research.md)
