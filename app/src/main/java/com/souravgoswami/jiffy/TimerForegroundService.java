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

public final class TimerForegroundService extends Service {
    public static final String ACTION_START = "com.souravgoswami.jiffy.action.START_TIMER_FOREGROUND";
    public static final String ACTION_PAUSE = "com.souravgoswami.jiffy.action.PAUSE_TIMER";
    public static final String ACTION_STOP = "com.souravgoswami.jiffy.action.STOP_TIMER";

    private static final String PREFS = "simple_cal";
    private static final String KEY_ACCENT_COLOR = "accent_color";
    private static final String KEY_TIMER_DURATION = "timer_duration";
    private static final String KEY_TIMER_REMAINING = "timer_remaining";
    private static final String KEY_TIMER_STARTED_AT = "timer_started_at";
    private static final String KEY_TIMER_STARTED_WALL = "timer_started_wall";
    private static final String KEY_TIMER_STARTED_ZONE = "timer_started_zone";
    private static final String KEY_TIMER_STARTED_OFFSET = "timer_started_offset";
    private static final String KEY_TIMER_RUNNING = "timer_running";
    private static final String CHANNEL_ID = "jiffy_timer";
    private static final int NOTIFICATION_ID = 1802;
    private static final long CHECK_INTERVAL_MS = 100L;
    private static final int UNKNOWN_ZONE_OFFSET_MS = Integer.MIN_VALUE;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastNotificationSecond = -1L;
    private final Runnable notificationTicker = new Runnable() {
        @Override
        public void run() {
            if (!isTimerRunning()) {
                stopForegroundService();
                return;
            }
            long remaining = timerRemainingMs();
            if (remaining <= 0L) {
                finishTimer();
                stopForegroundService();
                return;
            }
            long remainingSecond = (remaining + 999L) / 1000L;
            if (remainingSecond != lastNotificationSecond) {
                NotificationManager manager = getSystemService(NotificationManager.class);
                lastNotificationSecond = remainingSecond;
                if (manager != null) {
                    manager.notify(NOTIFICATION_ID, buildNotification(remaining));
                }
            }
            handler.postDelayed(this, CHECK_INTERVAL_MS);
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
            stopTimer();
            stopForegroundService();
            return START_NOT_STICKY;
        }
        if (ACTION_PAUSE.equals(action)) {
            pauseTimer();
            stopForegroundService();
            return START_NOT_STICKY;
        }

        if (!isTimerRunning()) {
            stopForegroundService();
            return START_NOT_STICKY;
        }
        long remaining = timerRemainingMs();
        if (remaining <= 0L) {
            finishTimer();
            stopForegroundService();
            return START_NOT_STICKY;
        }

        startAsForeground(remaining);
        handler.removeCallbacks(notificationTicker);
        lastNotificationSecond = (remaining + 999L) / 1000L;
        handler.postDelayed(notificationTicker, CHECK_INTERVAL_MS);
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

    private void startAsForeground(long remaining) {
        Notification notification = buildNotification(remaining);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification(long remaining) {
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_tab_timer)
                .setContentTitle("Jiffy Timer")
                .setContentText("Remaining: " + formatDuration(remaining))
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(System.currentTimeMillis() + remaining)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setColor(prefs.getInt(KEY_ACCENT_COLOR, Color.rgb(106, 148, 255)));
        builder.addAction(R.drawable.ic_action_pause, "Pause", serviceAction(ACTION_PAUSE, 1));
        builder.addAction(R.drawable.ic_action_stop, "Stop", serviceAction(ACTION_STOP, 2));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        }
        return builder.build();
    }

    private PendingIntent serviceAction(String action, int requestCode) {
        Intent intent = new Intent(this, TimerForegroundService.class);
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
                "Timer",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Shows when Jiffy's timer is running.");
        channel.setShowBadge(false);
        manager.createNotificationChannel(channel);
    }

    private boolean isTimerRunning() {
        return prefs != null && prefs.getBoolean(KEY_TIMER_RUNNING, false);
    }

    private long timerRemainingMs() {
        long remaining = Math.max(0L, prefs.getLong(KEY_TIMER_REMAINING, 0L));
        if (!isTimerRunning()) {
            return remaining;
        }

        long startedAtElapsed = prefs.getLong(KEY_TIMER_STARTED_AT, 0L);
        long startedAtWall = prefs.getLong(KEY_TIMER_STARTED_WALL, 0L);
        if (startedAtWall > 0L) {
            startedAtWall = correctClockDrift(startedAtWall);
            return Math.max(0L, remaining - correctedWallElapsedMs(
                    startedAtWall,
                    prefs.getString(KEY_TIMER_STARTED_ZONE, null),
                    prefs.getInt(KEY_TIMER_STARTED_OFFSET, UNKNOWN_ZONE_OFFSET_MS)
            ));
        }

        long nowElapsed = SystemClock.elapsedRealtime();
        if (startedAtElapsed > 0L && startedAtElapsed <= nowElapsed) {
            return Math.max(0L, remaining - (nowElapsed - startedAtElapsed));
        }
        return remaining;
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
                prefs.edit().putLong(KEY_TIMER_STARTED_WALL, startedAtWall).apply();
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

    private void pauseTimer() {
        long remaining = timerRemainingMs();
        prefs.edit()
                .putLong(KEY_TIMER_REMAINING, remaining)
                .putLong(KEY_TIMER_STARTED_AT, 0L)
                .putLong(KEY_TIMER_STARTED_WALL, 0L)
                .remove(KEY_TIMER_STARTED_ZONE)
                .putInt(KEY_TIMER_STARTED_OFFSET, UNKNOWN_ZONE_OFFSET_MS)
                .putBoolean(KEY_TIMER_RUNNING, false)
                .apply();
        clearClockObservation();
    }

    private void stopTimer() {
        long duration = Math.max(0L, prefs.getLong(KEY_TIMER_DURATION, 0L));
        prefs.edit()
                .putLong(KEY_TIMER_REMAINING, duration)
                .putLong(KEY_TIMER_STARTED_AT, 0L)
                .putLong(KEY_TIMER_STARTED_WALL, 0L)
                .remove(KEY_TIMER_STARTED_ZONE)
                .putInt(KEY_TIMER_STARTED_OFFSET, UNKNOWN_ZONE_OFFSET_MS)
                .putBoolean(KEY_TIMER_RUNNING, false)
                .apply();
        clearClockObservation();
    }

    private void finishTimer() {
        prefs.edit()
                .putLong(KEY_TIMER_REMAINING, 0L)
                .putLong(KEY_TIMER_STARTED_AT, 0L)
                .putLong(KEY_TIMER_STARTED_WALL, 0L)
                .remove(KEY_TIMER_STARTED_ZONE)
                .putInt(KEY_TIMER_STARTED_OFFSET, UNKNOWN_ZONE_OFFSET_MS)
                .putBoolean(KEY_TIMER_RUNNING, false)
                .apply();
        clearClockObservation();
        TimerAlert.show(this, prefs);
    }

    private void clearClockObservation() {
        lastObservedWallMs = 0L;
        lastObservedElapsedMs = 0L;
        lastObservedZoneId = null;
        lastObservedZoneOffsetMs = UNKNOWN_ZONE_OFFSET_MS;
    }

    private String formatDuration(long remainingMs) {
        long totalSeconds = (Math.max(0L, remainingMs) + 999L) / 1000L;
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
