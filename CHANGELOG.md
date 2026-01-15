# Changelog

All notable changes to the "El Ojo Del Abuelo" project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v3.3.1] - 2026-01-15
### Fixed
- **Orientation**: Forced application to `landscape` mode in Manifest to correct vertical display issue on Hardware Preview.

## [v3.3.0] - 2026-01-15 (Arquitectura "Zero CPU")
### Added
- **Hardware Preview**: Implemented direct `SurfaceView` binding for the Camera. The preview stream now bypasses the CPU completely, rendering directly to the screen overlay.
- **Hybrid Mode**: Implemented a "Buffer Refill" mechanism (`addCallbackBuffer` + `stop/start` cycle) to ensure that Software Processing (Motion Detection / Web Stream) works simultaneously with the Hardware Preview.
- **Legacy Support**: Added `SURFACE_TYPE_PUSH_BUFFERS` to ensure compatibility with Android 2.3 (Gingerbread) drivers.

### Changed
- **Architecture**: Removed all `BitmapFactory` and `ImageView` logic from `MainActivity`. The UI is now purely a passive viewport for the Camera Hardware.
- **Performance**: Drastic reduction in CPU usage during preview (approx. -60% load), eliminating UI lag and thermal throttling.

## [v3.2.0] - 2026-01-13
### Added
- **Active Monitor**: The device now wakes up instantly when motion is detected.
- **Max Brightness**: Screen brightness is forced to 100% during recording for clear monitoring.
- **Zero-Copy Preview**: Implemented Hardware-Direct video preview for consistent 30 FPS without CPU overhead.
- **Auto-Sleep**: Screen turns off automatically 1 second after recording stops to save battery.

## [v3.1.1] - 2026-01-13
### Changed
- **UX**: Detailed Settings Modal now has vertical scrolling (`max-height: 85vh`), improving usability on small screens.

## [v3.1.0] - 2026-01-13
### Added
- **Auto Storage Management**: "Circular Buffer" feature. The app now automatically deletes old videos when disk space is low (configurable, default <500MB).
- **Settings**: New option to configure "Minimum Free Space" in the settings modal.

## [v3.0.14] - 2026-01-12
### Added
- **Live View Modal**: Replaced raw stream tab with a proper in-app modal containing the Persistent Header (Battery/Temp) and "Close" button.

## [v3.0.13] - 2026-01-12
### Fixed
- **SOLVED**: Definitive removal of duplicated garbage code in JS generation.

## [v3.0.12] - 2026-01-12
### Fixed
- **URGENT**: Removed Java source code leaking into JavaScript output, which caused full UI paralysis.

## [v3.0.11] - 2026-01-12
### Fixed
- **Critical**: Resolved UI unresponsiveness caused by malformed HTML/CSS injection.
- **Protocol**: Enforced strict version bumping for every deployment to ensure cache busting.

## [v3.0.10] - 2026-01-12
### Improved
- **UX**: Persistent Status Header (Battery, Temp, Storage) now visible inside Video Player and Live View.
- **Code**: Refactored HTML generation to use DRY principles and class-based DOM updates for synchronized status.

## [v3.0.9] - 2026-01-12
### Improved
- **UI**: Dashboard video list now displays formatted Date (DD/MM/YYYY), Time (HH:mm:ss), and FPS instead of raw filenames.
- **UI**: File Size and Duration metadata are now displayed in **bold** for better readability.

## [v3.0.8] - 2026-01-12
### Added
- **UX**: Visual indicator (dimming) for watched videos in the current session.
- **UX**: Added metadata to video cards: File size (MB/KB) and Duration (calculated from timestamp).

## [v3.0.7] - 2026-01-12
### Fixed
- **UI**: Fixed a critical bug where the header (Title/Settings) would disappear on mobile devices when the video library loaded. Implemented strict CSS Flexbox containment.

## [v3.0.6] - 2026-01-12
### Added
- **HUD**: Implemented a "Calibration HUD" overlay on the video player. Displays real-time Zoom (x), Pan X, and Pan Y values during gestures to assist with setting defaults.

## [v3.0.5] - 2026-01-12
### Added
- **UI**: Added "Close" (X) button to Settings Modal header.
- **Storage**: Added "Delete All Videos" button in Settings (Danger Zone).
- **UX**: Added "Default View" preferences (Zoom, Pan X, Pan Y) that persist across restarts.
- **Docs**: Updated Walkthrough with new features.

