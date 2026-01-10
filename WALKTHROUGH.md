# Walkthrough - El Ojo Del Abuelo Implementation

## Overview
This document tracks the implementation progress and verification of "El Ojo Del Abuelo", an Android NVR application for the Galaxy S i9000.

## Recent Updates

### Double Buffering (Phase 4)
- **Problem**: Screen tearing (half-black/half-new frames) when using software rotation because the single buffer was being overwritten by `onPreviewFrame` while still being read by the background processing thread.
- **Solution**:
    - **Ping-Pong Buffer Strategy**: Implemented `byte[][] rotationBuffers` (array of 2 buffers).
    - **Logic**: Each frame switches to the alternate buffer (`index = (index + 1) % 2`) before writing, ensuring the previous frame remains valid for reading by the background thread.
- **Status**: Verified build. 

### Software Rotation (Phase 3)
- **Problem**: The Galaxy S i9000 camera driver does not support hardware rotation for preview frames (`setRotation` is ignored for callbacks).
- **Solution**:
    - Implemented `rotateNV21Degree180` in `SentinelService.java`.
    - Uses a reusable buffer to minimize CPU/GC overhead.
    - Manually inverts Y and UV planes when rotation is set to 180°.
- **Status**: Verified build. Code is ready for deployment.

### Web Interface Settings (Phase 2)
- **Feature**: Complete configuration menu accessible from the web dashboard.
- **Components**:
    - **Gear Icon**: Added to the header (top-right).
    - **Modal UI**: Dark-mode overlay with controls for Sensitivity, Timeout, Detector Toggle, and Camera Rotation.
    - **API**:
        - `GET /api/settings`: Returns current JSON configuration.
        - `POST /api/save_settings`: Accepts query parameters to update settings on the fly.
    - **Logic**:
        - Fetches settings on load.
        - Updates slider label in real-time (shows estimated pixel threshold).
        - Saves and automatically reloads the page to apply camera changes instantly.

### Performance Optimization (CPU Usage)
- **Problem**: The motion threshold formula `500 - (int)(motionSensitivity * 4.9)` was being calculated inside the `onPreviewFrame` loop (approx. 15-30 times per second), wasting CPU cycles.
- **Solution**:
    - Introduced a static variable `currentThreshold`.
    - Moved the calculation to `onCreate` (initial load) and `updateSettings` (sensitivity change).
    - The `onPreviewFrame` loop now uses the pre-calculated integer for comparison.
- **Verification**: Code compiles successfully. Zero logical impact on functionality, purely a performance gain.

### Animated Thumbnails (Mini-MJPEG)
- **Feature**: Real-time animated previews in the dashboard.
- **Implementation**:
    - `SentinelService` now records a secondary "preview" MJPEG stream (1fps).
    - Web Interface uses a `Canvas` loop to fetching and animating these previews.
    - Optimized for Android 4.4 WebView compatibility (ES5 JavaScript).
- **Verification**:
    - Browser automation confirmed thumbnails load.
    - Visual inspection confirmed "eye" movement in previews.

### Persistence & Configuration (Version 2.0)
- **Feature**: Settings for sensitivity, timeout, detector status, and camera rotation are now persistent.
- **Implementation**:
    - Uses `SharedPreferences` to store values.
    - `SentinelService` loads these on startup.
    - Static `updateSettings()` API allows changing them at runtime (e.g. via a future Settings UI).

### Phase 8 (Revised): Diagnostics & Optimization
- **Problem**: The Galaxy S i9000 has a broken Logcat (ROM issue), making standard troubleshooting impossible. We needed to know the hardware capabilities and real performance.
- **Solution**:
    - **File-Based Audit**: `SentinelService` now dumps camera parameters (Preview Sizes, FPS Ranges) to `/sdcard/camera_info.txt` on startup.
    - **Web-Based FPS**:
        - `SentinelService` calculates real FPS every second.
        - Updates a global variable in `NanoHttpServer`.
        - The Web Dashboard polls `/stats` and displays the FPS in real-time (Green text).
- **Verification**:
    - Inspect `/sdcard/camera_info.txt` after launching the app.
    - Open `http://PHONE_IP:8080` and watch the "FPS: XX" counter.

