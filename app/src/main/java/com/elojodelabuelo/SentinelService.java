package com.elojodelabuelo;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.content.SharedPreferences;

public class SentinelService extends Service {

    private static final String TAG = "Sentinel";
    private static SentinelService instance;
    private static final int NOTIFICATION_ID = 1;

    private PowerManager.WakeLock wakeLock;
    private Camera camera;
    private SurfaceTexture dummySurface;
    private NanoHttpServer httpServer;
    private MotionDetector motionDetector;
    private ThermalGuardian thermalGuardian;

    private HandlerThread processingThread;
    private Handler processingHandler;

    private boolean isRecording = false;
    private long lastMotionTime = 0;
    private File currentFile;
    private FileOutputStream fileOutputStream;
    private FileOutputStream previewOutputStream; // For mini-mjpeg
    private long lastPreviewTime = 0;
    
    // Phase 9.2: Optimization
    private boolean processNextFrame = true;

    // Configurable Settings (Version 2.0)
    public static int motionSensitivity = 90;
    public static int recordingTimeout = 10; // seconds
    public static volatile boolean isDetectorActive = true;
    public static int cameraRotation = 0; // 0 or 180
            
    // Optimization: Pre-calculated threshold
    private static int currentThreshold = 50;
    
    // Buffer management
    private static final int NUM_BUFFERS = 3;
    
    // Smart Thumbnail Logic
    private int maxMotionScore = -1;
    private byte[] bestFrameJpeg = null;
    
    // Software Rotation Buffer
    /**
     * <b>Ping-Pong Buffer Strategy</b>
     * <p>
     * A pool of 2 buffers is used to decouple the Prodcuer (Camera Thread) from the Consumer (Processing Thread).
     * <ul>
     *     <li><code>rotationBuffers[index]</code>: The "Back Buffer" being written to by the camera rotation logic.</li>
     *     <li><code>rotationBuffers[(index + 1) % 2]</code>: The "Front Buffer" currently being read/processed by the detector.</li>
     * </ul>
     * This eliminates "tearing" artifacts where high-speed motion would otherwise be partially overwritten during processing.
     * </p>
     */
    private byte[][] rotationBuffers; // Pool of buffers
    private int rotationBufferIndex = 0;

    // FPS Calculation (Diagnostics) - REMOVED
    // private int frameCount = 0; // Kept for recording stats if needed, or remove if unused. Keep frameCount for video file naming.
    private int frameCount = 0;
    private long recordingStartTime = 0;
    // Real FPS Removed
    
    // State Synchronization for Long-Polling
    public static final Object statusLock = new Object();
    public static volatile boolean isRecordingPublic = false;
    public static volatile boolean isCameraError = false; // Phase 13: Watchdog flag

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Load Preferences
        SharedPreferences prefs = getSharedPreferences("SentinelPrefs", MODE_PRIVATE);
        motionSensitivity = prefs.getInt("motionSensitivity", 90);
        recordingTimeout = prefs.getInt("recordingTimeout", 10);
        isDetectorActive = prefs.getBoolean("isDetectorActive", true);
        cameraRotation = prefs.getInt("cameraRotation", 0);
        
        // Calculate initial threshold (Phase 13: Exponential)
        currentThreshold = (int) (10000 * Math.pow(1 - (motionSensitivity / 100.0), 2));
        if (currentThreshold < 20) currentThreshold = 20;
        if (currentThreshold > 50000) currentThreshold = 50000;

        // 1. WakeLock
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ElOjoDelAbuelo:SentinelLock");
        wakeLock.acquire();

        // 2. Foreground Service
        instance = this;
        updateNotification(false);

        // 3. Components
        motionDetector = new MotionDetector();
        thermalGuardian = new ThermalGuardian();
        httpServer = new NanoHttpServer(this);
        httpServer.start();

        // 4. Processing Thread
        processingThread = new HandlerThread("FrameProcessor");
        processingThread.start();
        processingHandler = new Handler(processingThread.getLooper());