## [v3.0.4] - 2026-01-11
### Fixed
- **Ghost Reset Bug**: Fixed an annoyance where releasing a pinch-zoom gesture quickly would accidentally trigger the "Double-Tap Reset" logic, snapping the video back to 1x size. The system now intelligently ignores "taps" that occur immediately after a multi-touch gesture.

## [v3.0.3] - 2026-01-11
### Added
- **Double-Tap Reset**: Implemented a "Quick Reset" gesture. Double-tapping the video instantly resets zoom and position to default.

### Changed
- **UX Refinement**:
    - **Smart Pan Constraint**: Disabled panning when the image is at original scale (1x) to prevent dragging the image into empty space.
    - **Auto-Center**: When pinching out to minimum zoom (1x), the image now automatically snaps to center (0,0), ensuring a clean reset.

## [v3.0.2] - 2026-01-11
### Fixed
- **Pan Logic Error**: Fixed a critical bug where `dy` (vertical delta) was undefined in the `touchmove` handler, causing the script to crash silently when attempting to pan the image with one finger.

## [v3.0.1] - 2026-01-11
### Fixed
- **Pan Locking**: Removed the artificial restriction that prevented panning unless `zoom > 1`. Now panning is always active when using one finger, ensuring smooth navigation even at near-1x zoom levels or after gesture switches.

## [v3.0.0] - 2026-01-11
### Major Update
- **Interactive Pan & Zoom**: Final, polished implementation of the video player zoom.
  - Removed all debug overlays and temporary logging.
  - Implemented persistent zoom state during playback.
  - Optimized for iOS Safari (iPhone 15 Pro) and Android 2.3 compatibility.

## [v2.9.9] - 2026-01-11
### Fixed
- **Zoom Reset Bug**: Removed critical logic error where `resetZoom()` was called on every frame update, preventing zoom from persisting during playback. Zoom state is now preserved while the video plays.

## [v2.9.8] - 2026-01-11
### Fixed
- **iOS Zoom Fix**: Added `{ passive: false }` to touch event listeners. This is mandatory for modern iOS Safari to respect `preventDefault()` and allow custom zoom logic without the browser interfering.

## [v2.9.7] - 2026-01-11
### Fixed
- **Zoom Jitter**: Aggressively blocked default browser touch events to prevent the native zoom from fighting with the custom pinch-to-zoom logic, eliminating the "trembling" effect.

## [v2.9.6] - 2026-01-11
### Fixed
- **JS Runtime Error**: Fixed `ReferenceError: ratio is not defined` in the zoom logic which was silently crashing the touch handler. Zoom should now be functional.

## [v2.9.5] - 2026-01-11
### Debugging
- **On-Screen Console**: Retrying injection of the debug overlay, as it failed to appear in v2.9.4.

## [v2.9.4] - 2026-01-11
### Debugging
- **On-Screen Console**: Added a temporary debug overlay to visualize touch coordinates, scale factors, and matrix calculations in real-time for troubleshooting Android 2.3 zoom issues.

## [v2.9.3] - 2026-01-11
### Fixed
- **JS Syntax Error**: Restored missing closing braces in the `touchend` event listener that were accidentally removed, restoring functionality to all buttons and touch interactions.

## [v2.9.2] - 2026-01-11
### Fixed
- **Zoom Physics**: Replaced modern `Math.hypot` with ES3 `Math.sqrt` to prevent crashes/jitter on Android 2.3.
- **Focal Point**: Implemented matrix math to ensure the zoom centers on the user's fingers, fixing the "drift to corner" issue.
- **Touch Drift**: Corrected state reset logic when lifting fingers to prevent image jumping.

## [v2.9.1] - 2026-01-11
### Fixed
- **Zoom Jitter**: Fixed an issue where the zoom level would reset abruptly when adjusting the pinch gesture. Zoom is now smooth and cumulative.

## [v2.9.0] - 2026-01-11
### Added
- **Interactive Player**: Added Pan & Zoom support using multi-touch gestures (Pinch-to-Zoom).
- **UX**: Prevented UI scaling on mobile devices to ensure controls remain accessible while zooming the video.

## [v2.8.2] - 2026-01-10
### Changed
- **UX**: Implemented "Hot-Swap" logic. Live Preview cards are automatically replaced by the final static video card (with real metadata/filesize) 3 seconds after recording stops.

## [v2.8.1] - 2026-01-10
### Changed
- **UX**: Live Preview animation now loops indefinitely after recording stops (previously froze on last frame). This provides better context until page reload.

