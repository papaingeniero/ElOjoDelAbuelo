# 👁️ El Ojo Del Abuelo (Sentinel Project)

### Galaxy S i9000 Autonomous NVR System

**Mission:** To upcycle a legacy Samsung Galaxy S (i9000) device into a high-performance, autonomous IP security camera by overcoming Android 2.3 hardware limitations through advanced software engineering.

---

## 🚀 Technical Features

This project is not just a webcam app; it is a showcase of optimization for constrained environments (Single Core 1GHz, 512MB RAM).

### ✅ Core Engineering Achievements

*   **Double Buffering (Ping-Pong) Engine**:
    *   **Problem:** The single-threaded camera callback caused "screen tearing" (visual artifacts) when the extensive image processing took longer than the frame interval.
    *   **Solution:** A custom `rotationBuffers` pool (size 2) creates a "Ping-Pong" architecture. The Camera Thread writes to *Buffer A* while the Processing Thread reads *Buffer B*, completely decoupling input from output.

*   **Precise Software Rotation (180°)**:
    *   **Problem:** The Galaxy S i9000 driver ignores `setRotation()` for preview callbacks, making upside-down mounting impossible via standard API.
    *   **Solution:** A highly optimized byte-manipulation algorithm (`rotateNV21Degree180`) that inverts the Y (Luminance) and UV (Chrominance-Interleaved) planes manually in real-time with minimal GC pressure (reused buffers).

*   **Thermal Guardian Protocol**:
    *   **Problem:** Continuous processing on aged hardware leads to overheating and battery swelling.
    *   **Solution:** A background monitor checks the `sys/class/power_supply` thermal sensors every 5 seconds. If T > 45°C, the system enters "Cool Down Mode" (halting image analysis) until T < 40°C.

*   **Asynchronous I/O Pipeline**:
    *   **Problem:** Saving frames to the SD card on the main thread freezes the preview.
    *   **Solution:** Use of `SingleThreadExecutor` to offload all filesystem operations, prioritizing the live stream fluidity.

*   **NanoHTTPD Web Dashboard**:
    *   **Architecture:** Embedded lighttpd-style Java server.
    *   **Features:** MJPEG Streaming, Dynamic Settings API (`/api/settings`), and Animated Thumbnail generation.

---

## 🛠️ Usage

### Installation
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Remote Configuration
Access the control panel at `http://PHONE_IP:8080`.
*   **Rotation:** Toggle 180° inversion instantly.
*   **Sensitivity:** Adjust motion detection threshold (0-100%).
*   **Timeout:** Set post-motion recording duration.

---
*Built with ❤️ and Java.*
