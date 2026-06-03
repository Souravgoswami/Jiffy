package com.souravgoswami.jiffy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;

import java.util.Locale;
import java.util.TimeZone;

public final class StopwatchForegroundService extends Service {
    public static final String ACTION_START = "com.souravgoswami.jiffy.action.START_STOPWATCH_FOREGROUND";
    public static final String ACTION_LAP = "com.souravgoswami.jiffy.action.LAP_STOPWATCH";
    public static final String ACTION_STOP = "com.souravgoswami.jiffy.action.STOP_STOPWATCH";

    private static final String PREFS = "simple_cal";
    private static final String KEY_ACCENT_COLOR = "accent_color";
    private static final String KEY_STOPWATCH_ACCUMULATED = "stopwatch_accumulated";
    private static final String KEY_STOPWATCH_STARTED_AT = "stopwatch_started_at";
    private static final String KEY_STOPWATCH_STARTED_WALL = "stopwatch_started_wall";
    private static final String KEY_STOPWATCH_STARTED_ZONE = "stopwatch_started_zone";
    private static final String KEY_STOPWATCH_STARTED_OFFSET = "stopwatch_started_offset";
    private static final String KEY_STOPWATCH_RUNNING = "stopwatch_running";
    private static final String KEY_STOPWATCH_LAPS = "stopwatch_laps";
    private static final String CHANNEL_ID = "jiffy_stopwatch";
    private static final int NOTIFICATION_ID = 1801;
    private static final int UNKNOWN_ZONE_OFFSET_MS = Integer.MIN_VALUE;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable notificationTicker = new Runnable() {
        @Override
        public void run() {
            if (!isStopwatchRunning()) {
                stopForegroundService();
                return;
            }
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.notify(NOTIFICATION_ID, buildNotification());
            }
            handler.postDelayed(this, 1000L);
        }
    };

    private SharedPreferences prefs;
    private long lastObservedWallMs;
    private long lastObservedElapsedMs;
    private String lastObservedZoneId;
    private int lastObservedZoneOffsetMs = UNKNOWN_ZONE_OFFSET_MS;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopStopwatch();
            stopForegroundService();
            return START_NOT_STICKY;
        }
        if (ACTION_LAP.equals(action)) {
            recordLap();
        }

        if (!isStopwatchRunning()) {
            stopForegroundService();
            return START_NOT_STICKY;
        }

        startAsForeground();
        handler.removeCallbacks(notificationTicker);
        handler.postDelayed(notificationTicker, 1000L);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(notificationTicker);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startAsForeground() {
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification() {
        long elapsed = stopwatchElapsedMs();
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_tab_stopwatch)
                .setContentTitle("Jiffy Stopwatch")
                .setContentText("Running: " + formatNotificationDuration(elapsed))
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setWhen(System.currentTimeMillis() - elapsed)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setColor(prefs.getInt(KEY_ACCENT_COLOR, Color.rgb(106, 148, 255)));
        builder.addAction(R.drawable.ic_notification_lap, "Lap", serviceAction(ACTION_LAP, 1));
        builder.addAction(R.drawable.ic_notification_stop, "Stop", serviceAction(ACTION_STOP, 2));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        }
        return builder.build();
    }

    private PendingIntent serviceAction(String action, int requestCode) {
        Intent intent = new Intent(this, StopwatchForegroundService.class);
        intent.setAction(action);
        return PendingIntent.getService(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private void createNotificationChannel() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Stopwatch",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Shows when Jiffy's stopwatch is running.");
        channel.setShowBadge(false);
        manager.createNotificationChannel(channel);
    }

    private boolean isStopwatchRunning() {
        return prefs != null && prefs.getBoolean(KEY_STOPWATCH_RUNNING, false);
    }

    private long stopwatchElapsedMs() {
        long accumulated = Math.max(0L, prefs.getLong(KEY_STOPWATCH_ACCUMULATED, 0L));
        if (!isStopwatchRunning()) {
            return accumulated;
        }

        long startedAtElapsed = prefs.getLong(KEY_STOPWATCH_STARTED_AT, 0L);
        long startedAtWall = prefs.getLong(KEY_STOPWATCH_STARTED_WALL, 0L);
        if (startedAtWall > 0L) {
            startedAtWall = correctClockDrift(startedAtWall);
            return accumulated + correctedWallElapsedMs(
                    startedAtWall,
                    prefs.getString(KEY_STOPWATCH_STARTED_ZONE, null),
                    prefs.getInt(KEY_STOPWATCH_STARTED_OFFSET, UNKNOWN_ZONE_OFFSET_MS)
            );
        }

        long nowElapsed = SystemClock.elapsedRealtime();
        if (startedAtElapsed > 0L && startedAtElapsed <= nowElapsed) {
            return accumulated + (nowElapsed - startedAtElapsed);
        }
        return accumulated;
    }

    private long correctClockDrift(long startedAtWall) {
        long nowWall = System.currentTimeMillis();
        long nowElapsed = SystemClock.elapsedRealtime();
        TimeZone zone = TimeZone.getDefault();
        String currentZoneId = zone.getID();
        int currentOffset = zone.getOffset(nowWall);

        boolean hasObservation = lastObservedWallMs > 0L
                && lastObservedElapsedMs > 0L
                && lastObservedElapsedMs <= nowElapsed;
        if (hasObservation
                && sameZone(currentZoneId, currentOffset, lastObservedZoneId, lastObservedZoneOffsetMs)) {
            long wallDelta = nowWall - lastObservedWallMs;
            long elapsedDelta = nowElapsed - lastObservedElapsedMs;
            long drift = wallDelta - elapsedDelta;
            if (Math.abs(drift) > 1000L) {
                startedAtWall += drift;
                prefs.edit().putLong(KEY_STOPWATCH_STARTED_WALL, startedAtWall).apply();
            }
        }

        lastObservedWallMs = nowWall;
        lastObservedElapsedMs = nowElapsed;
        lastObservedZoneId = currentZoneId;
        lastObservedZoneOffsetMs = currentOffset;
        return startedAtWall;
    }

    private boolean sameZone(String leftZoneId, int leftOffset, String rightZoneId, int rightOffset) {
        return leftZoneId != null
                && leftZoneId.equals(rightZoneId)
                && leftOffset == rightOffset;
    }

    private long correctedWallElapsedMs(long startedWallMs, String startedZoneId, int startedOffsetMs) {
        long nowWall = System.currentTimeMillis();
        int startOffset = startedOffsetMs == UNKNOWN_ZONE_OFFSET_MS
                ? zoneOffsetFor(startedZoneId, startedWallMs)
                : startedOffsetMs;
        int currentOffset = TimeZone.getDefault().getOffset(nowWall);
        // Offset-correct local clock arithmetic so timezone/DST jumps do not add or remove elapsed time.
        long localElapsed = (nowWall + (long) currentOffset) - (startedWallMs + (long) startOffset);
        long zoneShift = (long) currentOffset - startOffset;
        return Math.max(0L, localElapsed - zoneShift);
    }

    private int zoneOffsetFor(String zoneId, long wallMs) {
        if (zoneId == null || zoneId.trim().isEmpty()) {
            return TimeZone.getDefault().getOffset(wallMs);
        }
        return TimeZone.getTimeZone(zoneId).getOffset(wallMs);
    }

    private void recordLap() {
        if (!isStopwatchRunning()) {
            return;
        }
        long elapsed = stopwatchElapsedMs();
        if (elapsed <= 0L) {
            return;
        }
        String laps = prefs.getString(KEY_STOPWATCH_LAPS, "");
        String updated = laps == null || laps.isEmpty() ? String.valueOf(elapsed) : laps + "," + elapsed;
        prefs.edit().putString(KEY_STOPWATCH_LAPS, updated).apply();
    }

    private void stopStopwatch() {
        long elapsed = stopwatchElapsedMs();
        prefs.edit()
                .putLong(KEY_STOPWATCH_ACCUMULATED, elapsed)
                .putLong(KEY_STOPWATCH_STARTED_AT, 0L)
                .putLong(KEY_STOPWATCH_STARTED_WALL, 0L)
                .remove(KEY_STOPWATCH_STARTED_ZONE)
                .putInt(KEY_STOPWATCH_STARTED_OFFSET, UNKNOWN_ZONE_OFFSET_MS)
                .putBoolean(KEY_STOPWATCH_RUNNING, false)
                .apply();
        clearClockObservation();
    }

    private void clearClockObservation() {
        lastObservedWallMs = 0L;
        lastObservedElapsedMs = 0L;
        lastObservedZoneId = null;
        lastObservedZoneOffsetMs = UNKNOWN_ZONE_OFFSET_MS;
    }

    private String formatNotificationDuration(long elapsedMs) {
        long totalSeconds = Math.max(0L, elapsedMs) / 1000L;
        long seconds = totalSeconds % 60L;
        long totalMinutes = totalSeconds / 60L;
        long minutes = totalMinutes % 60L;
        long hours = totalMinutes / 60L;
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void stopForegroundService() {
        handler.removeCallbacks(notificationTicker);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        stopSelf();
    }
}
