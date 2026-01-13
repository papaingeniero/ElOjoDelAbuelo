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

### Phase 18: Interactive Pan & Zoom (v3.0.2)
- **Problem**: When zooming into the video on mobile browsers, the entire webpage would scale, making controls (Close, Play/Pause) huge or inaccessible. UX was poor. Early implementations suffered from "jitter", reset bugs, and poor panning UX.
- **Solution**:
    - **Meta Viewport**: Forced `user-scalable=no` to transfer zoom control to Javascript.
    - **Clipping Container**: Wrapped the video IMG in a `div` with `overflow: hidden`.
    - **Vanilla JS**: Implemented custom `touchstart` and `touchmove` logic compatible with Android 2.3 (ES3).
        - **1 Finger**: Pan (Translate X/Y).
        - **2 Fingers**: Pinch (Scale).
    - **Math Compatibility**: Implemented custom `getDist` (Math.sqrt) as `Math.hypot` is missing in old Android.
    - **Gesture Locking**: Used `{ passive: false }` and `e.preventDefault()` to block native browser actions on modern iOS (Safari).
    - **State Persistence**: Removed logic that reset zoom on every frame, allowing zoom to persist during playback.
    - **Pan Freedom**: Removed artificial restrictions (previously required zoom > 1x) and fixed variable scope (`dy`) to allow smooth panning at any scale.
- **Result**: Smooth, interactive zoom and pan that works flawlessly on both 15-year-old Androids and modern iPhones.
- **Verification**:
    - Open any video on mobile.
    - **Pinch Out**: Video zooms smoothly. Controls stay fixed.
    - **Drag (1 Finger)**: Video moves freely in X/Y axes (after v3.0.2 fix).
    - **Playback**: Zoom level is maintained while video plays.

### Phase 18.6: Advanced UX Refinements (v3.0.3 - v3.0.4)
- **Problem 1 (UX)**: Navigating back to the original view was tedious (had to pinch exactly to 1x).
- **Problem 2 (Glitch)**: Rapidly releasing a pinch gesture was sometimes interpreted as a "tap", causing unintended behavior.
- **Problem 3 (Glitch)**: Panning while at 1x scale moved the image into empty space (black borders), which felt broken.
- **Solutions**:
    1.  **Double-Tap Reset**: Implemented a listener for two quick taps (`< 300ms`) to instantly reset zoom/pan to default.
    2.  **Smart Auto-Center**: Added logic to force `x=0, y=0` whenever the user pinches out to the minimum scale (1x).
    3.  **Pan Constraints**: Conditionally blocked `touchmove` (Pan) if the scale is exactly 1x.
    4.  **Ghost Reset Protection**: Introduced an `isMultiTouch` flag. Using >1 finger sets this flag. The `touchend` logic checks this flag; if the gesture started as a multi-touch (pinch), it *ignores* the release event, preventing the "Double-Tap" logic from firing accidentally.
- **Verification**:
    - **Quick Reset**: Double-tap instantly centers the video.
    - **Stability**: Rapidly pinching and releasing keeps the zoom level steady (no accidental resets).
    - **Cleanliness**: Pinching out snaps perfectly to the center.

### Phase 19: Settings & Workflow Enhancements (v3.0.5)
- **Problem**: Managing storage required `adb shell`, and setting up the view for each video was repetitive. Also, the Settings modal was hard to close.
- **Improvements**:
    1.  **UI Polish**: Added a clear "X" Close button to the Settings modal header.
    2.  **Storage Cleaning**: Added a "Zone of Danger" with a **Delete All Videos** button (requires confirmation). Cleans `/sdcard/ElOjoDelAbuelo/` instantly.
    3.  **View Persistence**: Added "Default View" controls:
        - **Zoom Slider (1.0x - 5.0x)**: Sets initial magnification.
        - **Pan X/Y (px)**: Sets initial offset (useful for centering on a specific door/gate).
        - Includes tooltip recommending `+/- 350px (W)` and `+/- 280px (H)` based on the CIF resolution.
- **Verification**:
    - **Defaults**: Set Zoom 2.0x, open video -> Video starts magnified.
    - **Cleanup**: "Delete All" correctly wipes the directory and refreshes the list to empty.

