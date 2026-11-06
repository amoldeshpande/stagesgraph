package com.stagesgraphs;

import java.util.ArrayList;

/** Monotonic-clock workout data, independent of Android and Bluetooth callbacks. */
public final class Workout {
    public static final long FRESH_MS = 5000;
    public final Series heart = new Series();
    public final Series power = new Series();
    private long started = -1, stopped;
    private boolean running;

    public boolean isRunning() { return running; }
    public void start(long now) {
        started = now;
        running = true;
        heart.reset(now);
        power.reset(now);
        sample(now);
    }
    public void stop(long now) {
        if (!running) return;
        sample(now);
        heart.frozenCurrent = heart.freshValue(now);
        power.frozenCurrent = power.freshValue(now);
        stopped = now;
        running = false;
    }
    public long duration(long now) {
        return started < 0 ? 0 : Math.max(0, (running ? now : stopped) - started);
    }
    public void sample(long now) {
        if (!running) return;
        heart.record(now);
        power.record(now);
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
        public int minimum, maximum;

        private void reset(long now) {
            points.clear();
            frozenCurrent = null;
            area = 0;
            covered = 0;
            integratedAt = now;
            minimum = maximum = 0;
        }
        public void observe(int value, long now) {
            integrate(now);
            latest = value;
            latestAt = now;
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
            return running ? freshValue(now) : frozenCurrent;
        }
        private void integrate(long now) {
            if (!running) return;
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
            if (value != null) { minimum = Math.min(minimum, value); maximum = Math.max(maximum, value); }
        }
    }
}
