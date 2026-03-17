# Android Object Capture – Architecture

## Overview
This project is a Kotlin + Jetpack Compose Android port of the iOS GuidedCaptureSample. It implements guided object capture with ARCore pose + depth, CameraX capture, COLMAP export, and a Filament-based viewer.

## Modules / Packages
- `core/` central state + guidance
  - `AppStateManager` controls app phases (READY → CAPTURING → REVIEWING → RECONSTRUCTING → VIEWING_MODEL → COMPLETED).
  - `OrbitManager` tracks azimuth coverage and recommends elevation bands.
  - `SessionScoreManager` computes scan quality.
  - `CaptureMetricsStore` exposes depth + scoring to UI.
- `capture/` capture pipeline
  - `CaptureController` orchestrates ARCore + CameraX, frame selection, metadata writing, and guidance.
  - `FrameProcessor` computes sharpness and selection scores.
- `ar/` ARCore utilities
  - `ARCoreManager` drives `Session.update()`, depth stats, and hit testing for anchor selection.
  - `PoseUtils` converts ARCore pose to COLMAP extrinsics.
- `camera/` CameraX pipeline
  - `CameraController` binds preview + analysis + image capture + video.
  - `VideoRecorder` records MP4 and provides timecode mapping.
- `storage/` persistence helpers
  - `CaptureFolderManager` builds `Documents/captures/<timestamp>` directories.
  - `MetadataWriter` writes JSON per frame.
  - `DraftManager` supports save/resume draft state.
  - `ZipUtils` exports dataset ZIP.
- `colmap/` dataset export
  - `ColmapExporter` writes `cameras.txt` / `images.txt` / `image_list.txt`.
- `ui/` Compose screens + components
  - `CaptureScreen`, `ReviewScreen`, `ReconstructionScreen`, `ViewerScreen`.

## Capture Flow
1. User taps **Start Capture**.
2. `AppStateManager` creates a session folder and starts `CaptureController`.
3. CameraX produces frames → `FrameProcessor` scores → accepted frames are saved.
4. Metadata JSON is written, along with `video_sync.csv` mapping image timestamps to video timecodes.
5. Orbit coverage and session score are updated and surfaced as guidance.
6. User taps **Review** → can delete frames, export COLMAP, or export ZIP.

## Drafts
Use **Save Draft** to persist capture progress (`draft.json`). **Resume Draft** restores the session folder and orbit coverage.

## COLMAP Export
Files written under `.../captures/<timestamp>/Colmap/`:
- `cameras.txt`
- `images.txt`
- `image_list.txt`
- `points3D.txt` (placeholder)

## Run COLMAP (Exact Commands)
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

## Viewer
The Viewer screen loads GLB/GLTF via Filament. Convert OBJ/PLY to GLB if needed.
An AR Placement mode is available to place the GLB/GLTF in the world using ARCore hit tests.
