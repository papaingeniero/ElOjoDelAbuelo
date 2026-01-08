package com.elojodelabuelo;

/**
 * Detects motion by comparing the current frame with the previous one.
 * Uses a stride to skip pixels for performance.
 */
public class MotionDetector {

    private byte[] previousFrame;
    private static final int STRIDE = 10;
    private static final int THRESHOLD = 50; // Pixel difference threshold
    private static final int MOTION_PIXEL_COUNT = 50; // Number of different pixels to trigger motion

    public int getMotionScore(byte[] currentFrame, int width, int height) {
        if (previousFrame == null || previousFrame.length != currentFrame.length) {
            previousFrame = currentFrame.clone();
            return 0;
        }

        int diffCount = 0;
        // YUV NV21 format: Y component is the first width * height bytes.
        // We only check luminance (Y) for motion.
        int limit = width * height;

        for (int i = 0; i < limit; i += STRIDE) {
            int val1 = currentFrame[i] & 0xFF;
            int val2 = previousFrame[i] & 0xFF; // previousFrame is updated at the end?
                                                // Actually, if we update it every frame, we detect inter-frame motion.

            if (Math.abs(val1 - val2) > THRESHOLD) {
                diffCount++;
            }
        }

        // Update previous frame for next comparison
        // Optimization: Instead of full clone, we could just swap reference if the
        // caller allocates new buffer.
        // But Camera.PreviewCallback reuses the same buffer often or we get a new one.
        // Since we need to persist the reference to 'currentFrame' as 'previousFrame'
        // for the NEXT call,
        // we must be careful if the buffer is reused by Camera API.
        // With setPreviewCallbackWithBuffer, we control the buffers.
        // For safety here, we clone. System copy is fast enough for 320x240.
        System.arraycopy(currentFrame, 0, previousFrame, 0, currentFrame.length);

        return diffCount;
    }
}
