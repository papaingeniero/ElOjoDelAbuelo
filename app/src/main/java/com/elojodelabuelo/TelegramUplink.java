package com.elojodelabuelo;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class TelegramUplink {

    private static final String LINE_FEED = "\r\n";
    private static final String BOUNDARY = "*****" + System.currentTimeMillis() + "*****";
    private static final String TWO_HYPHENS = "--";

    // Cola de subida serie (Single Thread)
    private static final ExecutorService uploadExecutor = Executors.newSingleThreadExecutor();

    public static void enviarPreview(final File file, final String token, final String chatId) {
        uploadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                subirArchivo(file, "video", "sendVideo", "", token, chatId, true);
            }
        });
    }

    public static void enviarClip(final File file, final String token, final String chatId, final String caption) {
        uploadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                subirArchivo(file, "document", "sendDocument", caption, token, chatId, false);
            }
        });
    }
    
    public static void sendTextMessage(final String msg, final String token, final String chatId) {
        uploadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (token == null || token.isEmpty() || chatId == null || chatId.isEmpty()) return;
                    
                    URL url = new URL("https://api.telegram.org/bot" + token + "/sendMessage");
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    
                    // --- LIMPIEZA: NO FORZAMOS FACTORY MANUAL ---
                    // Conscrypt ya está actuando globalmente. No tocar nada.
                    // ---------------------------------------------
                    
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    
                    String params = "chat_id=" + chatId + "&text=" + java.net.URLEncoder.encode(msg, "UTF-8");
                    DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                    dos.writeBytes(params);
                    dos.flush();
                    dos.close();
                    
                    int status = conn.getResponseCode();
                    SentinelService.logToWeb("TELEGRAM TEST MSG: " + status);
                    
                } catch (Exception e) {
                    SentinelService.logToWeb("TELEGRAM TEST FAIL: " + e.getMessage());
                }
            }
        });
    }

    private static void subirArchivo(final File file, final String fileField, final String endpoint, final String caption, final String token, final String chatId, final boolean silent) {
        try {
            if (token == null || token.isEmpty() || chatId == null || chatId.isEmpty()) return;

            FileInputStream fileInputStream = new FileInputStream(file);
            URL url = new URL("https://api.telegram.org/bot" + token + "/" + endpoint);
            
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            
            // --- LIMPIEZA: ELIMINADO EL BLOQUE setSSLSocketFactory ---
            // Confiamos en el Provider Conscrypt inyectado en SentinelService.onCreate()
            // ---------------------------------------------------------
            
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + BOUNDARY);

            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

            addTextField(dos, "chat_id", chatId);
            if (caption != null && !caption.isEmpty()) addTextField(dos, "caption", caption);
            if (silent) addTextField(dos, "disable_notification", "true");

            dos.writeBytes(TWO_HYPHENS + BOUNDARY + LINE_FEED);
            dos.writeBytes("Content-Disposition: form-data; name=\"" + fileField + "\"; filename=\"" + file.getName() + "\"" + LINE_FEED);
            dos.writeBytes(LINE_FEED);

            // Buffer de 8KB (Standard Page Size)
            int bytesAvailable = fileInputStream.available();
            int bufferSize = Math.min(bytesAvailable, 8192); 
            byte[] buffer = new byte[bufferSize];
            int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            while (bytesRead > 0) {
                dos.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, 8192);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }

            dos.writeBytes(LINE_FEED);
            dos.writeBytes(TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + LINE_FEED);

            fileInputStream.close();
            dos.flush();
            dos.close();

            int status = conn.getResponseCode();
            if (status == 200) {
                SentinelService.logToWeb("TELEGRAM OK: " + file.getName() + " (" + endpoint + ")");
            } else {
                // Leer error del servidor si falla
                try {
                    java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getErrorStream()));
                    String line;
                    StringBuilder err = new StringBuilder();
                    while ((line = br.readLine()) != null) err.append(line);
                    SentinelService.logToWeb("TELEGRAM SERVER ERROR: " + status + " -> " + err.toString());
                } catch(Exception ex) {
                    SentinelService.logToWeb("TELEGRAM ERROR: " + status);
                }
            }

        } catch (Exception e) {
            SentinelService.logToWeb("TELEGRAM FAIL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void addTextField(DataOutputStream dos, String name, String value) throws Exception {
        dos.writeBytes(TWO_HYPHENS + BOUNDARY + LINE_FEED);
        dos.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"" + LINE_FEED);
        dos.writeBytes(LINE_FEED);
        dos.writeBytes(value + LINE_FEED);
    }
}
