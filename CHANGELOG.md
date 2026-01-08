# Changelog

All notable changes to "El Ojo Del Abuelo" will be documented in this file.

## [v2.2] - 2026-01-08
### Added
- **Software Rotation (180°)**: Implemented `rotateNV21Degree180` to support upside-down mounting on the Galaxy S i9000 (hardware rotation unsupported in preview).
- **Double Buffering (Ping-Pong)**: Implemented a buffer pool strategy to eliminate screen tearing caused by race conditions between the camera thread and the background processing thread.

### Changed
- Removed hardware `setRotation` call for preview.
- Optimized buffer memory allocation to reuse arrays.

## [v2.1] - 2026-01-07
### Added
- **Animated Thumbnails**: The web dashboard now displays real-time animated previews (Mini-MJPEG) of the camera stream.
- **Web Interface Settings**: Added a configuration menu (Gear Icon) to the dashboard to control:
    - Motion Sensitivity (Slider with pixel threshold estimate).
    - Recording Timeout.
    - Motion Detector Activation.
    - Camera Rotation.

### Changed
- **Performance Optimization**: Moved motion threshold calculation out of the `onPreviewFrame` hot loop, reducing CPU usage significantly.
- **API**: Introduced `/api/settings` and `/api/save_settings` endpoints in `NanoHttpServer`.

## [v2.0] - 2026-01-07
### Added
- **Persistence**: Implemented `SharedPreferences` to save application settings across reboots.

## [v1.0] - Initial Release
### Added
- Basic Motion Detection (`MotionDetector.java`).
- `SentinelService` running as Foreground Service.
- HTTP Server (`NanoHttpServer.java`) for live streaming and dashboard.
- Thermal Protection (`ThermalGuardian.java`).
- Battery and Storage monitoring (`SystemStats.java`).
