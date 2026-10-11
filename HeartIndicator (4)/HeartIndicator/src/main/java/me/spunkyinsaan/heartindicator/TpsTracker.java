package me.spunkyinsaan.heartindicator;

import java.util.Arrays;

/** Szacuje TPS serwera z odstępów między pakietami WorldTimeUpdate (co 20 ticków) - tak jak TPS HUD. */
public final class TpsTracker {
    private static final float[] RATES = new float[20];
    private static int next = 0;
    private static long lastUpdate = -1;
    private static int samples = 0;

    private TpsTracker() {}

    public static synchronized void reset() {
        Arrays.fill(RATES, 0f);
        next = 0;
        samples = 0;
        lastUpdate = -1;
    }

    /** wołane przy każdym pakiecie aktualizacji czasu świata */
    public static synchronized void onTimeUpdate() {
        long now = System.currentTimeMillis();
        if (lastUpdate != -1) {
            float elapsed = (now - lastUpdate) / 1000f;
            if (elapsed > 0.01f) {
                float rate = Math.max(0f, Math.min(20f, 20f / elapsed));
                RATES[next] = rate;
                next = (next + 1) % RATES.length;
                samples = Math.min(samples + 1, RATES.length);
            }
        }
        lastUpdate = now;
    }

    /** średni TPS albo -1, gdy brak danych */
    public static synchronized float get() {
        if (samples == 0) return -1f;
        float sum = 0f;
        for (int i = 0; i < samples; i++) sum += RATES[i];
        return Math.max(0f, Math.min(20f, sum / samples));
    }

    /** przy zawieszonym serwerze (brak pakietów > 4 s) pokaż spadek */
    public static synchronized float getLive() {
        float v = get();
        if (v < 0) return v;
        if (lastUpdate != -1) {
            float since = (System.currentTimeMillis() - lastUpdate) / 1000f;
            if (since > 4f) return Math.min(v, Math.max(0f, 20f / since));
        }
        return v;
    }
}
