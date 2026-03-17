# Device Validation Checklist

This checklist is for on-device validation of SharedCamera alignment and AR placement.

## Preconditions
- ARCore-supported device with Depth API.
- Grant Camera and Microphone permissions.
- Use a stable, well-lit environment and a textured object.

## SharedCamera + CameraX Alignment
1. Launch the app and tap **Start Capture**.
2. Move slowly in a small arc; ensure tracking remains **TRACKING**.
3. Verify image timestamps are written to `Metadata/video_sync.csv`.
4. Verify `video_sync.csv` timecodes increase monotonically.
5. Open the recorded video (`Video/scan_video.mp4`) and spot-check frames against still images.
6. If drift is noticeable, adjust `VideoRecorder` timebase or switch to hardware timestamps.

## Depth Guidance
1. Move the device within 0.4–0.8 meters of the object.
2. Confirm the distance overlay changes from red to green.
3. Move closer (<0.4 m) and confirm auto-capture stops.
4. Move farther (>0.8 m) and confirm auto-capture stops.

## Orbit Heatmap + Guidance
1. Capture a full 360° pass at mid elevation.
2. Confirm the orbit ring shows filled sectors.
3. Confirm missing sectors remain red.
4. Observe band suggestions (Low/Mid/High) as you switch elevation.

## Draft Save / Resume
1. Tap **Save Draft** during capture.
2. Force-close the app.
3. Relaunch and tap **Resume Draft**.
4. Verify capture folder and orbit coverage are restored.

## Review + Export
1. Delete a few frames in Review and re-export.
2. Verify `images.txt` and `image_list.txt` reflect current images.
3. Export ZIP and confirm it includes Images, Metadata, Video, and Colmap.

## AR Placement
1. Export/convert a model to GLB and place it in `Models/`.
2. Open Viewer and tap **AR Placement**.
3. Tap a plane to place the model.
4. Validate scale/orientation and stability.