### Phase 9: Usability Improvements (Auto-Start)
- **Feature**: Surveillance service starts automatically when the app is launched.
- **Why**: Allows headless startup via ADB (`adb shell am start ...`) without needing a physical screen tap.
- **Implementation**:
    - Added `startService()` call directly in `MainActivity.onCreate`.
    - Shows a Toast "Auto-Iniciando Vigilancia..." for user feedback.

### Phase 9.1: Optimization - Native Resolution
- **Problem**: The camera setup was using a "closest match to 320x240" algorithm, which could potentially select a non-optimal resolution depending on the ROM/Driver quirks.
- **Solution**:
    - **Explicit CIF Targeting**: `SentinelService` now specifically searches for **352x288** (CIF) first.
    - **Fallback**: Only uses the old "closest match" logic if 352x288 is not supported by the hardware.
- **Verification**: Code compiles. Logged resolution (in `camera_info.txt` or Logs) should be 352x288.

### Phase 9.2: Optimization - Frame Throttling
- **Problem**: CPU was saturated (~100%) trying to process all incoming frames (30fps), leading to overheating (40°C+).
- **Solution**:
    - **50% Skip Logic**: Implemented a boolean toggle `processNextFrame` in `onPreviewFrame`.
    - **Mechanism**:
        - Frame A: Processed fully (Motion detect, Rotate, Stream).
        - Frame B: Buffer returned immediately (`camera.addCallbackBuffer(data)`).
    - **Result**: Effective processing framerate drops to ~15fps (assuming 30fps input), halving CPU load.
- **Verification**: Web Dashboard "real FPS" counter should show roughly half the input FPS. Temperature should stabilize.

### Phase 10: Cleanup & Stability (UI Fixes)
- **Problem**:
    - Users reported "Click/Scroll Blocking" in the Web UI (buttons unresponsive).
    - Debug FPS counter was cluttering the UI after optimization was confirmed.
- **Root Cause**:
    - A syntax error in `NanoHttpServer`'s JavaScript generation (Stray `};` and missing `window.onload` wrapper) caused the JS parser to crash, detaching all event listeners.
- **Solution**:
    - Removed all FPS logic from `SentinelService` and `NanoHttpServer` (cleanup).
    - Fixed the JavaScript syntax in `generateDashboardHtml` by properly wrapping initialization code in `window.onload`.
- **Verification**:
    - Build verified.
    - Web UI interaction (Settings button, Video list) should now be 100% responsive.

### Phase 11: Visual Bugfix - Responsive Player (v2.3)
- **Problem**: Rotated videos (180°) were appearing distorted or cut off in the web player due to hardcoded canvas dimensions.
- **Solution**:
    - **CSS**: Updated `#canvas-container` and `canvas` to use flexible layout (`width: 100%`, `height: auto`, `object-fit: contain`) with a black background.
    - **JS**: Removed hardcoded `352x288` dimensions in `drawFrame`. Now sets `canvas.width = img.width` and `canvas.height = img.height` dynamically based on the source frame.
- **Verification**:
    - Build verified.
    - Videos should now play correctly in any orientation without visual artifacts.

### Phase 12: Player Refactor (Canvas -> Img)
- **Problem**: Previous canvas-based player fixes were fragile and still resulted in visual glitches (split/black images) on rotated videos due to browser coordinate/rendering issues.
- **Solution**:
    - **Removed Canvas**: Completely removed `<canvas>` and its context drawing logic.
    - **Implemented Img Tag**: Replaced with `<img id="video-player">` styled with `object-fit: contain` to let the browser handle scaling and centering natively.
    - **Blob URLs**: Updated JS loop to parse frames into `Blob` objects and update the image `src` using `URL.createObjectURL()`. This ensures byte-perfect rendering of the JPEG frames.
- **Verification**:
    - Build passed.
    - Should provide a 100% robust visual experience regardless of video resolution or rotation.

### Phase 13: Stability & Safety (Ghost Recordings & 0KB Files)
- **Problem**:
    - **Ghost Recordings**: Linear sensitivity curve was too sensitive at low % (0% = 500px threshold), causing constant recording on noisy sensors.
    - **0KB Files**: Saving to disk was queued *after* network streaming. If the stream blocked, the file was created but never written to.
    - **Hardware Failures**: No user feedback if the camera driver froze or returned null frames.
