# Hardware Preview Implementation Walkthrough (v3.3.0)

> [!NOTE]
> For a deep technical dive into the "Zero CPU" architecture logic, see [HARDWARE_PREVIEW_WALKTHROUGH.md](file:///Users/david/Desktop/ElOjoDelAbuelo/docs/HARDWARE_PREVIEW_WALKTHROUGH.md).

## Changes Implemented

### 1. View Layer (`res/layout/activity_main.xml`)
- Replaced the CPU-intensive `ImageView` with a lightweight `SurfaceView`.
- **Why?** `SurfaceView` provides a dedicated drawing surface embedded in the view hierarchy, allowing the Camera hardware to write directly to display memory (overlay) without touching the DALVIK VM or consuming CPU cycles for Bitmap decoding.

### 2. Service Layer (`SentinelService.java`)
- Added `setPreviewSurface(SurfaceHolder holder)` static method.
- **Mechanism:**
    - Uses the existing robust `instance` singleton.
    - Calls `camera.setPreviewDisplay(holder)` to bind the camera's hardware output stream to the Activity's surface.
    - Handles safe detachment when the app is backgrounded (`holder` is null).

### 3. Controller Layer (`MainActivity.java`)
- **Cleanup:** Removed all Legacy Bitmap, Matrix, and byte[] data processing.
- **New Logic:** Implemented `SurfaceHolder.Callback`.
    - **`onCreate`**: Initializes the SurfaceView and sets the critical `SURFACE_TYPE_PUSH_BUFFERS` flag (Required for Android 2.3 Gingerbread).
    - **`surfaceCreated`**: "Plugs in" the screen to the service.
    - **`surfaceDestroyed`**: "Unplugs" the screen to prevent crashes.

## Verification Checklist (Manual)
- [x] **Zero CPU Goal:** No more `BitmapFactory.decodeByteArray` on the UI thread.
- [x] **Stability:** `SentinelService` core logic (`onPreviewFrame`) was left strictly untouched.
- [x] **Legacy Support:** `setType(PUSH_BUFFERS)` ensures compatibility with the Samsung Galaxy S (i9000).
- [x] **Hybrid Fix:** Implemented buffer refill logic (`addCallbackBuffer`) during surface switch to keep software processing alive.

## Next Steps for User
1. **Compile & Deploy** to the device.
2. Verify that the camera preview appears on the screen (Hardware).
3. Verify that the web stream/motion detection works (Software).
4. Check CPU usage (should be significantly lower while the app is open).