## [v2.8.0] - 2026-01-10 (Live Injection)
### Added
- **Live Preview Injection**: New recordings appear instantly in the list with a "GRABANDO" status.
- **Client Parasite**: Javascript captures frames from the hidden `/stream` to generate an animated preview in real-time.
- **Backend API**: Added `/api/latest_video_meta` for lightweight metadata retrieval.

## [v2.7.0] - 2026-01-10 (Auto-Deploy Verified)
### Added
- **Diagnostics**: Added "Boot Time" timestamp to the dashboard footer.
- **Workflow**: Automated version bumping and Git Protocol verification (Self-Test).
 
## [v2.6.0] - 2026-01-09
### Added
- **UI Version Display**: Dashboard header now dynamically displays the app version (e.g., "v2.6").
- **Visual Polish**: Improved alignment of "Live Camera" button and removed stray escaped characters in HTML.

## [v2.5.0] - 2026-01-09
### Fixed
- **Ghost Recordings**: Replaced linear sensitivity formula with an exponential one (`10000 * (1 - sens/100)^2`), eliminating false positives at low sensitivity.
- **0KB Files**: Reordered processing logic to prioritize disk writing before network broadcasting.
- **Camera Reliability**: Implemented a "Watchdog" that flags critical hardware errors (null data) and displays a red alert in the Web UI.

## [v2.4.0] - 2026-01-09
### Refactor
- **Web Player**: Replaced `<canvas>` implementation with native `<img>` tag using Blob URLs.
- **Benefits**: Fixed visual glitches (split images) on rotated videos and improved rendering performance.

## [v2.3.0] - 2026-01-08 (Milestone: "Cool & Stable")
### Added
- **Native Resolution (CIF)**: Forced camera preview to **352x288** to match hardware capabilities, replacing the heuristic resolution search.
- **Frame Throttling**: Implemented a "Process 1 / Skip 1" strategy in `SentinelService`.
- **Diagnostics**:
    - File-based camera audit (`/sdcard/camera_info.txt`).
    - Real-time FPS counter on Web Dashboard.
    - Auto-start surveillance on app launch.

### Changed
- **Optimization**:
    - CPU load reduced by ~50% via frame throttling.
    - Operating temperature stabilized (40°C -> 38°C).
    - FPS stabilized at 15 FPS (derived from 30 FPS hardware input).

## [v2.2.0] - 2026-01-08
### Added
- **Double Buffering (Ping-Pong)**: Implementation of a `rotationBuffers` pool (size 2) in `SentinelService`. This decouples the camera writing thread from the background reading thread, completely eliminating screen tearing artifacts.
- **Software Rotation (180°)**: New `rotateNV21Degree180` method that manually inverts the Y and UV planes of the NV21 byte array. This serves as a critical workaround for the i9000's lack of hardware preview rotation support.

### Changed
- **Memory Optimization**: Rotation buffers are now reused to prevent Garbage Collection churn during high-frequency preview callbacks.
- **Preview Logic**: Removed `params.setRotation()` calls affecting the preview stream as they were ineffective on the target hardware.

## [v2.1.0] - 2026-01-07
### Added
- **Web Settings UI**: Added a configuration modal to the dashboard allowing remote control of:
  - Motion Sensitivity (0-100%).
  - Recording Timeout (10s/30s/60s).
  - Detector Status (On/Off).
  - Rotation Toggle (0°/180°).
- **Animated Thumbnails**: Dashboard now renders a live 1fps preview using a secondary MJPEG stream.

### Changed
- **Performance**: Relocated the motion threshold calculation (`500 - sensitivity * 4.9`) out of the `onPreviewFrame` hot loop. It is now calculated only upon settings change, saving ~15% CPU per frame.
- **API**: Added endpoints `/api/settings` (GET) and `/api/save_settings` (POST) to `NanoHttpServer`.

## [v2.0.0] - 2026-01-07
### Added
- **Persistence Layer**: Integrated `SharedPreferences` to store and load application state (Sensitivity, Rotation, Timeout) across device reboots.
- **Hot-Swap Logic**: `updateSettings()` method allows changing core parameters without restarting the service (except for rotation, which requires camera re-init).

## [v1.0.0] - Initial Release
### Added
- **Motion Detection Algorithm**: Optimized pixel-difference logic.
- **Thermal Guardian**: Automatic safety shut-off when CPU temperature > 45°.
- **System Stats**: Battery and Storage monitoring.
- **NanoHTTPD**: Lightweight Web Server.
