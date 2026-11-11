package com.stagesgraphs;

/** Standard Bluetooth SIG Heart Rate and Cycling Power measurement payloads. */
public final class Packets {
    private static int unsigned16(byte[] bytes, int offset) {
        return (bytes[offset] & 255) | ((bytes[offset+1] & 255) << 8);
    }

    static final class Cadence {
        private int revolutions, eventTime;
        private long lastEvent = -1, lastPacket;
        private Integer rpm;

        void reset() {
            lastEvent = -1;
            rpm = null;
        }

        Integer decode(byte[] bytes, long now) {
            if (bytes == null || bytes.length < 4) return null;
            int flags = unsigned16(bytes,0);
            if ((flags & 32) == 0) return null;
            int offset = 4;
            if ((flags & 1) != 0) offset++;
            if ((flags & 4) != 0) offset += 2;
            if ((flags & 16) != 0) offset += 6;
            if (bytes.length < offset+4) return null;
            int nextRevolutions = unsigned16(bytes,offset);
            int nextEventTime = unsigned16(bytes,offset+2);
            if (lastEvent < 0 || now-lastPacket >= 5000) {
                rpm = null;
                lastEvent = now;
            } else if (nextRevolutions != revolutions || nextEventTime != eventTime) {
                int turns = (nextRevolutions-revolutions) & 65535;
                int ticks = (nextEventTime-eventTime) & 65535;
                rpm = turns > 0 && ticks > 0 && now-lastEvent < 64000
                        ? (int)Math.round(turns*61440.0/ticks) : null;
                if (rpm != null && rpm > 300) rpm = null;
                lastEvent = now;
            } else if (now-lastEvent >= 3000) {
                rpm = 0;
            }
            revolutions = nextRevolutions;
            eventTime = nextEventTime;
            lastPacket = now;
            return rpm;
        }
    }
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
