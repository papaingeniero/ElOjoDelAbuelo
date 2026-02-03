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
        // DESACTIVADO POR CONFIGURACIÓN DE USUARIO
        // No subimos preview, ahorramos datos y batería.
        SentinelService.logToWeb("ℹ️ Uplink: Preview omitido (Configuración Ahorro).");
    }

    public static void enviarClip(final File file, final String token, final String chatId, final String caption) {
        uploadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                SentinelService.logToWeb("🚀 Uplink: Iniciando subida dual (Foto + Video)...");

                // PASO 1: Extraer y enviar la MEJOR FOTO (La más pesada)
                byte[] bestFrame = extractBestFrame(file);
                if (bestFrame != null) {
                    subirBytesComoFoto(bestFrame, "sendPhoto", caption, token, chatId, false);
                } else {
                    sendTextMessage("🚨 " + caption, token, chatId);
                }
                
                // PASO 2: Enviar el VÍDEO COMPLETO como Documento
                subirArchivo(file, "document", "sendDocument", "📁 Evidencia Completa (MJPEG)", token, chatId, true);
            }
        });
    }
    
    /**
     * ALGORITMO: "El Peso Manda"
     * Escanea el archivo y extrae el frame JPEG que ocupa más bytes.
     */
    private static byte[] extractBestFrame(File mjpegFile) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(mjpegFile);
            byte[] fileData = new byte[(int) mjpegFile.length()];
            int totalRead = fis.read(fileData);
            
            if (totalRead < 1000) return null;

            int bestStart = -1;
            int bestLen = 0;
            int currentStart = -1;
            
            for (int i = 0; i < totalRead - 1; i++) {
                if ((fileData[i] & 0xFF) == 0xFF && (fileData[i+1] & 0xFF) == 0xD8) {
                    currentStart = i;
                }
                if (currentStart != -1 && (fileData[i] & 0xFF) == 0xFF && (fileData[i+1] & 0xFF) == 0xD9) {
                    int currentLen = (i + 2) - currentStart;
                    if (currentLen > bestLen) {
                        bestLen = currentLen;
                        bestStart = currentStart;
                    }
                    currentStart = -1;
                }
            }

            if (bestStart != -1 && bestLen > 0) {
                SentinelService.logToWeb("📸 SmartSnap: Frame seleccionado (" + (bestLen/1024) + " KB)");
                return Arrays.copyOfRange(fileData, bestStart, bestStart + bestLen);
            }
            return null;

        } catch (Exception e) {
            SentinelService.logToWeb("⚠️ Snapshot Error: " + e.getMessage());
            return null;
        } finally {
            try { if (fis != null) fis.close(); } catch (Exception e) {}
        }
    }

    // --- MÉTODOS DE RED (Optimizados: Streaming + UTF-8 + SSL EXPLICITO) ---

    public static void sendTextMessage(final String msg, final String token, final String chatId) {
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
            conn.getResponseCode(); // Trigger request
        } catch (Exception e) {}
    }

    private static void subirBytesComoFoto(final byte[] data, final String endpoint, final String caption, final String token, final String chatId, final boolean silent) {
        try {
            URL url = new URL("https://api.telegram.org/bot" + token + "/" + endpoint);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            
            // [FIX] Inyección SSL
            javax.net.ssl.SSLSocketFactory csFactory = getConscryptSocketFactory();
            if (csFactory != null) conn.setSSLSocketFactory(csFactory);

            conn.setChunkedStreamingMode(4096); 
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
            dos.writeBytes("Content-Disposition: form-data; name=\"photo\"; filename=\"snapshot.jpg\"" + LINE_FEED);
            dos.writeBytes(LINE_FEED);

            dos.write(data);

            dos.writeBytes(LINE_FEED);
            dos.writeBytes(TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + LINE_FEED);
            dos.flush();
            dos.close();

            int status = conn.getResponseCode();
            if (status != 200) SentinelService.logToWeb("TELEGRAM PHOTO ERR: " + status);

        } catch (Exception e) {
            SentinelService.logToWeb("TELEGRAM PHOTO FAIL: " + e.getMessage());
            e.printStackTrace();
        }
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
            String remoteFilename = file.getName().replace(".mjpeg", ".avi");
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