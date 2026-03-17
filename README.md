# FlamScan3D (Android)

FlamScan3D is an Android port of Apple’s GuidedCaptureSample with ARCore Depth, CameraX capture, COLMAP dataset export, and a Filament-based viewer. This repo contains **Android-only** sources and tooling.

## Highlights
- Guided capture with ARCore pose + Depth API
- Auto frame selection (sharpness, features, exposure stability, depth range)
- Orbit coverage heatmap + scan quality scoring
- COLMAP-compatible export (cameras.txt, images.txt)
- Filament viewer + AR placement mode

## Requirements
- Android Studio (latest stable)
- Android SDK 35 (Android 15)
- Device with ARCore + Depth support
  - Verified target: Samsung S25 Ultra (Android 15+)

## Quick Start
1. Open the project in Android Studio.
2. Sync Gradle.
3. Run:
   - `./gradlew test`
   - `./gradlew assembleDebug`
4. Install the APK from `app/build/outputs/apk/debug/app-debug.apk`.

## Docs
- `ANDROID_README.md` – build/run/test details and COLMAP workflow
- `ARCHITECTURE.md` – module design and data flow
- `VALIDATION_CHECKLIST.md` – verification steps

## Notes
- For release builds you’ll need a signing config. Debug builds are supported out of the box.
- The sample dataset lives in `sample_dataset/`.

