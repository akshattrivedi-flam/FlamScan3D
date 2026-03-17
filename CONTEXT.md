# ScanningObjectsUsingObjectCapture - Repo Context

## Summary
This repo is an iOS/iPadOS SwiftUI sample app that demonstrates a complete Object Capture workflow on device. It guides users through capture (object or area modes), provides onboarding and tutorial media, runs on-device photogrammetry reconstruction, and previews the generated USDZ model using AR Quick Look.

## Runtime Requirements
- Physical iPhone/iPad (no Simulator build).
- Device with LiDAR and A14 Bionic or later.
- iOS or iPadOS 18+.
Reference: `README.md`.

## App Entry Point
- `GuidedCaptureSample/GuidedCaptureSample/GuidedCaptureSampleApp.swift` defines the app entry, injects `AppDataModel.instance` into the environment, and sets `ContentView` as root.
- `GuidedCaptureSample/GuidedCaptureSample/Views/ContentView.swift` hosts `PrimaryView` and manages the idle timer.

## Core State Model
- `GuidedCaptureSample/GuidedCaptureSample/AppDataModel.swift`
  - Central observable model and state machine controlling capture, reconstruction, and viewing.
  - Holds `ObjectCaptureSession`, `PhotogrammetrySession`, and `CaptureFolderManager`.
  - Tracks `ModelState` and performs transitions in `performStateTransition`.
  - Listens to `ObjectCaptureSession` feedback and state updates via async tasks, pushing messages into `TimedMessageList`.
  - Manages tutorial behavior, overlay sheet pause/resume, and capture mode selection.
- `GuidedCaptureSample/GuidedCaptureSample/AppDataModel+Orbit.swift`
  - Defines the three orbit segments and utilities for UI display and ordering.

### ModelState Flow (High Level)
1. `.ready` -> `startNewCapture()` sets up session and folders.
2. `.capturing` while `ObjectCaptureSession` is active.
3. `.prepareToReconstruct` when capture completes (unless draft save).
4. `.reconstructing` during `PhotogrammetrySession.process`.
5. `.viewing` when USDZ is ready and shown.
6. `.completed` or `.restart` resets to start.
7. `.failed` shows error alert.

## Capture Storage
- `GuidedCaptureSample/GuidedCaptureSample/CaptureFolderManager.swift`
  - Creates a timestamped capture folder in Documents.
  - Subfolders: `Images/`, `Checkpoint/`, `Models/`.
  - Used for image capture input, reconstruction checkpoints, and USDZ output.

## Capture Modes
- Object mode: guided multi-orbit capture with flip recommendations and orbit tracking.
- Area mode: captures object plus surroundings; disables object masking for photogrammetry.
- Mode is toggled in the UI and stored in `AppDataModel.captureMode`.

## Onboarding / Review State Machine
- `GuidedCaptureSample/GuidedCaptureSample/OnboardingStateMachine.swift`
  - Defines `OnboardingState` and `OnboardingUserInput` with explicit transitions.
  - Used to drive review screens and guidance between scan passes.
- `GuidedCaptureSample/GuidedCaptureSample/Views/OnboardingView.swift`
  - Composes tutorial media, point cloud, and button actions for review.
- `GuidedCaptureSample/GuidedCaptureSample/Views/OnboardingTutorialView.swift`
  - Shows tutorial video or point cloud depending on state.
  - Displays localized guidance text and orbit progress icons.
- `GuidedCaptureSample/GuidedCaptureSample/Views/OnboardingButtonView.swift`
  - Renders review actions (Continue, Finish/Process, Skip, Flip, Save Draft).
  - Advances state machine, triggers new scan passes, and updates orbit/flip flags.
- Localized text sources for onboarding:
  - `GuidedCaptureSample/GuidedCaptureSample/OnboardingTutorialView+LocalizedString.swift`
  - `GuidedCaptureSample/GuidedCaptureSample/OnboardingButtonView+LocalizedString.swift`

## Capture UI
- `GuidedCaptureSample/GuidedCaptureSample/Views/CapturePrimaryView.swift`
  - Hosts `ObjectCaptureView` with overlay UI.
- `GuidedCaptureSample/GuidedCaptureSample/Views/CaptureOverlayView.swift`
  - Full-screen overlay with feedback, tutorial playback, and controls.
  - Shows tutorial on first capture, pauses session while video plays.
- `GuidedCaptureSample/GuidedCaptureSample/Views/TopOverlayButtons.swift`
  - Cancel, capture folders (gallery), or Next (review) button.
- `GuidedCaptureSample/GuidedCaptureSample/Views/BottomOverlayButtons.swift`
  - Capture start/continue, mode toggle, auto capture toggle, manual shot, image count, help, and reset box.
- `GuidedCaptureSample/GuidedCaptureSample/Views/FeedbackView.swift`
  - Displays active feedback messages.

## Reconstruction and Viewing
- `GuidedCaptureSample/GuidedCaptureSample/Views/ReconstructionPrimaryView.swift`
  - Drives photogrammetry processing, progress updates, and error handling.
  - Uses `UntilProcessingCompleteFilter` to terminate output stream after completion.
- `GuidedCaptureSample/GuidedCaptureSample/UntilProcessingCompleteFilter.swift`
  - Async sequence wrapper to end when processing completes or cancels.
- `GuidedCaptureSample/GuidedCaptureSample/Views/ProgressBarView.swift`
  - Shows reconstruction progress, stage, remaining time, and image count.
- `GuidedCaptureSample/GuidedCaptureSample/Views/ModelView.swift`
  - Presents USDZ model via AR Quick Look and returns to app on dismissal.

## Help and Tutorials
- `GuidedCaptureSample/GuidedCaptureSample/Views/HelpPageView.swift`
  - Tabbed help screens with capture types, capture tips, supported objects, and environment guidance.
- `GuidedCaptureSample/GuidedCaptureSample/Views/TutorialPageView.swift`
  - Shared layout for help pages.
- `GuidedCaptureSample/GuidedCaptureSample/Views/TutorialVideoView.swift`
  - Wraps a looping video player for tutorials.
- `GuidedCaptureSample/GuidedCaptureSample/Views/PlayerView.swift`
  - Uses `AVPlayer` with alpha masking to render instructional overlays.

## Feedback and Messaging
- `GuidedCaptureSample/GuidedCaptureSample/FeedbackMessages.swift`
  - Maps `ObjectCaptureSession.Feedback` to localized display strings.
- `GuidedCaptureSample/GuidedCaptureSample/TimedMessageList.swift`
  - Observable FIFO that shows feedback for a minimum duration, animating changes.

## Settings and Privacy
- `GuidedCaptureSample/Settings.bundle/Root.plist`
  - Adds the `show_tutorials` toggle used by `@AppStorage`.
- `GuidedCaptureSample/GuidedCaptureSample/PrivacyInfo.xcprivacy`
  - Declares use of UserDefaults category API for privacy manifest compliance.

## Project Notes
- `Configuration/SampleCode.xcconfig` supplies a bundle ID disambiguator for sample builds.
- `GuidedCaptureSample/GuidedCaptureSample/Info.plist` disables the scene manifest (`UIApplicationSceneManifest` false).

## Output Artifacts
- Capture images are stored under Documents/<timestamp>/Images/.
- Reconstruction output is written to Documents/<timestamp>/Models/model-mobile.usdz.
- Checkpoints stored under Documents/<timestamp>/Checkpoint/ and deleted after viewing starts.

