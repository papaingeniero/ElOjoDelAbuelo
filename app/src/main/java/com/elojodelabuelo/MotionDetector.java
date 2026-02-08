package com.elojodelabuelo;

/**
 * Detects motion by comparing the current frame with the previous one.
 * Uses a stride to skip pixels for performance.
 */
public class MotionDetector {

    private byte[] previousFrame;
    // Cambiamos de stride 30 a stride 10 porque hemos vuelto de VGA a CIF (3 veces menos pixeles)
    private static final int STRIDE = 10;
    private int colorDiffThreshold = 50; // Default (formerly constant THRESHOLD)
    private static final int MOTION_PIXEL_COUNT = 50; // Number of different pixels to trigger motion

    // 1. AÑADIR VARIABLES DE CLASE
    private static final float LIGHT_CHANGE_RATIO = 0.60f; // Umbral de "Luz Global" (60%)
    private boolean smartFilterEnabled = true;

    public void setSmartFilterEnabled(boolean enabled) {
        this.smartFilterEnabled = enabled;
    }

    public void setThreshold(int val) {
        if(val < 5) val = 5; 
        if(val > 100) val = 100;
        this.colorDiffThreshold = val;
    }

    public int getMotionScore(byte[] currentFrame, int width, int height) {
        if (previousFrame == null || previousFrame.length != currentFrame.length) {
            previousFrame = currentFrame.clone();
            return 0;
        }

        int diffCount = 0;
        int totalPixelsChecked = 0; // Nuevo contador
        // YUV NV21 format: Y component is the first width * height bytes.
        // We only check luminance (Y) for motion.
        int limit = width * height;

        for (int i = 0; i < limit; i += STRIDE) {
            totalPixelsChecked++;
            int val1 = currentFrame[i] & 0xFF;
            int val2 = previousFrame[i] & 0xFF; 

            if (Math.abs(val1 - val2) > colorDiffThreshold) {
                diffCount++;
            }
        }

        // --- LÓGICA SMART FILTER ---
        if (smartFilterEnabled && totalPixelsChecked > 0) {
            float changeRatio = (float) diffCount / totalPixelsChecked;
            if (changeRatio > LIGHT_CHANGE_RATIO) {
                // Es un cambio de luz (>60%). Actualizamos referencia pero ignoramos movimiento.
                System.arraycopy(currentFrame, 0, previousFrame, 0, currentFrame.length);
                return 0; 
            }
        }
        // ---------------------------

        // Update previous frame for next comparison
        System.arraycopy(currentFrame, 0, previousFrame, 0, currentFrame.length);

        return diffCount;
    }
}
