package com.elojodelabuelo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Transcodificador ligero para dispositivos Legacy (API 19+).
 * Convierte un stream MJPEG (fotos pegadas) en un vídeo MP4 (H.264)
 * compatible con Telegram Autoplay.
 */
public class MjpegToMp4 {

    private static final String TAG = "MjpegToMp4";
    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 4; // bajo de 5 a 3 FPS para que el timelapse se vea fluido
    private static final int IFRAME_INTERVAL = 1; // 1 segundo entre keyframes (para seeking rápido)
    private static final int BIT_RATE = 125000; // bajo un poco de 150kbps a 125kbps al bajar los fps

    public static File convert(File inputFile, File outputDir) {
        String outputName = inputFile.getName().replace(".mjpeg", ".mp4");
        File outputFile = new File(outputDir, outputName);

        MediaCodec encoder = null;
        MediaMuxer muxer = null;
        BufferedInputStream bis = null;

        try {
            SentinelService.logToWeb("🔄 TRANSCODING: Iniciando " + inputFile.getName() + " -> MP4...");
            long startTime = System.currentTimeMillis();

            // 1. Analizar MJPEG y extraer frames en memoria (solo offsets)
            List<byte[]> framesData = extractJpegFrames(inputFile);
            if (framesData.isEmpty()) {
                SentinelService.logToWeb("⚠️ Transcoding: No se encontraron frames en MJPEG.");
                return null;
            }

            // Usamos el primer frame para determinar dimensiones
            Bitmap firstBmp = BitmapFactory.decodeByteArray(framesData.get(0), 0, framesData.get(0).length);
            int width = firstBmp.getWidth();
            int height = firstBmp.getHeight();
            firstBmp.recycle();

            // 2. Configurar Encoder H.264
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar); // NV12
            format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

            encoder = MediaCodec.createEncoderByType(MIME_TYPE);
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder.start();

            // 3. Configurar Muxer (Empaquetador MP4)
            muxer = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int trackIndex = -1;
            boolean muxerStarted = false;

            // Buffers
            ByteBuffer[] inputBuffers = encoder.getInputBuffers();
            ByteBuffer[] outputBuffers = encoder.getOutputBuffers();
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            // 4. Bucle de Procesamiento Frame a Frame
            long presentationTimeUs = 0;
            long frameDurationUs = 1000000 / FRAME_RATE;

            for (byte[] jpgBytes : framesData) {
                // A) Decodificar JPG a Bitmap
                Bitmap bmp = BitmapFactory.decodeByteArray(jpgBytes, 0, jpgBytes.length);
                if (bmp == null)
                    continue;

                // --- [NUEVO] ZOOM DIGITAL & PAN (Sincronizado con Escala Web) ---
                // Leemos las preferencias WEB para replicar el encuadre exacto del navegador.
                try {
                    android.content.Context ctx = SentinelService.getAppContext();
                    if (ctx != null) {
                        android.content.SharedPreferences prefs = ctx.getSharedPreferences("SentinelPrefs",
                                android.content.Context.MODE_PRIVATE);
                        float webZoom = prefs.getFloat("webZoom", 1.0f);

                        // Solo procesamos si hay zoom digital activo (> 1.0)
                        if (webZoom > 1.0f) {
                            int webPanX = prefs.getInt("webPanX", 0);
                            int webPanY = prefs.getInt("webPanY", 0);

                            int srcW = bmp.getWidth();
                            int srcH = bmp.getHeight();

                            // 1. Calcular tamaño del Viewport (Ventana visible)
                            // Si Zoom=2.0, vemos la mitad de la imagen.
                            int viewW = (int) (srcW / webZoom);
                            int viewH = (int) (srcH / webZoom);

                            // 2. Calcular Origen del Recorte (Top-Left) basado en lógica CSS Translate
                            // La web usa translate(x%, y%). Un valor negativo mueve la imagen a la
                            // izquierda,
                            // lo que equivale a mover el viewport a la derecha (positivo).
                            // Fórmula inversa: Pixel de inicio = -1 * (Porcentaje / 100) * AnchoTotal
                            int cropX = (int) (-1 * (webPanX / 100.0f) * srcW);
                            int cropY = (int) (-1 * (webPanY / 100.0f) * srcH);

                            // 3. Clamping (Seguridad de bordes)
                            // Evitar coordenadas negativas
                            if (cropX < 0)
                                cropX = 0;
                            if (cropY < 0)
                                cropY = 0;

                            // Evitar salirnos por la derecha/abajo
                            if (cropX + viewW > srcW)
                                cropX = srcW - viewW;
                            if (cropY + viewH > srcH)
                                cropY = srcH - viewH;

                            // 4. Transformación Quirúrgica (Crop + Scale)
                            // Creamos el recorte
                            Bitmap cropped = Bitmap.createBitmap(bmp, cropX, cropY, viewW, viewH);
                            // Lo estiramos de nuevo al tamaño original (352x288) para llenar el vídeo
                            Bitmap scaled = Bitmap.createScaledBitmap(cropped, srcW, srcH, true); // true = Filtro
                                                                                                  // Bilinear

                            // 5. Gestión de Memoria Agresiva (Vital para Galaxy S)
                            if (cropped != bmp && cropped != scaled)
                                cropped.recycle();
                            if (bmp != scaled)
                                bmp.recycle();

                            // Sustituimos el bitmap original por la versión con zoom
                            bmp = scaled;
                        }
                    }
                } catch (Exception e) {
                    // Si falla el cálculo del zoom, no abortamos el vídeo.
                    // Logueamos y seguimos con la imagen original.
                    SentinelService.logToWeb("⚠️ MP4 Zoom Error: " + e.getMessage());
                } catch (OutOfMemoryError oom) {
                    SentinelService.logToWeb("⚠️ MP4 Zoom OOM: Memoria insuficiente, ignorando zoom.");
                    // System.gc(); // Opcional sugerencia al GC
                }
                // --- FIN ZOOM DIGITAL ---

                // B) Convertir Bitmap ARGB a YUV420 (NV12)
                byte[] yuvData = getNV12(width, height, bmp);
                bmp.recycle(); // Liberar RAM inmediatamente

                // C) Alimentar al Encoder
                int inputBufferIndex = encoder.dequeueInputBuffer(10000);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                    inputBuffer.clear();
                    inputBuffer.put(yuvData);
                    encoder.queueInputBuffer(inputBufferIndex, 0, yuvData.length, presentationTimeUs, 0);
                    presentationTimeUs += frameDurationUs;
                }

                // D) Sacar datos comprimidos del Encoder y meterlos al Muxer
                int outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000);
                while (outputBufferIndex >= 0) {
                    ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];

                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        // Info de configuración, ignorar pero no procesar
                        bufferInfo.size = 0;
                    }

                    if (bufferInfo.size != 0) {
                        if (!muxerStarted) {
                            throw new RuntimeException("muxer hasn't started");
                        }
                        outputBuffer.position(bufferInfo.offset);
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                        muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo);
                    }

                    encoder.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0);
                }

                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (muxerStarted) {
                        throw new RuntimeException("format changed twice");
                    }
                    MediaFormat newFormat = encoder.getOutputFormat();
                    trackIndex = muxer.addTrack(newFormat);
                    muxer.start();
                    muxerStarted = true;
                }
            }

            // E) Finalizar stream
            int inputBufferIndex = encoder.dequeueInputBuffer(10000);
            if (inputBufferIndex >= 0) {
                encoder.queueInputBuffer(inputBufferIndex, 0, 0, presentationTimeUs,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            }

            // Drenar lo que quede
            int outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000);
            while (outputBufferIndex >= 0) {
                if (bufferInfo.size != 0) {
                    if (muxerStarted) {
                        ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                        outputBuffer.position(bufferInfo.offset);
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                        muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo);
                    }
                }
                encoder.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0);
            }

            long duration = System.currentTimeMillis() - startTime;
            SentinelService.logToWeb("🔄 ✅ TRANSCODING: MP4 GENERADO: " + outputFile.getName() + " ("
                    + (outputFile.length() / 1024) + "KB) en " + duration + "ms");

            return outputFile;

        } catch (Exception e) {
            SentinelService.logToWeb("❌ MP4 TRANSCODE FAIL: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (encoder != null) {
                try {
                    encoder.stop();
                    encoder.release();
                } catch (Exception e) {
                }
            }
            if (muxer != null) {
                try {
                    muxer.stop();
                    muxer.release();
                } catch (Exception e) {
                }
            }
        }
    }

    // --- UTILS: LECTURA MJPEG ---
    private static List<byte[]> extractJpegFrames(File file) throws IOException {
        List<byte[]> frames = new ArrayList<>();
        FileInputStream fis = new FileInputStream(file);
        // Leemos todo a memoria (Cuidado con OOM si el preview es muy largo, pero suele
        // ser corto)
        // Si falla por RAM, habría que hacerlo con streams, pero para previews de 10
        // frames está bien.
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();

        int i = 0;
        while (i < data.length - 1) {
            if ((data[i] & 0xFF) == 0xFF && (data[i + 1] & 0xFF) == 0xD8) {
                int start = i;
                i += 2;
                while (i < data.length - 1) {
                    if ((data[i] & 0xFF) == 0xFF && (data[i + 1] & 0xFF) == 0xD9) {
                        int end = i + 2;
                        int len = end - start;
                        byte[] frame = new byte[len];
                        System.arraycopy(data, start, frame, 0, len);
                        frames.add(frame);
                        i = end;
                        break;
                    }
                    i++;
                }
            } else {
                i++;
            }
        }
        return frames;
    }

    // --- UTILS: ARGB a NV12 (YUV420SemiPlanar) ---
    // Esta conversión es necesaria porque MediaCodec no acepta Bitmaps
    // directamente.
    private static byte[] getNV12(int inputWidth, int inputHeight, Bitmap scaled) {
        int[] argb = new int[inputWidth * inputHeight];
        scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);

        byte[] yuv = new byte[inputWidth * inputHeight * 3 / 2];
        int frameSize = inputWidth * inputHeight;

        int yIndex = 0;
        int uvIndex = frameSize;

        int R, G, B, Y, U, V;

        for (int j = 0; j < inputHeight; j++) {
            for (int i = 0; i < inputWidth; i++) {
                int pixel = argb[j * inputWidth + i];
                R = (pixel >> 16) & 0xff;
                G = (pixel >> 8) & 0xff;
                B = pixel & 0xff;

                // Formula estándar RGB a YUV
                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

                yuv[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));

                if (j % 2 == 0 && i % 2 == 0) {
                    yuv[uvIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                    yuv[uvIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                }
            }
        }
        return yuv;
    }
}
