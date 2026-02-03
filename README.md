# Longlapse Capture

An Android app for creating long-term timelapses by capturing one photo per day. Photos are stored per project with optional daily reminders and a reference overlay to help keep framing consistent. You can export a sequence as an MP4 video.

## Features

- **Multiple projects** — Create separate timelapse projects, each with its own photos and settings.
- **Daily reminders** — Optional notification at a set time to take that day’s photo (with optional in-app prompt).
- **Reference overlay** — The first photo in a project can be shown as a semi-transparent overlay in the camera view to help align each new shot.
- **App-private storage** — Photos live in the app’s private directory and don’t clutter the device gallery.
- **Video export** — Encode all captures in a project into an MP4 (24 fps) using the device’s hardware encoder.

## Requirements

- **Android**: min SDK 26, target/compile SDK 35  
- **Camera** and **Post notifications** (Android 13+) permissions

## Build & run

1. Open the project in Android Studio (or use the command line).
2. Sync Gradle and build:
   ```bash
   ./gradlew assembleDebug
   ```
3. Install and run on a device or emulator:
   ```bash
   ./gradlew installDebug
   ```
   Or run the **Run** configuration from Android Studio.

## Tech stack

- **UI**: Jetpack Compose, Material 3  
- **Camera**: CameraX (preview + image capture)  
- **Persistence**: Room (projects + capture entries)  
- **Background**: WorkManager (daily reminder notifications)  
- **Images**: Coil 3 (reference overlay, list thumbnails)  
- **Export**: `MediaCodec` + `MediaMuxer` (H.264 MP4, no ffmpeg)

## Project structure

```
app/src/main/java/dev/ktown/longlapsecapture/
├── data/
│   ├── db/           # Room entities, DAOs, database
│   ├── repository/   # LonglapseRepository, ProjectWithStats
│   └── storage/      # PhotoStorage (app-private dirs)
├── di/               # ServiceLocator
├── export/           # TimelapseExporter (images → MP4)
├── reminder/         # ReminderScheduler, DailyReminderWorker
├── ui/
│   ├── screens/      # ProjectList, ProjectDetail, Camera
│   ├── theme/
│   └── LonglapseApp.kt
├── LonglapseApplication.kt
└── MainActivity.kt
```

## License

Private / unlicensed. Use as you like.
