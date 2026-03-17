# Android Object Capture (GuidedCaptureSample Port)

This is a Kotlin/Jetpack Compose Android port of the iOS GuidedCaptureSample. It provides guided object capture, ARCore pose + depth integration, CameraX image/video capture, COLMAP export, and a modular architecture with Hilt + coroutines.

## Device Requirements
- Android device with ARCore support and Depth API support.
- Back-facing camera.
- Android 8.0+ (minSdk 26).

## Build & Run
1. Open the repo root in Android Studio (the Android project is at the repo root).
2. Sync Gradle.
3. Build and run on a physical ARCore-capable device.

## Capture Workflow
1. Tap Start Capture on the Capture screen.
2. Move around the object to collect frames. Auto selection uses pose delta + sharpness + depth + features.
3. Use Manual Capture if you need to override the auto selector.
4. Tap Review to inspect frames, delete poor frames, or export ZIP.
5. Tap Export for COLMAP to create `cameras.txt`, `images.txt`, and `image_list.txt`.
6. Run COLMAP on a workstation (see script below).
7. Convert the resulting mesh to GLB/GLTF if needed, then open it in the Viewer screen.

Guidance overlays will prompt you to move closer/farther if depth is outside the default range (0.4m–0.8m). The session score is computed from mean frame score and orbit coverage and surfaced as guidance messages.
Orbit coverage is visualized as a circular heatmap ring with missing sectors highlighted in red.
You can Save Draft at any time and Resume Draft to continue a prior session.
SharedCamera mode is enabled by default and CameraX is filtered to the ARCore camera ID for tighter pose/capture alignment.

## Architecture
Packages follow the required layout:
- `core/` app state machine, orbit coverage, feedback.
- `capture/` capture session + frame processing.
- `ar/` ARCore pose/depth utilities.
- `camera/` CameraX image/video controllers.
- `storage/` folder management + metadata JSON.
- `colmap/` cameras.txt/images.txt exporter.
- `ui/` Compose screens + components.
- `model/` shared data types.
- `di/` Hilt module + entry points.

See `ARCHITECTURE.md` for a full dataflow + COLMAP command reference.

## COLMAP Export
Export output is written under `.../captures/<timestamp>/Colmap/` with:
- `cameras.txt`
- `images.txt`
- `image_list.txt`
- `points3D.txt` placeholder

Each captured frame also writes `Metadata/frame_XXXXXX.json` with pose/intrinsics/depth and frame score details.
Video alignment is stored in `Metadata/video_sync.csv` (image timestamp → video timecode).
Draft state is saved as `draft.json` inside the session folder. ZIP export is written as `capture_export.zip` in the session root.

## Run COLMAP
Use the provided script:

```
./run_colmap_on_dataset.sh /path/to/dataset
```

The script runs the typical COLMAP pipeline on the exported dataset.

## COLMAP Commands (Manual)
If you prefer to run the steps manually:

```
colmap feature_extractor \
  --image_path /path/to/dataset/Images \
  --database_path /path/to/dataset/database.db \
  --image_list_path /path/to/dataset/Colmap/image_list.txt \
  --ImageReader.camera_model PINHOLE \
  --ImageReader.camera_params ""

colmap exhaustive_matcher --database_path /path/to/dataset/database.db

colmap mapper \
  --database_path /path/to/dataset/database.db \
  --image_path /path/to/dataset/Images \
  --output_path /path/to/dataset/sparse

colmap image_undistorter \
  --image_path /path/to/dataset/Images \
  --input_path /path/to/dataset/sparse/0 \
  --output_path /path/to/dataset/dense \
  --output_type COLMAP

colmap patch_match_stereo --workspace_path /path/to/dataset/dense --workspace_format COLMAP
colmap stereo_fusion --workspace_path /path/to/dataset/dense --workspace_format COLMAP --input_type geometric --output_path /path/to/dataset/dense/fused.ply
colmap poisson_mesher --input_path /path/to/dataset/dense/fused.ply --output_path /path/to/dataset/dense/mesh.ply
```

## Tests
Unit tests are located in:
- `app/src/test/java/com/yourorg/objectcapture/tests/PoseUtilsTest.kt`
- `app/src/test/java/com/yourorg/objectcapture/tests/ColmapExporterTest.kt`
- `app/src/test/java/com/yourorg/objectcapture/tests/FrameSelectorTest.kt`

Instrumentation harnesses (device-only) are located in:
- `app/src/androidTest/java/com/yourorg/objectcapture/tests/SharedCameraInstrumentationTest.kt`
- `app/src/androidTest/java/com/yourorg/objectcapture/tests/ArPlacementInstrumentationTest.kt`

Run tests from Android Studio or with Gradle:

```
./gradlew test
```

See `VALIDATION_CHECKLIST.md` for on-device validation steps (SharedCamera alignment, AR placement, depth guidance, orbit coverage).

## Viewer Notes
The built-in viewer uses Filament and supports GLB/GLTF files. OBJ/PLY should be converted to GLB (for example with Blender or assimp) before viewing. An AR Placement mode is available from the Viewer screen for in-world placement (requires device validation for camera background compositing).

## TODOs / Hardware-Specific Work
- Validate ARCore SharedCamera + CameraX synchronization on target devices (SharedCamera session is enabled, camera ID is matched).
- Validate AR placement accuracy and background compositing on target devices.
- Wire real intrinsics from Camera2 characteristics into COLMAP export.
- Use actual per-frame pose deltas and feature extraction in `CaptureController`.

## Backend + Web Viewer
This repo includes a simple FastAPI backend and a Three.js web viewer:

- Backend: `server/`
- Viewer: `viewer/index.html`

Update the Android base URL in `app/build.gradle.kts`:
`buildConfigField("String", "BASE_URL", "\"https://server.com\"")`

Run the backend locally:
```bash
cd server
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
export BASE_URL=http://localhost:8000
uvicorn app:app --host 0.0.0.0 --port 8000
```

Once reconstruction completes, the app opens:
`https://server.com/viewer?model_url=...`
