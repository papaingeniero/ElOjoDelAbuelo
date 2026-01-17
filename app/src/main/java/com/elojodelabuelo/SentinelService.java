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
import android.os.StatFs;
import java.util.Arrays;
import java.util.Comparator;

public class SentinelService extends Service {

    private static final String TAG = "Sentinel";
    private static SentinelService instance;
    private static final int NOTIFICATION_ID = 1;

    // --- SOFTWARE PREVIEW INTERFACE ---
    public static java.util.List<String> debugLogs = java.util.Collections.synchronizedList(new java.util.ArrayList<String>());
    
    public interface UiPreviewCallback {
        void onFrame(byte[] jpegData);
    }

    private static UiPreviewCallback uiPreviewCallback;

    public static void setUiCallback(UiPreviewCallback cb) {
        uiPreviewCallback = cb;
    }
    // ----------------------------------

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

    // View Defaults (Phase 19)
    public static float defaultZoom = 1.0f;
    public static int defaultPanX = 0;
    public static int defaultPanY = 0;

    // Optimization: Pre-calculated threshold
    private static int currentThreshold = 50;

    // Buffer management
    private static final int NUM_BUFFERS = 3;

    // Smart Thumbnail Logic
    private int maxMotionScore = -1;
    private byte[] bestFrameJpeg = null;

    // Software Rotation Buffer
    private byte[][] rotationBuffers; // Pool of buffers
    private int rotationBufferIndex = 0;

    private int frameCount = 0;
    private long recordingStartTime = 0;

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

        defaultZoom = prefs.getFloat("defaultZoom", 1.0f);
        defaultPanX = prefs.getInt("defaultPanX", 0);
        defaultPanY = prefs.getInt("defaultPanY", 0);

        // Calculate initial threshold (Phase 13: Exponential)
        currentThreshold = (int) (10000 * Math.pow(1 - (motionSensitivity / 100.0), 2));
        if (currentThreshold < 20)
            currentThreshold = 20;
        if (currentThreshold > 50000)
            currentThreshold = 50000;

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

            // TRY-CATCH WRAPPER FOR API LEVEL COMPATIBILITY
            try {
                dummySurface = new SurfaceTexture(10);
                camera.setPreviewTexture(dummySurface);
            } catch (Throwable t) {
                // Fallback for API < 11 if SurfaceTexture fails (Safety Net)
                camera.setPreviewDisplay(null);
            }
            
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
        Camera.Size bestSize = null;
        for (Camera.Size size : sizes) {
            if (size.width == 352 && size.height == 288) {
                bestSize = size;
                break;
            }
        }

