package com.elojodelabuelo;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext; // [RESTORED] NATIVE SSL Support

public class TelegramUplink {

    private static final String LINE_FEED = "\r\n";
    private static final String BOUNDARY = "*****" + System.currentTimeMillis() + "*****";
    private static final String TWO_HYPHENS = "--";

    // Executor mono-hilo para asegurar orden cronológico y no saturar la CPU
    private static final ExecutorService uploadExecutor = Executors.newSingleThreadExecutor();

    // --- [RESTORED] EXPLICIT CONSCRYPT HELPER ---
    // Vital para Android 4.4, ya que HttpsURLConnection ignora el orden de Providers
    private static javax.net.ssl.SSLSocketFactory getConscryptSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS", "Conscrypt");
            sslContext.init(null, null, null);
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            SentinelService.logToWeb("⚠️ Conscrypt Explicit Fail: " + e.getMessage() + ". Using Default.");
            return null;
        }
    }
    // ---------------------------------

    public static void enviarPreview(final File file, final String token, final String chatId) {
        if (file == null || !file.exists()) return;

        uploadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                File mp4File = null;
                try {
                    // INTENTO 1: Transcodificar a MP4 (Autoplay)
                    SentinelService.logToWeb("🎥 Uplink: Generando Preview MP4...");
                    mp4File = MjpegToMp4.convert(file, file.getParentFile());

                    if (mp4File != null && mp4File.exists() && mp4File.length() > 0) {
                        SentinelService.logToWeb("🚀 Uplink: Subiendo Preview MP4 (Autoplay)...");
                        subirArchivo(mp4File, "video", "sendVideo", "🎥 Preview Rápido", token, chatId, true);
                    } else {
                        throw new RuntimeException("Transcoding returned null or empty file");
                    }

                } catch (Exception e) {
                    // FALLBACK: Enviar MJPEG original (como AVI para VLC) dobles
                    SentinelService.logToWeb("⚠️ Uplink: Falló Transcoding (" + e.getMessage() + "). Usando Fallback.");
                    subirArchivo(file, "document", "sendDocument", "⚠️ Preview (MJPEG Original)", token, chatId, true);

                } finally {
                    // Limpieza: Borrar el MP4 temporal para no llenar la SD
                    if (mp4File != null && mp4File.exists()) {
                        mp4File.delete();
                    }
                }
            }
        });
    }

    

    // --- MÉTODOS DE RED (Optimizados: Streaming + UTF-8 + SSL EXPLICITO) ---

    public static void sendTextMessage(final String msg, final String token, final String chatId) {
        uploadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (token == null || token.isEmpty()) return;
                    URL url = new URL("https://api.telegram.org/bot" + token + "/sendMessage");
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    
                    // [FIX] Inyección SSL
                    javax.net.ssl.SSLSocketFactory csFactory = getConscryptSocketFactory();
                    if (csFactory != null) conn.setSSLSocketFactory(csFactory);

                    conn.setConnectTimeout(10000);
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    
                    String params = "chat_id=" + chatId + "&text=" + java.net.URLEncoder.encode(msg, "UTF-8");
                    
                    DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                    dos.writeBytes(params);
                    dos.flush();
                    dos.close();
                    
                    int status = conn.getResponseCode(); // Trigger request
                    if (status != 200) {
                        SentinelService.logToWeb("⚠️ Telegram ALERT Error: HTTP " + status);
                    }
                } catch (Exception e) {
                    SentinelService.logToWeb("❌ Telegram ALERT Fail: " + e.getMessage());
                }
            }
        });
    }


    private static void subirArchivo(final File file, final String fileField, final String endpoint, final String caption, final String token, final String chatId, final boolean silent) {
        try {
            if (token == null || token.isEmpty()) return;
            FileInputStream fileInputStream = new FileInputStream(file);
            URL url = new URL("https://api.telegram.org/bot" + token + "/" + endpoint);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            
            // [FIX] Inyección SSL
            javax.net.ssl.SSLSocketFactory csFactory = getConscryptSocketFactory();
            if (csFactory != null) conn.setSSLSocketFactory(csFactory);

            conn.setChunkedStreamingMode(4096); 
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + BOUNDARY);

            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
            addTextField(dos, "chat_id", chatId);
            if (caption != null) addTextField(dos, "caption", caption);
            if (silent) addTextField(dos, "disable_notification", "true");

            dos.writeBytes(TWO_HYPHENS + BOUNDARY + LINE_FEED);
            String fieldName = endpoint.equals("sendDocument") ? "document" : fileField;
            
            // --- LOGICA EXTENSIÓN INTELIGENTE ---
            String remoteFilename = file.getName();
            if (remoteFilename.endsWith(".mjpeg")) {
                // HACK iOS: Si es MJPEG, lo disfrazamos de AVI para que VLC lo abra
                remoteFilename = remoteFilename.replace(".mjpeg", ".avi");
            }
            
            dos.writeBytes("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + remoteFilename + "\"" + LINE_FEED);
            dos.writeBytes(LINE_FEED);

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
                dos.flush();
            }
            dos.writeBytes(LINE_FEED);
            dos.writeBytes(TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + LINE_FEED);
            fileInputStream.close();
            dos.close();

            int status = conn.getResponseCode();
            if (status == 200) SentinelService.logToWeb("TELEGRAM VIDEO OK");
            else SentinelService.logToWeb("TELEGRAM VIDEO ERR: " + status);

        } catch (Exception e) {
            SentinelService.logToWeb("TELEGRAM VIDEO FAIL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void addTextField(DataOutputStream dos, String name, String value) throws Exception {
        dos.writeBytes(TWO_HYPHENS + BOUNDARY + LINE_FEED);
        dos.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"" + LINE_FEED);
        dos.writeBytes("Content-Type: text/plain; charset=UTF-8" + LINE_FEED); 
        dos.writeBytes(LINE_FEED);
        dos.write(value.getBytes("UTF-8"));
        dos.writeBytes(LINE_FEED);
    }
}