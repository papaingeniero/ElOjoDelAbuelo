package com.elojodelabuelo;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.HttpsURLConnection;

public class TelegramUplink {

    private static final String LINE_FEED = "\r\n";
    private static final String BOUNDARY = "*****" + System.currentTimeMillis() + "*****";
    private static final String TWO_HYPHENS = "--";

    // Executor mono-hilo para asegurar orden cronológico y no saturar la CPU
    private static final ExecutorService uploadExecutor = Executors.newSingleThreadExecutor();

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
                // Esto da una visualización inmediata en el chat.
                byte[] bestFrame = extractBestFrame(file);
                if (bestFrame != null) {
                    // Enviamos la foto con el caption (los emojis se verán aquí)
                    subirBytesComoFoto(bestFrame, "sendPhoto", caption, token, chatId, false);
                } else {
                    // Si falla la extracción, mandamos al menos el texto de alerta
                    sendTextMessage("🚨 " + caption, token, chatId);
                }
                
                // PASO 2: Enviar el VÍDEO COMPLETO como Documento
                // Se envía sin caption redundante (o uno breve) para no ensuciar el chat.
                // Usamos sendDocument para evitar errores de reproducción en Telegram.
                subirArchivo(file, "document", "sendDocument", "📁 Evidencia Completa (MJPEG)", token, chatId, true);
            }
        });
    }
    
    /**
     * ALGORITMO: "El Peso Manda"
     * Escanea el archivo y extrae el frame JPEG que ocupa más bytes.
     * Mayor tamaño = Más detalle/movimiento (habitualmente).
     */
    private static byte[] extractBestFrame(File mjpegFile) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(mjpegFile);
            // Leemos el archivo entero en memoria (Cuidado: MJPEGs grandes pueden dar OOM,
            // pero los clips de seguridad suelen ser < 5MB).
            byte[] fileData = new byte[(int) mjpegFile.length()];
            int totalRead = fis.read(fileData);
            
            if (totalRead < 1000) return null;

            int bestStart = -1;
            int bestLen = 0;
            int currentStart = -1;
            
            // Escaneo de bytes buscando cabeceras JPG (FF D8 ... FF D9)
            for (int i = 0; i < totalRead - 1; i++) {
                // Detectar Inicio (SOI): FF D8
                if ((fileData[i] & 0xFF) == 0xFF && (fileData[i+1] & 0xFF) == 0xD8) {
                    currentStart = i;
                }
                
                // Detectar Fin (EOI): FF D9
                if (currentStart != -1 && (fileData[i] & 0xFF) == 0xFF && (fileData[i+1] & 0xFF) == 0xD9) {
                    int currentLen = (i + 2) - currentStart;
                    
                    // ¿Es este el frame más gordo hasta ahora?
                    if (currentLen > bestLen) {
                        bestLen = currentLen;
                        bestStart = currentStart;
                    }
                    currentStart = -1; // Reset para buscar el siguiente
                }
            }

            if (bestStart != -1 && bestLen > 0) {
                SentinelService.logToWeb("📸 SmartSnap: Frame seleccionado (" + (bestLen/1024) + " KB)");
                return Arrays.copyOfRange(fileData, bestStart, bestStart + bestLen);
            }
            return null;

        } catch (Exception e) {
            SentinelService.logToWeb("⚠️ Snapshot Error: " + e.getMessage());
            return null; // Fallamos silenciosamente y subimos solo el vídeo
        } finally {
            try { if (fis != null) fis.close(); } catch (Exception e) {}
        }
    }

    // --- MÉTODOS DE RED (Optimizados: Streaming + UTF-8) ---

    public static void sendTextMessage(final String msg, final String token, final String chatId) {
        try {
            if (token == null || token.isEmpty()) return;
            URL url = new URL("https://api.telegram.org/bot" + token + "/sendMessage");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            
            String params = "chat_id=" + chatId + "&text=" + java.net.URLEncoder.encode(msg, "UTF-8");
            
            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
            dos.writeBytes(params);
            dos.flush();
            dos.close();
            conn.getResponseCode();
        } catch (Exception e) {}
    }

    private static void subirBytesComoFoto(final byte[] data, final String endpoint, final String caption, final String token, final String chatId, final boolean silent) {
        try {
            URL url = new URL("https://api.telegram.org/bot" + token + "/" + endpoint);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            
            // STREAMING MODE: Vital para no duplicar memoria
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

            // Escribir bytes de la imagen
            dos.write(data);

            dos.writeBytes(LINE_FEED);
            dos.writeBytes(TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + LINE_FEED);
            dos.flush();
            dos.close();

            int status = conn.getResponseCode();
            if (status != 200) SentinelService.logToWeb("TELEGRAM PHOTO ERR: " + status);

        } catch (Exception e) {
            SentinelService.logToWeb("TELEGRAM PHOTO FAIL: " + e.getMessage());
        }
    }

    private static void subirArchivo(final File file, final String fileField, final String endpoint, final String caption, final String token, final String chatId, final boolean silent) {
        try {
            if (token == null || token.isEmpty()) return;
            FileInputStream fileInputStream = new FileInputStream(file);
            URL url = new URL("https://api.telegram.org/bot" + token + "/" + endpoint);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            
            conn.setChunkedStreamingMode(4096); // Streaming vital
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
            // Forzamos nombre "document" si el endpoint es sendDocument
            String fieldName = endpoint.equals("sendDocument") ? "document" : fileField;
            dos.writeBytes("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + file.getName() + "\"" + LINE_FEED);
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
        }
    }

    private static void addTextField(DataOutputStream dos, String name, String value) throws Exception {
        dos.writeBytes(TWO_HYPHENS + BOUNDARY + LINE_FEED);
        dos.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"" + LINE_FEED);
        // Header explícito para UTF-8
        dos.writeBytes("Content-Type: text/plain; charset=UTF-8" + LINE_FEED); 
        dos.writeBytes(LINE_FEED);
        // Escribimos bytes UTF-8 para soportar emojis 🚨
        dos.write(value.getBytes("UTF-8"));
        dos.writeBytes(LINE_FEED);
    }
}