        // Fallback
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
        camera.setParameters(params);
        NanoHttpServer.setLastError("Camera OK. Size: " + PREVIEW_WIDTH + "x" + PREVIEW_HEIGHT);
    }

    private void writeCameraInfoToFile(Camera.Parameters params) {
        File logFile = new File(Environment.getExternalStorageDirectory(), "camera_info.txt");
        try {
            java.io.FileWriter writer = new java.io.FileWriter(logFile, false); 
            writer.write("--- CAMERA CAPABILITIES AUDIT ---\n");
            writer.write("Current Preview Rate: " + params.getPreviewFrameRate() + "\n");
            // ... (rest of logging kept simple)
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
                return;
            }
            isCameraError = false; 

            // Phase 9.2: Frame Throttling (50% Reduction)
            processNextFrame = !processNextFrame;
            if (!processNextFrame) {
                camera.addCallbackBuffer(data); 
                return;
            }

            if (thermalGuardian.isOverheating()) {
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
                    isRecording = false;
                    isRecordingPublic = false;
                    synchronized (statusLock) {
                        statusLock.notifyAll();
                    }
                    updateNotification(false);
                    closeRecordingFile();
                }
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
            } 

            // Force Sync public state to be safe
            isRecordingPublic = isRecording;

            final byte[] finalData = processedData; 
            processingHandler.post(new Runnable() {
                @Override
                public void run() {
                    processFrame(finalData);
                    camera.addCallbackBuffer(data); 
                }
            });
        }
    };

    private byte[] rotateNV21Degree180(byte[] data, int width, int height) {
        int size = width * height * 3 / 2;

        if (rotationBuffers == null || rotationBuffers[0].length != size) {
            rotationBuffers = new byte[2][size];
        }

        rotationBufferIndex = (rotationBufferIndex + 1) % 2;
        byte[] targetBuffer = rotationBuffers[rotationBufferIndex];

        int i = 0;
        int count = 0;

        // Invert Y 
        for (i = width * height - 1; i >= 0; i--) {
            targetBuffer[count++] = data[i];
        }

        // Invert U and V 
        for (i = size - 1; i >= width * height; i -= 2) {
            targetBuffer[count++] = data[i - 1]; // V
            targetBuffer[count++] = data[i]; // U
        }

        return targetBuffer;
    }

    private void processFrame(byte[] data) {
        try {
            YuvImage yuv = new YuvImage(data, ImageFormat.NV21, PREVIEW_WIDTH, PREVIEW_HEIGHT, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuv.compressToJpeg(new Rect(0, 0, PREVIEW_WIDTH, PREVIEW_HEIGHT), 60, out);
            byte[] jpeg = out.toByteArray();

            // 1. Record
            if (isRecording) {
                frameCount++;
                saveToFile(jpeg);
                long now = System.currentTimeMillis();
                if (now - lastPreviewTime > 1000) {
                    lastPreviewTime = now;
                    try {
                        if (previewOutputStream != null) previewOutputStream.write(jpeg);
                    } catch (IOException e) {}
                }
            }

            // 2. Stream
            try { httpServer.broadcast(jpeg); } catch (Exception e) {}

            // 3. UI Preview
            if (uiPreviewCallback != null) {
                try { uiPreviewCallback.onFrame(jpeg); } catch (Exception e) {}
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void openNewRecordingFile() {
        File dir = new File(Environment.getExternalStorageDirectory(), "ElOjoDelAbuelo");
        if (!dir.exists()) dir.mkdirs();

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        currentFile = new File(dir, "video_" + timeStamp + ".mjpeg");

        try {
            fileOutputStream = new FileOutputStream(currentFile);
            maxMotionScore = -1;
            bestFrameJpeg = null;
            frameCount = 0;
            recordingStartTime = System.currentTimeMillis();
            File previewFile = new File(dir, "preview_" + timeStamp + ".mjpeg");
            previewOutputStream = new FileOutputStream(previewFile);
            lastPreviewTime = 0;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void closeRecordingFile() {
        if (fileOutputStream != null) {
            try { fileOutputStream.close(); } catch (IOException e) {}
            fileOutputStream = null;
            if (previewOutputStream != null) {
                try { previewOutputStream.close(); } catch (IOException e) {}
                previewOutputStream = null;
            }
            long duration = System.currentTimeMillis() - recordingStartTime;
            if (duration > 0 && frameCount > 0) {
                int fps = (int) (frameCount * 1000 / duration);
                if (fps < 1) fps = 1;
                File newFile = new File(currentFile.getAbsolutePath().replace(".mjpeg", "_" + fps + "fps.mjpeg"));
                if (currentFile.renameTo(newFile)) currentFile = newFile; 
            }
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
                        } catch (IOException e) {}
                    }
                });
            }
        }
        manageStorage();
    }

    private void manageStorage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SharedPreferences prefs = getSharedPreferences("SentinelPrefs", MODE_PRIVATE);
                    int minMb = prefs.getInt("pref_min_free_space_mb", 500);
                    long minBytes = minMb * 1024L * 1024L;
                    File dir = new File(Environment.getExternalStorageDirectory(), "ElOjoDelAbuelo");
                    if (!dir.exists()) return;
                    StatFs stat = new StatFs(dir.getAbsolutePath());
                    long available = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
                    if (available < minBytes) {
                        File[] files = dir.listFiles();
                        if (files == null) return;
                        Arrays.sort(files, new Comparator<File>() {
                            public int compare(File f1, File f2) {
                                return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                            }
                        });
                        for (File f : files) {
                            if (f.isFile() && (f.getName().endsWith(".mjpeg") || f.getName().endsWith(".jpg"))) {
                                long size = f.length();
                                if (f.delete()) available += size;
                            }
                            if (available > minBytes + (50 * 1024 * 1024)) break;
                        }
                    }
                } catch (Exception e) {}
            }
        }).start();
    }

    private synchronized void saveToFile(byte[] jpeg) {
        if (fileOutputStream != null) {
            try { fileOutputStream.write(jpeg); } catch (IOException e) {}
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override
    public IBinder onBind(Intent intent) { return null; }
    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        if (httpServer != null) httpServer.stop();
        if (processingThread != null) processingThread.quit();
        closeRecordingFile();
    }

    public static void updateSettings(int sens, int time, boolean active, int rot) {
        motionSensitivity = sens;
        recordingTimeout = time;
        isDetectorActive = active;
        boolean rotationChanged = (cameraRotation != rot);
        cameraRotation = rot;
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
            editor.apply(); 
            if (rotationChanged && instance.processingHandler != null) {
                instance.processingHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (instance.camera != null) {
                            instance.camera.stopPreview();
                            instance.camera.release();
                            instance.camera = null;
                            instance.startCamera(); 
                        }
                    }
                });
            }
        }
    }

    public static void updateViewSettings(float zoom, int x, int y) {
        defaultZoom = zoom;
        defaultPanX = x;
        defaultPanY = y;
        if (instance != null) {
            SharedPreferences prefs = instance.getSharedPreferences("SentinelPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putFloat("defaultZoom", zoom);
            editor.putInt("defaultPanX", x);
            editor.putInt("defaultPanY", y);
            editor.apply();
        }
    }

    public static File getCurrentRecordingFile() {
        if (instance != null) return instance.currentFile;
        return null;
    }

    // --- HARDWARE PREVIEW ANCHOR (FINAL + ROTATION FIX) ---
    public static void setPreviewSurface(android.view.SurfaceHolder holder) {
        if (instance != null && instance.camera != null) {
            try {
                // 1. FRENAR
                instance.camera.stopPreview();
                
                // 2. CAMBIAR SUPERFICIE
                if (holder != null) {
                    instance.camera.setPreviewDisplay(holder);
                } else {
                    instance.camera.setPreviewDisplay(null);
                }
                
                // 3. RECUPERACIÓN DE BUFFERS (Vital para detección)
                int bufferSize = instance.PREVIEW_WIDTH * instance.PREVIEW_HEIGHT * android.graphics.ImageFormat.getBitsPerPixel(android.graphics.ImageFormat.NV21) / 8;
                instance.camera.setPreviewCallbackWithBuffer(null);
                instance.camera.setPreviewCallbackWithBuffer(instance.previewCallback);
                for (int i = 0; i < 3; i++) {
                    instance.camera.addCallbackBuffer(new byte[bufferSize]);
                }
                
                // 4. FIX ROTACIÓN: Girar 180 grados la imagen hardware
                instance.camera.setDisplayOrientation(180);
                
                // 5. ARRANCAR
                instance.camera.startPreview();
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
