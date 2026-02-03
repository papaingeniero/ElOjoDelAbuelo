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
import org.conscrypt.Conscrypt; // Brain Upgrade
import java.security.Security;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Typeface;

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
    private File currentPreviewFile; // [CORRECTION] Persist preview path
    private FileOutputStream fileOutputStream;
    private FileOutputStream previewOutputStream; // For mini-mjpeg
    private long lastPreviewTime = 0;
    // --- CONFIGURACIÓN DINÁMICA DEL OSD ---
    // Posición relativa (porcentaje de 0.0 a 1.0) para la fecha en pantalla
    public static volatile float OSD_X_PCT = 0.02f;
    public static volatile float OSD_Y_PCT = 0.05f;

    private Bitmap osdBitmap;
    private Canvas osdCanvas;
    private Paint osdPaint;
    private int[] osdPixels;
    private String lastOsdText = "";
    public static volatile int OSD_TEXT_SIZE = 12; // Default 12px as requested
    private static int OSD_WIDTH = 220;
    private static int OSD_HEIGHT = 30;

    // [NUEVO] Contador para el Throttling Dinámico (Modo Eco)
    private int frameSkipCounter = 0;

    // Stats Counters
    private int statsFrameProcessed = 0;
    private int statsFrameSkipped = 0;
    private int statsJpgGenerated = 0; // <--- AÑADIR ESTO
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

    // --- TELEGRAM CONFIG ---
    public static String telegramToken = "";
    public static String telegramChatId = "";

    // View Defaults (Phase 19)
    public static float defaultZoom = 1.0f;
    public static int defaultPanX = 0;
    public static int defaultPanY = 0;

    // Optimization: Pre-calculated threshold
    private static int currentThreshold = 50;

    // Buffer management
    private static final int NUM_BUFFERS = 3;


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

        // --- LA MAGIA: Inyectar OpenSSL Moderno (Conscrypt) ---
        try {
            Security.insertProviderAt(Conscrypt.newProvider(), 1);
            logToWeb("🔐 CONSCRYPT: Motor SSL moderno inyectado con éxito.");
        } catch (Exception e) {
            logToWeb("❌ CONSCRYPT ERROR: " + e.getMessage());
        }
        // ------------------------------------------

        logToWeb(">>> SENTINEL SERVICE CREATING... (Inicio Sistema)");

        // Load Preferences
        SharedPreferences prefs = getSharedPreferences("SentinelPrefs", MODE_PRIVATE);
        motionSensitivity = prefs.getInt("motionSensitivity", 90);
        recordingTimeout = prefs.getInt("recordingTimeout", 10);
        isDetectorActive = prefs.getBoolean("isDetectorActive", true);
        logToWeb("🛡️ SENSOR ESTADO: " + (isDetectorActive ? "ACTIVO (Vigilando)" : "INACTIVO (No Vigilando, Solo Cámara)")); // <--- ESTA LÍNEA
        cameraRotation = prefs.getInt("cameraRotation", 0);
        
        telegramToken = prefs.getString("tg_token", "");
        telegramChatId = prefs.getString("tg_chat_id", "");

        defaultZoom = prefs.getFloat("defaultZoom", 1.0f);
        defaultPanX = prefs.getInt("defaultPanX", 0);
        defaultPanY = prefs.getInt("defaultPanY", 0);
        
        OSD_TEXT_SIZE = prefs.getInt("osdTextSize", 12);
        OSD_X_PCT = prefs.getFloat("osdX", 0.02f);
        OSD_Y_PCT = prefs.getFloat("osdY", 0.05f);

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

        // [AÑADIR ESTA LÍNEA JUSTO AQUÍ]
        startADBWatchdog(); // <--- Inicia el vigilante del ADB
        
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
                logToWeb("📊 HEARTBEAT (60s): Temp: " + temp + "°C | Mem: " + freeMem + "MB Free / " + totalMem + "MB Total | Frames: " + statsFrameProcessed + " OK / " + statsFrameSkipped + " Skip | JPEG: " + statsJpgGenerated);
                
                // Reset contadores parciales
                statsFrameProcessed = 0;
                statsFrameSkipped = 0;
                statsJpgGenerated = 0; // <--- AÑADIR ESTO
                
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
    private int PREVIEW_WIDTH = 352;
    private int PREVIEW_HEIGHT = 288;


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
                int diff = Math.abs(size.width * size.height - 352 * 288);
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
        String ratioName = (Math.abs(ratio - 1.3333) < 0.05) ? "4:3 (Estándar)" : 
                           (Math.abs(ratio - 1.7777) < 0.05) ? "16:9 (Panorámico)" : 
                           (Math.abs(ratio - 1.2222) < 0.05) ? "CIF (Nativa/Óptima)" : "Ratio Atípico";
        
        logToWeb(" >>> � CHECK RESOLUCIÓN: " + PREVIEW_WIDTH + "x" + PREVIEW_HEIGHT + 
                 " | Ratio: " + String.format(Locale.US, "%.2f", ratio) + 
                 " | Tipo: " + ratioName + " <<<");
        // --------------------------------------------------------

        // 2. CONFIGURACIÓN FPS (MODO SEGURO / ESTABILIDAD) 🛡️
        try {
             // Solo le pedimos amablemente el rango que él mismo nos ofrezca
             List<int[]> ranges = params.getSupportedPreviewFpsRange();
             if (ranges != null) {
                 int[] bestRange = ranges.get(0); // Por defecto el primero
                 for (int[] range : ranges) {
                     // Buscamos un rango variable (ej. 15-30) en lugar de fijo (30-30)
                     // para dejarle respirar si lo necesita.
                     if (range[0] < range[1]) { 
                         bestRange = range;
                     }
                 }
                 params.setPreviewFpsRange(bestRange[0], bestRange[1]);
                 logToWeb("🛡️ FPS Estables aplicados: " + (bestRange[0]/1000) + "-" + (bestRange[1]/1000));
                 
                 // Limpiamos cualquier error previo
                 NanoHttpServer.setLastError("None"); 
             }
        } catch (Exception e) { 
            logToWeb("FPS Setup ignored: " + e.getMessage()); 
        }

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

    // --- NUEVO: MÉTODO ANTI-AMNESIA DEL ZOOM (Para cuando la pantalla parpadea) ---
    private void enforceSavedHardwareZoom() {
        if (camera == null) return;
        try {
            Camera.Parameters params = camera.getParameters();
            if (params.isZoomSupported()) {
                // Leemos directamente del disco para estar seguros (nada de RAM volátil)
                SharedPreferences prefs = getSharedPreferences("SentinelPrefs", MODE_PRIVATE);
                float targetZoomValue = prefs.getFloat("defaultZoom", 1.0f); // Usamos la clave que ya existe
                
                int targetZoomInt = (int) (targetZoomValue * 100);
                java.util.List<Integer> zoomRatios = params.getZoomRatios();
                
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
                    
                    // --- APLICAMOS SIEMPRE (SIN PREGUNTAR) ---
                    // Esto despierta al driver aunque él crea que ya tiene el zoom puesto
                    params.setZoom(bestIndex);
                    camera.setParameters(params);
                    
                    // Solo logueamos si realmente hay zoom (> 1.0x) para confirmar
                    if (bestIndex > 0) {
                        logToWeb("🔨 ZOOM FORZADO (Anti-Amnesia): " + (zoomRatios.get(bestIndex)/100f) + "x");
                    }
                }
            }
        } catch (Exception e) {
            logToWeb("Error re-applying zoom: " + e.getMessage());
        }
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
            // Si grabamos, mantenemos fluidez (2). Si solo vigilamos, ¡ULTRA VAGOS! (10)
            // Esto baja el procesamiento de vigilancia a ~3 FPS. Suficiente para detectar personas.
            int skipTarget = isRecording ? 2 : 10;

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
                int score = motionDetector.getMotionScore(data, PREVIEW_WIDTH, PREVIEW_HEIGHT);

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

            final byte[] finalData = data;
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
            
            // si hay sobrecalentamiento, nos vamos sin procesar nada
            if (lastOverheatState) return;

            // [OPTIMIZACIÓN TÉRMICA ROBUSTA] ❄️
            // Versión PRO: Usamos el cronómetro lastLazyTime
            if (!isRecording) {
                 long now = System.currentTimeMillis();
                 // Estrictamente 1 frame cada 2000ms (0.5 FPS)
                 if (now - lastLazyTime < 2000) { 
                     return; 
                 }
                 lastLazyTime = now;
            }
        
            // [NUEVO BLOQUE AQUI]👇
            // Si nadie está mirando (ni grabando, ni stream web),
            // nos ahorramos la compresión JPEG que es lo que más calienta.
            if (!isRecording && !httpServer.hasLiveClients()) {
                return;
            }
            // 👆[FIN BLOQUE NUEVO]
            
            statsJpgGenerated++; // <--- AÑADIR ESTO AQUÍ 🎯

            // [NUEVO BLOQUE DE ROTACIÓN PEREZOSA] 🐢
            byte[] dataToCompress = data;
            if (cameraRotation == 180) {
                 // Solo rotamos ahora que sabemos que vamos a usar la imagen
                 dataToCompress = rotateNV21Degree180(data, PREVIEW_WIDTH, PREVIEW_HEIGHT);
            }

            // [NUEVO] Tatuamos la fecha en los bytes brutos (OSD)
            imprintDate(dataToCompress, PREVIEW_WIDTH, PREVIEW_HEIGHT);
            
            // --- AQUI EMPIEZA EL GASTO DE CPU ---
            YuvImage yuv = new YuvImage(dataToCompress, ImageFormat.NV21, PREVIEW_WIDTH, PREVIEW_HEIGHT, null);
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
            frameCount = 0;
            recordingStartTime = System.currentTimeMillis();
            currentPreviewFile = new File(dir, "preview_" + timeStamp + ".mjpeg");
            previewOutputStream = new FileOutputStream(currentPreviewFile);
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

                // --- INICIO TELEGRAM TRIGGER ---
                if (!telegramToken.isEmpty() && !telegramChatId.isEmpty() && currentFile.exists()) {
                    logToWeb("🚀 Subiendo evidencia a Telegram...");

                    // 1. Preview (Silencioso y Autoplay)
                    // Se envía el archivo TIMELAPSE (1 FPS) para que Telegram lo trate como preview rápida
                    if (currentPreviewFile != null && currentPreviewFile.exists()) {
                         TelegramUplink.enviarPreview(currentPreviewFile, telegramToken, telegramChatId);
                    }

                    // 2. Clip (Archivo adjunto + Notificación) -> DESACTIVADO POR PETICIÓN DE USUARIO
                    // Se envía como documento para preservar calidad y generar alerta
                    // String caption = "🚨 MOVIMIENTO: " + currentFile.getName() + " (" + SystemStats.getBatteryLevel(this) + "%)";
                    // TelegramUplink.enviarClip(currentFile, telegramToken, telegramChatId, caption);
                }
                // --- FIN TELEGRAM TRIGGER ---
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

    public static void updateSettings(int sens, int time, boolean active, int rot, String tgToken, String tgChatId) {
        boolean rotationChanged = (cameraRotation != rot);
        // --- INICIO INSERCIÓN ---
        if (isDetectorActive != active) {
             logToWeb("🛡️ VIGILANDO CAMBIADO: " + (active ? "ACTIVADO (Vigilando)" : "DESACTIVADO (Solo Cámara)"));
        }
        // --- FIN INSERCIÓN ---
        // Telegram Update
        telegramToken = tgToken;
        telegramChatId = tgChatId;
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
            // --- FIX PERSISTENCIA TELEGRAM (v3.9.9-dev.3) ---
            editor.putString("tg_token", tgToken);
            editor.putString("tg_chat_id", tgChatId);
            // ------------------------------------------------
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

                instance.camera.startPreview(); // <--- 1. ARRANCAMOS PRIMERO (Sin esperar)

                // [NUEVO] RETARDO TÁCTICO ASÍNCRONO (1.5s) ⏱️
                // Esperamos a que el driver termine de "lavarse la cara" antes de pedirle el Zoom.
                if (instance != null && instance.processingHandler != null) {
                    instance.processingHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // Verificamos que el servicio y la cámara sigan vivos antes de tocar nada
                            if (instance != null && instance.camera != null) {
                                instance.enforceSavedHardwareZoom();
                            }
                        }
                    }, 1500); // 1500ms de espera
                }

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
            // Mantenemos solo las últimas 200 líneas para no llenar la memoria
            if (debugLogs.size() > 200) debugLogs.remove(0);
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


    // --------------------------------------------------------------------------
    // 🐕 ADB WATCHDOG V2: Con ciclo preventivo anti-zombis
    // --------------------------------------------------------------------------
    private void startADBWatchdog() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Espera inicial de seguridad
                try { Thread.sleep(60000); } catch (InterruptedException e) {}

                long lastForceReset = System.currentTimeMillis();
                // ⏱️ INTERVALO DE LIMPIEZA PREVENTIVA: 3 HORAS (en milisegundos)
                // Esto evita que se quede "Zombie/Offline" aunque el puerto parezca abierto
                long PREVENTIVE_INTERVAL = 3 * 60 * 60 * 1000; 

                while (true) {
                    try {
                        boolean isListening = checkADBPort();
                        long timeSinceLastReset = System.currentTimeMillis() - lastForceReset;

                        // CASO 1: El puerto está cerrado (Crash total)
                        if (!isListening) {
                            logToWeb("⚡ ADB Watchdog: ¡Puerto cerrado! Resucitando...");
                            restartADBD();
                            lastForceReset = System.currentTimeMillis();
                        } 
                        // CASO 2: Mantenimiento Preventivo (Anti-Zombi)
                        else if (timeSinceLastReset > PREVENTIVE_INTERVAL) {
                            logToWeb("♻️ ADB Watchdog: Mantenimiento preventivo (3h). Reiniciando demonio para evitar Zombis...");
                            restartADBD();
                            lastForceReset = System.currentTimeMillis();
                        }
                        // CASO 3: Todo parece ir bien
                        else {
                            logToWeb("🔍 ADB Watchdog: Estado OK. Próximo ciclo preventivo en: " + 
                                     ((PREVENTIVE_INTERVAL - timeSinceLastReset) / 60000) + " min.");
                        }

                        // Chequeo cada 30 minutos
                        Thread.sleep(1800000); 

                    } catch (Exception e) {
                        logToWeb("❌ ADB Watchdog Error: " + e.getMessage());
                        try { Thread.sleep(60000); } catch (InterruptedException ie) {} 
                    }
                }
            }
        }).start();
    }

    // Método auxiliar para no repetir código
    private void restartADBD() throws Exception {
        Process p = Runtime.getRuntime().exec(new String[]{
            "su", "-c", 
            "setprop service.adb.tcp.port 5555; stop adbd; start adbd"
        });
        p.waitFor();
    }

    // Método auxiliar para leer el estado de la red
    private boolean checkADBPort() {
        // [MEJORA] Test de Socket Real ("Handshake Probe")
        // No confiamos en netstat (zombis). Intentamos hablar el protocolo ADB.
        java.net.Socket socket = null;
        try {
            socket = new java.net.Socket();
            // Timeout agresivo: Si no conecta en 2s, está muerto.
            socket.connect(new java.net.InetSocketAddress("127.0.0.1", 5555), 2000);
            
            // Si conecta, enviamos el saludo inicial ADB: "CNXN" (0x434E584E) + Length + etc.
            // Para simplificar: Solo comprobar que el socket abre y acepta bytes.
            // Un zombi acepta SYN pero no hace handshake completo.
            
            // Enviamos 4 bytes dummy para ver si el canal de escritura está vivo
            socket.getOutputStream().write(new byte[]{0,0,0,0}, 0, 4);
            
            return true; // Conexión viva y tubería acepta datos
        } catch (Exception e) { 
            return false; // Connection refused o Timeout -> Zombi/Muerto
        } finally {
            if (socket != null) {
                try { socket.close(); } catch (Exception ignored) {}
            }
        }
    }


    private void initOSD() {
        if (osdBitmap == null) {
            // Recalculate dimensions based on text size
            // Date string "dd/MM/yy HH:mm:ss" is ~17 chars.
            // Width approx: 17 chars * (0.6 * size) + padding
            OSD_WIDTH = (int) (OSD_TEXT_SIZE * 0.6 * 19); 
            OSD_HEIGHT = (int) (OSD_TEXT_SIZE * 2.0);
            
            // Safety mins
            if (OSD_WIDTH < 100) OSD_WIDTH = 100;
            if (OSD_HEIGHT < 20) OSD_HEIGHT = 20;

            osdBitmap = Bitmap.createBitmap(OSD_WIDTH, OSD_HEIGHT, Bitmap.Config.ARGB_8888);
            osdCanvas = new Canvas(osdBitmap);
            osdPaint = new Paint();
            osdPaint.setColor(Color.WHITE);
            osdPaint.setTextSize(OSD_TEXT_SIZE);
            osdPaint.setTypeface(Typeface.MONOSPACE);
            osdPaint.setFakeBoldText(true);
            osdPaint.setAntiAlias(false);
            osdPixels = new int[OSD_WIDTH * OSD_HEIGHT];
        }
    }

    public static void updateOsdSize(Context context, int newSize) {
        OSD_TEXT_SIZE = newSize;
        context.getSharedPreferences("SentinelPrefs", MODE_PRIVATE)
               .edit()
               .putInt("osdTextSize", newSize)
               .commit();
        // Force redraw on next frame
        if (instance != null) {
            instance.osdBitmap = null;
        }
    }

    public static void updateOsdPosition(Context context, float x, float y) {
        OSD_X_PCT = x;
        OSD_Y_PCT = y;
        context.getSharedPreferences("SentinelPrefs", MODE_PRIVATE)
               .edit()
               .putFloat("osdX", x)
               .putFloat("osdY", y)
               .commit();
        // Force redraw not needed for position, next frame will pick it up
    }

    private void imprintDate(byte[] yuvData, int width, int height) {
        if (osdBitmap == null) initOSD();
        String currentText = new SimpleDateFormat("dd/MM/yy HH:mm:ss", Locale.US).format(new Date());
        if (!currentText.equals(lastOsdText)) {
            osdBitmap.eraseColor(Color.BLUE); // Fondo azul actúa como máscara de semitransparencia
            osdCanvas.drawText(currentText, 10, OSD_TEXT_SIZE * 1.5f, osdPaint);
            osdBitmap.getPixels(osdPixels, 0, OSD_WIDTH, 0, 0, OSD_WIDTH, OSD_HEIGHT);
            lastOsdText = currentText;
        }
        int posX = (int) (width * OSD_X_PCT);
        int posY = (int) (height * OSD_Y_PCT);
        if (posX + OSD_WIDTH > width) posX = width - OSD_WIDTH;
        if (posY + OSD_HEIGHT > height) posY = height - OSD_HEIGHT;
        if (posX < 0) posX = 0;
        if (posY < 0) posY = 0;
        int offsetUV = width * height;
        for (int y = 0; y < OSD_HEIGHT; y++) {
            for (int x = 0; x < OSD_WIDTH; x++) {
                int pixel = osdPixels[y * OSD_WIDTH + x];
                if (pixel != 0) { // <--- AÑADIR ESTA LINEA
                    int curX = posX + x;
                    int curY = posY + y;
                    int pos = curY * width + curX;
                    if (pos < yuvData.length) {
                        // CASO 1: Si el pixel es TEXTO (R/G/B > 128) -> WHITE PRO MODE
                        if (((pixel >> 8) & 0xff) > 128) {
                            yuvData[pos] = (byte) 255; // Luma MAX (Blanco Puro)
                            
                            // Inyectar color NEUTRO (Gris) para evitar bordes de color (Chroma vacía)
                            int posUV = offsetUV + (curY >> 1) * width + (curX & ~1);
                            if (posUV + 1 < yuvData.length) {
                                yuvData[posUV] = (byte) 128;     // V (Cr) -> 128 (Neutro)
                                yuvData[posUV + 1] = (byte) 128; // U (Cb) -> 128 (Neutro)
                            }
                        }
                        // CASO 2: Si el pixel es AZUL (B > 128) -> ES FONDO
                        else if ((pixel & 0xff) > 128) {
                            // Oscurecer el video original al 50% (Semitransparencia)
                            // Leemos, dividimos por 2 (>>1) y escribimos de vuelta.
                            int originalLuma = yuvData[pos] & 0xff;
                            yuvData[pos] = (byte) (originalLuma >> 1);
                        }
                    }
                }
            }
        }
    }

    //fin de la clase
}