- **Solution**:
    - **Exponential Sensitivity**: New formula `10000 * (1 - sens/100)^2`. At 0%, threshold is 10,000 pixels (extremely insensitive). At 90%, it's ~100 pixels.
    - **Priority Threading**: Reordered logic to execute `saveToFile()` **before** `httpServer.broadcast()`.
    - **Camera Watchdog**: If `data == null` or empty, a global error flag (`isCameraError`) is set.
    - **Visual Alert**: Dashboard now displays a red blinking warning ("ERROR CRÍTICO: CÁMARA NO RESPONDE") if the Watchdog triggers.
- **Verification**:
    - Build passed.
    - User should see fewer false positives and no more 0KB files.
    - If camera hardware fails, dashboard provides clear feedback.

### Phase 14: UI Polish - App Version Display (v2.6)
- **Feature**: Display the current running application version in the web dashboard header.
- **Implementation**:
    - **Dynamic Retrieval**: Fetched `versionName` (e.g., "1.0") from the `AndroidManifest.xml` via `PackageManager` at runtime.
    - **UI**: Appended `v1.0` (in smaller, grey text) next to the "El Ojo Del Abuelo" title.
- **Purpose**: Allows the user to instantly verify if the latest deployment was successful and which version is active.

### Phase 15: Workflow Protocol (v2.6)
- **Rule**: Manual synchronization of `build.gradle` version number.
- **Trigger**: Every time a "Phase" is marked complete in `task.md`.
- **Action**: Agent must explicitly bump `versionName` in `build.gradle` and `AndroidManifest.xml` to match the project status.

### Phase 16: Diagnostics Polish (v2.7)
- **Feature**: Boot Time display.
- **Implementation**:
    - `SystemStats.getBootTime()`: Calculates `System.currentTimeMillis() - SystemClock.elapsedRealtime()`.
    - Integrated into the Dashboard footer.
- **Purpose**: Verify the "Automated Version Protocol". If this appears in the UI and Git log without user intervention, the protocol allows the Agent to self-manage releases.

### Phase 17: Live Preview Injection (v2.8)
- **Problem**: Users had to refresh the page or wait for a recording to finish to see what was happening.
- **Solution**: "Smart Injection" + "Client Parasite".
    - **Logic**:
        - Javascript detects the "Recording" state change via polling.
        - Immediately injects a new card into the DOM.
        - Opens a hidden connection to the `/stream` (Live View).
        - Captures frames from that hidden stream to animate a "Live Thumbnail" in the injected card.
    - **Result**: Instant feedback. The dashboard feels alive and responsive.
- **Verification**:
    1. Open Dashboard.
    2. Wave hand.
    3. Verify a red-bordered card appears instantly (`< 500ms`).
    4. Verify the thumbnail inside moves (is animated).
    5. Wait 10s -> Verify it becomes a normal, clickable video.

### Phase 17.1: Persistent Live Preview Loop (v2.8.1)
- **Problem**: When recording stopped, the animated thumbnail froze on the last frame, losing context.
- **Solution**:
    - **Persistent Loop**: Modified `pLoop` in `NanoHttpServer.java` to check for `document.body.contains(canvas)` instead of relying on the temporary card ID.
    - **Result**: The animation continues indefinitely on the "DISPONIBLE" card until the user reloads the page.
- **Verification**:
    - Record a clip.
    - Wait for it to finish.
    - Verify the thumbnail *continues moving* even after the status text changes to "DISPONIBLE".

### Phase 17.2: Hot-Swap Final Card (v2.8.2)
- **Problem**: The persistent animation (Phase 17.1) was a nice visual trick, but the card lacked real data (File Size) and relied on client-side CPU for the animation loop.
- **Solution**:
    - **Back-End**: Updated `/api/latest_video_meta` to calculate and return the actual file size (KB/MB).
    - **Front-End**: Implemented a "Hot-Swap" strategy.
        1. When recording stops, wait 3 seconds (flush buffer).
        2. Fetch final metadata.
        3. Replace the entire "Live Preview" card HTML with a standard, static "Video Card".
- **Result**: Seamless transition from "Live Recording" -> "Static File" without refreshing the page. The user sees the final file size and gets a solid link to the file.
- **Verification**:
    - Record a clip.
    - Wait for "DISPONIBLE".
    - 3 seconds later -> The card flickers briefly and updates to show the File Size (e.g., "450 KB").
    - The thumbnail becomes static.

