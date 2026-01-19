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
    private final Set<OutputStream> liveStreamClients = Collections.synchronizedSet(new HashSet<OutputStream>());
    private static final int PORT = 8080;
    private static final String BOUNDARY = "ElOjoDelAbueloBoundary";
    private static final File STORAGE_DIR = new File(Environment.getExternalStorageDirectory(), "ElOjoDelAbuelo");

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
                    serverSocket = new ServerSocket(PORT);
                    while (isRunning) {
                        try {
                            Socket client = serverSocket.accept();
                            new Thread(new ClientHandler(client)).start();
                        } catch (IOException e) {
                            if (isRunning)
                                e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
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

                // 2. Route Request
                if (uri.equals("/stream")) {
                    serveLiveStream(os); // Blocks thread while streaming
                } else if (uri.startsWith("/video_") || uri.startsWith("/preview_")) {
                    serveVideoFile(os, uri.substring(1)); // Remove leading slash
                } else if (uri.startsWith("/thumbnails/")) {
                    serveThumbnail(os, uri.substring(12)); // Remove "/thumbnails/"
                } else if (uri.equals("/stats")) {
                    serveStats(os);
                } else if (uri.equals("/api/settings")) {
                    serveSettings(os);
                } else if (uri.equals("/api/delete_all_videos") && method.equals("POST")) {
                    // Phase 19: Delete All Handling
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
                    serveSaveSettings(os, uri);
                } else if (uri.equals("/api/latest_video_meta")) {
                    serveLatestVideoMeta(os);
                } else if (uri.startsWith("/wait_status")) {
                    serveWaitStatus(os, uri);
                } else if (uri.equals("/api/debug")) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("--- DEBUG ONLINE (v3.2.1-debug20d - NUCLEAR FIX 3) ---\n"); // Header with Version
                    if (SentinelService.debugLogs != null) {
                        for (String s : SentinelService.debugLogs) {
                            sb.append(s).append("\n");
                        }
                    }
                    String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\n" + sb.toString();
                    os.write(response.getBytes());
                } else {
                    serveDashboard(os);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (!liveStreamClients.contains(os)) {
                    try {
                        socket.close();
                    } catch (Exception e) {
                    }
                }
            }
        }

        private void serveLiveStream(OutputStream os) throws IOException {
            os.write("HTTP/1.1 200 OK\r\n".getBytes());
            os.write(("Content-Type: multipart/x-mixed-replace; boundary=" + BOUNDARY + "\r\n").getBytes());
            os.write("Connection: keep-alive\r\n".getBytes());
            os.write("\r\n".getBytes());
            os.flush();
            liveStreamClients.add(os);

            // Keep thread alive to prevent socket closure
            try {
                while (liveStreamClients.contains(os)) {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                // End
            }
        }

        private void serveVideoFile(OutputStream os, String fileName) throws IOException {
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
            int time = SentinelService.recordingTimeout;
            boolean active = SentinelService.isDetectorActive;
            int rot = SentinelService.cameraRotation;
            float defZoom = SentinelService.defaultZoom;
            int defPanX = SentinelService.defaultPanX;
            int defPanY = SentinelService.defaultPanY;
            SharedPreferences prefs = context.getSharedPreferences("SentinelPrefs", Context.MODE_PRIVATE);
            int minFreeSpace = prefs.getInt("pref_min_free_space_mb", 500); // Phase 24

            String json = String.format(Locale.US,
                    "{\"sens\":%d, \"time\":%d, \"active\":%b, \"rot\":%d, \"defZoom\":%.2f, \"defPanX\":%d, \"defPanY\":%d, \"minFreeSpace\":%d}",
                    sens, time, active, rot, defZoom, defPanX, defPanY, minFreeSpace);

            os.write("HTTP/1.1 200 OK\r\n".getBytes());
            os.write("Content-Type: application/json\r\n".getBytes());
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
            // Parse query params manually
            // uri format: /api/save_settings?sens=90&time=10&active=true&rot=0
            int sens = 90;
            int time = 10;
            boolean active = true;
            int rot = 0;
            float defZoom = 1.0f;
            int defPanX = 0;
            int defPanY = 0;

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
                            else if (key.equals("time"))
                                time = Integer.parseInt(val);
                            else if (key.equals("active"))
                                active = Boolean.parseBoolean(val);
                            else if (key.equals("rot"))
                                rot = Integer.parseInt(val);
                            else if (key.equals("defZoom"))
                                defZoom = Float.parseFloat(val);
                            else if (key.equals("defPanX"))
                                defPanX = Integer.parseInt(val);
                            else if (key.equals("defPanY"))
                                defPanY = Integer.parseInt(val);
                            else if (key.equals("min_free_space")) // Phase 24
                                context.getSharedPreferences("SentinelPrefs", Context.MODE_PRIVATE)
                                        .edit().putInt("pref_min_free_space_mb", Integer.parseInt(val)).commit();
                        }
                    }
                }
                SentinelService.updateSettings(sens, time, active, rot);
                SentinelService.updateViewSettings(defZoom, defPanX, defPanY);
            } catch (Exception e) {
                e.printStackTrace();
            }

            os.write("HTTP/1.1 200 OK\r\n".getBytes());
            os.write("Content-Type: text/plain\r\n".getBytes());
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

        private void serveDashboard(OutputStream os) throws IOException {
            String html = generateDashboardHtml();
            byte[] body = html.getBytes("UTF-8");

            os.write("HTTP/1.1 200 OK\r\n".getBytes());
            os.write("Content-Type: text/html; charset=utf-8\r\n".getBytes());
            os.write(("Content-Length: " + body.length + "\r\n").getBytes());
            os.write("\r\n".getBytes());
            os.write(body);
            os.flush();
        }

        private void send404(OutputStream os) throws IOException {
            os.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
        }

    }

    private String generateDashboardHtml() {
        StringBuilder listHtml = new StringBuilder();
        if (STORAGE_DIR.exists()) {
            File[] files = STORAGE_DIR.listFiles();
            if (files != null) {
                Arrays.sort(files, new Comparator<File>() {
                    @Override
                    public int compare(File f1, File f2) {
                        return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified()); // Newest first
                    }
                });

                for (File f : files) {
                    if (f.getName().startsWith("video_") && f.getName().endsWith(".mjpeg")) {
                        long sizeKb = f.length() / 1024;
                        String thumbName = f.getName().replace(".mjpeg", ".jpg");
                        File thumbFile = new File(STORAGE_DIR, thumbName);

                        listHtml.append("<div class='video-item' onclick=\"playVideo('").append(f.getName())
                                .append("')\">");

                        String timestamp = "";
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("video_(\\d{8}_\\d{6})")
                                .matcher(f.getName());
                        if (m.find()) {
                            timestamp = m.group(1);
                        }
                        File previewFile = new File(STORAGE_DIR, "preview_" + timestamp + ".mjpeg");

                        listHtml.append("<div class='thumb-container'>");
                        if (thumbFile.exists()) {
                            listHtml.append("<img src='/thumbnails/").append(thumbName).append("' class='thumb'>");
                        }
                        if (previewFile.exists()) {
                            listHtml.append("<canvas class='mini-canvas' data-src='/").append(previewFile.getName())
                                    .append("'></canvas>");
                        }
                        listHtml.append("</div>");

                        if (!thumbFile.exists() && !previewFile.exists()) {
                            listHtml.append("<div class='icon'>📼</div>");
                        }

                        // Phase 21: Metadata (Size & Duration)
                        String sizeStr;
                        if (f.length() > 1024 * 1024) {
                            sizeStr = String.format(Locale.US, "%.1f MB", f.length() / (1024.0 * 1024.0));
                        } else {
                            sizeStr = (f.length() / 1024) + " KB";
                        }

                        String durationStr = "";
                        try {
                            // Extract creation timestamp from filename: video_yyyyMMdd_HHmmss
                            if (timestamp.length() >= 15) {
                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss",
                                        Locale.US);
                                java.util.Date creationDate = sdf.parse(timestamp);
                                long durationMs = f.lastModified() - creationDate.getTime();
                                if (durationMs > 0) {
                                    durationStr = " | " + (durationMs / 1000) + "s";
                                }
                            }
                        } catch (Exception e) {
                            // Ignore date parse errors
                        }

                        // Phase 22: Human-Readable File Metadata
                        String dateStr = "Unknown Date";
                        String timeStr = "Unknown Time";
                        String fpsStr = "?? FPS";

                        // Parse: video_20251201_144000_15fps
                        java.util.regex.Matcher metaMatcher = java.util.regex.Pattern
                                .compile("video_(\\d{4})(\\d{2})(\\d{2})_(\\d{2})(\\d{2})(\\d{2})_(\\d+)fps")
                                .matcher(f.getName());

                        if (metaMatcher.find()) {
                            dateStr = "📅 " + metaMatcher.group(3) + "/" + metaMatcher.group(2) + "/"
                                    + metaMatcher.group(1);
                            timeStr = "⏰ " + metaMatcher.group(4) + ":" + metaMatcher.group(5) + ":"
                                    + metaMatcher.group(6);
                            fpsStr = "🎥 " + metaMatcher.group(7) + " FPS";
                        }

                        listHtml.append("<div class='info'>")
                                .append("<div style='font-size:15px; font-weight:bold; color:#ffffff; margin-bottom:4px;'>")
                                .append(dateStr).append(" &nbsp; ").append(timeStr)
                                .append("</div>")
                                .append("<div style='color:#ccc; font-size:13px;'>")
                                .append("<b>💾 ").append(sizeStr).append("</b>")
                                .append(" &nbsp;|&nbsp; ")
                                .append("<b>⏳ ").append(durationStr.replace(" | ", "")).append("</b>")
                                .append(" &nbsp;|&nbsp; ")
                                .append(fpsStr)
                                .append("</div></div>");
                        listHtml.append("</div>");
                    }
                }
            }
        }

        // Stats
        int batLevel = SystemStats.getBatteryLevel(context);
        boolean charging = SystemStats.isCharging(context);
        String freeStorage = SystemStats.getFreeStorageSpace();
        int temp = ThermalGuardian.getBatteryTemperature(context);

        // Version
        String versionName = "v?";
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
                ".header { padding: 20px; text-align: center; background: #1f1f1f; box-shadow: 0 2px 10px rgba(0,0,0,0.5); flex-shrink: 0; }\n"
                +
                ".stats-bar { display: flex; justify-content: space-around; background: #333; padding: 10px; margin: 10px; border-radius: 8px; font-size: 14px; flex-shrink: 0; }\n"
                +
                ".live-btn { display: inline-block; background: #d32f2f; color: white; padding: 15px 30px; border-radius: 50px; text-decoration: none; font-weight: bold; animation: pulse 2s infinite; }\n"
                +
                "@keyframes pulse { 0% { transform: scale(1); box-shadow: 0 0 0 0 rgba(211, 47, 47, 0.7); } 70% { transform: scale(1.05); box-shadow: 0 0 0 10px rgba(211, 47, 47, 0); } 100% { transform: scale(1); box-shadow: 0 0 0 0 rgba(211, 47, 47, 0); } }\n"
                +
                ".library { flex-grow: 1; flex-shrink: 1; flex-basis: 0; padding: 10px; overflow-y: auto; height: 100%; min-height: 0; }\n"
                +
                ".section-title { font-size: 0.9em; text-transform: uppercase; color: #888; margin-bottom: 10px; letter-spacing: 1px; }\n"
                +
                ".video-item { display: flex; align-items: center; background: #2c2c2c; margin-bottom: 10px; padding: 15px; border-radius: 12px; active: scale(0.98); transition: transform 0.1s, opacity 0.5s, filter 0.5s; }\n"
                +
                ".video-item.watched { opacity: 0.5; filter: grayscale(100%); }\n"
                +
                ".video-item:active { transform: scale(0.98); background: #3d3d3d; }\n" +
                ".video-item .icon { font-size: 24px; margin-right: 15px; }\n" +
                ".thumb-container { position: relative; width: 80px; height: 60px; margin-right: 15px; border-radius: 8px; overflow: hidden; background: #444; }\n"
                +
                ".thumb { width: 100%; height: 100%; object-fit: cover; position: absolute; top:0; left:0; }\n" +
                ".mini-canvas { width: 100%; height: 100%; position: absolute; top:0; left:0; z-index: 10; }\n" +
                ".video-item .info { flex: 1; font-size: 14px; }\n" +
                "/* Modal Player */\n" +
                "/* Modal Player */\n" +
                "#player-modal, #live-view-modal { display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: black; z-index: 1000; flex-direction: column; }\n"
                +
                "#canvas-container { flex: 1; display: flex; justify-content: center; align-items: center; overflow: hidden; position: relative; background-color: #000; width: 100%; height: auto; touch-action: none; }\n"
                +
                "img#video-player { max-width: 100%; max-height: 100%; width: 100%; height: 100%; object-fit: contain; display: block; transform-origin: 0 0; -webkit-transform-origin: 0 0; }\n"
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
                "label { font-size: 16px; }\n"
                +
                "</style>\n" +
                "</head><body>\n" +
                "\n" +
                (SentinelService.isCameraError
                        ? "<div class='camera-error'>⚠️ ERROR CRÍTICO: CÁMARA NO RESPONDE - REINICIA EL MÓVIL</div>\n"
                        : "")
                +
                commonHeader
                +
                "  <div style='text-align:center; padding-bottom:10px; flex-shrink:0;'>\n" +
                "     <div class='live-btn' onclick='openLiveView()' style='cursor:pointer;'>🔴 VER CÁMARA EN VIVO</div>\n"
                +
                "     <div style='margin-top:10px; font-size:12px; color:#666;'>Status: " + lastError + " | Boot: "
                + SystemStats.getBootTime() + "</div>\n" +
                "  </div>\n" +
                "\n" +
                "<div class='library'>\n" +
                "  <div class='section-title'>📼 Grabaciones</div>\n" +
                listHtml.toString() +
                "</div>\n" +
                "\n" +
                "<div id='player-modal'>\n" +
                commonHeader + // Phase 23: Persistent Header in Player Modal (Correct Placement)
                "  <div class='controls' style='justify-content:space-between;'>\n" +
                "     <span id='video-title'>Video</span>\n" +
                "     <button class='btn-close' onclick='closePlayer()'>❌</button>\n" +
                "  </div>\n" +
                "  <div id='canvas-container' style='position:relative;'>\n" +
                "     <div id='hud-stats' style='position:absolute; top:10px; left:10px; background:rgba(0,0,0,0.6); color:#0f0; padding:4px 8px; font-family:monospace; font-size:12px; pointer-events:none; z-index:100; border-radius:4px;'>ZOOM: 1.0x | X: 0 | Y: 0</div>\n"
                +
                "     <img id='video-player'>\n" +
                "  </div>\n" +
                "  <div class='controls'>\n" +
                "     <button class='btn-close' id='play-pause' style='font-size:24px;'>⏸</button>\n" +
                "     <span id='current-frame'>0</span>\n" +
                "     <input type='range' id='scrubber' min='0' max='100' value='0' disabled>\n" +
                "     <span id='total-frames'>...</span>\n" +
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
                "<div id='settings-modal'>\n" +
                "  <div class='settings-content'>\n" +
                "     <div style='display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #444; padding-bottom:10px; margin-bottom:20px;'>\n"
                +
                "        <h3 style='margin:0;'>Configuración ⚙️</h3>\n" +
                "        <span onclick='closeSettings()' style='cursor:pointer; font-size:24px;'>&times;</span>\n" +
                "     </div>\n"
                +
                "     \n" +
                "     <div class='settings-row'>\n" +
                "        <label>Detector Activado:</label>\n" +
                "        <input type='checkbox' id='set-active' style='transform: scale(1.5);'>\n" +
                "     </div>\n" +
                "\n" +
                "     <div style='margin-bottom:20px;'>\n" +
                "        <label>Sensibilidad: <span id='sens-label' style='color:#aaa; font-size:14px;'>90%</span></label>\n"
                +
                "        <div style='display:flex; align-items:center; margin-top:5px;'>\n" +
                "           <span style='font-size:12px;'>Min</span>\n" +
                "           <input type='range' id='sens-slider' min='0' max='100' oninput='updateSensLabel(this.value)'>\n"
                +
                "           <span style='font-size:12px;'>Max</span>\n" +
                "        </div>\n" +
                "     </div>\n" +
                "\n" +
                "     <div class='settings-row'>\n" +
                "        <label>Tiempo Extra:</label>\n" +
                "        <div>\n" +
                "           <select id='set-time' style='background:#333; color:white; padding:5px; border-radius:4px;'>\n"
                +
                "              <option value='10'>10 seg</option>\n" +
                "              <option value='30'>30 seg</option>\n" +
                "           </select>\n" +
                "        </div>\n" +
                "     </div>\n" +
                "     \n" +
                "     <!-- Phase 19: View Defaults -->\n" +
                "     <h4 style='border-bottom:1px solid #444; margin-top:20px; padding-bottom:5px; margin-bottom:10px;'>Vista Inicial (Zoom/Pan)</h4>\n"
                +
                "     <div style='margin-bottom:15px;'>\n" +
                "        <label>Zoom: <span id='zoom-label' style='color:#aaa;'>1.0x</span></label>\n" +
                "        <input type='range' id='def-zoom' min='1' max='5' step='0.1' style='width:100%;' oninput=\"document.getElementById('zoom-label').textContent=this.value+'x'\">\n"
                +
                "     </div>\n" +
                "     <div class='settings-row'>\n" +
                "        <label>Pan X (px):</label>\n" +
                "        <input type='number' id='def-pan-x' style='width:60px; background:#333; color:white; border:none; padding:5px;' placeholder='0'>\n"
                +
                "     </div>\n" +
                "     <div class='settings-row'>\n" +
                "        <label>Pan Y (px):</label>\n" +
                "        <input type='number' id='def-pan-y' style='width:60px; background:#333; color:white; border:none; padding:5px;' placeholder='0'>\n"
                +
                "     </div>\n" +
                "     <div style='font-size:11px; color:#aaa; margin-top:-10px; margin-bottom:20px;'>\n" +
                "        * Recomendado: +/- 350px (Ancho) y +/- 280px (Alto) para no perder imagen.\n" +
                "     </div>\n" +
                "\n" +
                "     <!-- Phase 19: Danger Zone -->\n" +
                "     <h4 style='border-bottom:1px solid #444; margin-top:20px; padding-bottom:5px; color:#c62828; margin-bottom:10px;'>Zona de Peligro</h4>\n"
                +
                "     <button class='btn-cancel' onclick='deleteAllVideos()' style='width:100%; margin-bottom:20px;'>🗑️ BORRAR TODOS LOS VIDEOS</button>\n"
                +
                "\n" +
                "     <div class='settings-row'>\n" +
                "        <label>Tiempo Grabación (s):</label>\n" +
                "        <input type='number' id='set-time' style='width:60px; padding:5px; background:#333; color:white; border:1px solid #555;' min='5' max='60'>\n"
                +
                "     </div>\n" +
                "     <!-- Phase 24: Min Space -->\n" +
                "     <div class='settings-row'>\n" +
                "        <label>Min. Espacio (MB):</label>\n" +
                "        <input type='number' id='set-min-space' style='width:70px; padding:5px; background:#333; color:white; border:1px solid #555;' min='100' max='5000'>\n"
                +
                "     </div>\n" +
                "     <div class='settings-row'>\n" +
                "        <label>Rotación:</label>\n" +
                "        <div>\n" +
                "           <input type='radio' name='rot' value='0' id='rot-0' checked> 0°\n" +
                "           <input type='radio' name='rot' value='180' id='rot-180'> 180°\n" +
                "        </div>\n" +
                "     </div>\n" +
                "\n" +
                "     <div style='display:flex; margin-top:20px;'>\n" +
                "        <button class='btn-save' onclick='saveSettings()'>GUARDAR</button>\n" +
                "        <button class='btn-cancel' onclick='closeSettings()'>CANCELAR</button>\n" +
                "     </div>\n" +
                "  </div>\n" +
                "</div>\n" +
                "\n" +
                "<script>\n" +
                "var frames = [];\n" +
                "var currentFrameIdx = 0;\n" +
                "var isPlaying = false;\n" +
                "var fps = 10;\n" +
                "var currentObjectUrl = null;\n" +
                "var appSettings = { defZoom: 1.0, defPanX: 0, defPanY: 0 };\n" +
                "\n" +
                "function playVideo(file) {\n" +
                "  // Phase 21: Mark as watched (Visual Indicator)\n" +
                "  var items = document.getElementsByClassName('video-item');\n" +
                "  for(var i=0; i<items.length; i++) {\n" +
                "     if(items[i].getAttribute('onclick').indexOf(file) !== -1) {\n" +
                "         items[i].classList.add('watched');\n" +
                "         break;\n" +
                "     }\n" +
                "  }\n" +
                "  \n" +
                "  document.getElementById('player-modal').style.display = 'flex';\n" +
                "  document.getElementById('video-title').textContent = file;\n" +
                "  frames = [];\n" +
                "  currentFrameIdx = 0;\n" +
                "  document.getElementById('scrubber').value = 0;\n" +
                "  \n" +
                "  // Apply Default View\n" +
                "  mat.s = appSettings.defZoom || 1.0;\n" +
                "  mat.x = appSettings.defPanX || 0;\n" +
                "  mat.y = appSettings.defPanY || 0;\n" +
                "  mat.initialS = mat.s;\n" +
                "  drag.initialX = mat.x;\n" +
                "  drag.initialY = mat.y;\n" +
                "  updateTransform();\n" +
                "  \n" +
                "  // Extract FPS\n" +
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
                "  \n" +
                "  // Find JPEG SOI (FF D8) and EOI (FF D9)\n" +
                "  while (true) {\n" +
                "    var start = -1;\n" +
                "    // Optimization: Loop manually to find FF D8\n" +
                "    for(var i=0; i<buffer.length-1; i++) {\n" +
                "      if(buffer[i] === 0xFF && buffer[i+1] === 0xD8) { start = i; break; }\n" +
                "    }\n" +
                "    if(start === -1) break; // No header yet, keep buffer\n" +
                "    \n" +
                "    var end = -1;\n" +
                "    for(var i=start+2; i<buffer.length-1; i++) {\n" +
                "      if(buffer[i] === 0xFF && buffer[i+1] === 0xD9) { end = i+2; break; }\n" +
                "    }\n" +
                "    if(end === -1) break; // No footer yet, keep buffer\n" +
                "    \n" +
                "    // Extract Jpeg Blob\n" +
                "    var jpegData = buffer.slice(start, end);\n" +
                "    var blob = new Blob([jpegData], {type: 'image/jpeg'});\n" +
                "    frames.push(blob);\n" +
                "    if(frames.length === 1) requestAnimationFrame(drawLoop);\n" +
                "    updateScrubber();\n" +
                "    \n" +
                "    // Remove processed part\n" +
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
                "   if(frames.length % 10 === 0) {\n" +
                "      if(frames.length > 0 && fps > 0)\n" +
                "          document.getElementById('total-frames').textContent = formatTime(frames.length / fps) + '...';\n"
                +
                "   }\n" +
                "}\n" +
                "\n" +
                "function drawLoop() {\n" +
                "  if (!isPlaying) return;\n" +
                "  if (frames.length > 0) {\n" +
                "     drawFrame(currentFrameIdx);\n" +
                "     currentFrameIdx = (currentFrameIdx + 1);\n" +
                "     if (currentFrameIdx >= frames.length) {\n" +
                "        currentFrameIdx = 0; // Loop\n" +
                "     }\n" +
                "     document.getElementById('scrubber').value = currentFrameIdx;\n" +
                "     document.getElementById('current-frame').textContent = formatTime(currentFrameIdx / fps);\n" +
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
                "document.getElementById('play-pause').addEventListener('click', function() {\n" +
                "    setIsPlaying(!isPlaying);\n" +
                "});\n" +
                "\n" +
                "function drawFrame(idx) {\n" +
                "   if(!frames[idx]) return;\n" +
                "   \n" +
                "   if (currentObjectUrl) URL.revokeObjectURL(currentObjectUrl);\n" +
                "   currentObjectUrl = URL.createObjectURL(frames[idx]);\n" +
                "   document.getElementById('video-player').src = currentObjectUrl;\n" +
                "}\n" +
                "\n" +
                "// --- Phase 18.2: Matrix Zoom Logic (ES3 Compatible) ---\n" +
                "var playerImg = document.getElementById('video-player');\n" +
                "var container = document.getElementById('canvas-container');\n" +
                "\n" +
                "// State Mappings\n" +
                "var mat = { x: 0, y: 0, s: 1 };\n" +
                "var drag = { startX: 0, startY: 0, initialX: 0, initialY: 0 };\n" +
                "var pinch = { dist: 0, midX: 0, midY: 0, initialS: 1, initialX: 0, initialY: 0 };\n" +
                "var lastTap = 0;\n" +
                "var isMultiTouch = false;\n" +
                "\n" +
                "function resetZoom() {\n" +
                "    mat.x = 0; mat.y = 0; mat.s = 1;\n" +
                "    updateTransform();\n" +
                "}\n" +
                "\n" +
                "function updateTransform() {\n" +
                "    var t = 'translate(' + mat.x + 'px, ' + mat.y + 'px) scale(' + mat.s + ')';\n" +
                "    playerImg.style.transform = t;\n" +
                "    playerImg.style.webkitTransform = t;\n" +
                "    var hud = document.getElementById('hud-stats');\n" +
                "    if(hud) hud.textContent = 'ZOOM: ' + mat.s.toFixed(1) + 'x | X: ' + Math.round(mat.x) + ' | Y: ' + Math.round(mat.y);\n"
                +
                "}\n" +
                "\n" +
                "// Helpers (ES3 Safe)\n" +
                "function getDist(t1, t2) {\n" +
                "    var dx = t1.pageX - t2.pageX;\n" +
                "    var dy = t1.pageY - t2.pageY;\n" +
                "    return Math.sqrt(dx*dx + dy*dy);\n" +
                "}\n" +
                "\n" +
                "function getMid(t1, t2) {\n" +
                "    return {\n" +
                "        x: (t1.pageX + t2.pageX) / 2,\n" +
                "        y: (t1.pageY + t2.pageY) / 2\n" +
                "    };\n" +
                "}\n" +
                "\n" +
                "container.addEventListener('touchstart', function(e) {\n" +
                "    if(e.cancelable) e.preventDefault();\n" +
                "    if (e.touches.length > 1) isMultiTouch = true;\n" +
                "    else isMultiTouch = false;\n" +
                "    \n" +
                "    if (e.touches.length === 2) {\n" +
                "        // PINCH START\n" +
                "        pinch.dist = getDist(e.touches[0], e.touches[1]);\n" +
                "        var mid = getMid(e.touches[0], e.touches[1]);\n" +
                "        pinch.midX = mid.x; \n" +
                "        pinch.midY = mid.y;\n" +
                "        pinch.initialS = mat.s;\n" +
                "        pinch.initialX = mat.x;\n" +
                "        pinch.initialY = mat.y;\n" +
                "    } else if (e.touches.length === 1) {\n" +
                "        // PAN START\n" +
                "        drag.startX = e.touches[0].pageX;\n" +
                "        drag.startY = e.touches[0].pageY;\n" +
                "        drag.initialX = mat.x;\n" +
                "        drag.initialY = mat.y;\n" +
                "    }\n" +
                "}, { passive: false });\n" +
                "\n" +
                "container.addEventListener('touchmove', function(e) {\n" +
                "    if(e.cancelable) e.preventDefault(); \n" +
                "    \n" +
                "    if (e.touches.length === 2 && pinch.dist > 0) {\n" +
                "        // PINCH UPDATE\n" +
                "        var newDist = getDist(e.touches[0], e.touches[1]);\n" +
                "        var scaleFactor = newDist / pinch.dist;\n" +
                "        var newScale = pinch.initialS * scaleFactor;\n" +
                "        \n" +
                "        // Clamp\n" +
                "        newScale = Math.max(1, Math.min(newScale, 5));\n" +
                "        if (newScale === 1) {\n" +
                "           mat.x = 0; mat.y = 0;\n" +
                "        }\n" +
                "        \n" +
                "        // Focal Point Logic: Keep mid stationary\n" +
                "        var ratio = newScale / pinch.initialS;\n" +
                "        mat.x = pinch.midX - (pinch.midX - pinch.initialX) * ratio;\n" +
                "        mat.y = pinch.midY - (pinch.midY - pinch.initialY) * ratio;\n" +
                "        mat.s = newScale;\n" +
                "        \n" +
                "        updateTransform();\n" +
                "    } else if (e.touches.length === 1 && mat.s > 1.05) {\n" +
                "        // PAN UPDATE\n" +
                "        var dx = e.touches[0].pageX - drag.startX;\n" +
                "        var dy = e.touches[0].pageY - drag.startY;\n" +
                "        mat.x = drag.initialX + dx;\n" +
                "        mat.y = drag.initialY + dy;\n" +
                "        updateTransform();\n" +
                "    }\n" +
                "}, { passive: false });\n" +
                "\n" +
                "container.addEventListener('touchend', function(e) {\n" +
                "    var now = new Date().getTime();\n" +
                "    if (e.touches.length === 0) {\n" +
                "        if (!isMultiTouch && (now - lastTap < 300)) {\n" +
                "            resetZoom();\n" +
                "            e.preventDefault();\n" +
                "        }\n" +
                "        isMultiTouch = false;\n" +
                "    }\n" +
                "    lastTap = now;\n" +
                "\n" +
                "    // If dropped to 1 finger, restart pan to prevent jump\n" +
                "    if (e.touches.length === 1) {\n" +
                "        drag.startX = e.touches[0].pageX;\n" +
                "        drag.startY = e.touches[0].pageY;\n" +
                "        drag.initialX = mat.x;\n" +
                "        drag.initialY = mat.y;\n" +
                "    }\n" +
                "});\n" +
                "\n" +
                "// Scrubber logic\n" +
                "document.getElementById('scrubber').addEventListener('input', function(e) {\n" +
                "   setIsPlaying(false);\n" +
                "   currentFrameIdx = parseInt(e.target.value);\n" +
                "   document.getElementById('current-frame').textContent = formatTime(currentFrameIdx / fps);\n" +
                "   drawFrame(currentFrameIdx);\n" +
                "});\n" +
                "document.getElementById('scrubber').addEventListener('change', function(e) { setIsPlaying(true); });\n"
                +
                "\n" +
                "function closePlayer() {\n" +
                "  document.getElementById('player-modal').style.display = 'none';\n" +
                "  setIsPlaying(false);\n" +
                "  frames = [];\n" +
                "}\n" +
                "\n" +

                "\n" +
                "// --- LIVE STATS UPDATER (Phase 8) ---\n" +
                "function startStatsUpdater() {\n" +
                "  var lastTemp = null;\n" +
                "  var currentTrendHtml = '';\n" +
                "  setInterval(function() {\n" +
                "    fetch('/stats').then(r => r.json()).then(data => {\n" +
                "      // Battery\n" +
                "      var batIcon = data.charging ? '⚡' : (data.bat > 20 ? '🔋' : '🪫');\n" +
                "      document.getElementById('stat-bat').innerText = batIcon + ' ' + data.bat + '%';\n" +
                "      // Temp (Sticky Trend)\n" +
                "      if (lastTemp === null) lastTemp = data.temp;\n" +
                "      if (data.temp > lastTemp) {\n" +
                "          currentTrendHtml = ' <span style=\"color:#ff4444; font-size:0.8em;\">▲</span>';\n" +
                "          lastTemp = data.temp;\n" +
                "      } else if (data.temp < lastTemp) {\n" +
                "          currentTrendHtml = ' <span style=\"color:#66ff66; font-size:0.8em;\">▼</span>';\n" +
                "          lastTemp = data.temp;\n" +
                "      }\n" +
                "      var tempIcon = data.temp > 40 ? '🔥' : '🌡️';\n" +
                "      document.getElementById('stat-temp').innerHTML = tempIcon + ' ' + data.temp + '°C' + currentTrendHtml;\n" +
                "      // Storage\n" +
                "      document.getElementById('stat-storage').innerText = '💾 ' + data.storage;\n" +
                "      // Status\n" +
                "      document.getElementById('stat-status').innerText = data.recording ? '🔴 GRABANDO' : '⏺️ VIGILANDO';\n"
                +
                "      document.getElementById('stat-status').style.color = data.recording ? '#ff4444' : '#ffffff';\n" +
                "    }).catch(e => console.log('Stats error', e));\n" +
                "  }, 5000);\n" +
                "}\n" +
                "// ------------------------------\n" +
                "\n" +
                "// Live View Logic\n" +
                "function openLiveView() {\n" +
                "    document.getElementById('live-view-modal').style.display = 'flex';\n" +
                "    document.getElementById('live-stream-img').src = '/stream';\n" +
                "    // Prevent user from leaving active stream if back button pressed\n" +
                "    history.pushState(null, null, location.href);\n" +
                "}\n" +
                "function closeLiveView() {\n" +
                "    document.getElementById('live-view-modal').style.display = 'none';\n" +
                "    document.getElementById('live-stream-img').src = '';\n" +
                "}\n" +
                "\n" +
                "// Settings Logic\n" +
                "function openSettings() {\n" +
                "    document.getElementById('settings-modal').style.display = 'flex';\n" +
                "    loadSettings();\n" +
                "}\n" +
                "function closeSettings() {\n" +
                "    document.getElementById('settings-modal').style.display = 'none';\n" +
                "}\n" +
                "function deleteAllVideos() {\n" +
                "    if(confirm('⚠️ ¡PELIGRO! ¿Seguro que quieres borrar TODOS los videos grabados? Esta acción es irreversible.')) {\n"
                +
                "        fetch('/api/delete_all_videos', { method: 'POST' })\n" +
                "        .then(function() { location.reload(); }).catch(function(e) { alert('Error: ' + e); });\n" +
                "    }\n" +
                "}\n" +
                "function updateSensLabel(val) {\n" +
                "    var px = 500 - (val * 4.9);\n" +
                "    document.getElementById('sens-label').textContent = val + '% (' + Math.round(px) + ' px)';\n" +
                "}\n" +
                "function loadSettings() {\n" +
                "  fetch('/api/settings').then(r=>r.json()).then(data => {\n" +
                "     document.getElementById('sens-slider').value = data.sens;\n" +
                "     document.getElementById('sens-label').innerText = data.sens + '%';\n" +
                "     document.getElementById('set-time').value = data.time;\n" +
                "     document.getElementById('set-active').checked = data.active;\n" +
                "     if(data.rot === 180) document.getElementById('rot-180').checked = true;\n" +
                "     else document.getElementById('rot-0').checked = true;\n" +
                "     \n" +
                "     appSettings = data;\n" +
                "     if(data.defZoom) {\n" +
                "        document.getElementById('def-zoom').value = data.defZoom;\n" +
                "        document.getElementById('zoom-label').innerText = parseFloat(data.defZoom).toFixed(1) + 'x';\n"
                +
                "     }\n" +
                "     if(data.defPanX !== undefined) document.getElementById('def-pan-x').value = data.defPanX;\n" +
                "     if(data.defPanY !== undefined) document.getElementById('def-pan-y').value = data.defPanY;\n" +
                "     if(data.minFreeSpace !== undefined) document.getElementById('set-min-space').value = data.minFreeSpace;\n"
                +
                "     \n" +
                "     updateSensLabel(data.sens);\n" +
                "  });\n" +
                "}\n" +
                "function saveSettings() {\n" +
                "    var active = document.getElementById('set-active').checked;\n" +
                "    var sens = document.getElementById('sens-slider').value;\n" +
                "    var time = document.getElementById('set-time').value;\n" +
                "    var rot = document.getElementById('rot-180').checked ? 180 : 0;\n" +
                "    var defZoom = document.getElementById('def-zoom').value;\n" +
                "    var defPanX = document.getElementById('def-pan-x').value || 0;\n" +
                "    var defPanY = document.getElementById('def-pan-y').value || 0;\n" +
                "    var minSpace = document.getElementById('set-min-space').value || 500;\n" +
                "\n" +
                "    // Show saving feedback\n" +
                "    document.querySelector('.btn-save').textContent = 'Guardando...';\n" +
                "    \n" +
                "    var qs = '?sens=' + sens + '&time=' + time + '&active=' + active + '&rot=' + rot +\n" +
                "             '&defZoom=' + defZoom + '&defPanX=' + defPanX + '&defPanY=' + defPanY +\n" +
                "             '&min_free_space=' + minSpace;\n" +
                "    \n" +
                "    fetch('/api/save_settings' + qs, { method: 'POST' })\n"
                +
                "    .then(function() {\n" +
                "        setTimeout(function() {\n" +
                "            location.reload();\n" +
                "        }, 800);\n" +
                "    });\n" +
                "}\n" +
                "// Load Settings on Start - CLEANED\n" +

                "\n" +
                "// Initialize on Load\n" +
                "window.onload = function() {\n" +
                "    loadSettings();\n" + // Extracted from fetch blob
                "    startStatsUpdater();\n" +
                "    pollStatus();\n" +
                "};\n" +
                "\n" +
                "// Real-Time Status (Long Polling)\n" +
                "var currentRecordingState = false;\n" +
                "function pollStatus() {\n" +
                "  fetch('/wait_status?current_state=' + currentRecordingState + '&_=' + Date.now())\n" +
                "     .then(r => r.json())\n" +
                "     .then(data => {\n" +
                "         // Phase 17: Live Injection Logic\n" +
                "         if (data.recording && !currentRecordingState) {\n" +
                "             injectLivePreview();\n" +
                "         } else if (!data.recording && currentRecordingState) {\n" +
                "             cleanupLivePreview();\n" +
                "         }\n" +
                "         \n" +
                "         currentRecordingState = data.recording;\n" +
                "         updateStatusIndicator(data.recording);\n" +
                "         setTimeout(pollStatus, 10); // Loop immediately\n" +
                "     })\n" +
                "     .catch(e => {\n" +
                "         console.log('Poll error', e);\n" +
                "         setTimeout(pollStatus, 2000); // Retry on error\n" +
                "     });\n" +
                "}\n" +
                "\n" +
                "function updateStatusIndicator(isRecording) {\n" +
                "   var els = document.querySelectorAll('.stat-status');\n" +
                "   els.forEach(function(el) {\n" +
                "       if(isRecording) {\n" +
                "          el.innerHTML = '🔴 GRABANDO';\n" +
                "          el.style.color = '#ff4444';\n" +
                "          el.style.fontWeight = 'bold';\n" +
                "          el.style.animation = 'pulse 1s infinite';\n" +
                "       } else {\n" +
                "          el.innerHTML = '⏺️ VIGILANDO...';\n" +
                "          el.style.color = '#aaaaaa';\n" +
                "          el.style.fontWeight = 'normal';\n" +
                "          el.style.animation = 'none';\n" +
                "       }\n" +
                "   });\n" +
                "}\n" +
                "\n" +
                "// Auto-Refresh Stats\n" +
                "function updateStats() {\n" +
                "  fetch('/stats?_=' + Date.now()).then(r => r.json()).then(data => {\n" +
                "     var batIcon = data.charging ? '⚡' : (data.bat > 20 ? '🔋' : '🪫');\n" +
                "     var tempIcon = data.temp > 40 ? '🔥' : '🌡️';\n" +
                "     document.querySelectorAll('.stat-bat').forEach(function(el) { el.innerHTML = batIcon + ' ' + data.bat + '%'; });\n"
                +
                "     document.querySelectorAll('.stat-temp').forEach(function(el) { el.innerHTML = tempIcon + ' ' + data.temp + '°C'; });\n"
                +
                "     document.querySelectorAll('.stat-storage').forEach(function(el) { el.innerHTML = '💾 ' + data.storage; });\n"
                +
                "     if(data.recording !== undefined) { updateStatusIndicator(data.recording); currentRecordingState = data.recording; }\n"
                +
                "  }).catch(e => console.log('Stats error', e));\n" +
                "}\n" +
                "\n" +
                "// Animated Thumbnails Logic\n" +
                "var miniCanvases = document.querySelectorAll('.mini-canvas');\n" +
                "if (miniCanvases.length > 0) {\n" +
                "    for(var i = 0; i < miniCanvases.length; i++) {\n" +
                "        var canvas = miniCanvases[i];\n" +
                "        var url = canvas.getAttribute('data-src');\n" +
                "        if(url) {\n" +
                "            loadMiniPreview(url, canvas);\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "function loadMiniPreview(url, canvas) {\n" +
                "    var ctx = canvas.getContext('2d');\n" +
                "    var frames = [];\n" +
                "    var idx = 0;\n" +
                "    \n" +
                "    fetch(url).then(response => {\n" +
                "        const reader = response.body.getReader();\n" +
                "        var buffer = new Uint8Array(0);\n" +
                "        \n" +
                "        // Simple loop to read everything\n" +
                "        function pump() {\n" +
                "            reader.read().then(({done, value}) => {\n" +
                "                if (done) { startAnimation(); return; }\n" +
                "                \n" +
                "                // Append\n" +
                "                var newBuffer = new Uint8Array(buffer.length + value.length);\n" +
                "                newBuffer.set(buffer); newBuffer.set(value, buffer.length);\n" +
                "                buffer = newBuffer;\n" +
                "                \n" +
                "                // Parse JPEGs on the fly (or wait for all? on the fly is better for memory, \n" +
                "                // but for \"Mini\" files (90kb), we can just wait? \n" +
                "                // Actually parsing on the fly is standard MJPEG.\n" +
                "                // Re-implement simplified parser\n" +
                "                \n" +
                "                while(true) {\n" +
                "                    var start = -1;\n" +
                "                    // Find SOI\n" +
                "                    for(var i=0; i<buffer.length-1; i++) {\n" +
                "                        if(buffer[i]===0xFF && buffer[i+1]===0xD8) { start=i; break; }\n" +
                "                    }\n" +
                "                    if(start === -1) break;\n" +
                "                    \n" +
                "                    var end = -1;\n" +
                "                    // Find EOI\n" +
                "                    for(var i=start+2; i<buffer.length-1; i++) {\n" +
                "                        if(buffer[i]===0xFF && buffer[i+1]===0xD9) { end=i+2; break; }\n" +
                "                    }\n" +
                "                    if(end === -1) break;\n" +
                "                    \n" +
                "                    var jpeg = buffer.slice(start, end);\n" +
                "                    // Safe Image Loading for Old WebViews\n" +
                "                    var blob = new Blob([jpeg], {type: 'image/jpeg'});\n" +
                "                    var imgUrl = URL.createObjectURL(blob);\n" +
                "                    var img = new Image();\n" +
                "                    img.onload = function() {\n" +
                "                        frames.push(this);\n" +
                "                        URL.revokeObjectURL(imgUrl);\n" +
                "                        if(frames.length === 1) startAnimation();\n" +
                "                    };\n" +
                "                    img.src = imgUrl;\n" +
                "                    \n" +
                "                    buffer = buffer.slice(end);\n" +
                "                }\n" +
                "                \n" +
                "                pump();\n" +
                "            });\n" +
                "        }\n" +
                "        pump();\n" +
                "    });\n" +
                "    \n" +
                "    function startAnimation() {\n" +
                "        if(canvas.isAnimating) return;\n" +
                "        canvas.isAnimating = true;\n" +
                "        \n" +
                "        function loop() {\n" +
                "            if(frames.length > 0) {\n" +
                "                ctx.drawImage(frames[idx], 0, 0, canvas.width, canvas.height); // Scaling done by drawImage\n"
                +
                "                idx = (idx + 1) % frames.length;\n" +
                "            }\n" +
                "            setTimeout(() => requestAnimationFrame(loop), 100); // 10 FPS\n" +
                "        }\n" +
                "        loop();\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "// --- Phase 17: Live Preview Injection (Parasite) ---\n" +
                "var parasiteInterval = null;\n" +
                "var parasiteBuffer = [];\n" +
                "var parasiteIdx = 0;\n" +
                "\n" +
                "function injectLivePreview() {\n" +
                "    console.log('Injecting Live Preview...');\n" +
                "    fetch('/api/latest_video_meta').then(r=>r.json()).then(meta => {\n" +
                "        if(!meta.filename) return;\n" +
                "        \n" +
                "        var container = document.querySelector('.library');\n" +
                "        var div = document.createElement('div');\n" +
                "        div.className = 'video-item';\n" +
                "        div.id = 'temp-preview-card';\n" +
                "        div.style.borderLeft = '4px solid #d32f2f';\n" +
                "        div.style.background = '#3e2727';\n" +
                "        \n" +
                "        div.innerHTML = \n" +
                "          \"<div class='thumb-container' style='border: 1px solid #d32f2f;'>\" +\n" +
                "             \"<canvas id='parasite-canvas' class='thumb' width='320' height='240'></canvas>\" + \n" +
                "          \"</div>\" +\n" +
                "          \"<div class='info'><b>\" + meta.filename + \"</b><br>\" + \n" +
                "             \"<span style='color:#ff4444; font-weight:bold; animation: blink 1s infinite;'>🔴 GRABANDO...</span>\" +\n"
                +
                "          \"</div>\";\n" +
                "          \n" +
                "        var title = container.querySelector('.section-title');\n" +
                "        title.parentNode.insertBefore(div, title.nextSibling);\n" +
                "        \n" +
                "        startParasite();\n" +
                "    });\n" +
                "}\n" +
                "\n" +
                "function startParasite() {\n" +
                "    var img = document.createElement('img');\n" +
                "    img.src = '/stream';\n" +
                "    img.style.display = 'none';\n" +
                "    img.id = 'hidden-stream-source';\n" +
                "    document.body.appendChild(img);\n" +
                "    \n" +
                "    var hiddenCanvas = document.createElement('canvas');\n" +
                "    hiddenCanvas.width = 320;\n" +
                "    hiddenCanvas.height = 240;\n" +
                "    var ctx = hiddenCanvas.getContext('2d');\n" +
                "    \n" +
                "    parasiteBuffer = [];\n" +
                "    \n" +
                "    // Capture Loop (1 FPS)\n" +
                "    parasiteInterval = setInterval(function() {\n" +
                "        try {\n" +
                "            ctx.drawImage(img, 0, 0, 320, 240);\n" +
                "            parasiteBuffer.push(hiddenCanvas.toDataURL('image/jpeg', 0.4));\n" +
                "            if (parasiteBuffer.length > 30) parasiteBuffer.shift();\n" +
                "        } catch(e) { console.log('Parasite capture error (waiting for stream)', e); }\n" +
                "    }, 1000);\n" +
                "    \n" +
                "    // Playback Loop\n" +
                "    var displayCanvas = document.getElementById('parasite-canvas');\n" +
                "    if(displayCanvas) {\n" +
                "        var dCtx = displayCanvas.getContext('2d');\n" +
                "        var pLoop = function() {\n" +
                "            if(parasiteBuffer.length > 0) {\n" +
                "                var i = new Image();\n" +
                "                i.onload = function() { dCtx.drawImage(i, 0, 0, 320, 240); };\n" +
                "                i.src = parasiteBuffer[parasiteIdx];\n" +
                "                parasiteIdx = (parasiteIdx + 1) % parasiteBuffer.length;\n" +
                "            }\n" +
                "            // Logic Change for v2.8.1: Check if canvas is still in DOM, regardless of parent ID\n" +
                "            if(document.body.contains(displayCanvas)) {\n" +
                "                setTimeout(pLoop, 100); // 10 FPS\n" +
                "            }\n" +
                "        };\n" +
                "        pLoop();\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "function cleanupLivePreview() {\n" +
                "    if(parasiteInterval) clearInterval(parasiteInterval);\n" +
                "    var img = document.getElementById('hidden-stream-source');\n" +
                "    if(img) document.body.removeChild(img);\n" +
                "    \n" +
                "    // Phase 17.2: Hot-Swap Logic\n" +
                "    console.log('Recording stopped. Waiting for buffer flush (3s)...');\n" +
                "    setTimeout(function() {\n" +
                "        var card = document.getElementById('temp-preview-card');\n" +
                "        if(card) {\n" +
                "             fetch('/api/latest_video_meta').then(r=>r.json()).then(meta => {\n" +
                "                if(!meta.filename) return; \n" +
                "                \n" +
                "                var newHtml = \n" +
                "                \"<div class='video-item' onclick=\\\"playVideo('\" + meta.filename + \"')\\\">\" +\n"
                +
                // Phase 23: Human-Readable Metadata
                "                \"<div class='info'>\" +\n" +
                "                \"     <div style='font-size:15px; font-weight:bold; color:#ffffff; margin-bottom:4px;'>📅 \" + meta.date + \" &nbsp; ⏰ \" + meta.time + \"</div>\" +\n"
                +
                "                \"     <div style='color:#ccc; font-size:13px;'>\" +\n" +
                "                \"         <b>💾 \" + meta.size + \"</b> &nbsp;|&nbsp; <b>⏳ \" + meta.duration + \"</b>\" +\n"
                +
                "                \"     </div>\" +\n" +
                "                \"</div></div>\";\n" +
                "                \n" +
                "                card.outerHTML = newHtml;\n" +
                "             });\n" +
                "        }\n" +
                "    }, 3000);\n" +
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
}
