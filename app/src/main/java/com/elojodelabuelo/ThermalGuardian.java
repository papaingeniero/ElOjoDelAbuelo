package com.elojodelabuelo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ThermalGuardian {

    private static final String TEMP_PATH = "/sys/class/power_supply/battery/temp";
    private static final int MAX_TEMP = 430; // 43.0 degrees Celsius

    private long lastTempCheckTime = 0;
    private boolean lastOverheatValue = false;
    private static final long CHECK_INTERVAL_MS = 15000; // 15 seconds

    public boolean isOverheating() {
        long now = System.currentTimeMillis();
        if (now - lastTempCheckTime < CHECK_INTERVAL_MS) {
            return lastOverheatValue;
        }

        lastTempCheckTime = now;
        File file = new File(TEMP_PATH);
        if (!file.exists()) {
            lastOverheatValue = false;
            return false;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            if (line != null) {
                int temp = Integer.parseInt(line.trim());
                lastOverheatValue = (temp > MAX_TEMP);
                return lastOverheatValue;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        lastOverheatValue = false;
        return false;
    }

    public static int getBatteryTemperature(android.content.Context context) {
        File file = new File(TEMP_PATH);
        if (!file.exists()) {
            // Fallback to BatteryManager if file not found? For now just return -1 or 0
            return 0;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            if (line != null) {
                int tempRaw = Integer.parseInt(line.trim());
                return tempRaw / 10; // Convert 420 -> 42
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        return 0;
    }
}
