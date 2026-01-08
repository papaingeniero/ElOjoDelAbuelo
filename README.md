# 👁️ El Ojo Del Abuelo (Sentinel Project)

**Android NVR & Surveillance System for Galaxy S i9000**

"El Ojo Del Abuelo" turns a legacy Android device (Samsung Galaxy S i9000, Android 2.3-4.4) into a robust, high-performance security camera. It is designed for extreme efficiency, minimal CPU usage, and thermal protection.

## Features

### 🎥 Core Capabilities
*   **Motion Detection**: Efficient algorithm optimized for single-core CPUs. Triggers recording only when significant movement is detected.
*   **Thermal Guardian**: Monitors CPU temperature in real-time. Pauses heavy processing if the device overheats (>45°C) to prevent damage.
*   **Foreground Service**: Runs reliably in the background with a persistent notification.

### ⚙️ v2.2: Advanced Logic (New!)
*   **Software Rotation (180°)**: Solves the hardware limitation of the i9000 driver. Images are inverted efficiently using a custom algorithm.
*   **Double Buffering (Ping-Pong)**: Uses a buffer pool to eliminate screen tearing during high-speed rotation processing. The camera write thread and processing read thread never conflict.

### 🌐 Web Dashboard
Access the camera via `http://DEVICE_IP:8080`.
*   **Live Stream**: MJPEG stream compatible with any browser.
*   **Animated Thumbnails**: Real-time previews of activity.
*   **Settings Menu ⚙️**: Configure sensitivity, timeout, and rotation remotely without touching the phone.

## Installation

1.  **Build**:
    ```bash
    ./gradlew assembleDebug
    ```
2.  **Install**:
    ```bash
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    ```
3.  **Run**:
    Open the app manually or via:
    ```bash
    adb shell am start -n com.elojodelabuelo/.MainActivity
    ```

## Settings
Persistent configuration is stored in `SharedPreferences`.
*   **Motion Sensitivity**: Adjusts the pixel change threshold.
*   **Recording Timeout**: Duration to keep recording after motion stops (10s, 30s, 60s).
*   **Camera Rotation**: 0° (Standard) or 180° (Inverted for ceiling mount).
