package com.souravgoswami.jiffy;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;

import java.util.TimeZone;

final class TimerAlarmScheduler {
    static final String ACTION_TIMER_FINISHED = "com.souravgoswami.jiffy.action.TIMER_FINISHED_ALARM";

    private static final String PREFS = "simple_cal";
    private static final String KEY_TIMER_REMAINING = "timer_remaining";
    private static final String KEY_TIMER_STARTED_AT = "timer_started_at";
    private static final String KEY_TIMER_STARTED_WALL = "timer_started_wall";
    private static final String KEY_TIMER_STARTED_ZONE = "timer_started_zone";
    private static final String KEY_TIMER_STARTED_OFFSET = "timer_started_offset";
    private static final String KEY_TIMER_RUNNING = "timer_running";
    private static final String KEY_TIMER_ALARM_ELAPSED = "timer_alarm_elapsed";
    private static final int REQUEST_CODE = 1804;
    private static final int UNKNOWN_ZONE_OFFSET_MS = Integer.MIN_VALUE;
    private static final long EARLY_TOLERANCE_MS = 500L;

    private TimerAlarmScheduler() {
    }

    static void schedule(Context context, SharedPreferences prefs) {
        if (context == null || prefs == null) {
            return;
        }
        if (!prefs.getBoolean(KEY_TIMER_RUNNING, false)) {
            cancel(context, prefs);
            return;
        }

        long remaining = timerRemainingMs(prefs);
        if (remaining <= 0L) {
            finishIfDue(context);
            return;
        }

        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            return;
        }

        long triggerElapsed = SystemClock.elapsedRealtime() + remaining;
        prefs.edit().putLong(KEY_TIMER_ALARM_ELAPSED, triggerElapsed).apply();
        PendingIntent operation = alarmIntent(context, PendingIntent.FLAG_UPDATE_CURRENT);

        if (canScheduleExactAlarms(alarmManager)) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerElapsed, operation);
                } else {
                    alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerElapsed, operation);
                }
                return;
            } catch (SecurityException ignored) {
                // Fall back below if the platform revokes exact-alarm access at runtime.
            }
        }

        PendingIntent showIntent = PendingIntent.getActivity(
                context,
                REQUEST_CODE,
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.setAlarmClock(
                new AlarmManager.AlarmClockInfo(System.currentTimeMillis() + remaining, showIntent),
                operation
        );
    }

    static void cancel(Context context) {
        cancel(context, sharedPrefs(context));
    }

    static void cancel(Context context, SharedPreferences prefs) {
        if (context == null) {
            return;
        }
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        PendingIntent operation = alarmIntent(context, PendingIntent.FLAG_NO_CREATE);
        if (alarmManager != null && operation != null) {
            alarmManager.cancel(operation);
            operation.cancel();
        }
        if (prefs != null) {
            prefs.edit().remove(KEY_TIMER_ALARM_ELAPSED).apply();
        }
    }

    static void finishIfDue(Context context) {
        SharedPreferences prefs = sharedPrefs(context);
        if (prefs == null) {
            return;
        }
        if (!prefs.getBoolean(KEY_TIMER_RUNNING, false)) {
            cancel(context, prefs);
            return;
        }

        long scheduledElapsed = prefs.getLong(KEY_TIMER_ALARM_ELAPSED, 0L);
        long remaining = timerRemainingMs(prefs);
        if (remaining > 0L
                && scheduledElapsed > 0L
                && SystemClock.elapsedRealtime() + EARLY_TOLERANCE_MS < scheduledElapsed) {
            schedule(context, prefs);
            return;
        }
        if (remaining > 0L && scheduledElapsed <= 0L) {
            schedule(context, prefs);
            return;
        }

        prefs.edit()
                .putLong(KEY_TIMER_REMAINING, 0L)
                .putLong(KEY_TIMER_STARTED_AT, 0L)
                .putLong(KEY_TIMER_STARTED_WALL, 0L)
                .remove(KEY_TIMER_STARTED_ZONE)
                .putInt(KEY_TIMER_STARTED_OFFSET, UNKNOWN_ZONE_OFFSET_MS)
                .putBoolean(KEY_TIMER_RUNNING, false)
                .remove(KEY_TIMER_ALARM_ELAPSED)
                .commit();
        TimerAlert.show(context, prefs);
        context.stopService(new Intent(context, TimerForegroundService.class));
    }

    private static SharedPreferences sharedPrefs(Context context) {
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static PendingIntent alarmIntent(Context context, int flags) {
        Intent intent = new Intent(context, TimerAlarmReceiver.class);
        intent.setAction(ACTION_TIMER_FINISHED);
        return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                flags | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static boolean canScheduleExactAlarms(AlarmManager alarmManager) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms();
    }

    private static long timerRemainingMs(SharedPreferences prefs) {
        long remaining = Math.max(0L, prefs.getLong(KEY_TIMER_REMAINING, 0L));
        if (!prefs.getBoolean(KEY_TIMER_RUNNING, false)) {
            return remaining;
        }

        long startedAtElapsed = prefs.getLong(KEY_TIMER_STARTED_AT, 0L);
        long startedAtWall = prefs.getLong(KEY_TIMER_STARTED_WALL, 0L);
        if (startedAtWall > 0L) {
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

    private static long correctedWallElapsedMs(long startedWallMs, String startedZoneId, int startedOffsetMs) {
        long nowWall = System.currentTimeMillis();
        int startOffset = startedOffsetMs == UNKNOWN_ZONE_OFFSET_MS
                ? zoneOffsetFor(startedZoneId, startedWallMs)
                : startedOffsetMs;
        int currentOffset = TimeZone.getDefault().getOffset(nowWall);
        long localElapsed = (nowWall + (long) currentOffset) - (startedWallMs + (long) startOffset);
        long zoneShift = (long) currentOffset - startOffset;
        return Math.max(0L, localElapsed - zoneShift);
    }

    private static int zoneOffsetFor(String zoneId, long wallMs) {
        if (zoneId == null || zoneId.trim().isEmpty()) {
            return TimeZone.getDefault().getOffset(wallMs);
        }
        return TimeZone.getTimeZone(zoneId).getOffset(wallMs);
    }
}
