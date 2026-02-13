package com.elojodelabuelo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A robust embedded Web Server/NVR.
 * Serves a modern mobile dashboard, handles MJPEG streaming, and provides video
 * playback.
 */
public class NanoHttpServer {

    private ServerSocket serverSocket;
    private Thread serverThread;
    private boolean isRunning = false;
    private Context context;
    private final Set<OutputStream> liveStreamClients = new HashSet<>();
    private static final int PORT = 8080;
    private static final String BOUNDARY = "ElOjoDelAbueloBoundary";
    private static final File STORAGE_DIR = new File(Environment.getExternalStorageDirectory(), "ElOjoDelAbuelo");
    // [FIX ZOMBIE] Usamos un mapa para trackear el latido de CADA sesión
    // individualmente.
    // Clave: SessionID, Valor: Timestamp del último latido
    private final Map<String, Long> sessionHeartbeats = new java.util.concurrent.ConcurrentHashMap<>();
    private static String lastError = "None";

    public static void setLastError(String error) {
        lastError = error;
    }

    // Phase 8: Real FPS Diagnostics - REMOVED

    public NanoHttpServer(Context context) {
        this.context = context;
    }

    public void start() {
        if (isRunning)
            return;
        isRunning = true;
        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SentinelService.logToWeb("🔍 NanoHttpServer: Attempting to bind port " + PORT);
                    serverSocket = new ServerSocket(PORT);
                    SentinelService.logToWeb("✅ NanoHttpServer: Port " + PORT + " bound successfully!");

                    while (isRunning) {
                        try {
                            Socket client = serverSocket.accept();
                            new Thread(new ClientHandler(client)).start();
                        } catch (IOException e) {
                            if (isRunning) {
                                SentinelService.logToWeb("⚠️ Client Accept Error: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (Exception e) {
                    SentinelService.logToWeb("❌ FATAL WEB SERVER ERROR: " + e.toString());
                    // Dump stack trace to log
                    for (StackTraceElement ste : e.getStackTrace()) {
                        SentinelService.logToWeb("    at " + ste.toString());
                    }
                }
            }
        });
        serverThread.start();
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null)
                serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        synchronized (liveStreamClients) {
            for (OutputStream os : liveStreamClients) {
                try {
                    os.close();
                } catch (Exception e) {
                }
            }
            liveStreamClients.clear();
        }
        sessionHeartbeats.clear(); // Clear all session heartbeats on server stop
    }

    public void broadcast(byte[] jpegData) {
        if (jpegData == null || liveStreamClients.isEmpty())
            return;

        synchronized (liveStreamClients) {
            Iterator<OutputStream> it = liveStreamClients.iterator();
            while (it.hasNext()) {
                OutputStream os = it.next();
                try {
                    os.write(("--" + BOUNDARY + "\r\n").getBytes());
                    os.write("Content-Type: image/jpeg\r\n".getBytes());
                    os.write(("Content-Length: " + jpegData.length + "\r\n\r\n").getBytes());
                    os.write(jpegData);
                    os.write("\r\n".getBytes());
                    os.flush();
                } catch (IOException e) {
                    it.remove();
                }
            }
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            InputStream is = null;
            OutputStream os = null;
            try {
                is = socket.getInputStream();
                os = socket.getOutputStream();

                // 1. Read Request
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line = reader.readLine();
                if (line == null)
                    return;

                StringTokenizer st = new StringTokenizer(line);
                String method = st.hasMoreTokens() ? st.nextToken() : "GET";
                String uri = st.hasMoreTokens() ? st.nextToken() : "/";

                // Parse query parameters for all requests that might need them
                java.util.Properties parms = new java.util.Properties();
                if (uri.contains("?")) {
                    String query = uri.substring(uri.indexOf("?") + 1);
                    String[] pairs = query.split("&");
                    for (String pair : pairs) {
                        String[] kv = pair.split("=");
                        if (kv.length == 2)
                            parms.setProperty(kv[0], java.net.URLDecoder.decode(kv[1], "UTF-8"));
                    }
                }

                // 2. Route Request
                // --- RUTAS OSD (V3.9.7) ---
                if (uri.toLowerCase(Locale.US).startsWith("/lab")) {
                    sendStringResponse(os, "text/html", WebMotionLab.getHtml());
                    return;
                }
                if (uri.equals("/config/osd")) {
                    sendStringResponse(os, "text/html", WebOsdEditor.getHtml());
                    return;
                }
                if (uri.startsWith("/api/set_osd")) {
                    String x = parms.getProperty("x");
                    String y = parms.getProperty("y");
                    String size = parms.getProperty("size");

                    if (x != null && y != null) {
                        try {
                            float xVal = Float.parseFloat(x);
                            float yVal = Float.parseFloat(y);
                            SentinelService.updateOsdPosition(context, xVal, yVal);

                            if (size != null) {
                                SentinelService.updateOsdSize(context, Integer.parseInt(size));
                            }

                            sendStringResponse(os, "text/plain", "OK");
                            return;
                        } catch (Exception e) {
                        }
                    }
                    sendStringResponse(os, "text/plain", "ERR");
                    return;
                }
                // ---------------------------
                if (uri.startsWith("/api/test_telegram")) {
                    String token = parms.getProperty("token");
                    String chat = parms.getProperty("chat");
                    if (token != null && chat != null) {
                        SentinelService.logToWeb("🔔 TEST REQUEST: Token=" + token.substring(0, 5) + "...");
                        TelegramUplink.sendTextMessage("🔔 TEST: ¡El Ojo del Abuelo está conectado! 👁️", token, chat);
                    }
                    os.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
                    return;
                }
                if (uri.startsWith("/stream")) {
                    SentinelService.logToWeb("📹 STREAM: Cliente conectado (IP: " + socket.getInetAddress() + ")");
                    serveLiveStream(os, parms); // Bloquea el hilo mientras transmite
                    SentinelService.logToWeb("📹 STREAM: Cliente desconectado");
                } else if (uri.equals("/api/keepalive")) {
                    // [FIX ZOMBIE] Latido con identidad
                    String sessionId = parms.getProperty("session_id");
                    if (sessionId != null && sessionHeartbeats.containsKey(sessionId)) {
                        sessionHeartbeats.put(sessionId, System.currentTimeMillis());
                    } else {
                        // Si llega un latido de una sesión que no conocemos (quizás reiniciamos el
                        // server),
                        // no hacemos nada. El stream morirá en 5s y el cliente reconectará.
                    }
                    os.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
                } else if (uri.startsWith("/video_") || uri.startsWith("/preview_")) {
                    // Solo logueamos si es video real, no previews, para no saturar
                    if (uri.startsWith("/video_")) {
                        SentinelService.logToWeb("📺 VIDEO: Reproduciendo " + uri);
                    }
                    serveVideoFile(os, uri.substring(1), method);
                } else if (uri.startsWith("/thumbnails/")) {
                    SentinelService.logToWeb("📺 THUMBNAIL: Enviando " + uri);
                    serveThumbnail(os, uri.substring(12)); // Remove "/thumbnails/"
                } else if (uri.equals("/stats")) {
                    serveStats(os);
                } else if (uri.equals("/api/settings")) {
                    serveSettings(os);
                } else if (uri.equals("/api/delete_all_videos") && method.equals("POST")) {
                    // Phase 19: Delete All Handling
                    SentinelService.logToWeb("🗑️ STORAGE: ¡Borrado masivo ejecutado desde Web!");
                    File dir = new File(Environment.getExternalStorageDirectory(), "ElOjoDelAbuelo");
                    if (dir.exists() && dir.isDirectory()) {
                        File[] files = dir.listFiles();
                        if (files != null) {
                            for (File f : files) {
                                if (f.isFile() && (f.getName().endsWith(".mjpeg") || f.getName().endsWith(".jpg"))) {
                                    f.delete();
                                }
                            }
                        }
                    }
                    os.write("HTTP/1.1 200 OK\r\n".getBytes());
                    os.write("Content-Type: text/plain\r\n".getBytes());
                    os.write("\r\n".getBytes());
                    os.write("Deleted".getBytes());

                } else if (uri.startsWith("/api/save_settings")) {
                    SentinelService.logToWeb("💾 CONFIG: Guardando ajustes desde Web");
                    serveSaveSettings(os, uri);
                } else if (uri.equals("/api/latest_video_meta")) {
                    serveLatestVideoMeta(os);
                } else if (uri.startsWith("/api/list_videos")) {
                    serveVideoList(os, uri);
                } else if (uri.startsWith("/wait_status")) {
                    serveWaitStatus(os, uri);
                } else if (uri.equals("/api/debug")) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(
                            "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'><title>Sentinel Debug</title>");
                    sb.append(
                            "<style>body{background:#000;color:#0f0;font-family:monospace;padding:10px;font-size:14px;-webkit-text-size-adjust:100%;} .btn{background:#300;color:#fff;border:1px solid #f00;padding:10px;margin-bottom:20px;cursor:pointer;width:100%;font-weight:bold;font-size:16px;}</style>");
                    sb.append("</head><body>");
                    sb.append(
                            "<button onclick='window.close()' style='float:right;margin:0;margin-top:5px;background:#c00;color:white;border:1px solid #fff;width:30px;height:30px;font-weight:bold;cursor:pointer;'>X</button>");

                    // Botón de Pánico ADB
                    sb.append("<h2>🔧 ADMIN PANEL</h2>");
                    sb.append(
                            "<button class='btn' onclick=\"fetch('/api/restart_adb').then(r=>alert('ADB Reiniciando... La conexión se cortará.'))\">⚠️ REINICIAR SERVICIO ADB (adbd)</button>");

                    sb.append("<h3>📝 SYSTEM LOGS & DIAGNOSTICS</h3><pre>");
                    sb.append("--- DEBUG ONLINE (v3.9.9) ---\n");
                    sb.append("Status: ").append(SentinelService.isDetectorActive ? "WATCHING" : "IDLE").append("\n");
                    sb.append("Recording: ").append(SentinelService.isRecordingPublic).append("\n");
                    sb.append("LiveStream Clients: ").append(liveStreamClients.size()).append("\n");
                    sb.append("Thermal State: ").append(SentinelService.lastOverheatState ? "OVERHEATING" : "NORMAL")
                            .append("\n");
                    sb.append("Stats (Last Minute): Processed=").append(SentinelService.statsFrameProcessed)
                            .append(" / Skipped=").append(SentinelService.statsFrameSkipped)
                            .append(" / JPEGs=").append(SentinelService.statsJpgGenerated).append("\n");

                    if (SentinelService.debugLogs != null) {
                        synchronized (SentinelService.debugLogs) {
                            for (String s : SentinelService.debugLogs) {
                                sb.append(s).append("\n");
                            }
                        }
                    }
                    sb.append("</pre></body></html>");

                    String response = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n\r\n"
                            + sb.toString();
                    os.write(response.getBytes());
                    // 2. AÑADIR LA RUTA DEL TRIGGER (Con diagnóstico completo)
                } else if (uri.equals("/api/restart_adb")) {
                    try {
                        SentinelService.logToWeb("⚠️ WEB TRIGGER: Intentando reiniciar ADB...");

                        // Lanzamos el proceso
                        Process process = Runtime.getRuntime().exec(new String[] { "su", "-c",
                                "setprop service.adb.tcp.port 5555; stop adbd; start adbd" });

                        // LEEMOS LA RESPUESTA (Las "Orejas")
                        // Es importante leer el stream de error por si el comando falla
                        java.io.BufferedReader adbReader = new java.io.BufferedReader(
                                new java.io.InputStreamReader(process.getErrorStream()));
                        StringBuilder output = new StringBuilder();
                        String adbOutputLine;
                        while ((adbOutputLine = adbReader.readLine()) != null) {
                            output.append(adbOutputLine).append(" ");
                        }

                        // Esperamos a que el comando termine y nos dé su veredicto (0 = Éxito)
                        int exitCode = process.waitFor();

                        if (exitCode == 0) {
                            SentinelService.logToWeb("✅ ÉXITO: ADB Reiniciado correctamente (Exit Code 0).");
                        } else {
                            // Si falla, logueamos qué ha dicho el sistema (ej: "Permission denied")
                            SentinelService.logToWeb("❌ FALLO ROOT (Código " + exitCode + "): " + output.toString());
                        }

                    } catch (Exception e) {
                        SentinelService.logToWeb("❌ EXCEPCIÓN JAVA crítica: " + e.getMessage());
                    }

                    // Respondemos al navegador
                    os.write("HTTP/1.1 200 OK\r\n\r\nOK".getBytes());

                } else {
                    SentinelService.logToWeb("🌐 WEB: Dashboard cargado");
                    serveDashboard(os);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // This block should only close the socket if it's not a live stream client
                // The live stream client is managed within serveLiveStream
                if (!liveStreamClients.contains(os)) {
                    try {
                        socket.close();
                    } catch (Exception e) {
                    }
                }
            }
        }

        private void serveLiveStream(OutputStream os, java.util.Properties parms) throws IOException {
            os.write("HTTP/1.1 200 OK\r\n".getBytes());
            os.write(("Content-Type: multipart/x-mixed-replace; boundary=" + BOUNDARY + "\r\n").getBytes());
            os.write("Connection: keep-alive\r\n".getBytes());
            os.write("\r\n".getBytes());
            os.flush();

            // [FIX ZOMBIE] 1. Obtener Session ID de la URL
            String sessionId = parms.getProperty("session_id");
            if (sessionId == null || sessionId.isEmpty()) {
                // Si no hay ID (ej: VLC o navegador viejo), generamos uno temporal
                sessionId = "legacy_" + System.currentTimeMillis();
            }

            // 2. Registrar inicio de sesión
            sessionHeartbeats.put(sessionId, System.currentTimeMillis());

            // 3. Añadir al pool de streams
            synchronized (liveStreamClients) {
                liveStreamClients.add(os);
            }
            SentinelService.logToWeb("🎥 Nuevo cliente Stream conectado (ID: " + sessionId + ")");

            try {
                while (true) {
                    // [FIX ZOMBIE] 4. WATCHDOG INDIVIDUAL
                    // Verificamos SOLO el latido de ESTA sesión.
                    Long lastBeat = sessionHeartbeats.get(sessionId);
                    if (lastBeat == null || (System.currentTimeMillis() - lastBeat) > 5000) {
                        SentinelService
                                .logToWeb("💀 WATCHDOG: Cliente mudo (ID: " + sessionId + ") > 5s. Cortando stream.");
                        break; // Rompe el bucle y cierra el socket en el finally
                    }
                    Thread.sleep(1000); // Revisamos cada segundo
                }
            } catch (InterruptedException e) {
                // End
            } finally {
                // Limpieza al salir
                sessionHeartbeats.remove(sessionId);
                synchronized (liveStreamClients) {
                    liveStreamClients.remove(os);
                }
                SentinelService.logToWeb("End of stream (ID: " + sessionId + ")");
            }
        }

        private void serveVideoFile(OutputStream os, String fileName, String method) throws IOException {
            File file = new File(STORAGE_DIR, fileName);
            if (!file.exists()) {
                send404(os);
                return;
            }

            // Simple MJPEG serving (as a download/stream)
            os.write("HTTP/1.1 200 OK\r\n".getBytes());
            os.write(("Content-Type: application/octet-stream\r\n").getBytes());
            os.write(("Content-Length: " + file.length() + "\r\n").getBytes());
            os.write("\r\n".getBytes());

            if ("HEAD".equalsIgnoreCase(method)) {
                os.flush();
                return;
            }

            FileInputStream fis = new FileInputStream(file);
            byte[] buf = new byte[8192];
            int len;
            while ((len = fis.read(buf)) > 0) {
                os.write(buf, 0, len);
            }
            fis.close();
            os.flush();
        }

        private void serveThumbnail(OutputStream os, String fileName) throws IOException {
            File file = new File(STORAGE_DIR, fileName);
            if (!file.exists()) {
                send404(os);
                return;
            }

            os.write("HTTP/1.1 200 OK\r\n".getBytes());
            os.write("Content-Type: image/jpeg\r\n".getBytes());
            os.write(("Content-Length: " + file.length() + "\r\n").getBytes());
            os.write("\r\n".getBytes());

            FileInputStream fis = new FileInputStream(file);
            byte[] buf = new byte[8192];
            int len;
            while ((len = fis.read(buf)) > 0) {
                os.write(buf, 0, len);
            }
            fis.close();
            os.flush();
        }

        private void serveStats(OutputStream os) throws IOException {
            int batLevel = SystemStats.getBatteryLevel(context);
            boolean charging = SystemStats.isCharging(context);
            String freeStorage = SystemStats.getFreeStorageSpace(); // e.g. "1.2 GB"
            int temp = ThermalGuardian.getBatteryTemperature(context);

            // Manual JSON construction to avoid external libs
            String json = String.format(
                    "{\"bat\":%d, \"charging\":%b, \"temp\":%d, \"storage\":\"%s\", \"recording\":%b}",
                    batLevel, charging, temp, freeStorage, SentinelService.isRecordingPublic);

            os.write("HTTP/1.1 200 OK\r\n".getBytes());
            os.write("Content-Type: application/json\r\n".getBytes());
            os.write("Cache-Control: no-cache\r\n".getBytes());
            os.write(("Content-Length: " + json.length() + "\r\n").getBytes());
            os.write("\r\n".getBytes());
            os.write(json.getBytes());
            os.flush();
        }

        /**
         * GET /api/settings
         * Returns the current application configuration.
         * Response: JSON {"sens": 90, "time": 10, "active": true, "rot": 0}
         */
        private void serveSettings(OutputStream os) throws IOException {
            // Retrieve current settings
            int sens = SentinelService.motionSensitivity;
            int contrast = SentinelService.contrastSensitivity;
            int time = SentinelService.recordingTimeout;
            boolean active = SentinelService.isDetectorActive;
            boolean preRecord = SentinelService.isPreRecordActive;
            int rot = SentinelService.cameraRotation;
            float defZoom = SentinelService.defaultZoom;
            int defPanX = SentinelService.defaultPanX;
            int defPanY = SentinelService.defaultPanY;

            SharedPreferences prefs = context.getSharedPreferences("SentinelPrefs", Context.MODE_PRIVATE);
            int minFreeSpace = prefs.getInt("pref_min_free_space_mb", 500);

            // WEB VIEW SETTINGS (Stored in Device Prefs)
            float webZoom = prefs.getFloat("webZoom", 1.0f);
            int webPanX = prefs.getInt("webPanX", 0);
            int webPanY = prefs.getInt("webPanY", 0);

            boolean stealth = SentinelService.stealthMode;
            boolean filter = SentinelService.filterFalsePositives;

            String json = String.format(Locale.US,
                    "{\"sens\":%d, \"contrast\":%d, \"time\":%d, \"active\":%b, \"preRecord\":%b, \"stealth\":%b, \"filter\":%b, \"rot\":%d, \"defZoom\":%.2f, \"defPanX\":%d, \"defPanY\":%d, \"minFreeSpace\":%d, \"webZoom\":%.2f, \"webPanX\":%d, \"webPanY\":%d, \"tgToken\":\"%s\", \"tgChatId\":\"%s\", \"tgActive\":%b}",
                    sens, contrast, time, active, preRecord, stealth, filter, rot, defZoom, defPanX, defPanY,
                    minFreeSpace,
                    webZoom,
                    webPanX,
                    webPanY,
                    SentinelService.telegramToken, SentinelService.telegramChatId, SentinelService.isTelegramActive);

            os.write("HTTP/1.1 200 OK\r\n".getBytes());
            os.write("Content-Type: application/json\r\n".getBytes());
            os.write("Cache-Control: no-cache, no-store, must-revalidate\r\n".getBytes());
            os.write("\r\n".getBytes());
            os.write(json.getBytes());
        }

        /**
         * POST /api/save_settings
         * Updates the application configuration on the fly.
         * Params: ?sens=INT&time=INT&active=BOOL&rot=INT
         *
         * @param uri The full request URI containing query parameters.
         */
        private void serveSaveSettings(OutputStream os, String uri) throws IOException {
            int sens = 90;
            int contrast = 50;
            int time = 10;
            boolean active = true;
            boolean preRecord = true;
            boolean stealth = false;
            boolean filter = true;
            int rot = 0;
            float defZoom = 1.0f;
            int defPanX = 0;
            int defPanY = 0;
            int minSpace = 500;
            // Web Vars
            float webZoom = 1.0f;
            int webPanX = 0;
            int webPanY = 0;
            // Telegram Vars (Heredar valores actuales para no borrarlos si no vienen en la
            // URL)
            String tgToken = SentinelService.telegramToken;
            String tgChatId = SentinelService.telegramChatId;

            try {

                if (uri.contains("?")) {
                    String query = uri.substring(uri.indexOf("?") + 1);
                    String[] pairs = query.split("&");
                    for (String pair : pairs) {
                        String[] kv = pair.split("=");
                        if (kv.length == 2) {
                            String key = kv[0];
                            String val = kv[1];

                            if (key.equals("sens"))
                                sens = Integer.parseInt(val);
                            else if (key.equals("contrast"))
                                contrast = Integer.parseInt(val);
                            else if (key.equals("time"))
                                time = Integer.parseInt(val);
                            else if (key.equals("active"))
                                active = Boolean.parseBoolean(val);
                            else if (key.equals("preRecord"))
                                preRecord = Boolean.parseBoolean(val);
                            else if (key.equals("stealth"))
                                stealth = Boolean.parseBoolean(val);
                            else if (key.equals("filter"))
                                filter = Boolean.parseBoolean(val);
                            else if (key.equals("rot"))
                                rot = Integer.parseInt(val);
                            else if (key.equals("defZoom"))
                                defZoom = Float.parseFloat(val);
                            else if (key.equals("defPanX"))
                                defPanX = Integer.parseInt(val);
                            else if (key.equals("defPanY"))
                                defPanY = Integer.parseInt(val);
                            else if (key.equals("min_free_space"))
                                minSpace = Integer.parseInt(val);
                            // New Web Vars
                            else if (key.equals("webZoom"))
                                webZoom = Float.parseFloat(val);
                            else if (key.equals("webPanX"))
                                webPanX = Integer.parseInt(val);
                            else if (key.equals("webPanY"))
                                webPanY = Integer.parseInt(val);
                            // Telegram Parsing (Con Decodificación URL)
                            else if (key.equals("tgToken"))
                                tgToken = java.net.URLDecoder.decode(val, "UTF-8");
                            else if (key.equals("tgChatId"))
                                tgChatId = java.net.URLDecoder.decode(val, "UTF-8");
                            else if (key.equals("tgActive")) {
                                boolean isActive = Boolean.parseBoolean(val);
                                SentinelService.isTelegramActive = isActive;
                                context.getSharedPreferences("SentinelPrefs", Context.MODE_PRIVATE).edit()
                                        .putBoolean("telegramActive", isActive).commit();
                            }
                        }
                    }
                }

                // Save Web Settings to Phone Prefs
                context.getSharedPreferences("SentinelPrefs", Context.MODE_PRIVATE).edit()
                        .putInt("pref_min_free_space_mb", minSpace)
                        .putFloat("webZoom", webZoom)
                        .putInt("webPanX", webPanX)
                        .putInt("webPanY", webPanY)
                        .commit();

                SentinelService.updateSettings(sens, contrast, time, active, rot, tgToken, tgChatId, preRecord,
                        stealth, filter);
                SentinelService.updateViewSettings(defZoom, defPanX, defPanY);
                SentinelService.updateViewSettings(defZoom, defPanX, defPanY);
            } catch (Exception e) {
                e.printStackTrace();
            }

            os.write("HTTP/1.1 200 OK\r\n".getBytes());
            os.write("Content-Type: text/plain\r\n".getBytes());
            // 👇 PEGA ESTA LÍNEA AQUÍ 👇
            os.write("Cache-Control: no-cache, no-store, must-revalidate\r\n".getBytes());
            os.write("\r\n".getBytes());
            os.write("OK".getBytes());
        }

        private void serveWaitStatus(OutputStream os, String uri) throws IOException {
            // Parse query params manually (uri contains ?current_state=true/false)
            boolean clientState = false;
            if (uri.contains("current_state=true"))
                clientState = true;

            long start = System.currentTimeMillis();
            synchronized (SentinelService.statusLock) {
                // Wait until state is different from clientState or timeout
                while (SentinelService.isRecordingPublic == clientState) {
                    long now = System.currentTimeMillis();
                    if (now - start > 30000)
                        break; // 30s heartbeat
                    try {
                        SentinelService.statusLock.wait(30000);
                    } catch (InterruptedException e) {
                    }
                }
            }

            String json = "{\"recording\":" + SentinelService.isRecordingPublic + "}";
            os.write("HTTP/1.1 200 OK\r\n".getBytes());
            os.write("Content-Type: application/json\r\n".getBytes());
            os.write("Cache-Control: no-cache\r\n".getBytes());
            os.write(("Content-Length: " + json.length() + "\r\n").getBytes());
            os.write("\r\n".getBytes());
            os.write(json.getBytes());
            os.flush();
        }

        private void serveLatestVideoMeta(OutputStream os) throws IOException {
            String json = "{}";
            File f = SentinelService.getCurrentRecordingFile();
            if (f != null) {
                String sizeStr = "0 KB";
                if (f.exists()) {
                    long bytes = f.length();
                    if (bytes > 1024 * 1024)
                        sizeStr = String.format("%.1f MB", bytes / (1024.0 * 1024.0));
                    else
                        sizeStr = (bytes / 1024) + " KB";
                }
                json = "{\"filename\":\"" + f.getName() + "\", \"status\":\""
                        + (SentinelService.isRecordingPublic ? "recording" : "idle") + "\", \"size\":\"" + sizeStr
                        + "\"}";
            } else {
                json = "{\"filename\":null, \"status\":\"" + (SentinelService.isRecordingPublic ? "recording" : "idle")
                        + "\", \"size\":null}";
            }

            os.write("HTTP/1.1 200 OK\r\n".getBytes());
            os.write("Content-Type: application/json\r\n".getBytes());
            os.write("Cache-Control: no-cache\r\n".getBytes());
            os.write(("\r\n").getBytes());
            os.write(json.getBytes());
            os.flush();
        }

        /**
         * GET /api/list_videos?offset=0&limit=10
         * Returns paginated list of videos with metadata (extracted from filename
         * only).
         * Response: JSON array [{name, size, date, thumb, preview, duration, fps}, ...]
         */
        private void serveVideoList(OutputStream os, String uri) throws IOException {
            int offset = 0;
            int limit = 10;

            // Parse query params
            try {
                if (uri.contains("?")) {
                    String query = uri.substring(uri.indexOf("?") + 1);
                    String[] pairs = query.split("&");
                    for (String pair : pairs) {
                        String[] kv = pair.split("=");
                        if (kv.length == 2) {
                            if (kv[0].equals("offset"))
                                offset = Integer.parseInt(kv[1]);
                            else if (kv[0].equals("limit"))
                                limit = Integer.parseInt(kv[1]);
                        }
                    }
                }
            } catch (Exception e) {
                /* Use defaults */ }

            StringBuilder jsonArray = new StringBuilder("[");

            if (STORAGE_DIR.exists()) {
                File[] files = STORAGE_DIR.listFiles();
                if (files != null) {
                    // Filter only .mjpeg videos
                    java.util.List<File> videos = new java.util.ArrayList<>();
                    for (File f : files) {
                        if (f.getName().startsWith("video_") && f.getName().endsWith(".mjpeg")) {
                            videos.add(f);
                        }
                    }

                    // Sort by date (newest first)
                    java.util.Collections.sort(videos, new Comparator<File>() {
                        @Override
                        public int compare(File f1, File f2) {
                            return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
                        }
                    });

                    // Paginate
                    int start = Math.min(offset, videos.size());
                    int end = Math.min(offset + limit, videos.size());

                    for (int i = start; i < end; i++) {
                        File f = videos.get(i);
                        String name = f.getName();
                        long size = f.length();
                        long date = f.lastModified();

                        // Extract metadata from filename ONLY (Regex)
                        String thumbName = name.replace(".mjpeg", ".jpg");
                        String timestamp = "";
                        int fps = 10; // Default
                        long duration = 0;

                        // Pattern: video_YYYYMMDD_HHMMSS_Xfps.mjpeg
                        java.util.regex.Matcher m = java.util.regex.Pattern
                                .compile("video_(\\d{8}_\\d{6})_(\\d+)fps")
                                .matcher(name);
                        if (m.find()) {
                            timestamp = m.group(1);
                            fps = Integer.parseInt(m.group(2));

                            // Duration from filename timestamp + lastModified
                            try {
                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss",
                                        Locale.US);
                                java.util.Date creationDate = sdf.parse(timestamp);
                                duration = (f.lastModified() - creationDate.getTime()) / 1000;
                                if (duration < 0)
                                    duration = 0;
                            } catch (Exception e) {
                            }
                        }

                        // Preview file check
                        String previewName = "preview_" + timestamp + ".mjpeg";
                        File previewFile = new File(STORAGE_DIR, previewName);
                        boolean hasPreview = previewFile.exists();
                        File thumbFile = new File(STORAGE_DIR, thumbName);
                        boolean hasThumb = thumbFile.exists();

                        if (i > start)
                            jsonArray.append(",");
                        jsonArray.append("{");
                        jsonArray.append("\"name\":\"").append(name).append("\",");
                        jsonArray.append("\"size\":").append(size).append(",");
                        jsonArray.append("\"date\":").append(date).append(",");
                        jsonArray.append("\"thumb\":").append(hasThumb ? "\"" + thumbName + "\"" : "null").append(",");
                        jsonArray.append("\"preview\":").append(hasPreview ? "\"" + previewName + "\"" : "null")
                                .append(",");
                        jsonArray.append("\"duration\":").append(duration).append(",");
                        jsonArray.append("\"fps\":").append(fps);
                        jsonArray.append("}");
                    }
                }
            }

            jsonArray.append("]");
            String json = jsonArray.toString();

            os.write("HTTP/1.1 200 OK\r\n".getBytes());
            os.write("Content-Type: application/json\r\n".getBytes());
            os.write("Cache-Control: no-cache\r\n".getBytes());
            os.write(("Content-Length: " + json.length() + "\r\n").getBytes());
            os.write("\r\n".getBytes());
            os.write(json.getBytes());
            os.flush();
        }

        private void serveDashboard(OutputStream os) throws IOException {
            String html = generateDashboardHtml();
            byte[] body = html.getBytes("UTF-8");

            os.write("HTTP/1.1 200 OK\r\n".getBytes());
            os.write("Content-Type: text/html; charset=utf-8\r\n".getBytes());
            os.write("Cache-Control: no-cache, no-store, must-revalidate\r\n".getBytes());
            os.write(("Content-Length: " + body.length + "\r\n").getBytes());
            os.write("\r\n".getBytes());
            os.write(body);
            os.flush();
        }

        private void send404(OutputStream os) throws IOException {
            os.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
        }

        private void sendStringResponse(OutputStream os, String contentType, String body) throws IOException {
            os.write("HTTP/1.1 200 OK\r\n".getBytes());
            os.write(("Content-Type: " + contentType + "\r\n").getBytes());
            os.write(("Content-Length: " + body.getBytes().length + "\r\n").getBytes());
            os.write("\r\n".getBytes());
            os.write(body.getBytes());
            os.flush();
        }

    }

    private String generateDashboardHtml() {
        // LAZY LOAD: Video list generated by JavaScript, not Java
        StringBuilder listHtml = new StringBuilder();
        listHtml.append("<div id='video-list-container'></div>");
        listHtml.append(
                "<div id='loading-sentinel' style='text-align:center; padding:20px; color:#888;'>⏳ Cargando grabaciones...</div>");

        // Stats
        int batLevel = SystemStats.getBatteryLevel(context);
        boolean charging = SystemStats.isCharging(context);
        String freeStorage = SystemStats.getFreeStorageSpace();
        int temp = ThermalGuardian.getBatteryTemperature(context);

        String versionName = "v1.0";
        try {
            versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            e.printStackTrace();
        }

        String batIcon = charging ? "⚡" : (batLevel > 20 ? "🔋" : "🪫");
        String tempIcon = temp > 40 ? "🔥" : "🌡️";

        String commonHeader = getCommonHeaderHtml(versionName, batIcon, batLevel, tempIcon, temp, freeStorage);

        return "<!DOCTYPE html>\n" +
                "<html><head>\n" +
                "<meta charset='UTF-8'>\n" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>\n"
                +
                "<style>\n" +
                "body { background-color: #121212; color: #ffffff; font-family: sans-serif; margin: 0; display: flex; flex-direction: column; height: 100vh; overflow: hidden; }\n"
                +
                (SentinelService.isCameraError
                        ? ".camera-error { background: #d32f2f; color: white; padding: 15px; text-align: center; font-weight: bold; animation: blink 1s infinite; z-index: 2000; } @keyframes blink { 50% { opacity: 0.5; } }\\n"
                        : "")
                +
                ".header { padding: 12px; text-align: center; background: #1f1f1f; box-shadow: 0 2px 10px rgba(0,0,0,0.5); flex-shrink: 0; }\n"
                +
                ".stats-bar { display: flex; justify-content: space-around; background: #333; padding: 10px; margin: 10px; border-radius: 8px; font-size: 14px; flex-shrink: 0; }\n"
                +
                ".live-btn { display: inline-block; background: #d32f2f; color: white; padding: 6px 30px; border-radius: 50px; text-decoration: none; font-weight: bold; animation: pulse 2s infinite; }\n"
                +
                "@keyframes pulse { 0% { transform: scale(1); box-shadow: 0 0 0 0 rgba(211, 47, 47, 0.7); } 70% { transform: scale(1.05); box-shadow: 0 0 0 10px rgba(211, 47, 47, 0); } 100% { transform: scale(1); box-shadow: 0 0 0 0 rgba(211, 47, 47, 0); } }\n"
                +
                ".library { flex-grow: 1; flex-shrink: 1; flex-basis: 0; padding: 10px; padding-top: 0px; overflow-y: auto; height: 100%; min-height: 0; }\n"
                +
                ".section-title { font-size: 0.9em; text-transform: uppercase; color: #888; margin-bottom: 10px; letter-spacing: 1px; }\n"
                +
                // ANTES: padding: 15px;
                // AHORA: padding: 8px; (Más ajustado al borde)
                ".video-item { display: flex; align-items: center; background: #2c2c2c; margin-bottom: 10px; padding: 6px; border-radius: 12px; active: scale(0.98); transition: transform 0.1s, opacity 0.5s, filter 0.5s; }\n"
                +
                ".video-item.watched { opacity: 0.5; filter: grayscale(100%); }\n" +
                ".video-item:active { transform: scale(0.98); background: #3d3d3d; }\n" +
                ".video-item .icon { font-size: 24px; margin-right: 15px; }\n" +
                // ANTES: width: 80px; height: 60px;
                // AHORA: width: 150px; height: 110px; (¡GIGANTE!)
                // ahora: 110x90
                // Además mantenemos el degradado bonito que te dije antes.
                ".thumb-container { position: relative; width: 110px; height: 90px; min-width: 110px; margin-right: 12px; border-radius: 8px; overflow: hidden; background: #000; display: flex; justify-content: center; align-items: center; }\n"
                +
                ".thumb { width: 100%; height: 100%; object-fit: cover; object-position: center; position: absolute; top:0; left:0; transform-origin: 0 0; z-index: 5; }\n"
                +
                ".mini-canvas { width: 100%; height: 100%; position: absolute; object-position: center; top:0; left:0; z-index: 10; transform-origin: 0 0; object-fit: cover; }\n"
                +
                ".video-item .info { flex: 1; font-size: 14px; }\n" +
                "/* Modal Player */\n" +
                "#player-modal, #live-view-modal { display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: black; z-index: 1000; flex-direction: column; }\n"
                +
                "#canvas-container { flex: 1; display: flex; justify-content: center; align-items: center; overflow: hidden; position: relative; background-color: #000; width: 100%; height: auto; touch-action: none; }\n"
                +
                "img#video-player, img#live-stream-img { max-width: 100%; max-height: 100%; width: 100%; height: 100%; object-fit: cover; display: block; transform-origin: 0 0; -webkit-transform-origin: 0 0; }\n"
                +
                ".controls { padding: 20px; background: rgba(20,20,20,0.9); display: flex; align-items: center; gap: 10px; }\n"
                +
                ".btn-close { color: white; background: none; border: none; font-size: 20px; padding: 10px; }\n" +
                "input[type=range] { flex: 1; }\n" +
                "/* Settings Modal */\n" +
                "#settings-modal { display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.8); z-index: 2000; justify-content: center; align-items: center; }\n"
                +
                ".settings-content { background: #222; padding: 25px; border-radius: 12px; width: 85%; max-width: 400px; color: white; box-shadow: 0 4px 15px rgba(0,0,0,0.5); max-height: 85vh; overflow-y: auto; }\n"
                +
                ".settings-row { margin-bottom: 20px; display: flex; align-items: center; justify-content: space-between; }\n"
                +
                ".btn-save { background: #2e7d32; color: white; padding: 10px 20px; border: none; border-radius: 5px; font-weight: bold; flex: 1; margin-right: 10px; }\n"
                +
                ".btn-cancel { background: #c62828; color: white; padding: 10px 20px; border: none; border-radius: 5px; font-weight: bold; flex: 1; }\n"
                +
                ".zoom-controls { display: flex; gap: 10px; margin-top: 5px; }\n" +
                "label { font-size: 16px; }\n" +
                "/* Switch Toggle */\n" +
                ".switch { position: relative; display: inline-block; width: 42px; height: 24px; }\n" +
                ".switch input { opacity: 0; width: 0; height: 0; }\n" +
                ".slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background-color: #555; transition: .4s; border-radius: 24px; }\n"
                +
                ".slider:before { position: absolute; content: ''; height: 18px; width: 18px; left: 3px; bottom: 3px; background-color: white; transition: .4s; border-radius: 50%;box-shadow: 0 2px 4px rgba(0,0,0,0.2); }\n"
                +
                "input:checked + .slider { background-color: #29b6f6; }\n" +
                "input:checked + .slider:before { transform: translateX(18px); }\n" +
                "</style>\n" +
                "</head><body>\n" +
                "\n" +
                (SentinelService.isCameraError
                        ? "<div class='camera-error'>⚠️ ERROR CRÍTICO: CÁMARA NO RESPONDE - REINICIA EL MÓVIL</div>\n"
                        : "")
                +
                // [FIX ZOMBIE] Generamos identidad única para esta pestaña
                "<script>var SESSION_ID = Math.random().toString(36).substring(7); console.log('Session ID:', SESSION_ID);</script>\n"
                +
                commonHeader
                +
                "  <div style='text-align:center; padding-bottom:10px; flex-shrink:0;'>\n" +
                "      <div class='live-btn' onclick='openLiveView()' style='cursor:pointer;'>🔴 VER CÁMARA EN VIVO</div>\n"
                +
                "      <div style='margin-top:10px; font-size:12px; color:#666;'>Status: " + lastError + " | Boot: "
                + SystemStats.getBootTime() + "</div>\n" +
                "  </div>\n" +
                "\n" +
                "<div class='library'>\n" +
                "  <div class='section-title'>📼 Grabaciones</div>\n" +
                listHtml.toString() +
                "</div>\n" +
                "\n" +
                "<div id='player-modal'>\n" +
                commonHeader +
                "  <div class='controls' style='justify-content:space-between;'>\n" +
                "      <span id='video-title'>Video</span>\n" +
                "      <button class='btn-close' onclick='closePlayer()'>❌</button>\n" +
                "  </div>\n" +
                "  <div id='canvas-container' style='position:relative;'>\n" +
                "      <div id='hud-stats' style='position:absolute; top:10px; left:10px; background:rgba(0,0,0,0.6); color:#0f0; padding:4px 8px; font-family:monospace; font-size:12px; pointer-events:none; z-index:100; border-radius:4px;'>ZOOM: 1.0x | X: 0 | Y: 0</div>\n"
                +
                "      <img id='video-player'>\n" +
                "  </div>\n" +
                "  <div class='controls'>\n" +
                "      <button class='btn-close' id='play-pause' style='font-size:24px;'>⏸</button>\n" +
                "      <span id='current-frame'>0</span>\n" +
                "      <input type='range' id='scrubber' min='0' max='100' value='0' disabled>\n" +
                "      <span id='total-frames'>...</span>\n" +
                "  </div>\n" +
                "</div>\n" +
                "\n" +
                "<div id='live-view-modal'>\n" +
                commonHeader +
                "  <div style='flex:1; display:flex; justify-content:center; align-items:center; background:#000; overflow:hidden;'>\n"
                +
                "      <img id='live-stream-img' style='max-width:100%; max-height:100%; object-fit:contain;'>\n" +
                "  </div>\n" +
                "  <div class='controls' style='justify-content:center;'>\n" +
                "      <button class='btn-close' onclick='closeLiveView()'>❌ CERRAR</button>\n" +
                "  </div>\n" +
                "</div>\n" +
                "\n" +
                // --- SETTINGS MODAL (UPDATED WITH DUAL ZOOM) ---
                "<div id='settings-modal'>\n" +
                "  <div class='settings-content'>\n" +
                "      <div style='display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #444; padding-bottom:10px; margin-bottom:20px;'>\n"
                +
                "         <h3 style='margin:0;'>Configuración ⚙️</h3>\n" +
                "         <span onclick='closeSettings()' style='cursor:pointer; font-size:24px;'>&times;</span>\n" +
                "      </div>\n" +

                // 1. WEB SETTINGS
                "      <div style='margin-bottom:15px; border-bottom:1px solid #444; padding-bottom:10px;'>" +
                "         <h4 style='margin:0 0 10px 0; color:#4fc3f7;'>🌍 Vista Web (Navegador)</h4>" +
                "         <label>Escala Web: <span id='web-zoom-val'>1.0x</span></label>" +
                "         <input type='range' id='web-zoom' min='1' max='3' step='0.1' value='1' style='width:100%' oninput='updateWebTransformFromInputs()'>"
                +
                "         <div style='display:flex; gap:10px; align-items:center; margin-top:5px;'>" +
                "           <span style='font-size:12px; color:#aaa; min-width:60px;'>Posición:</span>" +
                "           <div style='position:relative; flex:1; max-width:90px;'>" +
                "               <span style='position:absolute; left:8px; top:50%; transform:translateY(-50%); color:#aaa; pointer-events:none; font-size:14px;'>↔</span>"
                +
                "               <input type='number' id='web-pan-x' placeholder='X' style='width:100%; padding:5px 5px 5px 25px; background:#333; color:white; border:1px solid #555; border-radius:4px; text-align:right;' oninput='updateWebTransformFromInputs()'>"
                +
                "           </div>" +
                "           <div style='width:20px; flex-shrink:0;'></div>" + // FORCED SPACER
                "           <div style='position:relative; flex:1; max-width:90px;'>" +
                "               <span style='position:absolute; left:8px; top:50%; transform:translateY(-50%); color:#aaa; pointer-events:none; font-size:14px;'>↕</span>"
                +
                "               <input type='number' id='web-pan-y' placeholder='Y' style='width:100%; padding:5px 5px 5px 25px; background:#333; color:white; border:1px solid #555; border-radius:4px; text-align:right;' oninput='updateWebTransformFromInputs()'>"
                +
                "           </div>" +
                "         </div>" +
                "         <div style='font-size:11px; color:#aaa; margin-top:5px;'>* Zoom y Desplazamiento (Pan) del video mostrado en el navegador.</div>"
                +
                "      </div>" +

                // 2. HARDWARE SETTINGS
                "      <div style='margin-bottom:15px; border-bottom:1px solid #444; padding-bottom:10px;'>" +
                "         <h4 style='margin:0 0 10px 0; color:#ffa726;'>📱 Vista Abuelo (Pantalla)</h4>" +
                "         <label>Zoom Físico: <span id='hw-zoom-val'>1.0x</span></label>" +
                "         <input type='range' id='hw-zoom' min='1' max='4' step='0.1' style='width:100%' oninput=\"document.getElementById('hw-zoom-val').textContent=this.value+'x'\">"
                +
                "         <div style='display:flex; gap:10px; align-items:center; margin-top:5px;'>" +
                "           <span style='font-size:12px; color:#aaa; min-width:60px;'>Posición:</span>" +
                "           <div style='position:relative; flex:1; max-width:90px;'>" +
                "               <span style='position:absolute; left:8px; top:50%; transform:translateY(-50%); color:#aaa; pointer-events:none; font-size:14px;'>↔</span>"
                +
                "               <input type='number' id='hw-pan-x' placeholder='X' style='width:100%; padding:5px 5px 5px 25px; background:#333; color:white; border:1px solid #555; border-radius:4px; text-align:right;'>"
                +
                "           </div>" +
                "           <div style='width:20px; flex-shrink:0;'></div>" + // FORCED SPACER
                "           <div style='position:relative; flex:1; max-width:90px;'>" +
                "               <span style='position:absolute; left:8px; top:50%; transform:translateY(-50%); color:#aaa; pointer-events:none; font-size:14px;'>↕</span>"
                +
                "               <input type='number' id='hw-pan-y' placeholder='Y' style='width:100%; padding:5px 5px 5px 25px; background:#333; color:white; border:1px solid #555; border-radius:4px; text-align:right;'>"
                +
                "           </div>" +
                "         </div>" +
                "         <div style='font-size:11px; color:#aaa; margin-top:5px;'>* Zoom y Desplazamiento (Pan) del video mostrado en la pantalla del móvil de la cámara.</div>"
                +
                "      </div>" +

                "      <div class='settings-row' style='margin-top:15px; border-top:1px dashed #444; padding-top:10px;'>\n"
                +
                "          <label>Estampado Fecha (OSD):</label>\n" +
                "          <a href='/config/osd' target='_blank' style='background:#222; border:1px solid #0f0; color:#0f0; padding:6px 15px; text-decoration:none; border-radius:4px; font-size:13px; font-family:monospace;'>📟 AJUSTAR POSICIÓN</a>\n"
                +
                "      </div>\n" +
                "      <div style='font-size:11px; color:#aaa; margin-top:5px; margin-bottom:15px;'>* Abre el editor visual en una pestaña nueva.</div>\n"
                +

                // 3. GENERAL SETTINGS
                "      <div class='settings-row'>\n" +
                "         <label>Detector Activado:</label>\n" +
                "         <input type='checkbox' id='set-active' style='transform: scale(1.5);'>\n" +
                "      </div>\n" +
                "      <div style='font-size:11px; color:#aaa; margin-top:-5px; margin-bottom:10px;'>* Activa la vigilancia y grabación por movimiento.</div>\n"
                +
                "      <div class='settings-row'>\n" +
                "         <label>Pre-Record Buffer (5s):</label>\n" +
                "         <input type='checkbox' id='set-prerecord' style='transform: scale(1.5);'>\n" +
                "      </div>\n" +
                "      <div style='font-size:11px; color:#aaa; margin-top:-5px; margin-bottom:15px;'>* Graba lo que pasó ANTES del movimiento.</div>\n"
                +
                "      <div class='settings-row'>\n" +
                "         <label>🥷 Modo Sigilo (Pantalla Off):</label>\n" +
                "         <input type='checkbox' id='set-stealth' style='transform: scale(1.5);'>\n" +
                "      </div>\n" +
                "      <div style='font-size:11px; color:#aaa; margin-top:-5px; margin-bottom:15px;'>* Graba sin encender la pantalla (Ahorro Batería).</div>\n"
                +
                "      <div class='settings-row'>\n" +
                "         <label>🛡️ Filtrar Falsos Positivos:</label>\n" +
                "         <input type='checkbox' id='set-filter' style='transform: scale(1.5);'>\n" +
                "      </div>\n" +
                "      <div style='font-size:11px; color:#aaa; margin-top:-5px; margin-bottom:15px;'>* ON: Requiere 3 frames (evita luces). OFF: Grabación inmediata (1 frame).</div>\n"
                +
                "      <div class='settings-row'>\n" +
                "         <label>📏 Sensibilidad al Tamaño: <span id='sens-label' style='color:#aaa; font-size:14px;'>90%</span></label>\n"

                +
                "      </div>\n" +
                "      <div class='settings-row' style='margin-bottom:5px;'>\n" +
                "            <input type='range' id='sens-slider' min='0' max='100' oninput='updateSensLabel(this.value)'>\n"
                +
                "      </div>\n" +
                "\n" +
                "      <div class='settings-row'>\n" +
                "         <label>🎨 Sensibilidad al Contraste: <span id='contrast-label' style='color:#aaa; font-size:14px;'>50%</span></label>\n"
                +
                "      </div>\n" +
                "      <div class='settings-row' style='margin-bottom:5px;'>\n" +
                "            <input type='range' id='contrast-slider' min='0' max='100' oninput='updateContrastLabel(this.value)'>\n"
                +
                "      </div>\n" +
                "      <div style='font-size:10px; color:#666; margin-bottom:15px; text-align:right'>* 100% = Detecta camuflaje / 0% = Solo luces fuertes</div>\n"
                +
                "      <div style='font-size:11px; color:#aaa; margin-top:-15px; margin-bottom:15px;'>* Umbral de movimiento para iniciar grabación.</div>\n"
                +
                "      <div class='settings-row'>\n" +
                "         <label>Tiempo extra de Grabación:</label>\n" +
                "         <input type='number' id='set-time' style='width:60px; padding:5px; background:#333; color:white; border:1px solid #555; text-align:right;' min='5' max='60'>\n"
                +
                "      </div>\n" +
                "      <div style='font-size:11px; color:#aaa; margin-top:-5px; margin-bottom:10px;'>* Segundos a grabar tras cesar el movimiento.</div>\n"
                +
                "      <div class='settings-row'>\n" +
                "         <label>Min. Espacio (MB):</label>\n" +
                "         <input type='number' id='set-min-space' style='width:70px; padding:5px; background:#333; color:white; border:1px solid #555; text-align:right;' min='100' max='5000'>\n"
                +
                "      </div>\n" +
                "      <div style='margin-top:20px; text-align:right;'>\n" +
                "        <button onclick='openDeleteModal()' style='background:#330000; border:1px solid #550000; color:#ff4444; padding:8px 15px; border-radius:4px; font-size:12px;'>🗑️ BORRAR TODO</button>\n"
                +
                "      </div>\n" +
                "      <div style='font-size:11px; color:#aaa; margin-top:-5px; margin-bottom:10px;'>* Borra videos antiguos si queda menos espacio (MB).</div>\n"
                +
                "      <div class='settings-row'>\n" +
                "         <label>Rotación:</label>\n" +
                "         <div>\n" +
                "            <input type='radio' name='rot' value='0' id='rot-0' checked> 0°\n" +
                "            <input type='radio' name='rot' value='180' id='rot-180'> 180°\n" +
                "         </div>\n" +
                "      </div>\n" +
                "      <div style='font-size:11px; color:#aaa; margin-top:-5px; margin-bottom:10px;'>* Rota la imagen si aparece al revés (suelo en el techo).</div>\n"
                +
                // 4. TELEGRAM SETTINGS
                "      <div style='margin-bottom:15px; border-bottom:1px solid #444; padding-bottom:10px; margin-top:15px;'>"
                +
                "         <div style='display:flex; justify-content:space-between; align-items:center; margin-bottom:10px;'>\n"
                +
                "             <h4 style='margin:0; color:#29b6f6;'>✈️ Notificaciones Telegram</h4>\n" +
                "             <div style='display:flex; align-items:center;'>\n" +
                "                 <span style='font-size:12px; margin-right:10px; color:#ddd;'>Activar</span>\n" +
                "                 <label class='switch'>\n" +
                "                     <input type='checkbox' id='tg-active' checked>\n" +
                "                     <span class='slider'></span>\n" +
                "                 </label>\n" +
                "             </div>\n" +
                "         </div>\n" +
                "         <input type='hidden' id='tg-token' value=''>" +
                "         <input type='hidden' id='tg-chatid' value=''>" +
                "         <button onclick='openTelegramModal()' style='width:100%; padding:8px; background:#1a237e; border:1px solid #29b6f6; color:#29b6f6; border-radius:4px; cursor:pointer; font-size:13px;'>⚙️ Configurar Bot Token y Chat ID</button>"
                +
                "      </div>\n" +
                "\n" +

                "      <div style='font-size:11px; color:#aaa; margin-top:-5px; margin-bottom:15px;'>* Ignora cambios masivos de luz (bombillas).</div>\n"
                +
                "\n" +
                "      <div style='display:flex; margin-top:20px;'>\n" +
                "         <button class='btn-save' onclick='saveSettings()'>GUARDAR</button>\n" +
                "         <button class='btn-cancel' onclick='closeSettings()'>CANCELAR</button>\n" +
                "      </div>\n" +
                "      <!-- Opción B: Footer Diagnóstico -->\n" +
                "      <div style='margin-top:20px; text-align:center; padding-bottom:40px;'>\n" +
                "         <a href='#' onclick=\"window.open('/api/debug'); return false;\" style='color:#ef5350; text-decoration:none; font-size:12px; border-bottom:1px dotted #ef5350;'>⚠️ VER LOGS DE DEBUG</a>\n"
                +
                "      </div>\n" +
                "  </div>\n" +
                "</div>\n" +
                "\n" +
                "<!-- DATA DELETION CONFIRMATION MODAL (TWRP STYLE) -->\n" +
                "<div id='delete-modal' style='display:none; position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(20,0,0,0.95); z-index:5000; flex-direction:column; justify-content:center; align-items:center;'>\n"
                +
                "    <div style='text-align:center; color:#ff4444; margin-bottom:40px; padding:0 20px;'>\n" +
                "        <div style='font-size:60px; margin-bottom:20px;'>⚠️</div>\n" +
                "        <h2 style='margin:0; text-transform:uppercase; letter-spacing:2px;'>Zona de Peligro</h2>\n" +
                "        <p style='color:#ccc; margin-top:10px;'>¿Estás seguro de que quieres borrar <b>TODAS</b> las grabaciones?</p>\n"
                +
                "        <p style='color:#888; font-size:12px;'>Esta acción es irreversible.</p>\n" +
                "    </div>\n" +
                "\n" +
                "    <!-- SLIDER CONTAINER -->\n" +
                "    <div id='slider-track' style='position:relative; width:80%; max-width:300px; height:60px; background:#330000; border:2px solid #550000; border-radius:30px; overflow:hidden; touch-action:none; user-select:none;'>\n"
                +
                "        <div style='position:absolute; width:100%; height:100%; display:flex; justify-content:center; align-items:center; color:#aa5555; fontWeight:bold; font-size:14px; letter-spacing:1px; z-index:1;'>\n"
                +
                "            DESLIZA PARA BORRAR &gt;&gt;&gt;\n" +
                "        </div>\n" +
                "        <div id='slider-thumb' style='position:absolute; left:0; top:0; width:60px; height:60px; background:#ff4444; border-radius:50%; z-index:2; display:flex; justify-content:center; align-items:center; box-shadow:0 0 15px rgba(255,0,0,0.5); cursor:grab;'>\n"
                +
                "            🗑️\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <button onclick='closeDeleteModal()' style='margin-top:40px; background:none; border:1px solid #666; color:#888; padding:10px 30px; border-radius:20px;'>CANCELAR</button>\n"
                +
                "</div>\n" +
                "\n" +
                "<!-- TELEGRAM CONFIG MODAL -->\n" +
                "<div id='telegram-modal' style='display:none; position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,10,30,0.95); z-index:5000; flex-direction:column; justify-content:center; align-items:center;'>\n"
                +
                "    <div style='width:85%; max-width:350px; background:#1a1a2e; border:1px solid #29b6f6; border-radius:12px; padding:25px;'>\n"
                +
                "        <h3 style='margin:0 0 15px 0; color:#29b6f6; text-align:center;'>✈️ Configuración Bot Telegram</h3>\n"
                +
                "        <label style='font-size:12px; color:#aaa;'>Bot Token:</label>\n" +
                "        <div style='display:flex; align-items:center; margin-bottom:10px;'>\n" +
                "            <input type='password' id='tg-token-modal' placeholder='123456:ABC-Def...' style='flex:1; padding:8px; background:#333; color:#fff; border:1px solid #555; border-radius:4px;'>\n"
                +
                "            <span onclick='toggleTgToken()' style='margin-left:8px; cursor:pointer; font-size:18px; user-select:none;'>👁️</span>\n"
                +
                "        </div>\n" +
                "        <label style='font-size:12px; color:#aaa;'>Chat ID:</label>\n" +
                "        <input type='text' id='tg-chatid-modal' placeholder='12345678' style='width:100%; padding:8px; background:#333; color:#fff; border:1px solid #555; border-radius:4px; margin-bottom:15px; box-sizing:border-box;'>\n"
                +
                "        <button onclick='testTelegramFromModal()' style='width:100%; padding:8px; background:#0288d1; border:none; color:white; border-radius:4px; cursor:pointer; margin-bottom:10px;'>🔔 PROBAR CONEXIÓN</button>\n"
                +
                "        <div style='display:flex; gap:10px;'>\n" +
                "            <button onclick='saveTelegramModal()' style='flex:1; padding:8px; background:#2E7D32; border:none; color:white; border-radius:4px; cursor:pointer; font-weight:bold;'>✅ GUARDAR</button>\n"
                +
                "            <button onclick='closeTelegramModal()' style='flex:1; padding:8px; background:none; border:1px solid #666; color:#888; border-radius:4px; cursor:pointer;'>CANCELAR</button>\n"
                +
                "        </div>\n" +
                "    </div>\n" +
                "</div>\n" +
                "\n" +
                // --- FULL JAVASCRIPT LOGIC (RESTORED FROM YOUR BACKUP) ---
                "<script>\n" +
                "var frames = [];\n" +
                "var currentFrameIdx = 0;\n" +
                "var isPlaying = false;\n" +
                "var fps = 10;\n" +
                "var currentObjectUrl = null;\n" +
                "var gWebZoom = 1.0; var gWebPanX = 0; var gWebPanY = 0;\n" +
                "\n" +
                "// --- HEARTBEAT SYSTEM (SISTEMA DE LATIDOS) ---\n" +
                "var heartbeatInterval = null;\n" +
                "\n" +
                "function sendHeartbeat() {\n" +
                "    var sid = (typeof SESSION_ID !== 'undefined') ? SESSION_ID : 'unknown';\n" +
                "    fetch('/api/keepalive?session_id=' + sid).catch(e => {});\n" +
                "}\n" +
                "\n" +
                "function startHeartbeat() {\n" +
                "    // Solo arrancamos si no hay uno ya corriendo\n" +
                "    if (heartbeatInterval) return;\n" +
                "    sendHeartbeat(); // El primero inmediato\n" +
                "    heartbeatInterval = setInterval(sendHeartbeat, 2000); // Latido cada 2s\n" +
                "}\n" +
                "\n" +
                "function stopHeartbeat() {\n" +
                "    // Solo paramos si no hay NINGÚN consumidor activo (ni modal ni parásito)\n" +
                "    var modalOpen = document.getElementById('live-view-modal').style.display === 'flex';\n" +
                "    var parasiteActive = (document.getElementById('hidden-stream-source') !== null);\n" +
                "    \n" +
                "    if (!modalOpen && !parasiteActive) {\n" +
                "        if (heartbeatInterval) {\n" +
                "            clearInterval(heartbeatInterval);\n" +
                "            heartbeatInterval = null;\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "// --- ZOOM / TRANSFORM LOGIC (INJECTED) ---\n" +
                "function updateWebTransform(z, x, y) {\n" +
                "  document.getElementById('web-zoom-val').textContent = z + 'x';\n" +
                "  \n" +
                "  // LÓGICA DE PORCENTAJES (RESPONSIVE): \n" +
                "  // Al usar '%', el navegador calcula el desplazamiento relativo al ancho de cada imagen.\n" +
                "  // Si x=10, mueve el player grande un 10% de su ancho, y la miniatura un 10% de su ancho.\n" +
                "  // El resultado visual es idéntico en ambos sin matemáticas extra.\n" +
                "  \n" +
                "  var transformCSS = 'translate(' + x + '%, ' + y + '%) scale(' + z + ')';\n" +
                "  \n" +
                "  // 1. Aplicar a reproductores grandes (Modal y Live)\n" +
                "  var bigPlayers = document.querySelectorAll('#video-player, #live-stream-img');\n" +
                "  for(var i=0; i<bigPlayers.length; i++) { bigPlayers[i].style.transform = transformCSS; }\n" +
                "  \n" +
                "  // 2. Aplicar a miniaturas (Thumbnails y Canvas)\n" +
                "  var thumbs = document.querySelectorAll('img.thumb, canvas.mini-canvas');\n" +
                "  for(var k=0; k<thumbs.length; k++) { thumbs[k].style.transform = transformCSS; }\n" +
                "}\n" +
                "function updateWebTransformFromInputs() {\n" +
                "  var z = document.getElementById('web-zoom').value;\n" +
                "  var x = document.getElementById('web-pan-x').value || 0;\n" +
                "  var y = document.getElementById('web-pan-y').value || 0;\n" +
                "  gWebZoom = parseFloat(z); gWebPanX = parseInt(x); gWebPanY = parseInt(y);\n" +
                "  updateWebTransform(z, x, y);\n" +
                "}\n" +

                "function playVideo(file) {\n" +
                "  var items = document.getElementsByClassName('video-item');\n" +
                "  for(var i=0; i<items.length; i++) {\n" +
                "      var clickAttr = items[i].getAttribute('onclick');\n" +
                // --- EL SEGURO DE VIDA: Comprobamos que clickAttr exista antes del indexOf ---
                "      if(clickAttr && typeof clickAttr === 'string' && clickAttr.indexOf(file) !== -1) {\n" +
                "          items[i].classList.add('watched');\n" +
                "          break;\n" +
                "      }\n" +
                "  }\n" +
                "  document.getElementById('player-modal').style.display = 'flex';\n" +
                "  document.getElementById('video-title').textContent = file;\n" +
                "  frames = [];\n" +
                "  currentFrameIdx = 0;\n" +
                "  document.getElementById('scrubber').value = 0;\n" +
                "  resetZoom();\n" +
                "  \n" +
                "  var match = file.match(/_(\\d+)fps/);\n" +
                "  fps = match ? parseInt(match[1]) : 15;\n" +
                "  document.getElementById('total-frames').textContent = 'Loading...';\n" +
                "  \n" +
                "  fetch('/' + file).then(response => {\n" +
                "    const reader = response.body.getReader();\n" +
                "    return new ReadableStream({\n" +
                "      start(controller) {\n" +
                "        return Pump();\n" +
                "        function Pump() {\n" +
                "          return reader.read().then(({ done, value }) => {\n" +
                "            if (done) { controller.close(); onDownloadComplete(); return; }\n" +
                "            parseMJPEGChunk(value);\n" +
                "            Pump();\n" +
                "          });\n" +
                "        }\n" +
                "      }\n" +
                "    });\n" +
                "  });\n" +
                "}\n" +
                "\n" +
                "var buffer = new Uint8Array(0);\n" +
                "function parseMJPEGChunk(chunk) {\n" +
                "  var newBuffer = new Uint8Array(buffer.length + chunk.length);\n" +
                "  newBuffer.set(buffer); newBuffer.set(chunk, buffer.length);\n" +
                "  buffer = newBuffer;\n" +
                "  while (true) {\n" +
                "    var start = -1;\n" +
                "    for(var i=0; i<buffer.length-1; i++) { if(buffer[i] === 0xFF && buffer[i+1] === 0xD8) { start = i; break; } }\n"
                +
                "    if(start === -1) break;\n" +
                "    var end = -1;\n" +
                "    for(var i=start+2; i<buffer.length-1; i++) { if(buffer[i] === 0xFF && buffer[i+1] === 0xD9) { end = i+2; break; } }\n"
                +
                "    if(end === -1) break;\n" +
                "    var jpegData = buffer.slice(start, end);\n" +
                "    var blob = new Blob([jpegData], {type: 'image/jpeg'});\n" +
                "    frames.push(blob);\n" +
                "    if(frames.length === 1) requestAnimationFrame(drawLoop);\n" +
                "    updateScrubber();\n" +
                "    buffer = buffer.slice(end);\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "function formatTime(seconds) {\n" +
                "   var m = Math.floor(seconds / 60);\n" +
                "   var s = Math.floor(seconds % 60);\n" +
                "   return (m < 10 ? '0'+m : m) + ':' + (s < 10 ? '0'+s : s);\n" +
                "}\n" +
                "\n" +
                "function onDownloadComplete() {\n" +
                "   var duration = frames.length / fps;\n" +
                "   document.getElementById('total-frames').textContent = formatTime(duration);\n" +
                "   document.getElementById('scrubber').max = frames.length - 1;\n" +
                "   document.getElementById('scrubber').disabled = false;\n" +
                "   setIsPlaying(true);\n" +
                "}\n" +
                "\n" +
                "function updateScrubber() {\n" +
                "   if(frames.length % 10 === 0 && frames.length > 0 && fps > 0) {\n" +
                "       document.getElementById('total-frames').textContent = formatTime(frames.length / fps) + '...';\n"
                +
                "   }\n" +
                "}\n" +
                "\n" +
                "function drawLoop() {\n" +
                "  if (!isPlaying) return;\n" +
                "  if (frames.length > 0) {\n" +
                "      drawFrame(currentFrameIdx);\n" +
                "      currentFrameIdx = (currentFrameIdx + 1);\n" +
                "      if (currentFrameIdx >= frames.length) currentFrameIdx = 0;\n" +
                "      document.getElementById('scrubber').value = currentFrameIdx;\n" +
                "      document.getElementById('current-frame').textContent = formatTime(currentFrameIdx / fps);\n" +
                "  }\n" +
                "  setTimeout(() => requestAnimationFrame(drawLoop), 1000 / fps);\n" +
                "}\n" +
                "\n" +
                "function setIsPlaying(playing) {\n" +
                "    isPlaying = playing;\n" +
                "    document.getElementById('play-pause').textContent = isPlaying ? '⏸' : '▶';\n" +
                "    if(isPlaying) requestAnimationFrame(drawLoop);\n" +
                "}\n" +
                "\n" +
                "document.getElementById('play-pause').addEventListener('click', function() { setIsPlaying(!isPlaying); });\n"
                +
                "\n" +
                "function drawFrame(idx) {\n" +
                "   if(!frames[idx]) return;\n" +
                "   if (currentObjectUrl) URL.revokeObjectURL(currentObjectUrl);\n" +
                "   currentObjectUrl = URL.createObjectURL(frames[idx]);\n" +
                "   document.getElementById('video-player').src = currentObjectUrl;\n" +
                "}\n" +
                "\n" +
                "var playerImg = document.getElementById('video-player');\n" +
                "var container = document.getElementById('canvas-container');\n" +
                "var mat = { x: 0, y: 0, s: 1 };\n" +
                "var drag = { startX: 0, startY: 0, initialX: 0, initialY: 0 };\n" +
                "var pinch = { dist: 0, midX: 0, midY: 0, initialS: 1, initialX: 0, initialY: 0 };\n" +
                "var lastTap = 0;\n" +
                "var isMultiTouch = false;\n" +
                "\n" +
                "function resetZoom() { mat.x = gWebPanX; mat.y = gWebPanY; mat.s = gWebZoom; updateTransform(); }\n" +
                "function updateTransform() {\n" +
                "   var t = 'translate(' + mat.x + '%, ' + mat.y + '%) scale(' + mat.s + ')';\n" +
                "   playerImg.style.transform = t;\n" +
                "   playerImg.style.webkitTransform = t;\n" +
                "   var hud = document.getElementById('hud-stats');\n" +
                "   if(hud) hud.textContent = 'ZOOM: ' + mat.s.toFixed(1) + 'x | X: ' + Math.round(mat.x) + ' | Y: ' + Math.round(mat.y);\n"
                +
                "}\n" +
                "function getDist(t1, t2) { var dx = t1.pageX - t2.pageX; var dy = t1.pageY - t2.pageY; return Math.sqrt(dx*dx + dy*dy); }\n"
                +
                "function getMid(t1, t2) { return { x: (t1.pageX + t2.pageX) / 2, y: (t1.pageY + t2.pageY) / 2 }; }\n"
                +
                "container.addEventListener('touchstart', function(e) {\n" +
                "   if(e.cancelable) e.preventDefault();\n" +
                "   if (e.touches.length > 1) isMultiTouch = true; else isMultiTouch = false;\n" +
                "   if (e.touches.length === 2) {\n" +
                "       pinch.dist = getDist(e.touches[0], e.touches[1]);\n" +
                "       var mid = getMid(e.touches[0], e.touches[1]);\n" +
                "       pinch.midX = mid.x; pinch.midY = mid.y;\n" +
                "       pinch.initialS = mat.s; pinch.initialX = mat.x; pinch.initialY = mat.y;\n" +
                "   } else if (e.touches.length === 1) {\n" +
                "       drag.startX = e.touches[0].pageX; drag.startY = e.touches[0].pageY;\n" +
                "       drag.initialX = mat.x; drag.initialY = mat.y;\n" +
                "   }\n" +
                "}, { passive: false });\n" +
                "container.addEventListener('touchmove', function(e) {\n" +
                "   if(e.cancelable) e.preventDefault();\n" +
                "   if (e.touches.length === 2 && pinch.dist > 0) {\n" +
                "       var newDist = getDist(e.touches[0], e.touches[1]);\n" +
                "       var scaleFactor = newDist / pinch.dist;\n" +
                "       var newScale = Math.max(1, Math.min(pinch.initialS * scaleFactor, 5));\n" +
                "       var ratio = newScale / pinch.initialS;\n" +
                "       mat.x = pinch.midX - (pinch.midX - pinch.initialX) * ratio;\n" +
                "       mat.y = pinch.midY - (pinch.midY - pinch.initialY) * ratio;\n" +
                "       mat.s = newScale;\n" +
                "       updateTransform();\n" +
                "   } else if (e.touches.length === 1 && mat.s > 1.05) {\n" +
                "       var dx = e.touches[0].pageX - drag.startX;\n" +
                "       var dy = e.touches[0].pageY - drag.startY;\n" +
                "       mat.x = drag.initialX + dx; mat.y = drag.initialY + dy;\n" +
                "       updateTransform();\n" +
                "   }\n" +
                "}, { passive: false });\n" +
                "container.addEventListener('touchend', function(e) {\n" +
                "   var now = new Date().getTime();\n" +
                "   if (e.touches.length === 0) {\n" +
                "       if (!isMultiTouch && (now - lastTap < 300)) { resetZoom(); e.preventDefault(); }\n" +
                "       isMultiTouch = false;\n" +
                "   }\n" +
                "   lastTap = now;\n" +
                "   if (e.touches.length === 1) {\n" +
                "       drag.startX = e.touches[0].pageX; drag.startY = e.touches[0].pageY;\n" +
                "       drag.initialX = mat.x; drag.initialY = mat.y;\n" +
                "   }\n" +
                "});\n" +
                "document.getElementById('scrubber').addEventListener('input', function(e) { setIsPlaying(false); currentFrameIdx = parseInt(e.target.value); document.getElementById('current-frame').textContent = formatTime(currentFrameIdx / fps); drawFrame(currentFrameIdx); });\n"
                +
                "document.getElementById('scrubber').addEventListener('change', function(e) { setIsPlaying(true); });\n"
                +
                "function closePlayer() { document.getElementById('player-modal').style.display = 'none'; setIsPlaying(false); frames = []; }\n"
                +
                "\n" +
                "function startStatsUpdater() {\n" +
                "  var lastTemp = null;\n" +
                "  var lastTrend = '';\n" +
                "  setInterval(function() {\n" +
                "    fetch('/stats').then(r => r.json()).then(data => {\n" +
                "      var batIcon = data.charging ? '⚡' : (data.bat > 20 ? '🔋' : '🪫');\n" +
                "      \n" +
                "      // FIX: Usamos querySelectorAll para actualizar TODAS las copias de los stats (Main y Modales)\n"
                +
                "      document.querySelectorAll('.stat-bat').forEach(function(el) { el.innerText = batIcon + ' ' + data.bat + '%'; });\n"
                +
                "      \n" +
                "      if (lastTemp === null) lastTemp = data.temp;\n" +
                "      if (data.temp > lastTemp) lastTrend = ' <span style=\"color:#ff4444; font-size:0.8em;\">▲</span>';\n"
                +
                "      else if (data.temp < lastTemp) lastTrend = ' <span style=\"color:#66ff66; font-size:0.8em;\">▼</span>';\n"
                +
                "      lastTemp = data.temp;\n" +
                "      var tempIcon = data.temp > 40 ? '🔥' : '🌡️';\n" +
                "      \n" +
                "      document.querySelectorAll('.stat-temp').forEach(function(el) { el.innerHTML = tempIcon + ' ' + data.temp + '°C' + lastTrend; });\n"
                +
                "      document.querySelectorAll('.stat-storage').forEach(function(el) { el.innerText = '💾 ' + data.storage; });\n"
                +
                "      \n" +
                "      // Status update (color logic)\n" +
                "      document.querySelectorAll('.stat-status').forEach(function(el) {\n" +
                "          el.innerText = data.recording ? '🔴 GRABANDO' : '⏺️ VIGILANDO';\n" +
                "          el.style.color = data.recording ? '#ff4444' : '#ffffff';\n" +
                "      });\n" +
                "      \n" +
                "    }).catch(e => console.log('Stats error', e));\n" +
                "  }, 5000);\n" +
                "}\n" +
                "function openLiveView() {\n" +
                "    document.getElementById('live-view-modal').style.display = 'flex';\n" +
                "    document.getElementById('live-stream-img').src = '/stream?session_id=' + SESSION_ID;\n" +
                "    history.pushState(null, null, location.href);\n" +
                "    startHeartbeat();\n" + // <--- AÑADIDO
                "}\n" +
                "function closeLiveView() {\n" +
                "    document.getElementById('live-view-modal').style.display = 'none';\n" +
                "    // Limpieza agresiva del DOM\n" +
                "    var container = document.getElementById('live-view-modal').querySelector('div[style*=\"overflow:hidden\"]');\n"
                +
                "    var oldImg = document.getElementById('live-stream-img');\n" +
                "    if(oldImg) { oldImg.src = ''; oldImg.remove(); }\n" +
                "    \n" +
                "    var newImg = document.createElement('img');\n" +
                "    newImg.id = 'live-stream-img';\n" +
                "    // [FIX PREVIEW] Si el usuario vuelve a abrir, necesitamos que la imagen tenga src\n" +
                "    // Pero NO le ponemos src aqu para no cargar nada en background.\n" +
                "    newImg.src = '';\n" +
                "    newImg.style.maxWidth = '100%';\n" +
                "    newImg.style.maxHeight = '100%';\n" +
                "    newImg.style.objectFit = 'cover';\n" +
                "    newImg.style.display = 'block';\n" +
                "    if(typeof gWebZoom !== 'undefined') updateWebTransform(gWebZoom, gWebPanX, gWebPanY);\n" +
                "    container.appendChild(newImg);\n" +
                "    \n" +
                "    // Backup: Enviar señal de muerte inmediata\n" +
                "    fetch('/api/kill_stream').catch(e => {});\n" +
                "    // Parar latido (Si no hay grabación de fondo)\n" +
                "    stopHeartbeat();\n" +
                "}\n" +
                "function openSettings() {\n" +
                "   document.getElementById('settings-modal').style.display = 'flex';\n" +
                "   loadSettings();\n" +
                "}\n" +
                "function closeSettings() {\n" +
                "   document.getElementById('settings-modal').style.display = 'none';\n" +
                "}\n" +
                "function openDeleteModal() {\n" +
                "    document.getElementById('delete-modal').style.display = 'flex';\n" +
                "    resetSlider();\n" +
                "}\n" +
                "function closeDeleteModal() {\n" +
                "    document.getElementById('delete-modal').style.display = 'none';\n" +
                "}\n" +
                "\n" +
                "// --- SLIDER LOGIC ---\n" +
                "var slider = {\n" +
                "    track: null,\n" +
                "    thumb: null,\n" +
                "    isDragging: false,\n" +
                "    startX: 0,\n" +
                "    currentX: 0,\n" +
                "    maxDist: 0\n" +
                "};\n" +
                "\n" +
                "function initSlider() {\n" +
                "    slider.track = document.getElementById('slider-track');\n" +
                "    slider.thumb = document.getElementById('slider-thumb');\n" +
                "    \n" +
                "    // Mouse Events\n" +
                "    slider.thumb.addEventListener('mousedown', startDrag);\n" +
                "    window.addEventListener('mousemove', onDrag);\n" +
                "    window.addEventListener('mouseup', endDrag);\n" +
                "    \n" +
                "    // Touch Events\n" +
                "    slider.thumb.addEventListener('touchstart', startDrag, {passive:false});\n" +
                "    window.addEventListener('touchmove', onDrag, {passive:false});\n" +
                "    window.addEventListener('touchend', endDrag);\n" +
                "}\n" +
                "\n" +
                "function startDrag(e) {\n" +
                "    slider.isDragging = true;\n" +
                "    slider.startX = (e.type === 'mousedown') ? e.clientX : e.touches[0].clientX;\n" +
                "    slider.maxDist = slider.track.clientWidth - slider.thumb.clientWidth;\n" +
                "    slider.thumb.style.transition = 'none';\n" +
                "    if(e.cancelable) e.preventDefault();\n" +
                "}\n" +
                "\n" +
                "function onDrag(e) {\n" +
                "    if(!slider.isDragging) return;\n" +
                "    var clientX = (e.type.startsWith('touch')) ? e.touches[0].clientX : e.clientX;\n" +
                "    var dx = clientX - slider.startX;\n" +
                "    \n" +
                "    // Clamp\n" +
                "    if(dx < 0) dx = 0;\n" +
                "    if(dx > slider.maxDist) dx = slider.maxDist;\n" +
                "    \n" +
                "    slider.currentX = dx;\n" +
                "    slider.thumb.style.transform = 'translateX(' + dx + 'px)';\n" +
                "    \n" +
                "    // Visual Feedback (Opacity changes)\n" +
                "    var pct = dx / slider.maxDist;\n" +
                "    slider.track.children[0].style.opacity = 1 - pct;\n" +
                "    \n" +
                "    if(e.cancelable) e.preventDefault();\n" +
                "}\n" +
                "\n" +
                "function endDrag(e) {\n" +
                "    if(!slider.isDragging) return;\n" +
                "    slider.isDragging = false;\n" +
                "    \n" +
                "    var pct = slider.currentX / slider.maxDist;\n" +
                "    \n" +
                "    if(pct > 0.9) {\n" +
                "        // CONFIRMED!\n" +
                "        slider.thumb.style.transform = 'translateX(' + slider.maxDist + 'px)';\n" +
                "        slider.thumb.style.background = '#44ff44';\n" +
                "        slider.thumb.innerHTML = '✅';\n" +
                "        slider.track.children[0].innerText = \"BORRANDO...\";\n" +
                "        slider.track.children[0].style.opacity = 1;\n" +
                "        slider.track.children[0].style.color = \"#44ff44\";\n" +
                "        \n" +
                "        // EXECUTE WIPE\n" +
                "        fetch('/api/delete_all_videos', { method: 'POST' })\n" +
                "        .then(function() { \n" +
                "            setTimeout(function() { location.reload(); }, 1000); \n" +
                "        })\n" +
                "        .catch(function(e) { \n" +
                "            alert('Error: ' + e); \n" +
                "            resetSlider();\n" +
                "        });\n" +
                "        \n" +
                "    } else {\n" +
                "        // RESET (Snap back)\n" +
                "        resetSlider();\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "function resetSlider() {\n" +
                "    if(!slider.thumb) initSlider();\n" +
                "    slider.thumb.style.transition = 'transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275)';\n" +
                "    slider.thumb.style.transform = 'translateX(0px)';\n" +
                "    slider.thumb.style.background = '#ff4444';\n" +
                "    slider.thumb.innerHTML = '🗑️';\n" +
                "    slider.track.children[0].innerText = \"DESLIZA PARA BORRAR >>>\";\n" +
                "    slider.track.children[0].style.opacity = 1;\n" +
                "    slider.track.children[0].style.color = \"#aa5555\";\n" +
                "}\n" +
                "function updateSensLabel(val) {\n" +
                "   document.getElementById('sens-label').textContent = val + '%';\n" +
                "}\n" +
                "function loadSettings() {\n" +
                "  fetch('/api/settings').then(r=>r.json()).then(data => {\n" +
                "     document.getElementById('sens-slider').value = data.sens;\n" +
                "     document.getElementById('sens-label').innerText = data.sens + '%';\n" +
                "     document.getElementById('set-time').value = data.time;\n" +
                "     document.getElementById('set-active').checked = data.active;\n" +
                "     if(data.preRecord !== undefined) document.getElementById('set-prerecord').checked = data.preRecord;\n"
                +
                "     if(data.stealth !== undefined) document.getElementById('set-stealth').checked = data.stealth;\n"
                +
                "     if(data.filter !== undefined) document.getElementById('set-filter').checked = data.filter;\n"
                +
                "     if(data.rot === 180) document.getElementById('rot-180').checked = true;\n" +
                "     else document.getElementById('rot-0').checked = true;\n" +
                "     if(data.defZoom) {\n" +
                "        document.getElementById('hw-zoom').value = data.defZoom;\n" +
                "        document.getElementById('hw-zoom-val').textContent = parseFloat(data.defZoom).toFixed(1) + 'x';\n"
                +
                "     }\n" +
                "     if(data.defPanX !== undefined) document.getElementById('hw-pan-x').value = data.defPanX;\n" +
                "     if(data.defPanY !== undefined) document.getElementById('hw-pan-y').value = data.defPanY;\n" +
                "     if(data.minFreeSpace !== undefined) document.getElementById('set-min-space').value = data.minFreeSpace;\n"
                +
                "     // Web Settings\n" +
                "     if(data.webZoom) {\n" +
                "        gWebZoom = data.webZoom;\n" +
                "        gWebPanX = data.webPanX || 0;\n" +
                "        gWebPanY = data.webPanY || 0;\n" +
                "        document.getElementById('web-zoom').value = data.webZoom;\n" +
                "        document.getElementById('web-pan-x').value = data.webPanX || 0;\n" +
                "        document.getElementById('web-pan-y').value = data.webPanY || 0;\n" +
                "        updateWebTransform(data.webZoom, data.webPanX||0, data.webPanY||0);\n" +
                "     }\n" +
                "     // Telegram Settings\n" +
                "     if(data.tgToken) document.getElementById('tg-token').value = data.tgToken;\n" +
                "     if(data.tgChatId) document.getElementById('tg-chatid').value = data.tgChatId;\n" +
                "     if(data.tgActive !== undefined) document.getElementById('tg-active').checked = data.tgActive;\n" +
                "\n"
                +
                "     updateSensLabel(data.sens);\n" +
                "  });\n" +
                "}\n" +
                "function saveSettings() {\n" +
                "   var active = document.getElementById('set-active').checked;\n" +
                "   var preRecord = document.getElementById('set-prerecord').checked;\n" +
                "   var sens = document.getElementById('sens-slider').value;\n" +
                "   var contrast = document.getElementById('contrast-slider').value;\n" +
                "   var time = document.getElementById('set-time').value;\n" +
                "   var rot = document.getElementById('rot-180').checked ? 180 : 0;\n" +
                "   var defZoom = document.getElementById('hw-zoom').value;\n" +
                "   var defPanX = document.getElementById('hw-pan-x').value || 0;\n" +
                "   var defPanY = document.getElementById('hw-pan-y').value || 0;\n" +
                "   var minSpace = document.getElementById('set-min-space').value || 500;\n" +
                "   // Web Vars\n" +
                "   var wZoom = document.getElementById('web-zoom').value;\n" +
                "   var wPanX = document.getElementById('web-pan-x').value || 0;\n" +
                "   var wPanY = document.getElementById('web-pan-y').value || 0;\n" +
                "   var tgToken = encodeURIComponent(document.getElementById('tg-token').value);\n" +
                "   var tgChatId = encodeURIComponent(document.getElementById('tg-chatid').value);\n" +
                "   var tgActive = document.getElementById('tg-active').checked;\n" +

                "   \n" +
                "   document.querySelector('.btn-save').textContent = 'Guardando...';\n" +
                "   var stealth = document.getElementById('set-stealth').checked;\n" +
                "   var filter = document.getElementById('set-filter').checked;\n" +
                "   var qs = '?sens=' + sens + '&contrast=' + contrast + '&time=' + time + '&active=' + active + '&preRecord=' + preRecord + '&stealth=' + stealth + '&filter=' + filter + '&rot=' + rot +\n"
                +
                "            '&defZoom=' + defZoom + '&defPanX=' + defPanX + '&defPanY=' + defPanY +\n" +
                "            '&min_free_space=' + minSpace +\n" +
                "            '&webZoom=' + wZoom + '&webPanX=' + wPanX + '&webPanY=' + wPanY +\n" +
                "            '&tgToken=' + tgToken + '&tgChatId=' + tgChatId + '&tgActive=' + tgActive;\n"
                +
                "   fetch('/api/save_settings' + qs, { method: 'POST' })\n" +
                "   .then(function() { setTimeout(function() { location.reload(); }, 800); });\n" +
                "}\n" +
                "function testTelegram() {\n" +
                "    var t = document.getElementById('tg-token').value;\n" +
                "    var c = document.getElementById('tg-chatid').value;\n" +
                "    if(!t || !c) { alert('Introduce Token y Chat ID primero'); return; }\n" +
                "    fetch('/api/test_telegram?token=' + encodeURIComponent(t) + '&chat=' + encodeURIComponent(c))\n" +
                "    .then(function(){ alert('🔔 Mensaje de prueba enviado (Revisa tu Telegram)'); });\n" +
                "}\n" +
                "\n" +
                "                // ------ LAZY LOAD JAVASCRIPT ------\n" +
                "var currentOffset = 0; var LIMIT = 10; var isLoading = false; var noMoreVideos = false;\n" +
                "function loadMoreVideos() {\n" +
                "  if(isLoading || noMoreVideos) return;\n" +
                "  isLoading = true;\n" +
                "  fetch('/api/list_videos?offset=' + currentOffset + '&limit=' + LIMIT)\n" +
                "    .then(r => r.json())\n" +
                "    .then(videos => {\n" +
                "      if(videos.length === 0) { noMoreVideos = true; document.getElementById('loading-sentinel').innerHTML = '✅ Fin de grabaciones'; return; }\n"
                +
                "      renderCards(videos);\n" +
                "      currentOffset += videos.length;\n" +
                "      isLoading = false;\n" +
                "    })\n" +
                "    .catch(e => { console.log('Error loading videos', e); isLoading = false; });\n" +
                "}\n" +
                "function renderCards(videos) {\n" +
                "  var container = document.getElementById('video-list-container');\n" +
                "  videos.forEach(function(v) {\n" +
                "    var div = document.createElement('div');\n" +
                "    div.className = 'video-item';\n" +
                "    // Thumbnail\n" +
                "    var thumbHtml = '<div class=\\'thumb-container\\'>';\n" +
                "    if(v.thumb) thumbHtml += '<img src=\\'/thumbnails/' + v.thumb + '\\' class=\\'thumb\\' loading=\\'lazy\\'>';\n"
                +
                "    if(v.preview) thumbHtml += '<canvas class=\\'mini-canvas\\' width=\\'352\\' height=\\'288\\' data-src=\\'/' + v.preview + '\\'></canvas>';\n"
                +
                "    if(!v.thumb && !v.preview) thumbHtml += '<div class=\\'icon\\'>📼</div>';\n" +
                "    thumbHtml += '</div>';\n" +
                "    // Metadata\n" +
                "    var d = new Date(v.date);\n" +
                "    var dateStr = '📅 ' + ('0'+d.getDate()).slice(-2) + '/' + ('0'+(d.getMonth()+1)).slice(-2) + '/' + d.getFullYear();\n"
                +
                "    var timeStr = '⏰ ' + ('0'+d.getHours()).slice(-2) + ':' + ('0'+d.getMinutes()).slice(-2) + ':' + ('0'+d.getSeconds()).slice(-2);\n"
                +
                "    var sizeStr = v.size > 1024*1024 ? (v.size/(1024*1024)).toFixed(1) + ' MB' : Math.floor(v.size/1024) + ' KB';\n"
                +
                "    var durationStr = v.duration + 's';\n" +
                "    var infoHtml = '<div class=\\'info\\'>' +\n" +
                "      '<div style=\\'font-size:15px; font-weight:bold; color:#ffffff; margin-bottom:4px;\\'>' + dateStr + ' &nbsp; ' + timeStr + '</div>' +\n"
                +
                "      '<div style=\\'color:#ccc; font-size:13px;\\'><b>💾 ' + sizeStr + '</b> &nbsp;|&nbsp; <b>⏳ ' + durationStr + '</b></div></div>';\n"
                +
                "    div.innerHTML = thumbHtml + infoHtml;\n" +
                "    div.setAttribute('onclick', \"playVideo('\" + v.name + \"')\");\n" +
                "    container.appendChild(div);\n" +
                "    // Init canvas animation if preview exists\n" +
                "    if(v.preview) {\n" +
                "      var canvas = div.querySelector('.mini-canvas');\n" +
                "      if(canvas) loadMiniPreview('/' + v.preview, canvas);\n" +
                "    }\n" +
                "  });\n" +
                "  // Apply current zoom/pan to new thumbnails\n" +
                "  if(typeof updateWebTransformFromInputs === 'function') updateWebTransformFromInputs();\n" +
                "}\n" +
                "// IntersectionObserver for infinite scroll\n" +
                "var sentinel = document.getElementById('loading-sentinel');\n" +
                "if(sentinel && 'IntersectionObserver' in window) {\n" +
                "  var observer = new IntersectionObserver(function(entries) {\n" +
                "    if(entries[0].isIntersecting) loadMoreVideos();\n" +
                "  }, { rootMargin: '200px' });\n" +
                "  observer.observe(sentinel);\n" +
                "}\n" +
                "window.onload = function() {\n" +
                "   loadSettings();\n" +
                "   startStatsUpdater();\n" +
                "   pollStatus();\n" +
                "   loadMoreVideos();\n" +
                "};\n" +
                "var currentRecordingState = false;\n" +
                "var gCurrentRecFilename = null;\n" +
                "var gRecStartTime = 0;\n" +
                "function pollStatus() {\n" +
                "  fetch('/wait_status?current_state=' + currentRecordingState + '&_=' + Date.now())\n" +
                "      .then(r => r.json())\n" +
                "      .then(data => {\n" +
                "          if (data.recording && !currentRecordingState) { gRecStartTime = Date.now(); injectLivePreview(); }\n"
                +
                "          else if (!data.recording && currentRecordingState) { cleanupLivePreview(); }\n" +
                "          currentRecordingState = data.recording;\n" +
                "          updateStatusIndicator(data.recording);\n" +
                "          setTimeout(pollStatus, 10);\n" +
                "      })\n" +
                "      .catch(e => { setTimeout(pollStatus, 2000); });\n" +
                "}\n" +
                "function updateStatusIndicator(isRecording) {\n" +
                "   var els = document.querySelectorAll('.stat-status');\n" +
                "   els.forEach(function(el) {\n" +
                "       if(isRecording) {\n" +
                "           el.innerHTML = '🔴 GRABANDO';\n" +
                "           el.style.color = '#ff4444';\n" +
                "           el.style.fontWeight = 'bold';\n" +
                "           el.style.animation = 'pulse 1s infinite';\n" +
                "       } else {\n" +
                "           el.innerHTML = '⏺️ VIGILANDO...';\n" +
                "           el.style.color = '#aaaaaa';\n" +
                "           el.style.fontWeight = 'normal';\n" +
                "           el.style.animation = 'none';\n" +
                "       }\n" +
                "   });\n" +
                "}\n" +
                "var miniCanvases = document.querySelectorAll('.mini-canvas');\n" +
                "                // --- FIX CPU: OBSERVADOR INTELIGENTE ---\n" +
                "                var animationObserver = new IntersectionObserver(function(entries) {\n" +
                "                    entries.forEach(function(entry) {\n" +
                "                        var canvas = entry.target.querySelector('.mini-canvas');\n" +
                "                        if(!canvas) return;\n" +
                "                        if(entry.isIntersecting) {\n" +
                "                            canvas.isVisible = true;\n" +
                "                            if(canvas.hasData && !canvas.isAnimating && canvas.startAnim) canvas.startAnim();\n"
                +
                "                        } else {\n" +
                "                            canvas.isVisible = false;\n" +
                "                        }\n" +
                "                    });\n" +
                "                }, { threshold: 0.1 });\n" +
                "if (miniCanvases.length > 0) {\n" +
                "    for(var i = 0; i < miniCanvases.length; i++) {\n" +
                "        var canvas = miniCanvases[i];\n" +
                "        var url = canvas.getAttribute('data-src');\n" +
                "        if(url) { loadMiniPreview(url, canvas); }\n" +
                "    }\n" +
                "}\n" +
                "function loadMiniPreview(url, canvas) {\n" +
                "    var ctx = canvas.getContext('2d');\n" +
                "    var frames = []; var idx = 0;\n" +
                "    canvas.isVisible = false;\n" +
                "    canvas.hasData = false;\n" +
                "    var parentCard = canvas.closest('.video-item');\n" +
                "    if(parentCard) animationObserver.observe(parentCard);\n" +
                "    fetch(url).then(response => {\n" +
                "        const reader = response.body.getReader();\n" +
                "        var buffer = new Uint8Array(0);\n" +
                "        function pump() {\n" +
                "            reader.read().then(({done, value}) => {\n" +
                "                if (done) { startAnimation(); return; }\n" +
                "                var newBuffer = new Uint8Array(buffer.length + value.length);\n" +
                "                newBuffer.set(buffer); newBuffer.set(value, buffer.length);\n" +
                "                buffer = newBuffer;\n" +
                "                while(true) {\n" +
                "                    var start = -1;\n" +
                "                    for(var i=0; i<buffer.length-1; i++) { if(buffer[i]===0xFF && buffer[i+1]===0xD8) { start=i; break; } }\n"
                +
                "                    if(start === -1) break;\n" +
                "                    var end = -1;\n" +
                "                    for(var i=start+2; i<buffer.length-1; i++) { if(buffer[i]===0xFF && buffer[i+1]===0xD9) { end=i+2; break; } }\n"
                +
                "                    if(end === -1) break;\n" +
                "                    var jpeg = buffer.slice(start, end);\n" +
                "                    var blob = new Blob([jpeg], {type: 'image/jpeg'});\n" +
                "                    var imgUrl = URL.createObjectURL(blob);\n" +
                "                    var img = new Image();\n" +
                "                    img.onload = function() {\n" +
                "                        frames.push(this);\n" +
                "                        URL.revokeObjectURL(imgUrl);\n" +
                "                        if(frames.length === 1) startAnimation();\n" +
                "                    };\n" +
                "                    img.src = imgUrl;\n" +
                "                    buffer = buffer.slice(end);\n" +
                "                }\n" +
                "                pump();\n" +
                "            });\n" +
                "        }\n" +
                "        pump();\n" +
                "    });\n" +
                "    // FIX: Motor inteligente atado al objeto canvas\n" +
                "    canvas.startAnim = function() {\n" +
                "        if(canvas.isAnimating) return;\n" +
                "        canvas.isAnimating = true;\n" +
                "        function loop() {\n" +
                "            if(!canvas.isVisible) { canvas.isAnimating = false; return; } // <--- STOP SI NO SE VE\n" +
                "            if(frames.length > 0) {\n" +
                "                ctx.drawImage(frames[idx], 0, 0, canvas.width, canvas.height);\n" +
                "                idx = (idx + 1) % frames.length;\n" +
                "            }\n" +
                "            setTimeout(() => requestAnimationFrame(loop), 100);\n" +
                "        }\n" +
                "        loop();\n" +
                "    };\n" +
                "    // Puente para compatibilidad con código antiguo que llame a startAnimation()\n" +
                "    var startAnimation = function() { canvas.hasData = true; if(canvas.isVisible) canvas.startAnim(); };\n"
                + "}\n" +
                "var parasite = null;var parasiteInterval = null; var parasiteBuffer = []; var parasiteIdx = 0;\n" +
                "function injectLivePreview() {\n" +
                "   fetch('/api/latest_video_meta').then(r=>r.json()).then(meta => {\n" +
                "       if(!meta.filename) return;\n" +
                "       gCurrentRecFilename = meta.filename;\n" +
                "       var container = document.querySelector('.library');\n" +
                "       var div = document.createElement('div'); div.className = 'video-item'; div.id = 'temp-preview-card'; div.style.borderLeft = '4px solid #d32f2f'; div.style.background = '#3e2727';\n"
                +
                "       div.innerHTML = \"<div class='thumb-container' style='border: 1px solid #d32f2f;'><canvas id='parasite-canvas' class='thumb mini-canvas' width='352px' height='288px'></canvas></div><div class='info'><b>\" + meta.filename + \"</b><br><span style='color:#ff4444; font-weight:bold; animation: blink 1s infinite;'>🔴 GRABANDO...</span></div>\";\n"
                +
                "       var title = container.querySelector('.section-title'); title.parentNode.insertBefore(div, title.nextSibling);\n"
                +
                "       startParasite();\n" +
                "       if(typeof updateWebTransformFromInputs === 'function') updateWebTransformFromInputs();\n" +
                "   });\n" +
                "}\n" +
                "function startParasite() {\n" +
                "    if (!parasite) {\n" +
                "        parasite = document.createElement('img');\n" +
                "        parasite.id = 'hidden-stream-source';\n" +
                "        parasite.style.display = 'none';\n" +
                "        // Usamos el MISMO SessionID para que el servidor sepa que somos nosotros\n" +
                "        parasite.src = '/stream?session_id=' + SESSION_ID; \n" +
                "        document.body.appendChild(parasite);\n" +
                "        startHeartbeat(); // Iniciar latidos para mantener este stream vivo\n" +
                "        console.log('Parasite Stream Started (ID: ' + SESSION_ID + ')');\n" +
                "    }\n" +
                "   var hiddenCanvas = document.createElement('canvas'); hiddenCanvas.width = 352; hiddenCanvas.height = 288; var ctx = hiddenCanvas.getContext('2d');\n"
                +
                "   parasiteBuffer = [];\n" +
                "   parasiteInterval = setInterval(function() {\n" +
                "       try { ctx.drawImage(parasite, 0, 0, 352, 288); parasiteBuffer.push(hiddenCanvas.toDataURL('image/jpeg', 0.4)); if (parasiteBuffer.length > 30) parasiteBuffer.shift(); } catch(e) {}\n"
                +
                "   }, 1000);\n" +
                "   var displayCanvas = document.getElementById('parasite-canvas');\n" +
                "   if(displayCanvas) {\n" +
                "       var dCtx = displayCanvas.getContext('2d');\n" +
                "       var pLoop = function() {\n" +
                "           if(parasiteBuffer.length > 0) {\n" +
                "               var i = new Image(); i.onload = function() { dCtx.drawImage(i, 0, 0, 352, 288); }; i.src = parasiteBuffer[parasiteIdx]; parasiteIdx = (parasiteIdx + 1) % parasiteBuffer.length;\n"
                +
                "           }\n" +
                "           if(document.body.contains(displayCanvas)) setTimeout(pLoop, 100);\n" +
                "       };\n" +
                "       pLoop();\n" +
                "   }\n" +
                "}\n" +
                "function parseDateFromFilename(f) {\n" +
                "    var parts = f.match(/video_(\\d{4})(\\d{2})(\\d{2})_(\\d{2})(\\d{2})(\\d{2})/);\n" +
                "    if(!parts) return {date:\"Unknown\", time:\"Unknown\"};\n" +
                "    return {\n" +
                "        date: parts[3]+\"/\"+parts[2]+\"/\"+parts[1],\n" +
                "        time: \"⏰ \" + parts[4]+\":\"+parts[5]+\":\"+parts[6]\n" +
                "    };\n" +
                "}\n" +
                "function finalizeRecordingCard(filename) {\n" +
                "    var durationSec = Math.round((Date.now() - gRecStartTime) / 1000);\n" +
                "    var durationStr = durationSec + \"s\";\n" +
                "    var videoUrl = '/' + filename;\n" +
                "    fetch(videoUrl, { method: 'HEAD' })\n" +
                "    .then(function(response) {\n" +
                "        var bytes = response.headers.get('content-length');\n" +
                "        var sizeMB = (bytes / (1024*1024)).toFixed(1) + \" MB\";\n" +
                "        var card = document.getElementById('temp-preview-card');\n" +
                "        if(!card) return;\n" +
                "        if(parasiteInterval) clearInterval(parasiteInterval);\n" +
                "        var dt = parseDateFromFilename(filename);\n" +
                "        var dateStr = dt.date; var timeStr = dt.time;\n" +
                "        \n" +
                "        // Derive preview filename from video filename (remove FPS suffix)\n" +
                "        // video_YYYYMMDD_HHMMSS_XXfps.mjpeg -> preview_YYYYMMDD_HHMMSS.mjpeg\n" +
                "        var previewFilename = filename.replace(\"video_\", \"preview_\").replace(/_\\d+fps/, \"\");\n"
                +
                "        \n" +
                "        card.className = 'video-item';\n" +
                "        card.id = '';\n" +
                "        card.style = '';\n" +
                "        card.setAttribute('onclick', \"playVideo('\" + filename + \"')\");\n" +
                "        \n" +
                "        // Injected canvas for animation\n" +
                "        var innerContent = \"<div class='thumb-container'>\" +\n" +
                // VERSIÓN FINAL: con canvas, sin img estática encima. Sin parámetros basura,
                // sin reintentos, sin esperas extra.
                "            \"<canvas class='mini-canvas' data-src='/\" + previewFilename + \"'></canvas>\" +\n" +
                "            \"</div>\" +\n" +
                "            \"<div class='info'>\" +\n" +
                "            \"<div style='font-size:15px; font-weight:bold; color:#ffffff; margin-bottom:4px;'>\" + dateStr + \" &nbsp; \" + timeStr + \"</div>\" +\n"
                +
                "            \"<div style='color:#ccc; font-size:13px;'>\" +\n" +
                "            \"<b>💾 \" + sizeMB + \"</b>\" +\n" +
                "            \" &nbsp;|&nbsp; \" +\n" +
                "            \"<b>⏳ \" + durationStr + \"</b>\" +\n" +
                "            \"</div></div>\";\n" +
                "        card.innerHTML = innerContent;\n" +
                "        if(typeof gWebZoom !== 'undefined') updateWebTransform(gWebZoom, gWebPanX, gWebPanY);\n" +
                "        \n" +
                "        // Start animation immediately\n" +
                "        var canvas = card.querySelector('.mini-canvas');\n" +
                "        if(canvas) loadMiniPreview('/' + previewFilename, canvas);\n" +
                "        \n" +
                "    }).catch(function(err) {\n" +
                "        console.error(\"Error finalizing card:\", err);\n" +
                "       // location.reload(); // ANULADO: No recargues, solo quéjate en consola.\n" +
                "    });\n" +
                "}\n" +
                "\n" +
                "function cleanupLivePreview() {\n" +
                "   if(parasiteInterval) clearInterval(parasiteInterval);\n" +
                "   var img = document.getElementById('hidden-stream-source'); if(img) document.body.removeChild(img); parasite = null;\n"
                +
                "    stopHeartbeat();\n" + // <--- AÑADIDO
                "   setTimeout(function() { \n" +
                "       fetch('/api/latest_video_meta').then(r=>r.json()).then(meta => {\n" +
                "            if(meta.filename) {\n" +
                "                finalizeRecordingCard(meta.filename); \n" +
                "            } else {\n" +
                "                console.error(\"No filename found after finalization\");\n" +
                "                // location.reload(); //ANULADO \n" +
                "            }\n" +
                "       }).catch(e => console.log('Soft error cleanup', e)); // ANULADO el reload \n" +
                "   }, 1500);\n" +
                "}\n" +

                "function toggleTgToken() {\n" +
                "    var x = document.getElementById('tg-token-modal');\n" +
                "    x.type = (x.type === 'password') ? 'text' : 'password';\n" +
                "}\n" +
                "function openTelegramModal() {\n" +
                "    document.getElementById('tg-token-modal').value = document.getElementById('tg-token').value;\n" +
                "    document.getElementById('tg-chatid-modal').value = document.getElementById('tg-chatid').value;\n" +
                "    document.getElementById('telegram-modal').style.display = 'flex';\n" +
                "}\n" +
                "function closeTelegramModal() {\n" +
                "    document.getElementById('telegram-modal').style.display = 'none';\n" +
                "}\n" +
                "function saveTelegramModal() {\n" +
                "    document.getElementById('tg-token').value = document.getElementById('tg-token-modal').value;\n" +
                "    document.getElementById('tg-chatid').value = document.getElementById('tg-chatid-modal').value;\n" +
                "    closeTelegramModal();\n" +
                "}\n" +
                "function testTelegramFromModal() {\n" +
                "    var t = document.getElementById('tg-token-modal').value;\n" +
                "    var c = document.getElementById('tg-chatid-modal').value;\n" +
                "    if(!t || !c) { alert('Introduce Token y Chat ID primero'); return; }\n" +
                "    fetch('/api/test_telegram?token=' + encodeURIComponent(t) + '&chat=' + encodeURIComponent(c))\n" +
                "    .then(function(){ alert('🔔 Mensaje de prueba enviado (Revisa tu Telegram)'); });\n" +
                "}\n" +
                "\n" +
                "</script>\n" +
                "</body></html>";
    }

    // Phase 23: DRY Header Generation
    private String getCommonHeaderHtml(String versionName, String batIcon, int batLevel, String tempIcon, int temp,
            String freeStorage) {
        return "<div class='header' style='position:relative;'>\n" +
                "   <h1 style='font-size:18px; margin:0; display:inline-block;'>👁️ El Ojo del Abuelo <span style='font-size:0.7em; color:#aaa;'>"
                + versionName + "</span></h1>\n" +
                "   <span class='settings-btn' style='cursor:pointer; position:absolute; right:20px; top:50%; transform:translateY(-50%); font-size:24px;' onclick='openSettings()'>⚙️</span>\n"
                +
                "</div>\n" +
                "<div class='stats-bar'>\n" +
                "     <span id='stat-status' class='stat-status'>⏺️ VIGILANDO</span>\n" +
                "     <span id='stat-bat' class='stat-bat'>" + batIcon + " " + batLevel + "%</span>\n" +
                "     <span id='stat-temp' class='stat-temp'>" + tempIcon + " " + temp + "°C</span>\n" +
                "     <span id='stat-storage' class='stat-storage'>💾 " + freeStorage + "</span>\n" +
                "  </div>\n";
    }

    // Añadir esto en NanoHttpServer.java
    public boolean hasLiveClients() {
        // Ahora la verdad absoluta es el mapa de sesiones cardiacas
        return !sessionHeartbeats.isEmpty();
    }
}
