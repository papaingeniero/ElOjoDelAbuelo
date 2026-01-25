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
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class SentinelService extends Service {

    private static final String TAG = "Sentinel";
    private static SentinelService instance;
    private static final int NOTIFICATION_ID = 1;

    // --- SOFTWARE PREVIEW INTERFACE ---
    public static java.util.List<String> debugLogs = java.util.Collections
            .synchronizedList(new java.util.ArrayList<String>());

    public interface UiPreviewCallback {
        void onFrame(byte[] jpegData);
    }

    private static UiPreviewCallback uiPreviewCallback;
    // [NUEVO] Memoria para recordar la pantalla física si reiniciamos la cámara
    private static android.view.SurfaceHolder activeSurfaceHolder = null;

    public static void setUiCallback(UiPreviewCallback cb) {
        uiPreviewCallback = cb;
    }
    // ----------------------------------

    private PowerManager.WakeLock wakeLock;
    private PowerManager.WakeLock screenLock; // Lock para pantalla
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
    // [NUEVO] Contador para el Throttling Dinámico (Modo Eco)
    private int frameSkipCounter = 0;

    // Stats Counters
    private int statsFrameProcessed = 0;
    private int statsFrameSkipped = 0;
    private Handler statsHandler;
    private Runnable statsRunnable;

    // Añade esta línea:
    private boolean lastOverheatState = false; // Memoria de estado térmico
    // --------------------------------

// [NUEVO] Para el Cronómetro Estricto del Pintor Vago
    private long lastLazyTime = 0;
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
        logToWeb(">>> SENTINEL SERVICE CREATING... (Inicio Sistema)");

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

        // 1.5 Screen Lock (ENCENDER PANTALLA)
        // SCREEN_BRIGHT + ACQUIRE_CAUSES_WAKEUP enciende la pantalla al activarlo
        screenLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "ElOjoDelAbuelo:ScreenLock");
        screenLock.setReferenceCounted(false); // Asegurar que release siempre funciona

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
        // --- INICIO HEARTBEAT (60s) ---
        statsHandler = new Handler();
        statsRunnable = new Runnable() {
            @Override
            public void run() {
                // 1. Memoria
                long freeMem = Runtime.getRuntime().freeMemory() / 1024 / 1024;
                long totalMem = Runtime.getRuntime().totalMemory() / 1024 / 1024;
                
                // 2. Temperatura (Usamos el contexto de la aplicación)
                int temp = ThermalGuardian.getBatteryTemperature(getApplicationContext());
                
                // 3. Log Resumen
                logToWeb("📊 HEARTBEAT (60s): Temp: " + temp + "°C | Mem: " + freeMem + "MB / " + totalMem + "MB | Frames: " + statsFrameProcessed + " OK / " + statsFrameSkipped + " Skip");
                
                // Reset contadores parciales
                statsFrameProcessed = 0;
                statsFrameSkipped = 0;
                
                // Programar siguiente
                statsHandler.postDelayed(this, 60000);
            }
        };
        statsHandler.postDelayed(statsRunnable, 60000);
        // --- FIN HEARTBEAT ---

        
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
            logToWeb("Intentando abrir cámara...");
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
                // [NUEVO] LÓGICA DE RECUPERACIÓN DE PANTALLA (Amnesia Fix) 🧠✨
                // Preguntamos: "¿Teníamos una pantalla conectada antes de morir?"
                if (activeSurfaceHolder != null) {
                    // SÍ: La recuperamos. ¡Adiós pantalla congelada!
                    camera.setPreviewDisplay(activeSurfaceHolder);
                    logToWeb("Cámara reiniciada recuperando SurfaceHolder activo.");
                } else {
                    // NO: Estamos en background real. Usamos textura ciega.
                    dummySurface = new SurfaceTexture(10);
                    camera.setPreviewTexture(dummySurface);
                }
            } catch (Throwable t) {
                // Fallback for API < 11 if SurfaceTexture fails (Safety Net)
                camera.setPreviewDisplay(null);
            }

            camera.setPreviewCallbackWithBuffer(previewCallback);
            
            // [IMPORTANTE] Aseguramos que al reiniciar se mantenga tu rotación física de 180º
            camera.setDisplayOrientation(180);

            camera.startPreview();
            logToWeb("Cámara arrancada OK");

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

        // 1. Buscamos Resolución Nativa (CIF)
        Camera.Size bestSize = null;
        for (Camera.Size size : sizes) {
            if (size.width == 352 && size.height == 288) {
                bestSize = size;
                break;
            }
        }
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
        // --- TRAZA FORENSE INYECTADA (PARA DETECTAR EL RATIO) ---
        float ratio = (float) PREVIEW_WIDTH / PREVIEW_HEIGHT;
        String ratioName = (Math.abs(ratio - 1.3333) < 0.05) ? "4:3 (Perfecto)" : 
                           (Math.abs(ratio - 1.7777) < 0.05) ? "16:9 (Panorámico)" : 
                           (Math.abs(ratio - 1.2222) < 0.05) ? "CIF (Culpable probable)" : "Ratio Raro";
        
        logToWeb(">>> 🕵️ AUTORÍA RESOLUCIÓN: " + PREVIEW_WIDTH + "x" + PREVIEW_HEIGHT + 
                 " | Ratio Numérico: " + String.format(Locale.US, "%.2f", ratio) + 
                 " | Veredicto: " + ratioName + " <<<");
        // --------------------------------------------------------

        // 2. Optimización Térmica FPS (AUDITORÍA)
        try {
             java.util.List<int[]> ranges = params.getSupportedPreviewFpsRange();
             if (ranges != null) {
                 StringBuilder sb = new StringBuilder("FPS Ranges disponibles: ");
                 for (int[] range : ranges) {
                     sb.append("[").append(range[0]/1000).append("-").append(range[1]/1000).append("] ");
                     // INTENTO DE FORZAR MÁXIMO 15 FPS (Si existe)
                     // Buscamos un rango donde el MÁXIMO sea <= 15000 (15fps)
                     if (range[1] <= 15000) {
                         params.setPreviewFpsRange(range[0], range[1]);
                     }
                 }
                 logToWeb("🚀 " + sb.toString());
             }
        } catch (Exception e) { logToWeb("Error setting FPS: " + e.getMessage()); }

        // 3. [NUEVO] APLICAR ZOOM POR HARDWARE (La Lupa Fría 🔍❄️)
        if (params.isZoomSupported()) {
            SharedPreferences prefs = getSharedPreferences("SentinelPrefs", MODE_PRIVATE);
            float targetZoomValue = prefs.getFloat("defaultZoom", 1.0f);
            
            // Convertimos float (2.7) a int (270)
            int targetZoomInt = (int) (targetZoomValue * 100);
            
            List<Integer> zoomRatios = params.getZoomRatios();
            int bestIndex = 0;
            int minDiff = Integer.MAX_VALUE;

            if (zoomRatios != null) {
                // Buscamos el escalón más cercano
                for (int i = 0; i < zoomRatios.size(); i++) {
                    int diff = Math.abs(zoomRatios.get(i) - targetZoomInt);
                    if (diff < minDiff) {
                        minDiff = diff;
                        bestIndex = i;
                    }
                }
                params.setZoom(bestIndex);
                int finalZoom = zoomRatios.get(bestIndex);
                logToWeb("Hardware Zoom aplicado. Deseado: " + targetZoomInt + ", Conseguido: " + finalZoom);
                NanoHttpServer.setLastError("Cam OK. Zoom HW: " + (finalZoom/100f) + "x");
            }
        }

        camera.setParameters(params);
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

            // [NUEVO] MODO ECO: DYNAMIC THROTTLING
            frameSkipCounter++;
            int skipTarget = isRecording ? 2 : 5; 

            if (frameSkipCounter % skipTarget != 0) {
                statsFrameSkipped++; // <--- ¡AÑADE ESTO! (Contamos frame saltado)
                camera.addCallbackBuffer(data); 
                return;
            }
            statsFrameProcessed++; // <--- ¡AÑADE ESTO! (Contamos frame procesado)
            
            if (frameSkipCounter > 1000) frameSkipCounter = 0;

            // --- DETECCIÓN DE CAMBIO TÉRMICO ---
            boolean isNowOverheating = thermalGuardian.isOverheating();

            // Si antes estaba bien y ahora NO -> ALERTA DE CALOR
            if (isNowOverheating && !lastOverheatState) {
                logToWeb("🔥 THERMAL: ¡SOBRECALENTAMIENTO! (Overheat TRIGGERED) - Pausando visión.");
            } 
            // Si antes estaba mal y ahora SÍ -> ALERTA DE ENFRIAMIENTO
            else if (!isNowOverheating && lastOverheatState) {
                logToWeb("❄️ THERMAL: Temperatura normalizada (Overheat CLEARED) - Reanudando visión.");
            }
            
            lastOverheatState = isNowOverheating; // Actualizamos la memoria

            // Si está caliente, abortamos frame (como antes)
            if (isNowOverheating) {
                camera.addCallbackBuffer(data);
                return;
            }
            // -----------------------------------

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
                    closeRecordingFile();
                    synchronized (statusLock) { statusLock.notifyAll(); }
                    updateNotification(false);
                }
            } else {
                int score = motionDetector.getMotionScore(processedData, PREVIEW_WIDTH, PREVIEW_HEIGHT);

                if (score > currentThreshold) {
                    lastMotionTime = System.currentTimeMillis();
                    if (!isRecording) {
                        openNewRecordingFile();
                        isRecording = true;
                        isRecordingPublic = true;
                        if (screenLock != null) { screenLock.acquire(); }
                        try { sendBroadcast(new Intent("com.elojodelabuelo.ACTION_REC_START")); } catch (Exception e) {}
                        synchronized (statusLock) { statusLock.notifyAll(); }
                        updateNotification(true);
                        logToWeb("MOTION DETECTED! Rec Started. Score: " + score);
                    }
                }

                if (isRecording) {
                    if (score > maxMotionScore) {
                        maxMotionScore = score;
                        try {
                            YuvImage yuv = new YuvImage(processedData, ImageFormat.NV21, PREVIEW_WIDTH, PREVIEW_HEIGHT, null);
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            yuv.compressToJpeg(new Rect(0, 0, PREVIEW_WIDTH, PREVIEW_HEIGHT), 80, out);
                            bestFrameJpeg = out.toByteArray();
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                }

                if (isRecording && (System.currentTimeMillis() - lastMotionTime > (recordingTimeout * 1000L))) {
                    isRecording = false;
                    isRecordingPublic = false;
                    closeRecordingFile();
                    synchronized (statusLock) { statusLock.notifyAll(); }
                    updateNotification(false);
                    logToWeb("Rec Stopped (Timeout)");
                }
            }
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

    // ------------------------------------------------------------------------
    // SUSTITUIR EN SentinelService.java (MODO "PINTOR VAGO")
    // ------------------------------------------------------------------------
    private void processFrame(byte[] data) {
        try {
            boolean uiAlive = (uiPreviewCallback != null);
            
            // [OPTIMIZACIÓN TÉRMICA ROBUSTA] ❄️
            // Versión PRO: Usamos el cronómetro lastLazyTime
            if (!isRecording && !uiAlive) {
                 long now = System.currentTimeMillis();
                 // Estrictamente 1 frame cada 2000ms (0.5 FPS)
                 if (now - lastLazyTime < 2000) { 
                     return; 
                 }
                 lastLazyTime = now;
            }

            // --- AQUI EMPIEZA EL GASTO DE CPU ---
            YuvImage yuv = new YuvImage(data, ImageFormat.NV21, PREVIEW_WIDTH, PREVIEW_HEIGHT, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            long startEncode = System.currentTimeMillis(); // ⏱️ Inicio Crono
            yuv.compressToJpeg(new Rect(0, 0, PREVIEW_WIDTH, PREVIEW_HEIGHT), 60, out);
            long duration = System.currentTimeMillis() - startEncode; // ⏱️ Fin Crono

            // Si tarda más de 100ms, es una alerta amarilla de CPU
            if (duration > 100) {
                logToWeb("⚠️ CPU SLOW: JPEG Encode tardó " + duration + "ms");
            }

            byte[] jpeg = out.toByteArray();

            // 1. Record
            if (isRecording) {
                frameCount++;
                saveToFile(jpeg);
                long now = System.currentTimeMillis();
                if (now - lastPreviewTime > 1000) {
                    lastPreviewTime = now;
                    try {
                        if (previewOutputStream != null)
                            previewOutputStream.write(jpeg);
                    } catch (IOException e) {
                    }
                }
            }

            // 2. Stream
            try {
                httpServer.broadcast(jpeg);
            } catch (Exception e) {
            }

            // 3. UI Preview
            if (uiAlive) {
                try {
                    uiPreviewCallback.onFrame(jpeg);
                } catch (Exception e) {
                    uiPreviewCallback = null; 
                }
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
        // --- SOLTAR PANTALLA ---
        if (screenLock != null && screenLock.isHeld()) {
            screenLock.release(); 
        }
        try {
            sendBroadcast(new Intent("com.elojodelabuelo.ACTION_REC_STOP"));
        } catch (Exception e) {
        }
        
        // --- CERRAR FICHEROS ---
        if (fileOutputStream != null) {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
            }
            fileOutputStream = null;
            if (previewOutputStream != null) {
                try {
                    previewOutputStream.close();
                } catch (IOException e) {
                }
                previewOutputStream = null;
            }
            
            // Renombrar con FPS reales
            long duration = System.currentTimeMillis() - recordingStartTime;
            if (duration > 0 && frameCount > 0) {
                int fps = (int) (frameCount * 1000 / duration);
                if (fps < 1) fps = 1;
                File newFile = new File(currentFile.getAbsolutePath().replace(".mjpeg", "_" + fps + "fps.mjpeg"));
                if (currentFile.renameTo(newFile))
                    currentFile = newFile;
            }
            
            // Guardar Thumbnail
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
                        } catch (IOException e) {
                        }
                    }
                });
            }
        }
        logToWeb("File Closed: " + currentFile.getName());
        manageStorage();

        // [NUEVO] FIX GHOST TRIGGER: RESET DEL CEREBRO 🧠✨
        // Borramos la memoria del detector para evitar el "salto temporal"
        motionDetector = new MotionDetector();
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
                    if (!dir.exists())
                        return;
                    StatFs stat = new StatFs(dir.getAbsolutePath());
                    long available = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
                    if (available < minBytes) {
                        File[] files = dir.listFiles();
                        if (files == null)
                            return;
                        Arrays.sort(files, new Comparator<File>() {
                            public int compare(File f1, File f2) {
                                return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                            }
                        });
                        for (File f : files) {
                            if (f.isFile() && (f.getName().endsWith(".mjpeg") || f.getName().endsWith(".jpg"))) {
                                long size = f.length();
                                if (f.delete())
                                    available += size;
                            }
                            if (available > minBytes + (50 * 1024 * 1024))
                                break;
                        }
                    }
                } catch (Exception e) {
                }
            }
        }).start();
    }

    private synchronized void saveToFile(byte[] jpeg) {
        if (fileOutputStream != null) {
            try {
                fileOutputStream.write(jpeg);
            } catch (IOException e) {
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
        logToWeb("!!! SENTINEL SERVICE DESTROYED !!!");
        instance = null;
        super.onDestroy();
        if (wakeLock != null && wakeLock.isHeld())
            wakeLock.release();
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        if (httpServer != null)
            httpServer.stop();
        if (processingThread != null)
            processingThread.quit();
        closeRecordingFile();
        if (screenLock != null && screenLock.isHeld())
            screenLock.release();
    }

    public static void updateSettings(int sens, int time, boolean active, int rot) {
        boolean rotationChanged = (cameraRotation != rot);
        motionSensitivity = sens;
        recordingTimeout = time;
        isDetectorActive = active;
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
                // [NUEVO] Log para saber por qué se reinicia
                logToWeb("Config Update: Rotation changed to " + rot + "°. Restarting Camera...");
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

            // [NUEVO] APLICAR ZOOM LIVE SIN REINICIAR CÁMARA
            // Como el zoom ahora es por Hardware, tenemos que decírselo a la cámara YA.
            if (instance.camera != null) {
                try {
                    Camera.Parameters params = instance.camera.getParameters();
                    if (params.isZoomSupported()) {
                        int targetZoomInt = (int) (zoom * 100);
                        List<Integer> zoomRatios = params.getZoomRatios();
                        int bestIndex = 0;
                        int minDiff = Integer.MAX_VALUE;
                        if (zoomRatios != null) {
                            for (int i = 0; i < zoomRatios.size(); i++) {
                                int diff = Math.abs(zoomRatios.get(i) - targetZoomInt);
                                if (diff < minDiff) {
                                    minDiff = diff;
                                    bestIndex = i;
                                }
                            }
                            params.setZoom(bestIndex);
                            instance.camera.setParameters(params);
                            logToWeb("Live Zoom Update: " + (zoomRatios.get(bestIndex)/100f) + "x");
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error Live Zoom", e);
                }
            }
        }
    }
    public static File getCurrentRecordingFile() {
        if (instance != null)
            return instance.currentFile;
        return null;
    }

    // --- HARDWARE PREVIEW ANCHOR (FINAL FIX: PANTALLA NEGRA) ---
    public static void setPreviewSurface(android.view.SurfaceHolder holder) {
        // 1. [NUEVO] Guardamos (o borramos) la referencia en memoria
        // Esto es lo que permite que el servicio recuerde la pantalla si se reinicia la cámara.
        activeSurfaceHolder = holder; 

        if (instance != null && instance.camera != null) {
            try {
                // 2. FRENAR (Imprescindible para tocar nada)
                instance.camera.stopPreview();

                // 3. CAMBIAR SUPERFICIE
                if (holder != null) {
                    instance.camera.setPreviewDisplay(holder);
                    logToWeb("Surface ATTACHED (Pantalla conectada)");
                } else {
                    // Si nos vamos a background, volvemos a la textura ciega si es posible
                    if (instance.dummySurface != null) {
                        instance.camera.setPreviewTexture(instance.dummySurface);
                        logToWeb("Surface DETACHED (Modo Background)");
                    } else {
                        instance.camera.setPreviewDisplay(null);
                    }
                }

                // 4. EL "RE-ENGANCHE" (SOLUCIÓN PANTALLA NEGRA)
                int bufferSize = instance.PREVIEW_WIDTH * instance.PREVIEW_HEIGHT
                        * android.graphics.ImageFormat.getBitsPerPixel(android.graphics.ImageFormat.NV21) / 8;
                
                try {
                    instance.camera.setPreviewCallbackWithBuffer(null); // Limpieza suave
                    instance.camera.setPreviewCallbackWithBuffer(instance.previewCallback); // ¡CONEXIÓN!
                } catch (Exception e) {
                    Log.e(TAG, "Error re-hooking callback", e);
                    logToWeb("CRITICAL Surface Error (Re-hook failed): " + e.getMessage());
                }

                // 5. RELLENAR BUFFERS
                for (int i = 0; i < 3; i++) {
                    instance.camera.addCallbackBuffer(new byte[bufferSize]);
                }

                // 6. FIX ROTACIÓN y ARRANQUE
                instance.camera.setDisplayOrientation(180);
                instance.camera.startPreview();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }    

    // --- SISTEMA DE LOGS HÍBRIDO (RAM + DISCO) ---
    // La variable debugLogs ya está definida arriba (línea 45)

    public static void logToWeb(final String msg) {
        final String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        final String entry = "[" + time + "] " + msg;
        
        // 1. Logcat estándar (Para depurar con Android Studio)
        Log.d(TAG, msg); 
        
        // 2. RAM (Para el servidor Web /log)
        synchronized (debugLogs) {
            debugLogs.add(entry);
            // Mantenemos solo las últimas 50 líneas para no llenar la memoria
            if (debugLogs.size() > 50) debugLogs.remove(0);
        }

        // 3. DISCO (Persistencia Real para ADB: tail -f abuelolog.log)
        // Lo lanzamos en un hilo aparte (new Thread) para que guardar en la SD
        // no frene ni un milisegundo a la cámara.
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File dir = new File(Environment.getExternalStorageDirectory(), "ElOjoDelAbuelo");
                    if (!dir.exists()) dir.mkdirs();
                    File logFile = new File(dir, "abuelolog.log");
                    
                    // El 'true' en FileWriter activa el modo APPEND (añadir al final)
                    BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true)); 
                    buf.append(entry);
                    buf.newLine();
                    buf.close();
                } catch (IOException e) {
                    // Si falla el log, fallamos en silencio para no romper nada más
                }
            }
        }).start();
    }
}