        // 5. Camera
        startCamera();
    }

    private void updateNotification(boolean recording) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle("El Ojo Del Abuelo")
                .setContentText(recording ? "🔴 GRABANDO..." : "Vigilancia Activa (Esperando...)")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentIntent(pendingIntent);

        Notification notif = builder.build();
        startForeground(NOTIFICATION_ID, notif);
    }

    private void startCamera() {
        try {
            camera = Camera.open();
            
            // --- DIAGNOSTICS AUDIT (Phase 8 - REVISED File Based) ---
            Camera.Parameters params = camera.getParameters();
            writeCameraInfoToFile(params);
            // -----------------------------------

            setupCameraParameters();

            // Calculate buffer size
            int bufferSize = PREVIEW_WIDTH * PREVIEW_HEIGHT * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8;
            for (int i = 0; i < NUM_BUFFERS; i++) {
                camera.addCallbackBuffer(new byte[bufferSize]);
            }

            dummySurface = new SurfaceTexture(10);
            camera.setPreviewTexture(dummySurface);
            camera.setPreviewCallbackWithBuffer(previewCallback);
            camera.startPreview();

        } catch (Exception e) {
            e.printStackTrace();
            NanoHttpServer.setLastError("Camera Error: " + e.toString());
        }
    }

    // Globals to store actual size
    private int PREVIEW_WIDTH = 320;
    private int PREVIEW_HEIGHT = 240;

    private void setupCameraParameters() {
        Camera.Parameters params = camera.getParameters();
        java.util.List<Camera.Size> sizes = params.getSupportedPreviewSizes();

        // Phase 9.1: Optimization - Native Resolution (CIF)
        // Explicitly look for 352x288 first
        Camera.Size bestSize = null;
        for (Camera.Size size : sizes) {
            if (size.width == 352 && size.height == 288) {
                bestSize = size;
                break;
            }
        }

        // Fallback: Find closest size to 320x240 if CIF not found
        if (bestSize == null) {
            bestSize = sizes.get(0);
            int minDiff = Integer.MAX_VALUE;

            for (Camera.Size size : sizes) {
                int diff = Math.abs(size.width * size.height - 320 * 240);
                if (diff < minDiff) {
                    minDiff = diff;
                    bestSize = size;
                }
            }
        }

        PREVIEW_WIDTH = bestSize.width;
        PREVIEW_HEIGHT = bestSize.height;

        params.setPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
        // params.setRotation(cameraRotation); // REMOVED: Hardware rotation not supported for Preview on i9000 driver
        
        camera.setParameters(params);
        NanoHttpServer.setLastError("Camera OK. Size: " + PREVIEW_WIDTH + "x" + PREVIEW_HEIGHT);
    }
    
    private void writeCameraInfoToFile(Camera.Parameters params) {
        File logFile = new File(Environment.getExternalStorageDirectory(), "camera_info.txt");
        try {
            java.io.FileWriter writer = new java.io.FileWriter(logFile, false); // Overwrite
            
            writer.write("--- CAMERA CAPABILITIES AUDIT ---\n");
            writer.write("Current Preview Rate: " + params.getPreviewFrameRate() + "\n");
            
            java.util.List<Integer> rates = params.getSupportedPreviewFrameRates();
            if (rates != null) {
                writer.write("Supported FPS: ");
                for (Integer fps : rates) writer.write(fps + " ");
                writer.write("\n");
            }

            java.util.List<Camera.Size> sizes = params.getSupportedPreviewSizes();
            if (sizes != null) {
                writer.write("Supported Sizes: ");
                for (Camera.Size size : sizes) writer.write(size.width + "x" + size.height + " ");
                writer.write("\n");
            }
            writer.write("--------------------------------\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(final byte[] data, final Camera camera) {
            // Phase 13: Camera Watchdog
            if (data == null || data.length == 0) {
                 isCameraError = true;
                 // Don't restart here, just flag it for the user to see
                 return; 
            }
            isCameraError = false; // Recover if we get data


            // Phase 9.2: Frame Throttling (50% Reduction)
            // Process 1, Skip 1 to save CPU
            processNextFrame = !processNextFrame;
            if (!processNextFrame) {
                camera.addCallbackBuffer(data); // Must return buffer!
                return;
            }

            // --- REAL FPS COUNTER REMOVED ---


            if (thermalGuardian.isOverheating()) {
                // Pause specific logic or just drop frame
                // Ensure we return buffer
                camera.addCallbackBuffer(data);
                return;
            }
            
            // Software Rotation
            byte[] processedData = data;
            if (cameraRotation == 180) {
                 processedData = rotateNV21Degree180(data, PREVIEW_WIDTH, PREVIEW_HEIGHT);
            }
            
            // Motion Detection Logic
            if (!isDetectorActive) {
                if (isRecording) {
                    // Force stop if detector disabled while recording
                    isRecording = false;
                    isRecordingPublic = false;
                    synchronized (statusLock) {
                        statusLock.notifyAll();
                    }
                    updateNotification(false);
                    closeRecordingFile();
                }
                // Skip motion logic, but allow streaming below
            } else {
                int score = motionDetector.getMotionScore(processedData, PREVIEW_WIDTH, PREVIEW_HEIGHT);
                
                // Optimized: Use pre-calculated threshold
                if (score > currentThreshold) {
                    lastMotionTime = System.currentTimeMillis();
                    if (!isRecording) {
                        isRecording = true;
                        isRecordingPublic = true;
                        synchronized (statusLock) {
                            statusLock.notifyAll();
                        }
                        updateNotification(true);
                        openNewRecordingFile();
                    }
                }

                // PEAK MOTION LOGIC
                if (isRecording) {
                    if (score > maxMotionScore) {
                        maxMotionScore = score;
                        // Capture best frame immediately in memory
                        try {
                            YuvImage yuv = new YuvImage(processedData, ImageFormat.NV21, PREVIEW_WIDTH, PREVIEW_HEIGHT, null);
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            yuv.compressToJpeg(new Rect(0, 0, PREVIEW_WIDTH, PREVIEW_HEIGHT), 80, out); 
                            bestFrameJpeg = out.toByteArray();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                // Check timeout
                if (isRecording && (System.currentTimeMillis() - lastMotionTime > (recordingTimeout * 1000L))) {
                    isRecording = false;
                    isRecordingPublic = false;
                    synchronized (statusLock) {
                        statusLock.notifyAll();
                    }
                    updateNotification(false);
                    closeRecordingFile();
                }
            } // End of isDetectorActive check

            // Force Sync public state to be safe
            isRecordingPublic = isRecording;

            final byte[] finalData = processedData; // Need final for inner class if not using lambda
            processingHandler.post(new Runnable() {
                @Override
                public void run() {
                    processFrame(finalData);
                    camera.addCallbackBuffer(data); // Return buffer after processing (Must verify if we need to return 'data' specifically. Yes, 'data' is the buffer owned by Camera)
                }
            });
        }
    };
    
    /**
     * Rotates a YUV (NV21) image 180 degrees via software.
     * <p>
     * <b>Algorithm:</b> Efficiently reverses the Y plane and the UV plane (in pairs)
     * to achieve a full 180-degree flip. This is necessary because the Galaxy S i9000
     * driver does not support hardware rotation for preview callbacks.
     * </p>
     * <p>
     * <b>Optimization:</b> Direct byte manipulation is used instead of creating Bitmap objects
     * to avoid high Garbage Collection overhead on the 512MB RAM of the device.
     * </p>
     * <p>
     * <b>Double Buffering (Ping-Pong):</b> Uses a pool of 2 buffers to switch the
     * writing target every frame. This prevents "tearing" where the Camera thread
     * overwrites the buffer while the Background thread is still processing it.
     * </p>
     *
     * @param data The raw NV21 byte array from the camera.
     * @param width Frame width.
     * @param height Frame height.
     * @return The rotated byte array (from the pool).
     */
    private byte[] rotateNV21Degree180(byte[] data, int width, int height) {
        int size = width * height * 3 / 2;
        
        // 1. Initialize Buffer Pool (Ping-Pong)
        if (rotationBuffers == null) {
            rotationBuffers = new byte[2][size];
        }
        // Safety: Recreate if resolution changed
        if (rotationBuffers[0].length != size) {
             rotationBuffers = new byte[2][size];
        }

        // 2. Switch Buffer
        rotationBufferIndex = (rotationBufferIndex + 1) % 2;
        byte[] targetBuffer = rotationBuffers[rotationBufferIndex];

        int i = 0;
        int count = 0;

        // 3. Invert Y (Writing to targetBuffer)
        for (i = width * height - 1; i >= 0; i--) {
            targetBuffer[count++] = data[i];
        }

        // 4. Invert U and V (Writing to targetBuffer)
        for (i = size - 1; i >= width * height; i -= 2) {
            targetBuffer[count++] = data[i - 1]; // V
            targetBuffer[count++] = data[i];     // U
        }

        return targetBuffer;
    }

    private void processFrame(byte[] data) {
        // Convert NV21 to JPEG
        try {
            YuvImage yuv = new YuvImage(data, ImageFormat.NV21, PREVIEW_WIDTH, PREVIEW_HEIGHT, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuv.compressToJpeg(new Rect(0, 0, PREVIEW_WIDTH, PREVIEW_HEIGHT), 60, out);
            byte[] jpeg = out.toByteArray();

            // Phase 13: Priorities - Record FIRST, then Stream
            
            // 1. Record (Disk I/O)
            if (isRecording) {
                frameCount++;
                saveToFile(jpeg);

                // Smart Preview Recording (1fps)
                long now = System.currentTimeMillis();
                if (now - lastPreviewTime > 1000) {
                    lastPreviewTime = now;
                    try {
                        if (previewOutputStream != null) {
                            previewOutputStream.write(jpeg);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            // 2. Stream (Network I/O - Protected)
            try {
                httpServer.broadcast(jpeg);
            } catch (Exception e) {
                Log.e(TAG, "Stream broadcast failed: " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void openNewRecordingFile() {
        File dir = new File(Environment.getExternalStorageDirectory(), "ElOjoDelAbuelo");
        if (!dir.exists())
            dir.mkdirs();

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        currentFile = new File(dir, "video_" + timeStamp + ".mjpeg");

        try {
            fileOutputStream = new FileOutputStream(currentFile);
            // Reset Smart Thumbnail stats
            maxMotionScore = -1;
            bestFrameJpeg = null;

            // FPS Stats
            frameCount = 0;
            recordingStartTime = System.currentTimeMillis();

            // Preview File
            File previewFile = new File(dir, "preview_" + timeStamp + ".mjpeg");
            previewOutputStream = new FileOutputStream(previewFile);
            lastPreviewTime = 0;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void closeRecordingFile() {
        if (fileOutputStream != null) {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            fileOutputStream = null;

            // Close preview
            if (previewOutputStream != null) {
                try {
                    previewOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                previewOutputStream = null;
            }

            // Calculate FPS & Rename
            long duration = System.currentTimeMillis() - recordingStartTime;
            if (duration > 0 && frameCount > 0) {
                int fps = (int) (frameCount * 1000 / duration);
                if (fps < 1)
                    fps = 1;
                // Rename file to include FPS
                File newFile = new File(currentFile.getAbsolutePath().replace(".mjpeg", "_" + fps + "fps.mjpeg"));
                if (currentFile.renameTo(newFile)) {
                    currentFile = newFile; // Update reference for Thumbnail logic below
                }
            }

            // Save the BEST FRAME as .jpg (Thumbnail) in background
            if (bestFrameJpeg != null && currentFile != null) {
                final byte[] jpegToSave = bestFrameJpeg;
                final File videoFile = currentFile;
                processingHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String jpgPath = videoFile.getAbsolutePath().replace(".mjpeg", ".jpg");
                            FileOutputStream fos = new FileOutputStream(jpgPath);
                            fos.write(jpegToSave);
                            fos.close();
                            Log.d(TAG, "Smart Thumbnail saved: " + jpgPath);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    }

    private synchronized void saveToFile(byte[] jpeg) {
        if (fileOutputStream != null) {
            try {
                fileOutputStream.write(jpeg);
            } catch (IOException e) {
                e.printStackTrace();
                // If write fails, maybe close file
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        if (httpServer != null) {
            httpServer.stop();
        }
        if (processingThread != null) {
            processingThread.quit();
        }
        closeRecordingFile();
    }
    
    /**
     * Updates global configuration settings and persists them.
     * <p>
     * If the rotation setting changes, this method triggers a camera restart
     * to ensure the buffer sizes and logic are re-initialized correctly.
     * </p>
     * @param sens Motion sensitivity (0-100)
     * @param time Recording timeout in seconds
     * @param active Detector active state
     * @param rot Rotation degree (0 or 180)
     */
    public static void updateSettings(int sens, int time, boolean active, int rot) {
        motionSensitivity = sens;
        recordingTimeout = time;
        isDetectorActive = active;
        boolean rotationChanged = (cameraRotation != rot);

        cameraRotation = rot;
        
        // Update Threshold (Phase 13: Exponential)
        currentThreshold = (int) (10000 * Math.pow(1 - (motionSensitivity / 100.0), 2));
        if (currentThreshold < 20) currentThreshold = 20;
        if (currentThreshold > 50000) currentThreshold = 50000;

        if (instance != null) {
            SharedPreferences prefs = instance.getSharedPreferences("SentinelPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("motionSensitivity", sens);
            editor.putInt("recordingTimeout", time);
            editor.putBoolean("isDetectorActive", active);
            editor.putInt("cameraRotation", rot);
            editor.apply(); // Async save

            if (rotationChanged) {
                // Restart camera to apply rotation
                // Do on main handler to be safe
                if (instance.processingHandler != null) {
                    instance.processingHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (instance.camera != null) {
                                instance.camera.stopPreview();
                                instance.camera.release();
                                instance.camera = null;
                                instance.startCamera(); // Will use new rotation
                            }
                        }
                    });
                }
            }
        }
    }
}
