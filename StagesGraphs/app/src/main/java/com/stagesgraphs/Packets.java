package com.stagesgraphs;

/** Standard Bluetooth SIG Heart Rate and Cycling Power measurement payloads. */
public final class Packets {
    public static Integer decode(byte[] bytes, boolean heartRate) {
        if (bytes == null || bytes.length < 2) return null;
        if (heartRate) {
            boolean wide = (bytes[0] & 1) != 0;
            if (wide && bytes.length < 3) return null;
            // If sensor-contact detection is supported, reject no-contact readings.
            if ((bytes[0] & 4) != 0 && (bytes[0] & 2) == 0) return null;
            return (bytes[1] & 255) | (wide ? (bytes[2] & 255) << 8 : 0);
        }
        if (bytes.length < 4) return null;
        return (int)(short)((bytes[2] & 255) | ((bytes[3] & 255) << 8));
    }
}
