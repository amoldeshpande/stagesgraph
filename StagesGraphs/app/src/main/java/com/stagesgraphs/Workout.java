package com.stagesgraphs;

import java.util.ArrayList;

/** Monotonic-clock workout data, independent of Android and Bluetooth callbacks. */
public final class Workout {
    public static final long FRESH_MS = 5000;
    public final Series heart = new Series();
    public final Series power = new Series();
    public final Series cadence = new Series();
    private long started = -1, stopped, pausedAt = -1;
    private boolean running;

    public boolean isRunning() { return running; }
    public boolean isPaused() { return pausedAt >= 0; }
    private boolean isRecording() { return running && !isPaused(); }
    public void start(long now) {
        started = now;
        running = true;
        pausedAt = -1;
        heart.reset(now);
        power.reset(now);
        cadence.reset(now);
        sample(now);
    }
    public void stop(long now) {
        if (!running) return;
        if (!isPaused()) pause(now);
        stopped = pausedAt;
        pausedAt = -1;
        running = false;
    }
    public void pause(long now) {
        if (!isRecording()) return;
        sample(now);
        heart.frozenCurrent = heart.freshValue(now);
        power.frozenCurrent = power.freshValue(now);
        cadence.frozenCurrent = cadence.freshValue(now);
        pausedAt = now;
    }
    public void resume(long now) {
        if (!isPaused()) return;
        started += now - pausedAt;
        pausedAt = -1;
        heart.integratedAt = now;
        power.integratedAt = now;
        cadence.integratedAt = now;
        sample(now);
    }
    public long duration(long now) {
        long end = isPaused() ? pausedAt : running ? now : stopped;
        return started < 0 ? 0 : Math.max(0, end - started);
    }
    public void sample(long now) {
        if (!isRecording()) return;
        heart.record(now);
        power.record(now);
        cadence.record(now);
    }
    public static String time(long milliseconds) {
        long seconds = milliseconds / 1000;
        return seconds >= 3600
                ? String.format(java.util.Locale.US, "%d:%02d:%02d", seconds/3600, seconds/60%60, seconds%60)
                : String.format(java.util.Locale.US, "%d:%02d", seconds/60, seconds%60);
    }
    public static final class Sample {
        public final long elapsed;
        public final Integer value;
        Sample(long elapsed, Integer value) { this.elapsed = elapsed; this.value = value; }
    }
    public final class Series {
        public final ArrayList<Sample> points = new ArrayList<>();
        private Integer latest, frozenCurrent;
        private long latestAt, integratedAt, covered;
        private double area;
        public int minimum;
        private Integer maximum;

        public Integer maximum() { return maximum; }

        private void reset(long now) {
            points.clear();
            frozenCurrent = null;
            area = 0;
            covered = 0;
            integratedAt = now;
            minimum = 0;
            maximum = null;
        }
        public void observe(int value, long now) {
            integrate(now);
            latest = value;
            latestAt = now;
            if (isRecording()) trackRange(value);
        }
        public void disconnect(long now) {
            integrate(now);
            latest = null;
        }
        private Integer freshValue(long now) {
            return latest != null && now >= latestAt && now - latestAt < FRESH_MS ? latest : null;
        }
        public Integer current(long now) {
            if (started < 0) return null;
            return isRecording() ? freshValue(now) : frozenCurrent;
        }
        private void integrate(long now) {
            if (!isRecording()) return;
            long end = latest == null ? integratedAt : Math.min(now, latestAt + FRESH_MS);
            if (end > integratedAt) {
                long span = end - integratedAt;
                area += latest * (double) span;
                covered += span;
            }
            integratedAt = Math.max(integratedAt, now);
        }
        public Double average(long now) {
            integrate(now);
            return covered == 0 ? null : area / covered;
        }
        private void record(long now) {
            integrate(now);
            Integer value = freshValue(now);
            long elapsed = duration(now);
            if (!points.isEmpty() && points.get(points.size()-1).elapsed == elapsed) points.remove(points.size()-1);
            points.add(new Sample(elapsed, value));
            if (value != null) trackRange(value);
        }
        private void trackRange(int value) {
            minimum = Math.min(minimum, value);
            if (maximum == null || value > maximum) maximum = value;
        }
    }
}
