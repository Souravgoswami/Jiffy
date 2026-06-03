package com.souravgoswami.jiffy;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.DateTimeException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public final class MainActivity extends Activity {
    private static final String PREFS = "simple_cal";
    private static final String KEY_THEME = "theme";
    private static final String KEY_TEXT_COLOR = "text_color";
    private static final String KEY_CUSTOM_TEXT_COLOR = "custom_text_color";
    private static final String KEY_ACCENT_COLOR = "accent_color";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_BOLD = "bold";
    private static final String KEY_24_HOUR = "hour_24";
    private static final String KEY_WEEK_NUMBERS = "week_numbers";
    private static final String KEY_EXCLUDE_WEEKENDS = "exclude_weekends";
    private static final String KEY_INCLUDE_START_DATE = "include_start_date";
    private static final String KEY_MONDAY_FIRST = "starts_monday";
    private static final String KEY_DEFAULT_SCREEN = "default_screen";
    private static final String KEY_DATE_FORMAT = "date_format";
    private static final String KEY_HIGHLIGHTED = "highlighted_dates";
    private static final String KEY_WORLD_ZONES = "world_zones";
    private static final String KEY_STOPWATCH_ACCUMULATED = "stopwatch_accumulated";
    private static final String KEY_STOPWATCH_STARTED_AT = "stopwatch_started_at";
    private static final String KEY_STOPWATCH_STARTED_WALL = "stopwatch_started_wall";
    private static final String KEY_STOPWATCH_STARTED_ZONE = "stopwatch_started_zone";
    private static final String KEY_STOPWATCH_STARTED_OFFSET = "stopwatch_started_offset";
    private static final String KEY_STOPWATCH_RUNNING = "stopwatch_running";
    private static final String KEY_STOPWATCH_LAPS = "stopwatch_laps";
    private static final String KEY_TIMER_DURATION = "timer_duration";
    private static final String KEY_TIMER_REMAINING = "timer_remaining";
    private static final String KEY_TIMER_STARTED_AT = "timer_started_at";
    private static final String KEY_TIMER_STARTED_WALL = "timer_started_wall";
    private static final String KEY_TIMER_STARTED_ZONE = "timer_started_zone";
    private static final String KEY_TIMER_STARTED_OFFSET = "timer_started_offset";
    private static final String KEY_TIMER_RUNNING = "timer_running";
    private static final String KEY_TIMER_SOUND = "timer_sound";
    private static final String KEY_TIMER_SOUND_URI = "timer_sound_uri";
    private static final String KEY_TIMER_FINISH_ALERTED = "timer_finish_alerted";
    private static final int UNKNOWN_ZONE_OFFSET_MS = Integer.MIN_VALUE;

    private static final int THEME_OLED = 0;
    private static final int THEME_DARK_GRAY = 1;
    private static final int THEME_LIGHT = 2;
    private static final int THEME_SYSTEM = 3;

    private static final int SCREEN_CALENDAR = 0;
    private static final int SCREEN_CLOCK = 1;
    private static final int SCREEN_CALCULATOR = 2;
    private static final int SCREEN_WORLD = 3;
    private static final int SCREEN_STOPWATCH = 4;
    private static final int SCREEN_TIMER = 5;
    private static final int SCREEN_COUNT = 6;
    private static final int REQUEST_NOTIFICATIONS = 401;
    private static final int REQUEST_TIMER_TUNE = 402;
    private static final String PERMISSION_POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";
    private static final long DEFAULT_TIMER_DURATION_MS = 5L * 60L * 1000L;

    private static final int DATE_FORMAT_DMY_ORDINAL = 0;
    private static final int DATE_FORMAT_WEEKDAY_DMY_ORDINAL = 1;
    private static final int DATE_FORMAT_MDY_ORDINAL = 2;
    private static final int DATE_FORMAT_WEEKDAY_MDY_ORDINAL = 3;
    private static final int DATE_FORMAT_NUMERIC_DMY = 4;
    private static final int DATE_FORMAT_NUMERIC_MDY = 5;
    private static final int DATE_FORMAT_ISO = 6;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable clockTicker = new Runnable() {
        @Override
        public void run() {
            updateClockText();
            if (uiTickerRunning) {
                handler.postDelayed(this, tickerDelayMs());
            }
        }
    };

    private SharedPreferences prefs;
    private LinearLayout root;
    private FrameLayout content;
    private LinearLayout bottomContainer;
    private LinearLayout calendarTab;
    private LinearLayout clockTab;
    private LinearLayout calculatorTab;
    private LinearLayout worldTab;
    private LinearLayout stopwatchTab;
    private LinearLayout timerTab;
    private TextView clockFace;
    private TextView clockDate;
    private TextView stopwatchFace;
    private LinearLayout stopwatchLapList;
    private TextView stopwatchStartButton;
    private TextView stopwatchLapButton;
    private TextView stopwatchResetButton;
    private TextView timerFace;
    private EditText timerHourInput;
    private EditText timerMinuteInput;
    private EditText timerSecondInput;
    private TextView timerStartButton;
    private TextView timerStopButton;
    private CheckBox timerSoundCheckBox;
    private TextView timerTuneButton;
    private TextView timerTuneHint;
    private TextView monthTitle;
    private TableLayout calendarTable;
    private LinearLayout worldList;
    private YearMonth visibleMonth;
    private int activeScreen;
    private boolean uiTickerRunning;
    private long lastWorldEpochSecond = -1L;
    private long stopwatchAccumulatedMs;
    private long stopwatchStartedAtMs;
    private long stopwatchStartedAtWallMs;
    private String stopwatchStartedZoneId;
    private int stopwatchStartedZoneOffsetMs = UNKNOWN_ZONE_OFFSET_MS;
    private long stopwatchLastObservedWallMs;
    private long stopwatchLastObservedElapsedMs;
    private String stopwatchLastObservedZoneId;
    private int stopwatchLastObservedZoneOffsetMs = UNKNOWN_ZONE_OFFSET_MS;
    private boolean stopwatchRunning;
    private final List<Long> stopwatchLaps = new ArrayList<>();
    private long timerDurationMs;
    private long timerRemainingMs;
    private long timerStartedAtMs;
    private long timerStartedAtWallMs;
    private String timerStartedZoneId;
    private int timerStartedZoneOffsetMs = UNKNOWN_ZONE_OFFSET_MS;
    private long timerLastObservedWallMs;
    private long timerLastObservedElapsedMs;
    private String timerLastObservedZoneId;
    private int timerLastObservedZoneOffsetMs = UNKNOWN_ZONE_OFFSET_MS;
    private boolean timerRunning;
    private boolean updatingTimerInputs;
    private Set<String> highlightedDates;
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceListener = (sharedPreferences, key) -> {
        if (isStopwatchPreference(key)) {
            restoreStopwatchState(null);
            updateStopwatchText();
            renderStopwatchLaps();
            refreshStopwatchControls();
        } else if (isTimerPreference(key)) {
            restoreTimerState(null);
            updateTimerText();
            refreshTimerInputs();
            refreshTimerControls();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        ensureDefaults();
        restoreStopwatchState(savedInstanceState);
        restoreTimerState(savedInstanceState);
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener);
        highlightedDates = new HashSet<>(prefs.getStringSet(KEY_HIGHLIGHTED, new HashSet<String>()));
        normalizeHighlightedDates();
        visibleMonth = YearMonth.now();
        activeScreen = screenFromPreference();
        buildShell();
        showActiveScreen();
        syncStopwatchForegroundService(false);
        syncTimerForegroundService(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        restoreStopwatchState(null);
        restoreTimerState(null);
        updateClockText();
        startUiTicker();
        syncStopwatchForegroundService(false);
        syncTimerForegroundService(false);
    }

    @Override
    protected void onPause() {
        persistStopwatchState();
        persistTimerState();
        stopUiTicker();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        saveStopwatchToBundle(outState);
        saveTimerToBundle(outState);
        persistStopwatchState();
        persistTimerState();
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        stopUiTicker();
        if (prefs != null) {
            prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener);
        }
        super.onDestroy();
    }

    private void ensureDefaults() {
        SharedPreferences.Editor editor = null;
        if (!prefs.contains(KEY_THEME)) {
            if (editor == null) {
                editor = prefs.edit();
            }
            editor.putInt(KEY_THEME, THEME_SYSTEM);
        }
        if (!prefs.contains(KEY_WEEK_NUMBERS)) {
            if (editor == null) {
                editor = prefs.edit();
            }
            editor.putBoolean(KEY_WEEK_NUMBERS, true);
        }
        if (!prefs.contains(KEY_TIMER_SOUND)) {
            if (editor == null) {
                editor = prefs.edit();
            }
            editor.putBoolean(KEY_TIMER_SOUND, true);
        }
        if (editor != null) {
            editor.apply();
        }
    }

    private void restoreStopwatchState(Bundle savedInstanceState) {
        stopwatchLaps.clear();
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_STOPWATCH_ACCUMULATED)) {
            stopwatchAccumulatedMs = savedInstanceState.getLong(KEY_STOPWATCH_ACCUMULATED, 0L);
            stopwatchStartedAtMs = savedInstanceState.getLong(KEY_STOPWATCH_STARTED_AT, 0L);
            stopwatchStartedAtWallMs = savedInstanceState.getLong(KEY_STOPWATCH_STARTED_WALL, 0L);
            stopwatchStartedZoneId = savedInstanceState.getString(KEY_STOPWATCH_STARTED_ZONE);
            stopwatchStartedZoneOffsetMs = savedInstanceState.getInt(KEY_STOPWATCH_STARTED_OFFSET, UNKNOWN_ZONE_OFFSET_MS);
            stopwatchRunning = savedInstanceState.getBoolean(KEY_STOPWATCH_RUNNING, false);
            long[] laps = savedInstanceState.getLongArray(KEY_STOPWATCH_LAPS);
            if (laps != null) {
                for (long lap : laps) {
                    if (lap >= 0L) {
                        stopwatchLaps.add(lap);
                    }
                }
            }
        } else {
            stopwatchAccumulatedMs = prefs.getLong(KEY_STOPWATCH_ACCUMULATED, 0L);
            stopwatchStartedAtMs = prefs.getLong(KEY_STOPWATCH_STARTED_AT, 0L);
            stopwatchStartedAtWallMs = prefs.getLong(KEY_STOPWATCH_STARTED_WALL, 0L);
            stopwatchStartedZoneId = prefs.getString(KEY_STOPWATCH_STARTED_ZONE, null);
            stopwatchStartedZoneOffsetMs = prefs.getInt(KEY_STOPWATCH_STARTED_OFFSET, UNKNOWN_ZONE_OFFSET_MS);
            stopwatchRunning = prefs.getBoolean(KEY_STOPWATCH_RUNNING, false);
            decodeStopwatchLaps(prefs.getString(KEY_STOPWATCH_LAPS, ""));
        }
        normalizeStopwatchState();
    }

    private void saveStopwatchToBundle(Bundle outState) {
        outState.putLong(KEY_STOPWATCH_ACCUMULATED, stopwatchAccumulatedMs);
        outState.putLong(KEY_STOPWATCH_STARTED_AT, stopwatchStartedAtMs);
        outState.putLong(KEY_STOPWATCH_STARTED_WALL, stopwatchStartedAtWallMs);
        outState.putString(KEY_STOPWATCH_STARTED_ZONE, stopwatchStartedZoneId);
        outState.putInt(KEY_STOPWATCH_STARTED_OFFSET, stopwatchStartedZoneOffsetMs);
        outState.putBoolean(KEY_STOPWATCH_RUNNING, stopwatchRunning);
        outState.putLongArray(KEY_STOPWATCH_LAPS, stopwatchLapsArray());
    }

    private void persistStopwatchState() {
        if (prefs == null) {
            return;
        }
        prefs.edit()
                .putLong(KEY_STOPWATCH_ACCUMULATED, stopwatchAccumulatedMs)
                .putLong(KEY_STOPWATCH_STARTED_AT, stopwatchStartedAtMs)
                .putLong(KEY_STOPWATCH_STARTED_WALL, stopwatchStartedAtWallMs)
                .putString(KEY_STOPWATCH_STARTED_ZONE, stopwatchStartedZoneId)
                .putInt(KEY_STOPWATCH_STARTED_OFFSET, stopwatchStartedZoneOffsetMs)
                .putBoolean(KEY_STOPWATCH_RUNNING, stopwatchRunning)
                .putString(KEY_STOPWATCH_LAPS, encodeStopwatchLaps())
                .apply();
    }

    private void normalizeStopwatchState() {
        stopwatchAccumulatedMs = Math.max(0L, stopwatchAccumulatedMs);
        if (!stopwatchRunning) {
            stopwatchStartedAtMs = 0L;
            stopwatchStartedAtWallMs = 0L;
            stopwatchStartedZoneId = null;
            stopwatchStartedZoneOffsetMs = UNKNOWN_ZONE_OFFSET_MS;
            clearStopwatchClockObservation();
            return;
        }

        long nowWall = System.currentTimeMillis();
        if (stopwatchStartedAtWallMs <= 0L) {
            long nowElapsed = SystemClock.elapsedRealtime();
            if (stopwatchStartedAtMs > 0L && stopwatchStartedAtMs <= nowElapsed) {
                stopwatchStartedAtWallMs = nowWall - Math.max(0L, nowElapsed - stopwatchStartedAtMs);
            } else {
                stopwatchStartedAtWallMs = nowWall;
            }
        }
        if (stopwatchStartedZoneId == null || stopwatchStartedZoneId.trim().isEmpty()) {
            stopwatchStartedZoneId = TimeZone.getDefault().getID();
        }
        if (stopwatchStartedZoneOffsetMs == UNKNOWN_ZONE_OFFSET_MS) {
            stopwatchStartedZoneOffsetMs = zoneOffsetFor(stopwatchStartedZoneId, stopwatchStartedAtWallMs);
        }
        stopwatchStartedAtMs = 0L;
        observeStopwatchClockNow();
    }

    private long correctedWallElapsedMs(long startedWallMs, String startedZoneId, int startedOffsetMs) {
        if (startedWallMs <= 0L) {
            return 0L;
        }
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

    private void anchorStopwatchToNow() {
        stopwatchStartedAtMs = 0L;
        stopwatchStartedAtWallMs = System.currentTimeMillis();
        TimeZone zone = TimeZone.getDefault();
        stopwatchStartedZoneId = zone.getID();
        stopwatchStartedZoneOffsetMs = zone.getOffset(stopwatchStartedAtWallMs);
        observeStopwatchClockNow();
    }

    private void clearStopwatchAnchor() {
        stopwatchStartedAtMs = 0L;
        stopwatchStartedAtWallMs = 0L;
        stopwatchStartedZoneId = null;
        stopwatchStartedZoneOffsetMs = UNKNOWN_ZONE_OFFSET_MS;
        clearStopwatchClockObservation();
    }

    private void observeStopwatchClockNow() {
        stopwatchLastObservedWallMs = System.currentTimeMillis();
        stopwatchLastObservedElapsedMs = SystemClock.elapsedRealtime();
        TimeZone zone = TimeZone.getDefault();
        stopwatchLastObservedZoneId = zone.getID();
        stopwatchLastObservedZoneOffsetMs = zone.getOffset(stopwatchLastObservedWallMs);
    }

    private void clearStopwatchClockObservation() {
        stopwatchLastObservedWallMs = 0L;
        stopwatchLastObservedElapsedMs = 0L;
        stopwatchLastObservedZoneId = null;
        stopwatchLastObservedZoneOffsetMs = UNKNOWN_ZONE_OFFSET_MS;
    }

    private void correctStopwatchClockDrift() {
        if (!stopwatchRunning || stopwatchStartedAtWallMs <= 0L) {
            return;
        }
        long nowWall = System.currentTimeMillis();
        long nowElapsed = SystemClock.elapsedRealtime();
        TimeZone zone = TimeZone.getDefault();
        String currentZoneId = zone.getID();
        int currentOffset = zone.getOffset(nowWall);

        boolean hasObservation = stopwatchLastObservedWallMs > 0L
                && stopwatchLastObservedElapsedMs > 0L
                && stopwatchLastObservedElapsedMs <= nowElapsed;
        if (hasObservation
                && sameZone(currentZoneId, currentOffset, stopwatchLastObservedZoneId, stopwatchLastObservedZoneOffsetMs)) {
            long wallDelta = nowWall - stopwatchLastObservedWallMs;
            long elapsedDelta = nowElapsed - stopwatchLastObservedElapsedMs;
            long drift = wallDelta - elapsedDelta;
            if (Math.abs(drift) > 1000L) {
                stopwatchStartedAtWallMs += drift;
                persistStopwatchState();
            }
        }

        stopwatchLastObservedWallMs = nowWall;
        stopwatchLastObservedElapsedMs = nowElapsed;
        stopwatchLastObservedZoneId = currentZoneId;
        stopwatchLastObservedZoneOffsetMs = currentOffset;
    }

    private boolean sameZone(String leftZoneId, int leftOffset, String rightZoneId, int rightOffset) {
        return leftZoneId != null
                && leftZoneId.equals(rightZoneId)
                && leftOffset == rightOffset;
    }

    private long[] stopwatchLapsArray() {
        long[] laps = new long[stopwatchLaps.size()];
        for (int index = 0; index < stopwatchLaps.size(); index++) {
            laps[index] = stopwatchLaps.get(index);
        }
        return laps;
    }

    private String encodeStopwatchLaps() {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < stopwatchLaps.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(stopwatchLaps.get(index));
        }
        return builder.toString();
    }

    private void decodeStopwatchLaps(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return;
        }
        String[] parts = encoded.split(",");
        for (String part : parts) {
            try {
                long lap = Long.parseLong(part);
                if (lap >= 0L) {
                    stopwatchLaps.add(lap);
                }
            } catch (NumberFormatException ignored) {
                // Ignore malformed persisted entries and keep the usable laps.
            }
        }
    }

    private boolean isStopwatchPreference(String key) {
        return KEY_STOPWATCH_ACCUMULATED.equals(key)
                || KEY_STOPWATCH_STARTED_AT.equals(key)
                || KEY_STOPWATCH_STARTED_WALL.equals(key)
                || KEY_STOPWATCH_STARTED_ZONE.equals(key)
                || KEY_STOPWATCH_STARTED_OFFSET.equals(key)
                || KEY_STOPWATCH_RUNNING.equals(key)
                || KEY_STOPWATCH_LAPS.equals(key);
    }

    private void restoreTimerState(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_TIMER_DURATION)) {
            timerDurationMs = savedInstanceState.getLong(KEY_TIMER_DURATION, DEFAULT_TIMER_DURATION_MS);
            timerRemainingMs = savedInstanceState.getLong(KEY_TIMER_REMAINING, timerDurationMs);
            timerStartedAtMs = savedInstanceState.getLong(KEY_TIMER_STARTED_AT, 0L);
            timerStartedAtWallMs = savedInstanceState.getLong(KEY_TIMER_STARTED_WALL, 0L);
            timerStartedZoneId = savedInstanceState.getString(KEY_TIMER_STARTED_ZONE);
            timerStartedZoneOffsetMs = savedInstanceState.getInt(KEY_TIMER_STARTED_OFFSET, UNKNOWN_ZONE_OFFSET_MS);
            timerRunning = savedInstanceState.getBoolean(KEY_TIMER_RUNNING, false);
        } else {
            timerDurationMs = prefs.getLong(KEY_TIMER_DURATION, DEFAULT_TIMER_DURATION_MS);
            timerRemainingMs = prefs.getLong(KEY_TIMER_REMAINING, timerDurationMs);
            timerStartedAtMs = prefs.getLong(KEY_TIMER_STARTED_AT, 0L);
            timerStartedAtWallMs = prefs.getLong(KEY_TIMER_STARTED_WALL, 0L);
            timerStartedZoneId = prefs.getString(KEY_TIMER_STARTED_ZONE, null);
            timerStartedZoneOffsetMs = prefs.getInt(KEY_TIMER_STARTED_OFFSET, UNKNOWN_ZONE_OFFSET_MS);
            timerRunning = prefs.getBoolean(KEY_TIMER_RUNNING, false);
        }
        normalizeTimerState();
    }

    private void saveTimerToBundle(Bundle outState) {
        outState.putLong(KEY_TIMER_DURATION, timerDurationMs);
        outState.putLong(KEY_TIMER_REMAINING, timerRemainingMs);
        outState.putLong(KEY_TIMER_STARTED_AT, timerStartedAtMs);
        outState.putLong(KEY_TIMER_STARTED_WALL, timerStartedAtWallMs);
        outState.putString(KEY_TIMER_STARTED_ZONE, timerStartedZoneId);
        outState.putInt(KEY_TIMER_STARTED_OFFSET, timerStartedZoneOffsetMs);
        outState.putBoolean(KEY_TIMER_RUNNING, timerRunning);
    }

    private void persistTimerState() {
        if (prefs == null) {
            return;
        }
        prefs.edit()
                .putLong(KEY_TIMER_DURATION, timerDurationMs)
                .putLong(KEY_TIMER_REMAINING, timerRemainingMs)
                .putLong(KEY_TIMER_STARTED_AT, timerStartedAtMs)
                .putLong(KEY_TIMER_STARTED_WALL, timerStartedAtWallMs)
                .putString(KEY_TIMER_STARTED_ZONE, timerStartedZoneId)
                .putInt(KEY_TIMER_STARTED_OFFSET, timerStartedZoneOffsetMs)
                .putBoolean(KEY_TIMER_RUNNING, timerRunning)
                .apply();
    }

    private void normalizeTimerState() {
        timerDurationMs = clampTimerDuration(timerDurationMs);
        timerRemainingMs = Math.max(0L, Math.min(timerRemainingMs, timerDurationMs));
        if (!timerRunning) {
            timerStartedAtMs = 0L;
            timerStartedAtWallMs = 0L;
            timerStartedZoneId = null;
            timerStartedZoneOffsetMs = UNKNOWN_ZONE_OFFSET_MS;
            clearTimerClockObservation();
            return;
        }

        long nowWall = System.currentTimeMillis();
        if (timerStartedAtWallMs <= 0L) {
            long nowElapsed = SystemClock.elapsedRealtime();
            if (timerStartedAtMs > 0L && timerStartedAtMs <= nowElapsed) {
                timerStartedAtWallMs = nowWall - Math.max(0L, nowElapsed - timerStartedAtMs);
            } else {
                timerStartedAtWallMs = nowWall;
            }
        }
        if (timerStartedZoneId == null || timerStartedZoneId.trim().isEmpty()) {
            timerStartedZoneId = TimeZone.getDefault().getID();
        }
        if (timerStartedZoneOffsetMs == UNKNOWN_ZONE_OFFSET_MS) {
            timerStartedZoneOffsetMs = zoneOffsetFor(timerStartedZoneId, timerStartedAtWallMs);
        }
        timerStartedAtMs = 0L;
        observeTimerClockNow();
        if (currentTimerRemainingMs() <= 0L) {
            timerRunning = false;
            clearTimerAnchor();
            timerRemainingMs = 0L;
        }
    }

    private void anchorTimerToNow() {
        timerStartedAtMs = 0L;
        timerStartedAtWallMs = System.currentTimeMillis();
        TimeZone zone = TimeZone.getDefault();
        timerStartedZoneId = zone.getID();
        timerStartedZoneOffsetMs = zone.getOffset(timerStartedAtWallMs);
        observeTimerClockNow();
    }

    private void clearTimerAnchor() {
        timerStartedAtMs = 0L;
        timerStartedAtWallMs = 0L;
        timerStartedZoneId = null;
        timerStartedZoneOffsetMs = UNKNOWN_ZONE_OFFSET_MS;
        clearTimerClockObservation();
    }

    private void observeTimerClockNow() {
        timerLastObservedWallMs = System.currentTimeMillis();
        timerLastObservedElapsedMs = SystemClock.elapsedRealtime();
        TimeZone zone = TimeZone.getDefault();
        timerLastObservedZoneId = zone.getID();
        timerLastObservedZoneOffsetMs = zone.getOffset(timerLastObservedWallMs);
    }

    private void clearTimerClockObservation() {
        timerLastObservedWallMs = 0L;
        timerLastObservedElapsedMs = 0L;
        timerLastObservedZoneId = null;
        timerLastObservedZoneOffsetMs = UNKNOWN_ZONE_OFFSET_MS;
    }

    private void correctTimerClockDrift() {
        if (!timerRunning || timerStartedAtWallMs <= 0L) {
            return;
        }
        long nowWall = System.currentTimeMillis();
        long nowElapsed = SystemClock.elapsedRealtime();
        TimeZone zone = TimeZone.getDefault();
        String currentZoneId = zone.getID();
        int currentOffset = zone.getOffset(nowWall);

        boolean hasObservation = timerLastObservedWallMs > 0L
                && timerLastObservedElapsedMs > 0L
                && timerLastObservedElapsedMs <= nowElapsed;
        if (hasObservation
                && sameZone(currentZoneId, currentOffset, timerLastObservedZoneId, timerLastObservedZoneOffsetMs)) {
            long wallDelta = nowWall - timerLastObservedWallMs;
            long elapsedDelta = nowElapsed - timerLastObservedElapsedMs;
            long drift = wallDelta - elapsedDelta;
            if (Math.abs(drift) > 1000L) {
                timerStartedAtWallMs += drift;
                persistTimerState();
            }
        }

        timerLastObservedWallMs = nowWall;
        timerLastObservedElapsedMs = nowElapsed;
        timerLastObservedZoneId = currentZoneId;
        timerLastObservedZoneOffsetMs = currentOffset;
    }

    private boolean isTimerPreference(String key) {
        return KEY_TIMER_DURATION.equals(key)
                || KEY_TIMER_REMAINING.equals(key)
                || KEY_TIMER_STARTED_AT.equals(key)
                || KEY_TIMER_STARTED_WALL.equals(key)
                || KEY_TIMER_STARTED_ZONE.equals(key)
                || KEY_TIMER_STARTED_OFFSET.equals(key)
                || KEY_TIMER_RUNNING.equals(key);
    }

    private long clampTimerDuration(long durationMs) {
        long max = 99L * 60L * 60L * 1000L + 59L * 60L * 1000L + 59L * 1000L;
        return Math.max(0L, Math.min(durationMs, max));
    }

    private void syncStopwatchForegroundService(boolean askNotificationPermission) {
        Intent intent = new Intent(this, StopwatchForegroundService.class);
        if (!stopwatchRunning) {
            stopService(intent);
            return;
        }

        if (askNotificationPermission) {
            requestNotificationPermission();
        }
        intent.setAction(StopwatchForegroundService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void syncTimerForegroundService(boolean askNotificationPermission) {
        Intent intent = new Intent(this, TimerForegroundService.class);
        if (!timerRunning) {
            stopService(intent);
            return;
        }

        if (askNotificationPermission) {
            requestNotificationPermission();
        }
        intent.setAction(TimerForegroundService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (checkSelfPermission(PERMISSION_POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        requestPermissions(new String[]{PERMISSION_POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
    }

    private boolean hasNotificationPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || checkSelfPermission(PERMISSION_POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS) {
            refreshTimerSoundControls();
        }
    }

    private void buildShell() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(backgroundColor());

        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        bottomContainer = new LinearLayout(this);
        bottomContainer.setOrientation(LinearLayout.VERTICAL);
        bottomContainer.setClipChildren(false);
        bottomContainer.setClipToPadding(false);
        root.addView(bottomContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        setContentView(root);
        applySystemInsets();
        styleSystemBars();
        buildBottomBar();
    }

    private void buildBottomBar() {
        bottomContainer.removeAllViews();
        bottomContainer.setBackgroundColor(surfaceColor());

        View divider = new View(this);
        divider.setBackgroundColor(strokeColor());
        bottomContainer.addView(divider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Math.max(1, dp(1))
        ));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        row.setPadding(dp(6), dp(6), dp(6), dp(8));
        bottomContainer.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        HorizontalScrollView tabScroll = new HorizontalScrollView(this);
        tabScroll.setHorizontalScrollBarEnabled(false);
        tabScroll.setClipChildren(false);
        tabScroll.setClipToPadding(false);
        tabScroll.setPadding(0, dp(1), 0, dp(1));
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER_VERTICAL);
        tabs.setClipChildren(false);
        tabs.setClipToPadding(false);
        tabScroll.addView(tabs, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        int screenWidthDp = getResources()
                .getConfiguration()
                .screenWidthDp;

        boolean compactTabs = screenWidthDp < 700;

        calendarTab = bottomTab("Calendar", SCREEN_CALENDAR, R.drawable.ic_tab_calendar, compactTabs);
        clockTab = bottomTab("Clock", SCREEN_CLOCK, R.drawable.ic_tab_clock, compactTabs);
        calculatorTab = bottomTab("Date Calc", SCREEN_CALCULATOR, R.drawable.ic_tab_calculator, compactTabs);
        worldTab = bottomTab("World Time", SCREEN_WORLD, R.drawable.ic_tab_world, compactTabs);
        stopwatchTab = bottomTab("Stopwatch", SCREEN_STOPWATCH, R.drawable.ic_tab_stopwatch, compactTabs);
        timerTab = bottomTab("Timer", SCREEN_TIMER, R.drawable.ic_tab_timer, compactTabs);

        tabs.addView(calendarTab);
        tabs.addView(clockTab);
        tabs.addView(calculatorTab);
        tabs.addView(worldTab);
        tabs.addView(stopwatchTab);
        tabs.addView(timerTab);

        View spacer = new View(this);
        row.addView(tabScroll, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(spacer, new LinearLayout.LayoutParams(dp(4), 1));

        TextView menu = actionButton("\u22EE");
        menu.setTextSize(fontSize() + 2);
        menu.setContentDescription("More options");
        setButtonTooltip(menu, "More Options");
        menu.setOnClickListener(this::showMainMenu);
        LinearLayout.LayoutParams menuParams = new LinearLayout.LayoutParams(dp(48), dp(44));
        menuParams.setMargins(0, dp(1), 0, dp(4));
        row.addView(menu, menuParams);

        updateTabStyles();
    }

    private LinearLayout bottomTab(String label, int screen, int iconRes, boolean isCompact) {
        LinearLayout tab = new LinearLayout(this);
        tab.setOrientation(LinearLayout.HORIZONTAL);
        tab.setGravity(Gravity.CENTER);
        tab.setClickable(true);
        tab.setFocusable(true);
        tab.setBackground(ripple(surfaceColor(), strokeColor(), dp(8)));
        tab.setPadding(dp(8), 0, dp(8), 0);
        setButtonTooltip(tab, label);
        attachButtonFeedback(tab);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(effectiveTextColor());
        tab.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22)));

        if (!isCompact) {
            TextView text = new TextView(this);
            text.setText(label);
            text.setTextColor(effectiveTextColor());
            text.setTextSize(Math.max(10, fontSize()));
            text.setTypeface(Typeface.DEFAULT, boldTypeface());
            text.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            textParams.setMargins(dp(3), 0, 0, 0);
            tab.addView(text, textParams);
        }

        tab.setOnClickListener(view -> {
            switchToScreen(screen, screenDirection(screen), true);
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                isCompact ? dp(44) : dp(105),
                dp(44)
        );
        params.setMargins(dp(2), dp(1), dp(2), dp(4));
        tab.setLayoutParams(params);
        return tab;
    }

    private void showActiveScreen() {
        showActiveScreen(false, 0);
    }

    private void showActiveScreen(boolean animate, int direction) {
        switch (activeScreen) {
            case SCREEN_CLOCK:
                showClock(animate, direction);
                break;
            case SCREEN_CALCULATOR:
                showCalculator(animate, direction);
                break;
            case SCREEN_WORLD:
                showWorld(animate, direction);
                break;
            case SCREEN_STOPWATCH:
                showStopwatch(animate, direction);
                break;
            case SCREEN_TIMER:
                showTimer(animate, direction);
                break;
            case SCREEN_CALENDAR:
            default:
                showCalendar(animate, direction);
                break;
        }
        updateTabStyles();
    }

    private void switchToScreen(int screen, int direction, boolean animate) {
        if (activeScreen == screen) {
            return;
        }
        activeScreen = screen;
        showActiveScreen(animate, direction);
        restartUiTicker();
    }

    private int screenDirection(int targetScreen) {
        return targetScreen >= activeScreen ? 1 : -1;
    }

    private int screenFromPreference() {
        String screen = prefs.getString(KEY_DEFAULT_SCREEN, "calendar");
        if ("clock".equals(screen)) {
            return SCREEN_CLOCK;
        }
        if ("calculator".equals(screen)) {
            return SCREEN_CALCULATOR;
        }
        if ("world".equals(screen)) {
            return SCREEN_WORLD;
        }
        if ("stopwatch".equals(screen)) {
            return SCREEN_STOPWATCH;
        }
        if ("timer".equals(screen)) {
            return SCREEN_TIMER;
        }
        return SCREEN_CALENDAR;
    }

    private String screenPreferenceValue(int screen) {
        switch (screen) {
            case SCREEN_CLOCK:
                return "clock";
            case SCREEN_CALCULATOR:
                return "calculator";
            case SCREEN_WORLD:
                return "world";
            case SCREEN_STOPWATCH:
                return "stopwatch";
            case SCREEN_TIMER:
                return "timer";
            case SCREEN_CALENDAR:
            default:
                return "calendar";
        }
    }

    private void updateTabStyles() {
        if (calendarTab == null || clockTab == null || calculatorTab == null
                || worldTab == null || stopwatchTab == null || timerTab == null) {
            return;
        }
        styleTab(calendarTab, activeScreen == SCREEN_CALENDAR);
        styleTab(clockTab, activeScreen == SCREEN_CLOCK);
        styleTab(calculatorTab, activeScreen == SCREEN_CALCULATOR);
        styleTab(worldTab, activeScreen == SCREEN_WORLD);
        styleTab(stopwatchTab, activeScreen == SCREEN_STOPWATCH);
        styleTab(timerTab, activeScreen == SCREEN_TIMER);
    }

    private void styleTab(LinearLayout tab, boolean active) {
        int background = active ? accentColor() : surfaceColor();
        int text = active ? contrastText(background) : effectiveTextColor();
        tab.setBackground(ripple(background, active ? accentColor() : strokeColor(), dp(8)));
        for (int i = 0; i < tab.getChildCount(); i++) {
            View child = tab.getChildAt(i);
            if (child instanceof ImageView) {
                ((ImageView) child).setColorFilter(text);
            } else if (child instanceof TextView) {
                TextView label = (TextView) child;
                label.setTextColor(text);
                label.setTextSize(Math.max(10, fontSize()));
                label.setTypeface(Typeface.DEFAULT, boldTypeface());
            }
        }
        tab.animate()
                .translationY(active ? -dp(1) : 0f)
                .setDuration(140L)
                .start();
    }

    private void showClock(boolean animate, int direction) {
        clockFace = null;
        clockDate = null;
        worldList = null;
        clearStopwatchUi();
        clearTimerUi();

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(dp(20), dp(20), dp(20), dp(20));
        layout.setBackgroundColor(backgroundColor());
        attachClockGestures(layout);

        layout.addView(screenTitle("Current Time", R.drawable.ic_tab_clock, false), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        clockFace = plainText("", fontSize() + 14);
        clockFace.setGravity(Gravity.CENTER);
        clockFace.setSingleLine(false);
        LinearLayout.LayoutParams faceParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        faceParams.setMargins(0, dp(18), 0, dp(8));
        layout.addView(clockFace, faceParams);

        clockDate = plainText("", fontSize());
        clockDate.setGravity(Gravity.CENTER);
        layout.addView(clockDate, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView hint = plainText("Use the bottom menu for 12/24-hour time and appearance.", fontSize() - 2);
        hint.setGravity(Gravity.CENTER);
        hint.setTextColor(mutedTextColor());
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        hintParams.setMargins(0, dp(18), 0, 0);
        layout.addView(hint, hintParams);

        setScreenContent(layout, animate, direction);
        updateClockText();
    }

    private void showCalculator(boolean animate, int direction) {
        clockFace = null;
        clockDate = null;
        worldList = null;
        clearStopwatchUi();
        clearTimerUi();

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(backgroundColor());
        screen.setPadding(dp(16), dp(16), dp(16), dp(16));
        attachScreenCycleGesture(screen, false);

        screen.addView(screenTitle("Date Calc", R.drawable.ic_tab_calculator, false), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        ScrollView scrollView = new ScrollView(this);
        attachScreenCycleGesture(scrollView, false);
        LinearLayout calculator = buildDateCalculatorView(LocalDate.now(), selectedDateOrToday(), false);
        scrollView.addView(calculator);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        );
        scrollParams.setMargins(0, dp(12), 0, 0);
        screen.addView(scrollView, scrollParams);

        setScreenContent(screen, animate, direction);
    }

    private void showWorld(boolean animate, int direction) {
        clockFace = null;
        clockDate = null;
        clearStopwatchUi();
        clearTimerUi();
        lastWorldEpochSecond = -1L;

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(backgroundColor());
        screen.setPadding(dp(14), dp(14), dp(14), dp(14));
        attachScreenCycleGesture(screen, false);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        header.addView(screenTitle("World Time", R.drawable.ic_tab_world, false), new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        TextView add = actionButton("Add");
        add.setCompoundDrawablePadding(dp(1));
        add.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_add, 0, 0, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            add.setCompoundDrawableTintList(ColorStateList.valueOf(effectiveTextColor()));
        }
        add.setOnClickListener(view -> showAddWorldZoneDialog());
        header.addView(add, new LinearLayout.LayoutParams(dp(96), dp(42)));
        screen.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        ScrollView scrollView = new ScrollView(this);
        attachScreenCycleGesture(scrollView, false);
        worldList = new LinearLayout(this);
        worldList.setOrientation(LinearLayout.VERTICAL);
        worldList.setPadding(0, dp(10), 0, dp(10));
        scrollView.addView(worldList);
        screen.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        setScreenContent(screen, animate, direction);
        renderWorldTimes();
    }

    private void clearStopwatchUi() {
        stopwatchFace = null;
        stopwatchLapList = null;
        stopwatchStartButton = null;
        stopwatchLapButton = null;
        stopwatchResetButton = null;
    }

    private void clearTimerUi() {
        timerFace = null;
        timerHourInput = null;
        timerMinuteInput = null;
        timerSecondInput = null;
        timerStartButton = null;
        timerStopButton = null;
        timerSoundCheckBox = null;
        timerTuneButton = null;
        timerTuneHint = null;
    }

    private LinearLayout screenTitle(String text, int iconRes, boolean colorfulIcon) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        if (!colorfulIcon) {
            icon.setColorFilter(effectiveTextColor());
        }
        row.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));

        TextView title = plainText(text, fontSize() + 5);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(dp(7), 0, 0, 0);
        row.addView(title, titleParams);
        return row;
    }

    private void showStopwatch(boolean animate, int direction) {
        clockFace = null;
        clockDate = null;
        worldList = null;
        clearTimerUi();

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setGravity(Gravity.CENTER_HORIZONTAL);
        screen.setBackgroundColor(backgroundColor());
        screen.setPadding(dp(16), dp(18), dp(16), dp(16));
        attachScreenCycleGesture(screen, false);

        screen.addView(screenTitle("Stopwatch", R.drawable.ic_tab_stopwatch, false), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        stopwatchFace = plainText("", fontSize() + 14);
        stopwatchFace.setGravity(Gravity.CENTER);
        stopwatchFace.setSingleLine(false);
        LinearLayout.LayoutParams faceParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        faceParams.setMargins(0, dp(18), 0, dp(14));
        screen.addView(stopwatchFace, faceParams);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);

        TextView start = actionButton("Start", R.drawable.ic_action_start);
        TextView lap = actionButton("Lap", R.drawable.ic_action_lap);
        TextView reset = actionButton("Reset", R.drawable.ic_action_reset);
        stopwatchStartButton = start;
        stopwatchLapButton = lap;
        stopwatchResetButton = reset;

        Runnable[] refreshControls = new Runnable[1];
        refreshControls[0] = this::refreshStopwatchControls;

        start.setOnClickListener(view -> {
            if (stopwatchRunning) {
                stopwatchAccumulatedMs = stopwatchElapsedMs();
                stopwatchRunning = false;
                clearStopwatchAnchor();
                persistStopwatchState();
                syncStopwatchForegroundService(false);
                updateStopwatchText();
                refreshControls[0].run();
            } else {
                anchorStopwatchToNow();
                stopwatchRunning = true;
                persistStopwatchState();
                syncStopwatchForegroundService(true);
                updateStopwatchText();
                refreshControls[0].run();
            }
        });
        lap.setOnClickListener(view -> {
            long elapsed = stopwatchElapsedMs();
            if (elapsed > 0L) {
                stopwatchLaps.add(elapsed);
                persistStopwatchState();
                renderStopwatchLaps();
                refreshControls[0].run();
            }
        });
        reset.setOnClickListener(view -> {
            if (stopwatchElapsedMs() > 0L) {
                showResetStopwatchDialog(() -> {
                    stopwatchRunning = false;
                    clearStopwatchAnchor();
                    stopwatchAccumulatedMs = 0L;
                    stopwatchLaps.clear();
                    persistStopwatchState();
                    syncStopwatchForegroundService(false);
                    updateStopwatchText();
                    renderStopwatchLaps();
                    refreshControls[0].run();
                });
            }
        });

        addStopwatchButton(buttons, start);
        addStopwatchButton(buttons, lap);
        addStopwatchButton(buttons, reset);
        screen.addView(buttons, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        ScrollView lapScroll = new ScrollView(this);
        attachScreenCycleGesture(lapScroll, false);
        stopwatchLapList = new LinearLayout(this);
        stopwatchLapList.setOrientation(LinearLayout.VERTICAL);
        stopwatchLapList.setPadding(0, dp(12), 0, dp(8));
        lapScroll.addView(stopwatchLapList);
        LinearLayout.LayoutParams lapParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        );
        lapParams.setMargins(0, dp(8), 0, 0);
        screen.addView(lapScroll, lapParams);

        setScreenContent(screen, animate, direction);
        updateStopwatchText();
        renderStopwatchLaps();
        refreshControls[0].run();
    }

    private void showResetStopwatchDialog(Runnable onReset) {
        TextView message = dialogText("Reset the stopwatch and clear all laps?", fontSize());
        message.setPadding(dp(18), dp(8), dp(18), dp(8));
        dialogBuilder("Reset Stopwatch")
                .setView(message)
                .setPositiveButton("Reset", (dialog, which) -> onReset.run())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addStopwatchButton(LinearLayout row, TextView button) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        row.addView(button, params);
    }

    private void styleStopwatchButton(TextView button, boolean enabled) {
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.42f);
    }

    private void refreshStopwatchControls() {
        if (stopwatchStartButton == null || stopwatchLapButton == null || stopwatchResetButton == null) {
            return;
        }
        boolean hasTime = stopwatchElapsedMs() > 0L;
        setActionButtonContent(
                stopwatchStartButton,
                stopwatchRunning ? "Stop" : "Start",
                stopwatchRunning ? R.drawable.ic_action_stop : R.drawable.ic_action_start
        );
        styleStopwatchButton(stopwatchStartButton, true);
        styleStopwatchButton(stopwatchLapButton, hasTime);
        styleStopwatchButton(stopwatchResetButton, hasTime);
        restartUiTicker();
    }

    private long stopwatchElapsedMs() {
        if (!stopwatchRunning) {
            return stopwatchAccumulatedMs;
        }
        correctStopwatchClockDrift();
        return stopwatchAccumulatedMs + correctedWallElapsedMs(
                stopwatchStartedAtWallMs,
                stopwatchStartedZoneId,
                stopwatchStartedZoneOffsetMs
        );
    }

    private void updateStopwatchText() {
        if (stopwatchFace != null) {
            stopwatchFace.setText(formatStopwatchDuration(stopwatchElapsedMs()));
        }
    }

    private String formatStopwatchDuration(long elapsedMs) {
        long totalCentiseconds = Math.max(0L, elapsedMs) / 10L;
        long centiseconds = totalCentiseconds % 100L;
        long totalSeconds = totalCentiseconds / 100L;
        long seconds = totalSeconds % 60L;
        long totalMinutes = totalSeconds / 60L;
        long minutes = totalMinutes % 60L;
        long hours = totalMinutes / 60L;
        return String.format(Locale.US, "%02d:%02d:%02d:%02d", hours, minutes, seconds, centiseconds);
    }

    private void renderStopwatchLaps() {
        if (stopwatchLapList == null) {
            return;
        }
        stopwatchLapList.removeAllViews();
        if (stopwatchLaps.isEmpty()) {
            TextView empty = plainText("Laps appear here.", fontSize());
            empty.setGravity(Gravity.CENTER);
            empty.setTextColor(mutedTextColor());
            stopwatchLapList.addView(empty, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            return;
        }

        for (int index = stopwatchLaps.size() - 1; index >= 0; index--) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(8), dp(12), dp(8));
            row.setBackground(rounded(surfaceColor(), strokeColor(), dp(8)));

            TextView lapName = plainText("Lap " + (index + 1), fontSize());
            lapName.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            lapName.setCompoundDrawablePadding(dp(6));
            lapName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_lap, 0, 0, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                lapName.setCompoundDrawableTintList(ColorStateList.valueOf(effectiveTextColor()));
            }
            row.addView(lapName, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView lapTime = plainText(formatStopwatchDuration(stopwatchLaps.get(index)), fontSize());
            lapTime.setGravity(Gravity.RIGHT);
            row.addView(lapTime, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, dp(8));
            stopwatchLapList.addView(row, params);
        }
    }

    private void showTimer(boolean animate, int direction) {
        clockFace = null;
        clockDate = null;
        worldList = null;
        clearStopwatchUi();
        ensureTimerSoundChannels();

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setGravity(Gravity.CENTER_HORIZONTAL);
        screen.setBackgroundColor(backgroundColor());
        screen.setPadding(dp(16), dp(18), dp(16), dp(16));
        attachScreenCycleGesture(screen, false);

        screen.addView(screenTitle("Timer", R.drawable.ic_tab_timer, false), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        timerFace = plainText("", fontSize() + 18);
        timerFace.setGravity(Gravity.CENTER);
        timerFace.setSingleLine(false);
        LinearLayout.LayoutParams faceParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        faceParams.setMargins(0, dp(18), 0, dp(14));
        screen.addView(timerFace, faceParams);

        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.HORIZONTAL);
        fields.setGravity(Gravity.CENTER);
        timerHourInput = timerInput();
        timerMinuteInput = timerInput();
        timerSecondInput = timerInput();
        addTimerInput(fields, "Hours", timerHourInput);
        addTimerInput(fields, "Minutes", timerMinuteInput);
        addTimerInput(fields, "Seconds", timerSecondInput);
        screen.addView(fields, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                syncTimerDurationFromInputs();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        };
        timerHourInput.addTextChangedListener(watcher);
        timerMinuteInput.addTextChangedListener(watcher);
        timerSecondInput.addTextChangedListener(watcher);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams buttonRowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        buttonRowParams.setMargins(0, dp(16), 0, 0);

        TextView start = actionButton("Start", R.drawable.ic_action_start);
        TextView stop = actionButton("Stop", R.drawable.ic_action_stop);
        timerStartButton = start;
        timerStopButton = stop;

        start.setOnClickListener(view -> {
            if (timerRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });
        stop.setOnClickListener(view -> stopTimer());

        addTimerButton(buttons, start);
        addTimerButton(buttons, stop);
        screen.addView(buttons, buttonRowParams);

        timerSoundCheckBox = optionCheckBox("Timer Sound", prefs.getBoolean(KEY_TIMER_SOUND, true));
        timerSoundCheckBox.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(KEY_TIMER_SOUND, checked).apply();
            refreshTimerSoundControls();
        });
        LinearLayout.LayoutParams soundParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        soundParams.setMargins(0, dp(14), 0, 0);
        screen.addView(timerSoundCheckBox, soundParams);

        timerTuneButton = actionButton(timerTuneButtonText(), R.drawable.ic_tab_timer);
        timerTuneButton.setOnClickListener(view -> showTimerTunePicker());
        LinearLayout.LayoutParams tuneParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(42)
        );
        tuneParams.setMargins(0, dp(8), 0, 0);
        screen.addView(timerTuneButton, tuneParams);

        timerTuneHint = plainText("", fontSize() - 2);
        timerTuneHint.setGravity(Gravity.CENTER);
        timerTuneHint.setTextColor(mutedTextColor());
        screen.addView(timerTuneHint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView hint = plainText("Set a duration, then start the countdown.", fontSize() - 2);
        hint.setGravity(Gravity.CENTER);
        hint.setTextColor(mutedTextColor());
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        hintParams.setMargins(0, dp(14), 0, 0);
        screen.addView(hint, hintParams);

        setScreenContent(screen, animate, direction);
        refreshTimerInputs();
        updateTimerText();
        refreshTimerControls();
        refreshTimerSoundControls();
    }

    private void ensureTimerSoundChannels() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            TimerAlert.ensureChannels(this, manager, prefs);
        }
    }

    private void showTimerTunePicker() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        Uri defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Timer Tune");
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultUri);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, timerTuneUri());
        try {
            startActivityForResult(intent, REQUEST_TIMER_TUNE);
        } catch (ActivityNotFoundException ignored) {
            Toast.makeText(this, "No sound picker found.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_TIMER_TUNE || resultCode != RESULT_OK || data == null) {
            return;
        }
        Uri picked = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        SharedPreferences.Editor editor = prefs.edit().putBoolean(KEY_TIMER_SOUND, true);
        if (picked == null || picked.equals(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))) {
            editor.remove(KEY_TIMER_SOUND_URI);
        } else {
            editor.putString(KEY_TIMER_SOUND_URI, picked.toString());
        }
        editor.apply();
        ensureTimerSoundChannels();
        if (timerTuneButton != null) {
            timerTuneButton.setText(timerTuneButtonText());
        }
        refreshTimerSoundControls();
        refreshUi();
    }

    private Uri timerTuneUri() {
        String stored = prefs.getString(KEY_TIMER_SOUND_URI, null);
        if (stored == null || stored.trim().isEmpty()) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        return Uri.parse(stored);
    }

    private String timerTuneButtonText() {
        return "Timer Tune: " + timerTuneLabel();
    }

    private String timerTuneLabel() {
        String stored = prefs.getString(KEY_TIMER_SOUND_URI, null);
        if (stored == null || stored.trim().isEmpty()) {
            return "Default Notification Tune";
        }
        try {
            Ringtone ringtone = RingtoneManager.getRingtone(this, Uri.parse(stored));
            if (ringtone != null) {
                String title = ringtone.getTitle(this);
                if (title != null && !title.trim().isEmpty()) {
                    return title;
                }
            }
        } catch (RuntimeException ignored) {
            return "Selected Tune";
        }
        return "Selected Tune";
    }

    private void refreshTimerSoundControls() {
        if (timerSoundCheckBox == null || timerTuneButton == null) {
            return;
        }
        boolean soundEnabled = prefs.getBoolean(KEY_TIMER_SOUND, true);
        boolean notificationAllowed = hasNotificationPermission();
        boolean tuneEnabled = soundEnabled && notificationAllowed;
        timerSoundCheckBox.setEnabled(notificationAllowed);
        timerSoundCheckBox.setAlpha(notificationAllowed ? 1f : 0.42f);
        timerTuneButton.setText(timerTuneButtonText());
        styleStopwatchButton(timerTuneButton, tuneEnabled);
        if (timerTuneHint != null) {
            if (!notificationAllowed) {
                timerTuneHint.setText("Notification permission is needed for timer tunes.");
            } else if (!soundEnabled) {
                timerTuneHint.setText("Enable Timer Sound to pick a tune.");
            } else {
                timerTuneHint.setText("Pick a tune or use Android's Default Notification Tune.");
            }
        }
    }

    private EditText timerInput() {
        EditText input = new EditText(this);
        input.setGravity(Gravity.CENTER);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setSingleLine(true);
        input.setSelectAllOnFocus(true);
        input.setTextColor(effectiveTextColor());
        input.setTextSize(Math.max(14, fontSize() + 2));
        input.setTypeface(Typeface.DEFAULT, boldTypeface());
        input.setBackground(ripple(surfaceColor(), strokeColor(), dp(8)));
        input.setPadding(dp(6), 0, dp(6), 0);
        return input;
    }

    private void addTimerInput(LinearLayout row, String label, EditText input) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setGravity(Gravity.CENTER);
        group.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44)
        ));

        TextView text = plainText(label, fontSize() - 2);
        text.setGravity(Gravity.CENTER);
        text.setTextColor(mutedTextColor());
        group.addView(text, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dp(4), 0, dp(4), 0);
        row.addView(group, params);
    }

    private void addTimerButton(LinearLayout row, TextView button) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        row.addView(button, params);
    }

    private void startTimer() {
        if (timerRunning) {
            return;
        }
        if (timerRemainingMs <= 0L) {
            timerRemainingMs = timerDurationMs;
        }
        if (timerRemainingMs <= 0L) {
            return;
        }
        anchorTimerToNow();
        timerRunning = true;
        prefs.edit().putBoolean(KEY_TIMER_FINISH_ALERTED, false).apply();
        persistTimerState();
        syncTimerForegroundService(true);
        updateTimerText();
        refreshTimerInputs();
        refreshTimerControls();
    }

    private void pauseTimer() {
        if (!timerRunning) {
            return;
        }
        timerRemainingMs = currentTimerRemainingMs();
        timerRunning = false;
        clearTimerAnchor();
        persistTimerState();
        syncTimerForegroundService(false);
        updateTimerText();
        refreshTimerInputs();
        refreshTimerControls();
    }

    private void stopTimer() {
        timerRunning = false;
        clearTimerAnchor();
        timerRemainingMs = timerDurationMs;
        persistTimerState();
        syncTimerForegroundService(false);
        updateTimerText();
        refreshTimerInputs();
        refreshTimerControls();
    }

    private long currentTimerRemainingMs() {
        if (!timerRunning) {
            return Math.max(0L, timerRemainingMs);
        }
        correctTimerClockDrift();
        long elapsed = correctedWallElapsedMs(
                timerStartedAtWallMs,
                timerStartedZoneId,
                timerStartedZoneOffsetMs
        );
        return Math.max(0L, timerRemainingMs - elapsed);
    }

    private void updateTimerText() {
        if (timerRunning && currentTimerRemainingMs() <= 0L) {
            finishTimer();
        }
        if (timerFace != null) {
            timerFace.setText(formatTimerDuration(currentTimerRemainingMs()));
        }
    }

    private void finishTimer() {
        timerRunning = false;
        clearTimerAnchor();
        timerRemainingMs = 0L;
        persistTimerState();
        TimerAlert.show(this, prefs);
        syncTimerForegroundService(false);
        refreshTimerInputs();
        refreshTimerControls();
    }

    private String formatTimerDuration(long remainingMs) {
        long totalCentiseconds = (Math.max(0L, remainingMs) + 9L) / 10L;
        long centiseconds = totalCentiseconds % 100L;
        long totalSeconds = totalCentiseconds / 100L;
        long seconds = totalSeconds % 60L;
        long totalMinutes = totalSeconds / 60L;
        long minutes = totalMinutes % 60L;
        long hours = totalMinutes / 60L;
        return String.format(Locale.US, "%02d:%02d:%02d:%02d", hours, minutes, seconds, centiseconds);
    }

    private void refreshTimerInputs() {
        if (timerHourInput == null || timerMinuteInput == null || timerSecondInput == null) {
            return;
        }
        updatingTimerInputs = true;
        long totalSeconds = Math.max(0L, timerDurationMs) / 1000L;
        long seconds = totalSeconds % 60L;
        long totalMinutes = totalSeconds / 60L;
        long minutes = totalMinutes % 60L;
        long hours = totalMinutes / 60L;
        timerHourInput.setText(String.format(Locale.US, "%02d", hours));
        timerMinuteInput.setText(String.format(Locale.US, "%02d", minutes));
        timerSecondInput.setText(String.format(Locale.US, "%02d", seconds));
        boolean editable = !timerRunning && timerRemainingMs == timerDurationMs;
        timerHourInput.setEnabled(editable);
        timerMinuteInput.setEnabled(editable);
        timerSecondInput.setEnabled(editable);
        float alpha = editable ? 1f : 0.55f;
        timerHourInput.setAlpha(alpha);
        timerMinuteInput.setAlpha(alpha);
        timerSecondInput.setAlpha(alpha);
        updatingTimerInputs = false;
    }

    private void refreshTimerControls() {
        if (timerStartButton == null || timerStopButton == null) {
            return;
        }
        boolean hasDuration = timerDurationMs > 0L;
        long remainingMs = currentTimerRemainingMs();
        boolean timerFinished = !timerRunning && hasDuration && remainingMs == 0L;
        boolean hasProgress = timerRunning || remainingMs != timerDurationMs;
        setActionButtonContent(
                timerStartButton,
                timerRunning ? "Pause" : "Start",
                timerRunning ? R.drawable.ic_action_pause : R.drawable.ic_action_start
        );
        setActionButtonContent(
                timerStopButton,
                timerFinished ? "Reset" : "Stop",
                timerFinished ? R.drawable.ic_action_reset : R.drawable.ic_action_stop
        );
        styleStopwatchButton(timerStartButton, timerRunning || hasDuration);
        styleStopwatchButton(timerStopButton, hasProgress);
        refreshTimerSoundControls();
        restartUiTicker();
    }

    private void syncTimerDurationFromInputs() {
        if (updatingTimerInputs || timerRunning) {
            return;
        }
        long hours = parseTimerInput(timerHourInput, 99L);
        long minutes = parseTimerInput(timerMinuteInput, 59L);
        long seconds = parseTimerInput(timerSecondInput, 59L);
        timerDurationMs = (hours * 60L * 60L + minutes * 60L + seconds) * 1000L;
        timerDurationMs = clampTimerDuration(timerDurationMs);
        timerRemainingMs = timerDurationMs;
        updateTimerText();
        refreshTimerControls();
    }

    private long parseTimerInput(EditText input, long max) {
        if (input == null) {
            return 0L;
        }
        try {
            long value = Long.parseLong(input.getText().toString());
            return Math.max(0L, Math.min(value, max));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private void renderWorldTimesIfNeeded() {
        if (activeScreen != SCREEN_WORLD || worldList == null) {
            return;
        }
        long epochSecond = Instant.now().getEpochSecond();
        if (epochSecond != lastWorldEpochSecond) {
            lastWorldEpochSecond = epochSecond;
            renderWorldTimes();
        }
    }

    private void renderWorldTimes() {
        if (worldList == null) {
            return;
        }
        worldList.removeAllViews();
        Instant now = Instant.now();
        ZoneId localZone = ZoneId.systemDefault();
        String localZoneId = localZone.getId();
        addWorldRow(localZoneId, now, localZone, true);

        List<String> zones = worldZoneIds();
        for (String zoneId : zones) {
            if (!localZoneId.equals(zoneId)) {
                addWorldRow(zoneId, now, localZone, false);
            }
        }
    }

    private void addWorldRow(String zoneId, Instant now, ZoneId localZone, boolean pinned) {
        ZoneId zone;
        try {
            zone = ZoneId.of(zoneId);
        } catch (DateTimeException ignored) {
            return;
        }

        ZonedDateTime zoned = ZonedDateTime.ofInstant(now, zone);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(10), dp(10));
        row.setBackground(rounded(surfaceColor(), strokeColor(), dp(8)));

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);

        TextView name = plainText(zoneDisplayName(zoneId), fontSize());
        name.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        textColumn.addView(name);

        TextView relation = plainText(zoneRelationText(zone, localZone, now), Math.max(10, fontSize() - 3));
        relation.setTextColor(mutedTextColor());
        textColumn.addView(relation);

        TextView dst = plainText(dstStatusText(zone, now), Math.max(10, fontSize() - 3));
        dst.setTextColor(mutedTextColor());
        textColumn.addView(dst);

        row.addView(textColumn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView time = plainText(formatWorldTime(zoned), fontSize() + 7);
        time.setGravity(Gravity.CENTER);
        int timeBackground = zone.equals(localZone) ? mix(accentColor(), surfaceColor(), 0.36f) : mix(accentColor(), surfaceColor(), 0.18f);
        time.setTextColor(contrastText(timeBackground));
        time.setBackground(rounded(timeBackground, Color.TRANSPARENT, dp(24)));
        row.addView(time, new LinearLayout.LayoutParams(dp(156), dp(48)));

        ImageButton delete = iconButton(R.drawable.ic_delete, pinned ? "Current timezone cannot be removed" : "Remove " + zoneDisplayName(zoneId));
        if (pinned) {
            delete.setEnabled(false);
            delete.setAlpha(0.36f);
            delete.setColorFilter(mutedTextColor());
        } else {
            delete.setOnClickListener(view -> {
                List<String> current = worldZoneIds();
                current.remove(zoneId);
                saveWorldZoneIds(current);
                renderWorldTimes();
            });
        }
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        deleteParams.setMargins(dp(8), 0, 0, 0);
        row.addView(delete, deleteParams);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(8));
        worldList.addView(row, params);
    }

    private void showAddWorldZoneDialog() {
        List<String> zones = new ArrayList<>(ZoneId.getAvailableZoneIds());
        Collections.sort(zones, (left, right) -> zonePickerLabel(left).compareToIgnoreCase(zonePickerLabel(right)));

        LinearLayout layout = dialogLayout();

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Search City Or Timezone");
        search.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        search.setTextColor(dialogTextColor());
        search.setHintTextColor(mutedTextColor());
        search.setTextSize(fontSize());
        search.setTypeface(Typeface.DEFAULT, boldTypeface());
        search.setPadding(dp(12), 0, dp(12), 0);
        search.setBackground(rounded(dialogButtonColor(), strokeForColor(dialogButtonColor()), dp(8)));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44)
        );
        searchParams.setMargins(0, 0, 0, dp(10));
        layout.addView(search, searchParams);

        ScrollView resultsScroll = new ScrollView(this);
        LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        resultsScroll.addView(results);
        layout.addView(resultsScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(360)
        ));

        AlertDialog dialog = dialogBuilder("Add World Time")
                .setView(layout)
                .setNegativeButton("Cancel", null)
                .create();

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                renderWorldZoneSearchResults(results, zones, text.toString(), dialog);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        dialog.setOnShowListener(shown -> {
            renderWorldZoneSearchResults(results, zones, search.getText().toString(), dialog);
            search.requestFocus();
        });
        dialog.show();
    }

    private void renderWorldZoneSearchResults(LinearLayout results, List<String> zones, String query, AlertDialog dialog) {
        results.removeAllViews();
        String normalizedQuery = query.trim().toLowerCase(Locale.US);
        int shown = 0;
        for (String zone : zones) {
            String label = zonePickerLabel(zone);
            if (!normalizedQuery.isEmpty() && !label.toLowerCase(Locale.US).contains(normalizedQuery)) {
                continue;
            }
            addWorldZoneSearchResult(results, label, zone, dialog);
            shown++;
            if (shown >= 80) {
                break;
            }
        }

        if (shown == 0) {
            TextView empty = dialogText("No matching timezones.", fontSize());
            empty.setGravity(Gravity.CENTER);
            empty.setTextColor(mutedTextColor());
            results.addView(empty, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(54)
            ));
        }
    }

    private void addWorldZoneSearchResult(LinearLayout results, String label, String zone, AlertDialog dialog) {
        TextView item = dialogText(label, Math.max(12, fontSize() - 1));
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(12), 0, dp(12), 0);
        item.setBackground(ripple(Color.TRANSPARENT, Color.TRANSPARENT, dp(6)));
        attachButtonFeedback(item);
        item.setOnClickListener(view -> {
            List<String> current = worldZoneIds();
            if (!zone.equals(ZoneId.systemDefault().getId()) && !current.contains(zone)) {
                current.add(zone);
                saveWorldZoneIds(current);
                renderWorldTimes();
            }
            dialog.dismiss();
        });
        results.addView(item, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
        ));
    }

    private List<String> worldZoneIds() {
        String saved = prefs.getString(KEY_WORLD_ZONES, null);
        List<String> zones = new ArrayList<>();
        if (saved == null) {
            return zones;
        }
        if (saved.trim().isEmpty()) {
            return zones;
        }
        String[] parts = saved.split("\\|");
        for (String part : parts) {
            if (!part.isEmpty() && !zones.contains(part)) {
                zones.add(part);
            }
        }
        return zones;
    }

    private void saveWorldZoneIds(List<String> zones) {
        StringBuilder builder = new StringBuilder();
        for (String zone : zones) {
            if (builder.length() > 0) {
                builder.append('|');
            }
            builder.append(zone);
        }
        prefs.edit().putString(KEY_WORLD_ZONES, builder.toString()).apply();
    }

    private String zonePickerLabel(String zoneId) {
        return zoneDisplayName(zoneId) + " (" + zoneId + ")";
    }

    private String zoneDisplayName(String zoneId) {
        int slash = zoneId.lastIndexOf('/');
        String name = slash >= 0 ? zoneId.substring(slash + 1) : zoneId;
        return name.replace('_', ' ');
    }

    private String zoneRelationText(ZoneId zone, ZoneId localZone, Instant now) {
        int targetOffset = zone.getRules().getOffset(now).getTotalSeconds();
        int localOffset = localZone.getRules().getOffset(now).getTotalSeconds();
        int diffMinutes = Math.round((targetOffset - localOffset) / 60f);
        if (diffMinutes == 0) {
            return "Current timezone";
        }
        String direction = diffMinutes > 0 ? "ahead" : "behind";
        return formatHourDifference(Math.abs(diffMinutes)) + " " + direction;
    }

    private String formatHourDifference(int minutes) {
        int hours = minutes / 60;
        int remainder = minutes % 60;
        if (remainder == 0) {
            return hours + (hours == 1 ? " hour" : " hours");
        }
        if (remainder == 30) {
            return String.format(Locale.US, "%.1f hours", minutes / 60.0);
        }
        if (hours == 0) {
            return remainder + " minutes";
        }
        return hours + "h " + remainder + "m";
    }

    private String dstStatusText(ZoneId zone, Instant now) {
        TimeZone timeZone = TimeZone.getTimeZone(zone);
        if (!timeZone.useDaylightTime()) {
            return "DST not applicable";
        }
        return zone.getRules().isDaylightSavings(now) ? "DST active" : "Standard time";
    }

    private String formatWorldTime(ZonedDateTime time) {
        if (prefs.getBoolean(KEY_24_HOUR, false)) {
            return String.format(Locale.US, "%02d:%02d:%02d", time.getHour(), time.getMinute(), time.getSecond());
        }
        int hour = time.getHour();
        String marker = hour < 12 ? "AM" : "PM";
        int hour12 = hour % 12;
        if (hour12 == 0) {
            hour12 = 12;
        }
        return String.format(Locale.US, "%02d:%02d:%02d %s", hour12, time.getMinute(), time.getSecond(), marker);
    }

    private void showCalendar(boolean animate, int direction) {
        clockFace = null;
        clockDate = null;
        worldList = null;
        clearStopwatchUi();
        clearTimerUi();

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(backgroundColor());
        attachScreenSwitchGesture(screen);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setPadding(dp(8), dp(8), dp(8), dp(4));
        screen.addView(nav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        ImageButton previous = iconButton(R.drawable.ic_chevron_left, "Previous month");
        previous.setContentDescription("Previous month");
        previous.setOnClickListener(view -> navigateMonth(-1));
        nav.addView(previous, new LinearLayout.LayoutParams(dp(44), dp(42)));

        monthTitle = plainText("", fontSize() + 2);
        monthTitle.setGravity(Gravity.CENTER);
        monthTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        nav.addView(monthTitle, new LinearLayout.LayoutParams(0, dp(42), 1f));

        ImageButton next = iconButton(R.drawable.ic_chevron_right, "Next month");
        next.setContentDescription("Next month");
        next.setOnClickListener(view -> navigateMonth(1));
        nav.addView(next, new LinearLayout.LayoutParams(dp(44), dp(42)));

        TextView today = actionButton("Today");
        today.setOnClickListener(view -> {
            visibleMonth = YearMonth.now();
            renderCalendar();
        });
        LinearLayout.LayoutParams todayParams = new LinearLayout.LayoutParams(dp(74), dp(42));
        todayParams.setMargins(dp(6), 0, 0, 0);
        nav.addView(today, todayParams);

        TextView jump = actionButton("Pick Date");
        jump.setOnClickListener(view -> showJumpDialog());
        LinearLayout.LayoutParams jumpParams = new LinearLayout.LayoutParams(dp(104), dp(42));
        jumpParams.setMargins(dp(6), 0, 0, 0);
        nav.addView(jump, jumpParams);

        HorizontalScrollView optionsScroll = new HorizontalScrollView(this);
        optionsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout options = new LinearLayout(this);
        options.setOrientation(LinearLayout.HORIZONTAL);
        options.setGravity(Gravity.CENTER_VERTICAL);
        options.setPadding(dp(8), dp(4), dp(8), dp(8));
        optionsScroll.addView(options);
        screen.addView(optionsScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        CheckBox weekNumbers = optionCheckBox("Week #", prefs.getBoolean(KEY_WEEK_NUMBERS, true));
        weekNumbers.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(KEY_WEEK_NUMBERS, checked).apply();
            renderCalendar();
        });
        options.addView(weekNumbers);

        CheckBox startsMonday = optionCheckBox("Starts Mon", prefs.getBoolean(KEY_MONDAY_FIRST, false));
        startsMonday.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(KEY_MONDAY_FIRST, checked).apply();
            renderCalendar();
        });
        options.addView(startsMonday);

        calendarTable = new TableLayout(this);
        calendarTable.setStretchAllColumns(false);
        calendarTable.setShrinkAllColumns(false);
        calendarTable.setPadding(dp(6), dp(6), dp(6), dp(8));
        attachCalendarMonthSwipe(calendarTable);

        LinearLayout calendarArea = new LinearLayout(this);
        calendarArea.setOrientation(LinearLayout.VERTICAL);
        calendarArea.setBackgroundColor(backgroundColor());
        calendarArea.addView(calendarTable, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        View emptySpace = new View(this);
        emptySpace.setBackgroundColor(backgroundColor());
        attachCalendarEmptyGestures(emptySpace);
        calendarArea.addView(emptySpace, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        screen.addView(calendarArea, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        setScreenContent(screen, animate, direction);

        renderCalendar();
    }

    private void setScreenContent(View newScreen, boolean animate, int direction) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        if (!animate || content.getChildCount() == 0) {
            content.removeAllViews();
            content.addView(newScreen, params);
            return;
        }

        View oldScreen = content.getChildAt(0);
        int offset = dp(54) * (direction == 0 ? 1 : direction);
        newScreen.setAlpha(0f);
        newScreen.setTranslationX(offset);
        content.addView(newScreen, params);

        oldScreen.animate()
                .alpha(0f)
                .translationX(-offset)
                .setDuration(150L)
                .withEndAction(() -> content.removeView(oldScreen))
                .start();
        newScreen.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(180L)
                .start();
    }

    private void renderCalendar() {
        if (calendarTable == null || monthTitle == null) {
            return;
        }

        monthTitle.setText(visibleMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault())
                + " " + visibleMonth.getYear());
        calendarTable.removeAllViews();

        boolean showWeekNumbers = prefs.getBoolean(KEY_WEEK_NUMBERS, true);
        boolean mondayFirst = prefs.getBoolean(KEY_MONDAY_FIRST, false);
        DayOfWeek firstDay = mondayFirst ? DayOfWeek.MONDAY : DayOfWeek.SUNDAY;
        WeekFields weekFields = WeekFields.of(firstDay, 1);

        TableRow header = new TableRow(this);
        header.setGravity(Gravity.CENTER);
        if (showWeekNumbers) {
            header.addView(headerCell("#", 0.65f));
        }
        for (int i = 0; i < 7; i++) {
            DayOfWeek day = firstDay.plus(i);
            header.addView(headerCell(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()), 1f));
        }
        calendarTable.addView(header);

        LocalDate firstOfMonth = visibleMonth.atDay(1);
        int offset = daysBetween(firstDay, firstOfMonth.getDayOfWeek());
        LocalDate cursor = firstOfMonth.minusDays(offset);

        for (int rowIndex = 0; rowIndex < 6; rowIndex++) {
            TableRow row = new TableRow(this);
            row.setGravity(Gravity.CENTER);

            if (showWeekNumbers) {
                int week = cursor.get(weekFields.weekOfWeekBasedYear());
                row.addView(weekNumberCell(String.valueOf(week)));
            }

            for (int col = 0; col < 7; col++) {
                LocalDate date = cursor.plusDays(col);
                row.addView(dayCell(
                        date,
                        date.getMonth() == visibleMonth.getMonth()
                ));
            }

            calendarTable.addView(row);
            cursor = cursor.plusWeeks(1);
        }
    }

    private int daysBetween(DayOfWeek start, DayOfWeek current) {
        int diff = current.getValue() - start.getValue();
        return diff < 0 ? diff + 7 : diff;
    }

    private View headerCell(String text, float weight) {
        TextView label = plainText(text, Math.max(11, fontSize() - 1));
        label.setGravity(Gravity.CENTER);
        label.setTextColor(mutedTextColor());
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setLayoutParams(new TableRow.LayoutParams(0, dp(34), weight));
        return label;
    }

    private View weekNumberCell(String text) {
        TextView label = plainText(text, Math.max(11, fontSize() - 2));
        label.setGravity(Gravity.CENTER);
        label.setTextColor(mutedTextColor());
        label.setLayoutParams(new TableRow.LayoutParams(0, dp(50), 0.65f));
        return label;
    }

    private View dayCell(LocalDate date, boolean inMonth) {
        FrameLayout frame = new FrameLayout(this);
        TableRow.LayoutParams params = new TableRow.LayoutParams(0, dp(50), 1f);
        params.setMargins(dp(2), dp(2), dp(2), dp(2));
        frame.setLayoutParams(params);
        frame.setClickable(true);
        frame.setLongClickable(true);

        boolean highlighted = highlightedDates.contains(date.toString());
        boolean today = LocalDate.now().equals(date);
        int fill = highlighted ? accentColor() : backgroundColor();
        int stroke = today ? accentColor() : Color.TRANSPARENT;
        frame.setBackground(rounded(fill, stroke, dp(7)));

        TextView label = plainText(String.valueOf(date.getDayOfMonth()), fontSize());
        label.setGravity(Gravity.CENTER);
        label.setTextColor(highlighted ? contrastText(accentColor()) : (inMonth ? effectiveTextColor() : mutedTextColor()));
        label.setTypeface(Typeface.DEFAULT, highlighted || today ? Typeface.BOLD : boldTypeface());
        label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        frame.addView(label, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        frame.setContentDescription(calendarDayDescription(date, inMonth, today, highlighted));
        frame.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

        frame.setOnClickListener(view -> selectDate(date));
        frame.setOnLongClickListener(view -> {
            LocalDate from = selectedDate();
            if (from != null && !from.equals(date)) {
                showDateCalculator(from, date);
            } else {
                showDateCounter(date);
            }
            return true;
        });
        attachCalendarMonthSwipe(frame);
        return frame;
    }

    private String calendarDayDescription(LocalDate date, boolean inMonth, boolean today, boolean highlighted) {
        StringBuilder description = new StringBuilder(formatDate(date));
        if (today) {
            description.append(", today");
        }
        if (highlighted) {
            description.append(", highlighted");
        }
        if (!inMonth) {
            description.append(", outside this month");
        }
        description.append(". Tap to highlight. Long press for date details.");
        return description.toString();
    }

    private void navigateMonth(int months) {
        visibleMonth = visibleMonth.plusMonths(months);
        renderCalendar();
        animateMonthTransition(months);
    }

    private void switchScreens() {
        switchToNextScreen();
    }

    private void switchToNextScreen() {
        int target = (activeScreen + 1) % SCREEN_COUNT;
        switchToScreen(target, 1, true);
    }

    private void switchToPreviousScreen() {
        int target = activeScreen == SCREEN_CALENDAR ? SCREEN_TIMER : activeScreen - 1;
        switchToScreen(target, -1, true);
    }

    private void animateMonthTransition(int direction) {
        if (calendarTable == null) {
            return;
        }
        int offset = dp(36) * (direction >= 0 ? 1 : -1);
        calendarTable.setAlpha(0.35f);
        calendarTable.setTranslationX(offset);
        calendarTable.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(170L)
                .start();
        if (monthTitle != null) {
            monthTitle.setAlpha(0.55f);
            monthTitle.setTranslationX(offset / 2f);
            monthTitle.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(170L)
                    .start();
        }
    }

    private void attachCalendarMonthSwipe(View view) {
        attachSwipeGestures(view, () -> navigateMonth(1), () -> navigateMonth(-1), null, false);
    }

    private void attachCalendarEmptyGestures(View view) {
        attachSwipeGestures(view, this::switchToNextScreen, this::switchToPreviousScreen, this::switchToNextScreen, true);
    }

    private void attachScreenSwitchGesture(View view) {
        attachSwipeGestures(view, this::switchToNextScreen, this::switchToPreviousScreen, null, false);
    }

    private void attachClockGestures(View view) {
        attachSwipeGestures(view, this::switchToNextScreen, this::switchToPreviousScreen, this::switchToNextScreen, true);
    }

    private void attachScreenCycleGesture(View view, boolean consumeTouches) {
        attachSwipeGestures(view, this::switchToNextScreen, this::switchToPreviousScreen, null, consumeTouches);
    }

    private void attachSwipeGestures(View view, Runnable swipeLeft, Runnable swipeRight, Runnable verticalSwipe, boolean consumeTouches) {
        float[] downX = new float[1];
        float[] downY = new float[1];
        view.setClickable(true);
        view.setOnTouchListener((touched, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX[0] = event.getRawX();
                    downY[0] = event.getRawY();
                    return consumeTouches;
                case MotionEvent.ACTION_UP:
                    float dx = event.getRawX() - downX[0];
                    float dy = event.getRawY() - downY[0];
                    float absX = Math.abs(dx);
                    float absY = Math.abs(dy);
                    if (absX > dp(56) && absX > absY * 1.35f) {
                        if (dx < 0 && swipeLeft != null) {
                            swipeLeft.run();
                            return true;
                        }
                        if (dx > 0 && swipeRight != null) {
                            swipeRight.run();
                            return true;
                        }
                    }
                    if (absY > dp(72) && absY > absX * 1.25f && verticalSwipe != null) {
                        verticalSwipe.run();
                        return true;
                    }
                    return consumeTouches;
                case MotionEvent.ACTION_CANCEL:
                    return consumeTouches;
                default:
                    return false;
            }
        });
    }

    private void normalizeHighlightedDates() {
        if (highlightedDates.size() <= 1) {
            return;
        }
        String latest = null;
        for (String date : highlightedDates) {
            if (latest == null || date.compareTo(latest) > 0) {
                latest = date;
            }
        }
        highlightedDates.clear();
        if (latest != null) {
            highlightedDates.add(latest);
        }
        prefs.edit().putStringSet(KEY_HIGHLIGHTED, new HashSet<>(highlightedDates)).apply();
    }

    private void selectDate(LocalDate date) {
        highlightedDates.clear();
        highlightedDates.add(date.toString());
        prefs.edit().putStringSet(KEY_HIGHLIGHTED, new HashSet<>(highlightedDates)).apply();
        renderCalendar();
    }

    private LocalDate selectedDate() {
        if (highlightedDates.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(highlightedDates.iterator().next());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private LocalDate selectedDateOrToday() {
        LocalDate date = selectedDate();
        return date == null ? LocalDate.now() : date;
    }

    private void showDateCounter(LocalDate date) {
        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(today, date);
        StringBuilder message = new StringBuilder();
        if (days > 0) {
            message.append("Today to ").append(formatDate(date)).append("\n")
                    .append(days).append(days == 1 ? " day remaining" : " days remaining");
        } else if (days < 0) {
            message.append(formatDate(date)).append("\n")
                    .append(Math.abs(days)).append(Math.abs(days) == 1 ? " day ago" : " days ago");
        } else {
            message.append(formatDate(date)).append("\nToday");
        }

        TextView messageView = dialogText(message.toString(), fontSize());
        messageView.setPadding(dp(24), dp(8), dp(24), dp(8));

        dialogBuilder("Date Counter")
                .setView(messageView)
                .setPositiveButton("Calculate", (dialog, which) -> showDateCalculator(date))
                .setNegativeButton("Close", null)
                .show();
    }

    private void showDateCalculator(LocalDate preselectedDate) {
        showDateCalculator(LocalDate.now(), preselectedDate);
    }

    private void showDateCalculator(LocalDate fromDate, LocalDate toDate) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(buildDateCalculatorView(fromDate, toDate, true));
        dialogBuilder("Calculate Dates")
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .show();
    }

    private LinearLayout buildDateCalculatorView(LocalDate fromDate, LocalDate toDate, boolean inDialog) {
        LinearLayout layout = inDialog ? dialogLayout() : screenSectionLayout();
        TextView result = calculatorText("", fontSize() + 5, inDialog);
        result.setGravity(Gravity.CENTER);
        result.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        result.setPadding(dp(8), dp(10), dp(8), dp(10));

        LocalDate[] start = new LocalDate[]{fromDate};
        LocalDate[] end = new LocalDate[]{toDate};

        CheckBox includeStart = calculatorCheckBox("Include Start Date", prefs.getBoolean(KEY_INCLUDE_START_DATE, false), inDialog);
        CheckBox excludeWeekends = calculatorCheckBox("Exclude Saturday & Sunday", prefs.getBoolean(KEY_EXCLUDE_WEEKENDS, false), inDialog);
        TextView fromLabel = datePickerLabel("", inDialog);
        TextView toLabel = datePickerLabel("", inDialog);

        layout.addView(includeStart);
        layout.addView(excludeWeekends);
        layout.addView(result);
        layout.addView(fromLabel);
        layout.addView(toLabel);

        Runnable[] update = new Runnable[1];
        update[0] = () -> {
            boolean exclude = excludeWeekends.isChecked();
            boolean include = includeStart.isChecked();
            long days = countDaysBetween(start[0], end[0], exclude, include);
            fromLabel.setText("From: " + formatDate(start[0]));
            toLabel.setText("To: " + formatDate(end[0]));
            result.setText(formatDateDistance(days, exclude));
        };

        includeStart.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(KEY_INCLUDE_START_DATE, checked).apply();
            update[0].run();
        });
        excludeWeekends.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(KEY_EXCLUDE_WEEKENDS, checked).apply();
            update[0].run();
        });
        fromLabel.setOnClickListener(view -> showDatePickerDialog("Pick From Date", start[0], date -> {
            start[0] = date;
            update[0].run();
        }));
        toLabel.setOnClickListener(view -> showDatePickerDialog("Pick To Date", end[0], date -> {
            end[0] = date;
            update[0].run();
        }));
        update[0].run();

        return layout;
    }

    private long countDaysBetween(LocalDate from, LocalDate to, boolean excludeWeekends, boolean includeStartDate) {
        int direction = to.isBefore(from) ? -1 : 1;
        long includedStart = includeStartDate && (!excludeWeekends || !isWeekend(from)) ? direction : 0;
        if (!excludeWeekends) {
            return ChronoUnit.DAYS.between(from, to) + includedStart;
        }
        long days = 0;
        LocalDate cursor = from;
        while (!cursor.equals(to)) {
            cursor = cursor.plusDays(direction);
            if (!isWeekend(cursor)) {
                days += direction;
            }
        }
        return days + includedStart;
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private String formatDateDistance(long days, boolean weekdaysOnly) {
        String unit = weekdaysOnly ? "weekday" : "day";
        long absolute = Math.abs(days);
        String units = absolute == 1 ? unit : unit + "s";
        if (days < 0) {
            return absolute + " " + units + " ago";
        }
        return absolute + " " + units + " remaining";
    }

    private LocalDate pickerDate(DatePicker picker) {
        return LocalDate.of(picker.getYear(), picker.getMonth() + 1, picker.getDayOfMonth());
    }

    private TextView calculatorText(String text, int sizeSp, boolean inDialog) {
        return inDialog ? dialogText(text, sizeSp) : plainText(text, sizeSp);
    }

    private CheckBox calculatorCheckBox(String label, boolean checked, boolean inDialog) {
        return inDialog ? dialogCheckBox(label, checked) : optionCheckBox(label, checked);
    }

    private TextView datePickerLabel(String text, boolean inDialog) {
        TextView label = calculatorText(text, Math.max(11, fontSize() - 3), inDialog);
        label.setGravity(Gravity.CENTER_VERTICAL);
        label.setClickable(true);
        label.setFocusable(true);
        label.setPadding(dp(12), 0, dp(12), 0);
        label.setSingleLine(false);
        int background = inDialog ? dialogButtonColor() : surfaceColor();
        label.setBackground(ripple(background, strokeForColor(background), dp(8)));
        attachButtonFeedback(label);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(8), 0, dp(4));
        label.setLayoutParams(params);
        label.setMinHeight(dp(38));
        return label;
    }

    private void showDatePickerDialog(String title, LocalDate initialDate, DatePicked onPicked) {
        DatePicker picker = new DatePicker(this);
        picker.updateDate(initialDate.getYear(), initialDate.getMonthValue() - 1, initialDate.getDayOfMonth());

        LinearLayout layout = dialogLayout();
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.addView(picker, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        dialogBuilder(title)
                .setView(layout)
                .setPositiveButton("Set", (dialog, which) -> {
                    onPicked.onPicked(pickerDate(picker));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showJumpDialog() {
        LinearLayout layout = dialogLayout();
        NumberPicker month = new NumberPicker(this);
        NumberPicker year = new NumberPicker(this);

        String[] monthNames = new String[12];
        for (int i = 0; i < 12; i++) {
            monthNames[i] = Month.of(i + 1).getDisplayName(TextStyle.FULL, Locale.getDefault());
        }
        month.setMinValue(1);
        month.setMaxValue(12);
        month.setDisplayedValues(monthNames);
        month.setValue(visibleMonth.getMonthValue());

        year.setMinValue(1900);
        year.setMaxValue(2200);
        year.setValue(visibleMonth.getYear());

        layout.addView(dialogText("Month", fontSize()));
        layout.addView(month);
        layout.addView(dialogText("Year", fontSize()));
        layout.addView(year);

        dialogBuilder("Pick Date")
                .setView(layout)
                .setPositiveButton("Pick", (dialog, which) -> {
                    visibleMonth = YearMonth.of(year.getValue(), month.getValue());
                    renderCalendar();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMainMenu(View anchor) {
        PopupWindow popup = new PopupWindow(this);
        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(0, dp(8), 0, dp(8));
        menu.setBackground(rounded(menuBackgroundColor(), strokeForColor(menuBackgroundColor()), dp(8)));

        addPopupMenuItem(menu, popup, "Customizability", R.drawable.ic_menu_customize, this::showCustomizationDialog);
        addPopupMenuItem(menu, popup, "About", R.drawable.ic_menu_about, this::showAboutDialog);
        addPopupMenuItem(menu, popup, "Exit", R.drawable.ic_menu_exit, this::exitApp);

        popup.setContentView(menu);
        popup.setWidth(dp(250));
        popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setFocusable(true);
        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setAnimationStyle(R.style.MenuPopupAnimation);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popup.setElevation(dp(8));
        }
        popup.showAtLocation(root, Gravity.BOTTOM | Gravity.RIGHT, dp(8), bottomContainer.getHeight() + dp(8));
    }

    private void applySystemInsets() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                view.setPadding(
                        insets.getSystemWindowInsetLeft(),
                        insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(),
                        insets.getSystemWindowInsetBottom()
                );
                return insets;
            });
            root.requestApplyInsets();
        }
    }

    private void addPopupMenuItem(LinearLayout menu, PopupWindow popup, String label, int iconRes, Runnable action) {
        TextView item = plainText(label, fontSize());
        item.setTextColor(menuTextColor());
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setCompoundDrawablePadding(dp(10));
        item.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            item.setCompoundDrawableTintList(ColorStateList.valueOf(menuTextColor()));
        }
        item.setPadding(dp(16), 0, dp(18), 0);
        item.setBackground(ripple(Color.TRANSPARENT, Color.TRANSPARENT, dp(4)));
        setButtonTooltip(item, label);
        attachButtonFeedback(item);
        item.setOnClickListener(view -> {
            handler.postDelayed(() -> {
                popup.dismiss();
                action.run();
            }, 70L);
        });
        menu.addView(item, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
        ));
    }

    private void showCustomizationDialog() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = dialogLayout();
        scrollView.addView(layout);

        TextView themeLabel = dialogText("Display Mode", fontSize());
        themeLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        layout.addView(themeLabel);

        RadioGroup themeGroup = new RadioGroup(this);
        themeGroup.setOrientation(RadioGroup.VERTICAL);
        RadioButton system = radioButton("System", THEME_SYSTEM);
        RadioButton dark = radioButton("Dark Gray Mode", THEME_DARK_GRAY);
        RadioButton light = radioButton("Light Mode", THEME_LIGHT);
        RadioButton oled = radioButton("OLED Black Mode", THEME_OLED);
        themeGroup.addView(system);
        themeGroup.addView(dark);
        themeGroup.addView(light);
        themeGroup.addView(oled);
        themeGroup.check(themeToId(prefs.getInt(KEY_THEME, THEME_SYSTEM), system, dark, light, oled));
        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int theme = THEME_SYSTEM;
            if (checkedId == dark.getId()) {
                theme = THEME_DARK_GRAY;
            } else if (checkedId == light.getId()) {
                theme = THEME_LIGHT;
            } else if (checkedId == oled.getId()) {
                theme = THEME_OLED;
            }
            prefs.edit().putInt(KEY_THEME, theme).apply();
            refreshUi();
        });
        layout.addView(themeGroup);

        addDialogButton(layout, "Text Colour", R.drawable.ic_dialog_text_colour, view -> showColorPicker(
                "Text Colour",
                effectiveTextColor(),
                color -> {
                    prefs.edit()
                            .putBoolean(KEY_CUSTOM_TEXT_COLOR, true)
                            .putInt(KEY_TEXT_COLOR, color)
                            .apply();
                    refreshUi();
                }
        ));

        addDialogButton(layout, "Accent Colour", R.drawable.ic_dialog_accent_colour, view -> showColorPicker(
                "Accent Colour",
                accentColor(),
                color -> {
                    prefs.edit().putInt(KEY_ACCENT_COLOR, color).apply();
                    refreshUi();
                }
        ));

        addDialogButton(layout, "Reset Colours To Default", R.drawable.ic_dialog_reset_colours, view -> {
            prefs.edit()
                    .putBoolean(KEY_CUSTOM_TEXT_COLOR, false)
                    .remove(KEY_TEXT_COLOR)
                    .remove(KEY_ACCENT_COLOR)
                    .apply();
            refreshUi();
        });

        addDialogButton(layout, "Startup Screen", R.drawable.ic_menu_modes, view -> showModesDialog());
        addDialogButton(layout, "Date Format: " + formatDate(LocalDate.now()), R.drawable.ic_dialog_date_format, view -> showDateFormatDialog());

        TextView sizeLabel = dialogText("Font Size: " + fontSize() + "sp", fontSize());
        layout.addView(sizeLabel);
        SeekBar fontSize = new SeekBar(this);
        fontSize.setMax(9);
        fontSize.setProgress(fontSize() - 10);
        fontSize.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress + 10;
                sizeLabel.setText("Font Size: " + value + "sp");
                prefs.edit().putInt(KEY_FONT_SIZE, value).apply();
                refreshUi();
            }
        });
        layout.addView(fontSize);

        CheckBox bold = dialogCheckBox("Bold Font", prefs.getBoolean(KEY_BOLD, false));
        bold.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(KEY_BOLD, checked).apply();
            refreshUi();
        });
        layout.addView(bold);

        CheckBox hour24 = dialogCheckBox("24-Hour Clock", prefs.getBoolean(KEY_24_HOUR, false));
        hour24.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(KEY_24_HOUR, checked).apply();
            updateClockText();
        });
        layout.addView(hour24);

        dialogBuilder("Customizability")
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .show();
    }

    private void showDateFormatDialog() {
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);
        group.setPadding(dp(18), dp(8), dp(18), dp(8));

        int current = prefs.getInt(KEY_DATE_FORMAT, DATE_FORMAT_DMY_ORDINAL);
        for (int format = 0; format <= DATE_FORMAT_ISO; format++) {
            RadioButton option = radioButton(dateFormatLabel(format), format);
            group.addView(option);
            if (format == current) {
                group.check(option.getId());
            }
        }

        dialogBuilder("Date Format")
                .setView(group)
                .setPositiveButton("Save", (dialog, which) -> {
                    int checkedId = group.getCheckedRadioButtonId();
                    for (int i = 0; i < group.getChildCount(); i++) {
                        RadioButton option = (RadioButton) group.getChildAt(i);
                        if (option.getId() == checkedId) {
                            prefs.edit().putInt(KEY_DATE_FORMAT, (Integer) option.getTag()).apply();
                            refreshUi();
                            break;
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int themeToId(int theme, RadioButton system, RadioButton dark, RadioButton light, RadioButton oled) {
        if (theme == THEME_SYSTEM) {
            return system.getId();
        }
        if (theme == THEME_DARK_GRAY) {
            return dark.getId();
        }
        if (theme == THEME_LIGHT) {
            return light.getId();
        }
        return oled.getId();
    }

    private RadioButton radioButton(String text, int tag) {
        RadioButton button = new RadioButton(this);
        button.setId(View.generateViewId());
        button.setText(text);
        button.setTag(tag);
        button.setTextColor(dialogTextColor());
        button.setTextSize(fontSize());
        button.setTypeface(Typeface.DEFAULT, boldTypeface());
        tintCompoundButton(button);
        return button;
    }

    private void addDialogButton(LinearLayout layout, String text, View.OnClickListener listener) {
        addDialogButton(layout, text, 0, listener);
    }

    private void addDialogButton(LinearLayout layout, String text, int iconRes, View.OnClickListener listener) {
        View button = iconRes == 0 ? dialogActionButton(text) : dialogActionButton(text, iconRes);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44)
        );
        params.setMargins(0, dp(8), 0, 0);
        layout.addView(button, params);
    }

    private void showColorPicker(String title, int initialColor, ColorPicked callback) {
        LinearLayout layout = dialogLayout();
        View preview = new View(this);
        preview.setBackgroundColor(initialColor);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
        );
        previewParams.setMargins(0, 0, 0, dp(10));
        layout.addView(preview, previewParams);

        int[] color = {initialColor};
        SeekBar red = colorSeek(Color.red(initialColor));
        SeekBar green = colorSeek(Color.green(initialColor));
        SeekBar blue = colorSeek(Color.blue(initialColor));

        addColorRow(layout, "Red", red);
        addColorRow(layout, "Green", green);
        addColorRow(layout, "Blue", blue);

        SeekBar.OnSeekBarChangeListener listener = new SimpleSeekListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                color[0] = Color.rgb(red.getProgress(), green.getProgress(), blue.getProgress());
                preview.setBackgroundColor(color[0]);
            }
        };
        red.setOnSeekBarChangeListener(listener);
        green.setOnSeekBarChangeListener(listener);
        blue.setOnSeekBarChangeListener(listener);

        dialogBuilder(title)
                .setView(layout)
                .setPositiveButton("Set", (dialog, which) -> callback.onPicked(color[0]))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private SeekBar colorSeek(int value) {
        SeekBar seek = new SeekBar(this);
        seek.setMax(255);
        seek.setProgress(value);
        return seek;
    }

    private void addColorRow(LinearLayout layout, String label, SeekBar seekBar) {
        TextView text = dialogText(label, fontSize());
        layout.addView(text);
        layout.addView(seekBar);
    }

    private void showModesDialog() {
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);
        group.setPadding(dp(18), dp(8), dp(18), dp(8));

        RadioButton calendar = radioButton("Open Calendar First", SCREEN_CALENDAR);
        RadioButton clock = radioButton("Open Clock First", SCREEN_CLOCK);
        RadioButton calculator = radioButton("Open Date Calc First", SCREEN_CALCULATOR);
        RadioButton world = radioButton("Open World First", SCREEN_WORLD);
        RadioButton stopwatch = radioButton("Open Stopwatch First", SCREEN_STOPWATCH);
        RadioButton timer = radioButton("Open Timer First", SCREEN_TIMER);
        group.addView(calendar);
        group.addView(clock);
        group.addView(calculator);
        group.addView(world);
        group.addView(stopwatch);
        group.addView(timer);
        int selected = screenFromPreference();
        for (int i = 0; i < group.getChildCount(); i++) {
            RadioButton option = (RadioButton) group.getChildAt(i);
            if ((Integer) option.getTag() == selected) {
                group.check(option.getId());
                break;
            }
        }

        dialogBuilder("Startup Screen")
                .setView(group)
                .setPositiveButton("Save", (dialog, which) -> {
                    int chosen = SCREEN_CALENDAR;
                    int checkedId = group.getCheckedRadioButtonId();
                    for (int i = 0; i < group.getChildCount(); i++) {
                        RadioButton option = (RadioButton) group.getChildAt(i);
                        if (option.getId() == checkedId) {
                            chosen = (Integer) option.getTag();
                            break;
                        }
                    }
                    prefs.edit().putString(KEY_DEFAULT_SCREEN, screenPreferenceValue(chosen)).apply();
                    activeScreen = chosen;
                    refreshUi();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAboutDialog() {
        String version = "0.0.1";
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = info.versionName == null ? version : info.versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        LinearLayout layout = dialogLayout();
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_launcher);
        icon.setAdjustViewBounds(true);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(112), dp(120));
        iconParams.setMargins(0, dp(6), 0, dp(8));
        layout.addView(icon, iconParams);

        TextView name = dialogText("Jiffy", fontSize() + 8);
        name.setGravity(Gravity.CENTER);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        layout.addView(name, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView author = dialogText("Sourav Goswami", fontSize());
        author.setGravity(Gravity.CENTER);
        author.setTextColor(dialogTextColor());
        author.setAlpha(0.78f);
        layout.addView(author, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView versionView = dialogText("Version " + version, Math.max(12, fontSize() - 1));
        versionView.setGravity(Gravity.CENTER);
        versionView.setTextColor(accentColor());
        versionView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams versionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        versionParams.setMargins(0, dp(10), 0, dp(14));
        layout.addView(versionView, versionParams);

        TextView about = dialogText("Jiffy is an offline calendar, date calc, clock, stopwatch, timer, and world time app. It highlights dates, counts days, times laps, runs countdowns, and shows local and world times with DST-aware time zones.", fontSize());
        about.setGravity(Gravity.CENTER);
        about.setPadding(dp(8), dp(4), dp(8), dp(4));
        layout.addView(about, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        dialogBuilder("About Jiffy")
                .setView(layout)
                .setPositiveButton("Close", null)
                .show();
    }

    private void exitApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        } else {
            finish();
        }
    }

    private void refreshUi() {
        if (root != null) {
            root.setBackgroundColor(backgroundColor());
        }
        styleSystemBars();
        buildBottomBar();
        showActiveScreen();
    }

    private void updateClockText() {
        String text = formatClock(LocalTime.now());
        if (clockFace != null) {
            clockFace.setText(text);
        }
        if (clockDate != null) {
            clockDate.setText(formatDate(LocalDate.now()));
        }
        renderWorldTimesIfNeeded();
        updateStopwatchText();
        updateTimerText();
    }

    private void startUiTicker() {
        if (uiTickerRunning) {
            return;
        }
        uiTickerRunning = true;
        handler.removeCallbacks(clockTicker);
        handler.post(clockTicker);
    }

    private void stopUiTicker() {
        uiTickerRunning = false;
        handler.removeCallbacks(clockTicker);
    }

    private void restartUiTicker() {
        if (!uiTickerRunning) {
            return;
        }
        handler.removeCallbacks(clockTicker);
        handler.post(clockTicker);
    }

    private long tickerDelayMs() {
        if (activeScreen == SCREEN_CLOCK) {
            return 10L;
        }
        if (activeScreen == SCREEN_STOPWATCH && stopwatchRunning) {
            return 10L;
        }
        if (activeScreen == SCREEN_TIMER && timerRunning) {
            return 10L;
        }
        if (activeScreen == SCREEN_WORLD) {
            return 250L;
        }
        return 1000L;
    }

    private String formatDate(LocalDate date) {
        return formatDate(date, prefs.getInt(KEY_DATE_FORMAT, DATE_FORMAT_DMY_ORDINAL));
    }

    private String dateFormatLabel(int format) {
        return formatDate(LocalDate.of(2026, 6, 3), format);
    }

    private String formatDate(LocalDate date, int format) {
        String weekday = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.US);
        String month = date.getMonth().getDisplayName(TextStyle.FULL, Locale.US);
        String ordinal = ordinal(date.getDayOfMonth());
        String dmy = ordinal + " " + month + ", " + date.getYear();
        switch (format) {
            case DATE_FORMAT_WEEKDAY_DMY_ORDINAL:
                return weekday + ", " + dmy;
            case DATE_FORMAT_MDY_ORDINAL:
                return month + " " + ordinal + ", " + date.getYear();
            case DATE_FORMAT_WEEKDAY_MDY_ORDINAL:
                return weekday + ", " + month + " " + ordinal + " " + date.getYear();
            case DATE_FORMAT_NUMERIC_DMY:
                return String.format(Locale.US, "%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
            case DATE_FORMAT_NUMERIC_MDY:
                return String.format(Locale.US, "%02d/%02d/%04d", date.getMonthValue(), date.getDayOfMonth(), date.getYear());
            case DATE_FORMAT_ISO:
                return date.toString();
            case DATE_FORMAT_DMY_ORDINAL:
            default:
                return dmy;
        }
    }

    private String ordinal(int day) {
        int mod100 = day % 100;
        if (mod100 >= 11 && mod100 <= 13) {
            return day + "th";
        }
        switch (day % 10) {
            case 1:
                return day + "st";
            case 2:
                return day + "nd";
            case 3:
                return day + "rd";
            default:
                return day + "th";
        }
    }

    private String formatClock(LocalTime time) {
        int centiseconds = time.getNano() / 10_000_000;
        if (prefs.getBoolean(KEY_24_HOUR, false)) {
            return String.format(Locale.US,
                    "%02d:%02d:%02d:%02d",
                    time.getHour(),
                    time.getMinute(),
                    time.getSecond(),
                    centiseconds);
        }
        int hour = time.getHour();
        String marker = hour < 12 ? "AM" : "PM";
        int hour12 = hour % 12;
        if (hour12 == 0) {
            hour12 = 12;
        }
        return String.format(Locale.US,
                "%02d:%02d:%02d:%02d %s",
                hour12,
                time.getMinute(),
                time.getSecond(),
                centiseconds,
                marker);
    }

    private CheckBox optionCheckBox(String label, boolean checked) {
        CheckBox box = new CheckBox(this);
        box.setText(label);
        box.setChecked(checked);
        box.setTextColor(effectiveTextColor());
        box.setTextSize(fontSize());
        box.setTypeface(Typeface.DEFAULT, boldTypeface());
        box.setPadding(dp(4), 0, dp(8), 0);
        tintCompoundButton(box);
        return box;
    }

    private void tintCompoundButton(android.widget.CompoundButton button) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int[][] states = new int[][]{
                    new int[]{android.R.attr.state_checked},
                    new int[]{}
            };
            int[] colors = new int[]{accentColor(), mutedTextColor()};
            button.setButtonTintList(new ColorStateList(states, colors));
        }
    }

    private TextView plainText(String text, int sizeSp) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(effectiveTextColor());
        view.setTextSize(Math.max(10, sizeSp));
        view.setTypeface(Typeface.DEFAULT, boldTypeface());
        view.setPadding(dp(4), dp(3), dp(4), dp(3));
        return view;
    }

    private TextView dialogText(String text, int sizeSp) {
        TextView view = plainText(text, sizeSp);
        view.setTextColor(dialogTextColor());
        return view;
    }

    private TextView dialogTitle(String title) {
        TextView view = dialogText(title, fontSize() + 6);
        view.setPadding(dp(24), dp(18), dp(24), dp(8));
        return view;
    }

    private AlertDialog.Builder dialogBuilder(String title) {
        return new AlertDialog.Builder(this).setCustomTitle(dialogTitle(title));
    }

    private TextView actionButton(String text) {
        TextView view = plainText(text, fontSize());
        view.setGravity(Gravity.CENTER);
        view.setClickable(true);
        view.setFocusable(true);
        view.setMinWidth(dp(40));
        view.setBackground(ripple(surfaceColor(), strokeColor(), dp(8)));
        view.setPadding(dp(10), 0, dp(10), 0);
        setButtonTooltip(view, text);
        attachButtonFeedback(view);
        return view;
    }

    private TextView actionButton(String text, int iconRes) {
        TextView view = actionButton(text);
        setActionButtonContent(view, text, iconRes);
        return view;
    }

    private void setActionButtonContent(TextView view, String text, int iconRes) {
        view.setText(text);
        setButtonTooltip(view, text);
        view.setCompoundDrawablePadding(dp(5));
        view.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view.setCompoundDrawableTintList(ColorStateList.valueOf(effectiveTextColor()));
        }
    }

    private ImageButton iconButton(int drawableRes, String contentDescription) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(drawableRes);
        button.setColorFilter(effectiveTextColor());
        button.setBackground(ripple(surfaceColor(), strokeColor(), dp(8)));
        button.setContentDescription(contentDescription);
        button.setPadding(dp(10), dp(10), dp(10), dp(10));
        button.setScaleType(ImageView.ScaleType.CENTER);
        button.setClickable(true);
        button.setFocusable(true);
        setButtonTooltip(button, contentDescription);
        attachButtonFeedback(button);
        return button;
    }

    private TextView dialogActionButton(String text) {
        TextView view = dialogText(text, fontSize());
        view.setGravity(Gravity.CENTER);
        view.setClickable(true);
        view.setFocusable(true);
        view.setMinWidth(dp(40));
        view.setBackground(ripple(dialogButtonColor(), strokeForColor(dialogButtonColor()), dp(8)));
        view.setPadding(dp(10), 0, dp(10), 0);
        setButtonTooltip(view, text);
        attachButtonFeedback(view);
        return view;
    }

    private LinearLayout dialogActionButton(String text, int iconRes) {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setGravity(Gravity.CENTER);
        view.setClickable(true);
        view.setFocusable(true);
        view.setMinimumWidth(dp(40));
        view.setBackground(ripple(dialogButtonColor(), strokeForColor(dialogButtonColor()), dp(8)));
        view.setPadding(dp(10), 0, dp(10), 0);
        setButtonTooltip(view, text);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(dialogTextColor());
        view.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22)));

        TextView label = dialogText(text, fontSize());
        label.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        labelParams.setMargins(dp(7), 0, 0, 0);
        view.addView(label, labelParams);

        attachButtonFeedback(view);
        return view;
    }

    private void setButtonTooltip(View view, String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        view.setContentDescription(text);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.setTooltipText(text);
        } else {
            view.setOnLongClickListener(touched -> {
                Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
                return true;
            });
        }
    }

    private void attachButtonFeedback(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setElevation(dp(1));
        }
        view.setOnTouchListener((touched, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    touched.animate().cancel();
                    touched.animate()
                            .scaleX(0.97f)
                            .scaleY(0.97f)
                            .alpha(0.86f)
                            .setDuration(60L)
                            .start();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        touched.setTranslationZ(dp(2));
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    touched.animate().cancel();
                    touched.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .setDuration(110L)
                            .start();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        touched.setTranslationZ(0f);
                    }
                    break;
                default:
                    break;
            }
            return false;
        });
    }

    private CheckBox dialogCheckBox(String label, boolean checked) {
        CheckBox box = optionCheckBox(label, checked);
        box.setTextColor(dialogTextColor());
        return box;
    }

    private LinearLayout dialogLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(18), dp(8), dp(18), dp(8));
        return layout;
    }

    private LinearLayout screenSectionLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(6), dp(6), dp(6), dp(6));
        return layout;
    }

    private int fontSize() {
        int value = prefs.getInt(KEY_FONT_SIZE, 12);
        return Math.max(10, Math.min(19, value));
    }

    private int boldTypeface() {
        return prefs.getBoolean(KEY_BOLD, false) ? Typeface.BOLD : Typeface.NORMAL;
    }

    private int accentColor() {
        return prefs.getInt(KEY_ACCENT_COLOR, Color.rgb(106, 148, 255));
    }

    private int selectedTheme() {
        return prefs.getInt(KEY_THEME, THEME_SYSTEM);
    }

    private int activeTheme() {
        int selected = selectedTheme();
        if (selected != THEME_SYSTEM) {
            return selected;
        }
        int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightMode == Configuration.UI_MODE_NIGHT_NO) {
            return THEME_LIGHT;
        }
        if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            return THEME_DARK_GRAY;
        }
        return THEME_DARK_GRAY;
    }

    private int backgroundColor() {
        switch (activeTheme()) {
            case THEME_DARK_GRAY:
                return Color.rgb(18, 26, 42);
            case THEME_LIGHT:
                return Color.rgb(250, 250, 250);
            case THEME_OLED:
            default:
                return Color.BLACK;
        }
    }

    private int surfaceColor() {
        switch (activeTheme()) {
            case THEME_DARK_GRAY:
                return Color.rgb(31, 42, 58);
            case THEME_LIGHT:
                return Color.WHITE;
            case THEME_OLED:
            default:
                return Color.rgb(8, 8, 8);
        }
    }

    private int strokeColor() {
        if (activeTheme() == THEME_DARK_GRAY) {
            return Color.rgb(42, 53, 80);
        }
        return mix(effectiveTextColor(), backgroundColor(), 0.22f);
    }

    private int mutedTextColor() {
        if (activeTheme() == THEME_DARK_GRAY) {
            return Color.rgb(168, 179, 209);
        }
        return mix(effectiveTextColor(), backgroundColor(), 0.56f);
    }

    private int effectiveTextColor() {
        if (prefs.getBoolean(KEY_CUSTOM_TEXT_COLOR, false)) {
            return prefs.getInt(KEY_TEXT_COLOR, Color.WHITE);
        }
        switch (activeTheme()) {
            case THEME_LIGHT:
                return Color.rgb(28, 28, 28);
            case THEME_DARK_GRAY:
                return Color.rgb(233, 237, 246);
            case THEME_OLED:
            default:
                return Color.WHITE;
        }
    }

    private int dialogTextColor() {
        return activeTheme() == THEME_LIGHT ? Color.WHITE : effectiveTextColor();
    }

    private int dialogButtonColor() {
        switch (activeTheme()) {
            case THEME_LIGHT:
                return Color.rgb(70, 70, 70);
            case THEME_DARK_GRAY:
                return Color.rgb(38, 52, 73);
            case THEME_OLED:
            default:
                return Color.rgb(28, 28, 28);
        }
    }

    private int menuBackgroundColor() {
        switch (activeTheme()) {
            case THEME_LIGHT:
                return Color.rgb(58, 58, 58);
            case THEME_DARK_GRAY:
                return Color.rgb(23, 31, 49);
            case THEME_OLED:
            default:
                return surfaceColor();
        }
    }

    private int menuTextColor() {
        return Color.WHITE;
    }

    private int strokeForColor(int color) {
        return mix(dialogTextColor(), color, 0.28f);
    }

    private int mix(int foreground, int background, float amount) {
        int red = Math.round(Color.red(background) + (Color.red(foreground) - Color.red(background)) * amount);
        int green = Math.round(Color.green(background) + (Color.green(foreground) - Color.green(background)) * amount);
        int blue = Math.round(Color.blue(background) + (Color.blue(foreground) - Color.blue(background)) * amount);
        return Color.rgb(red, green, blue);
    }

    private int contrastText(int color) {
        double luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0;
        return luminance > 0.68 ? Color.BLACK : Color.WHITE;
    }

    private GradientDrawable rounded(int fill, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        if (Color.alpha(stroke) != 0) {
            drawable.setStroke(Math.max(1, dp(1)), stroke);
        }
        return drawable;
    }

    private Drawable ripple(int fill, int stroke, int radius) {
        GradientDrawable content = rounded(fill, stroke, radius);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return content;
        }
        GradientDrawable mask = rounded(Color.WHITE, Color.TRANSPARENT, radius);
        int rippleBase = mix(accentColor(), effectiveTextColor(), 0.46f);
        int rippleColor = Color.argb(96, Color.red(rippleBase), Color.green(rippleBase), Color.blue(rippleBase));
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, mask);
    }

    private void styleSystemBars() {
        getWindow().setStatusBarColor(backgroundColor());
        getWindow().setNavigationBarColor(backgroundColor());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = 0;
            if (activeTheme() == THEME_LIGHT) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            }
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private interface ColorPicked {
        void onPicked(int color);
    }

    private interface DatePicked {
        void onPicked(LocalDate date);
    }

    private abstract static class SimpleSeekListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }
}