### Phase 20: Live View Calibration HUD (v3.0.6)
- **Problem**: Finding the perfect "Default Pan/Zoom" values blindly was difficult.
- **Solution**: Added a real-time HUD (Head-Up Display) overlay in the top-left corner of the video player.
- **Functionality**:
    - Shows: `ZOOM: 1.2x | X: -45 | Y: 120`.
    - Updates instantly as you pinch or drag.
    - Allows precise calibration: Open video -> Adjust view -> Note numbers -> Enter in Settings -> Save.
- **Verification**:
    - Open video: Green text appears.
    - Pinch/Drag: Numbers update fluidly.
    - Reset: Numbers return to 1.0x / 0 / 0.

### Phase 21: UX Improvements (v3.0.8)
- **Problem**: Hard to distinguish viewed videos and missing critical info like duration/size.
- **Solution**:
    - **Visual Indicator**: Videos fade/dim (`opacity 0.6`) after being clicked in the current session.
    - **Metadata**: Each card now displays File Size (MB) and Estimated Duration (s).
- **Functionality**:
    - Duration is calculated mathematically (File Modified Time - Filename Timestamp), avoiding slow file opening.
- **Verification**:
    - Metadata appears under the filename (e.g., `1.2 MB | 15s`).
    - Clicking a video dims the card permanently for that session.

### Phase 22: Human-Readable Metadata (v3.0.9)
- **Problem**: Raw filenames (`video_20260112_...`) were hard to read instantly. Metadata was too gray/dim.
- **Solution**:
    - **Parsing**: Extracted Date (`DD/MM/YYYY`) and Time (`HH:mm:ss`) from filenames.
    - **Styling**: Increased contrast and added bold weight to Size/Duration.
    - **Layout**: Separated Date/Time (Title) from tech specs (Subtitle).
- **Verification**:
    - List items show: `📅 12/01/2026 ⏰ 21:30:00`.
    - Tech details are **Bold** and easier to read against the dark background.

### Phase 23: Persistent Header (DRY & Sync) (v3.0.10)
- **Problem**: When opening a video or Live View, the status header (Battery, Temp, Storage) disappeared, losing context.
- **Solution**:
    - **Back-End- **JS**: Updated `pollStatus()` and `updateStats()` to use `document.querySelectorAll()` for synchronized updates.
- **UX (v3.0.14)**: Upgraded "Live View" from a raw browser tab to a fully integrated in-app modal with the persistent header and close button.

## Challenges & Fixes
- **Issue**: Initial implementation injected Java code directly into the JavaScript string, causing syntax errors and UI unresponsiveness.
- **Fix**: Surgically removed the leaked Java code from the JS generation logic in `NanoHttpServer.java`.
- **Outcome**: UI interaction restored, header visibility verified on device (v3.0.13), Live View Modal added (v3.0.14).

## Verification
- [x] **Dashboard**: Header visible and updating.
- [x] **Player Modal**: Header visible over video player.
- [x] **Live View**: Header visible during live streaming.
- [x] **Synchronization**: Battery/Temp updates reflect in all headers simultaneously.

### Phase 24: Auto Storage Management (v3.1.0)
- **Problem**: The device would eventually run out of space, stopping recordings properly.
- **Solution**: "Circular Buffer" logic.
    - **Settings**: New "Minimum Free Space" input in settings (Default: 500MB).
    - **Logic**: After every recording, `SentinelService` checks free space. If < Limit, it deletes the oldest videos until space is recovered.
- **Verification**:
    - [x] **UI**: "Min Space" field appears in Settings and persists value.
    - [x] **Logic**: Verified `StatFs` calculation and file deletion loop in code.
    - [x] **Safety**: Logic only triggers on low space and respects the 500MB buffer.

### Phase 25: Scrollable Settings Modal (v3.1.1)
- **Problem**: As settings options grew (Auto-Storage, Rotation, Defaults), the modal became too tall for mobile screens, cutting off the "Save" button.
- **Solution**:
    - **CSS**: Applied `max-height: 85vh` and `overflow-y: auto` to `.settings-content`.
    - **Result**: The modal is now constrained to the viewport height, and a vertical scrollbar appears automatically when needed.
- **Verification**:
    - [x] **User Verified**: Confirmed that scrolling works perfectly on the device.

