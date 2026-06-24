package com.souravgoswami.jiffy;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

// Shared state, persistence, gestures, and UI styling for the split MainActivity implementation.
abstract class JiffyActivityBase extends Activity {

    protected static final String PREFS = "simple_cal";
    protected static final String KEY_THEME = "theme";
    protected static final String KEY_TEXT_COLOR = "text_color";
    protected static final String KEY_CUSTOM_TEXT_COLOR = "custom_text_color";
    protected static final String KEY_ACCENT_COLOR = "accent_color";
    protected static final String KEY_FONT_SIZE = "font_size";
    protected static final String KEY_BOLD = "bold";
    protected static final String KEY_24_HOUR = "hour_24";
    protected static final String KEY_WEEK_NUMBERS = "week_numbers";
    protected static final String KEY_EXCLUDE_WEEKENDS = "exclude_weekends";
    protected static final String KEY_INCLUDE_START_DATE = "include_start_date";
    protected static final String KEY_MONDAY_FIRST = "starts_monday";
    protected static final String KEY_DEFAULT_SCREEN = "default_screen";
    protected static final String KEY_DATE_FORMAT = "date_format";
    protected static final String KEY_HIGHLIGHTED = "highlighted_dates";
    protected static final String KEY_DAILY_NOTES = "daily_notes";
    protected static final String KEY_YEARLY_NOTES = "yearly_notes";
    protected static final String KEY_DAILY_NOTE_COLOR = "daily_note_color";
    protected static final String KEY_YEARLY_NOTE_COLOR = "yearly_note_color";
    protected static final String KEY_WORLD_ZONES = "world_zones";
    protected static final String KEY_STOPWATCH_ACCUMULATED = "stopwatch_accumulated";
    protected static final String KEY_STOPWATCH_STARTED_AT = "stopwatch_started_at";
    protected static final String KEY_STOPWATCH_STARTED_WALL = "stopwatch_started_wall";
    protected static final String KEY_STOPWATCH_STARTED_ZONE = "stopwatch_started_zone";
    protected static final String KEY_STOPWATCH_STARTED_OFFSET = "stopwatch_started_offset";
    protected static final String KEY_STOPWATCH_RUNNING = "stopwatch_running";
    protected static final String KEY_STOPWATCH_LAPS = "stopwatch_laps";
    protected static final String KEY_TIMER_DURATION = "timer_duration";
    protected static final String KEY_TIMER_REMAINING = "timer_remaining";
    protected static final String KEY_TIMER_STARTED_AT = "timer_started_at";
    protected static final String KEY_TIMER_STARTED_WALL = "timer_started_wall";
    protected static final String KEY_TIMER_STARTED_ZONE = "timer_started_zone";
    protected static final String KEY_TIMER_STARTED_OFFSET = "timer_started_offset";
    protected static final String KEY_TIMER_RUNNING = "timer_running";
    protected static final String KEY_TIMER_SOUND = "timer_sound";
    protected static final String KEY_TIMER_SOUND_URI = "timer_sound_uri";
    protected static final String KEY_TIMER_FINISH_ALERTED = "timer_finish_alerted";
    protected static final int UNKNOWN_ZONE_OFFSET_MS = Integer.MIN_VALUE;

    protected static final int THEME_OLED = 0;
    protected static final int THEME_DARK_GRAY = 1;
    protected static final int THEME_LIGHT = 2;
    protected static final int THEME_SYSTEM = 3;

    protected static final int SCREEN_CALENDAR = 0;
    protected static final int SCREEN_CLOCK = 1;
    protected static final int SCREEN_CALCULATOR = 2;
    protected static final int SCREEN_WORLD = 3;
    protected static final int SCREEN_STOPWATCH = 4;
    protected static final int SCREEN_TIMER = 5;
    protected static final int SCREEN_DIARY = 6;
    protected static final int SCREEN_COUNT = 7;
    protected static final int REQUEST_NOTIFICATIONS = 401;
    protected static final int REQUEST_TIMER_TUNE = 402;
    protected static final String PERMISSION_POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";
    protected static final long DEFAULT_TIMER_DURATION_MS = 5L * 60L * 1000L;
    protected static final int MIN_FONT_SIZE = 9;
    protected static final int MAX_FONT_SIZE = 18;
    protected static final int DEFAULT_DAILY_NOTE_COLOR = 0xff6c6cff;
    protected static final int DEFAULT_YEARLY_NOTE_COLOR = 0xffffcb00;
    protected static final int NOTE_TAB_DAILY = 0;
    protected static final int NOTE_TAB_YEARLY = 1;
    protected static final int NOTE_TAB_ALL = 2;
    protected static final int DIARY_VIEW_DAILY = 0;
    protected static final int DIARY_VIEW_YEARLY = 1;
    protected static final int CALCULATOR_OFFSET_DAYS = 0;
    protected static final int CALCULATOR_OFFSET_WEEKS = 1;
    protected static final int CALCULATOR_OFFSET_MONTHS = 2;

    protected static final int DATE_FORMAT_DMY_ORDINAL = 0;
    protected static final int DATE_FORMAT_WEEKDAY_DMY_ORDINAL = 1;
    protected static final int DATE_FORMAT_MDY_ORDINAL = 2;
    protected static final int DATE_FORMAT_WEEKDAY_MDY_ORDINAL = 3;
    protected static final int DATE_FORMAT_NUMERIC_DMY = 4;
    protected static final int DATE_FORMAT_NUMERIC_MDY = 5;
    protected static final int DATE_FORMAT_ISO = 6;

    protected final Handler handler = new Handler(Looper.getMainLooper());
    protected final Runnable clockTicker = new Runnable() {
        @Override
        public void run() {
            updateClockText();
            if (uiTickerRunning) {
                handler.postDelayed(this, tickerDelayMs());
            }
        }
    };

    protected SharedPreferences prefs;
    protected LinearLayout root;
    protected FrameLayout content;
    protected LinearLayout bottomContainer;
    protected LinearLayout calendarTab;
    protected LinearLayout clockTab;
    protected LinearLayout calculatorTab;
    protected LinearLayout worldTab;
    protected LinearLayout stopwatchTab;
    protected LinearLayout timerTab;
    protected LinearLayout diaryTab;
    protected TextView clockFace;
    protected TextView clockDate;
    protected TextView stopwatchFace;
    protected LinearLayout stopwatchLapList;
    protected TextView stopwatchStartButton;
    protected TextView stopwatchLapButton;
    protected TextView stopwatchResetButton;
    protected TextView timerFace;
    protected EditText timerHourInput;
    protected EditText timerMinuteInput;
    protected EditText timerSecondInput;
    protected TextView timerStartButton;
    protected TextView timerStopButton;
    protected CheckBox timerSoundCheckBox;
    protected TextView timerTuneButton;
    protected TextView timerTuneHint;
    protected TextView monthTitle;
    protected TableLayout calendarTable;
    protected LinearLayout worldList;
    protected YearMonth visibleMonth;
    protected YearMonth diaryDailyFilter;
    protected String diaryDailySearchQuery = "";
    protected LocalDate observedDate;
    protected LocalDate calculatorFromDate;
    protected LocalDate calculatorToDate;
    protected LocalDate calculatorOffsetBaseDate;
    protected int activeScreen;
    protected int activeDiaryView = DIARY_VIEW_DAILY;
    protected int calculatorOffsetAmount = 1;
    protected int calculatorOffsetUnit = CALCULATOR_OFFSET_DAYS;
    protected int calculatorOffsetDirection = 1;
    protected boolean calculatorFromFollowsToday;
    protected boolean calculatorOffsetBaseFollowsToday = true;
    protected boolean uiTickerRunning;
    protected long lastWorldEpochSecond = -1L;
    protected long stopwatchAccumulatedMs;
    protected long stopwatchStartedAtMs;
    protected long stopwatchStartedAtWallMs;
    protected String stopwatchStartedZoneId;
    protected int stopwatchStartedZoneOffsetMs = UNKNOWN_ZONE_OFFSET_MS;
    protected long stopwatchLastObservedWallMs;
    protected long stopwatchLastObservedElapsedMs;
    protected String stopwatchLastObservedZoneId;
    protected int stopwatchLastObservedZoneOffsetMs = UNKNOWN_ZONE_OFFSET_MS;
    protected boolean stopwatchRunning;
    protected final List<Long> stopwatchLaps = new ArrayList<>();
    protected long timerDurationMs;
    protected long timerRemainingMs;
    protected long timerStartedAtMs;
    protected long timerStartedAtWallMs;
    protected String timerStartedZoneId;
    protected int timerStartedZoneOffsetMs = UNKNOWN_ZONE_OFFSET_MS;
    protected long timerLastObservedWallMs;
    protected long timerLastObservedElapsedMs;
    protected String timerLastObservedZoneId;
    protected int timerLastObservedZoneOffsetMs = UNKNOWN_ZONE_OFFSET_MS;
    protected boolean timerRunning;
    protected boolean updatingTimerInputs;
    protected Set<String> highlightedDates;
    protected final Map<String, String> dailyNotes = new HashMap<>();
    protected final Map<String, String> yearlyNotes = new HashMap<>();
    protected final Set<Integer> expandedYearlyMonths = new HashSet<>();
    protected int yearlyExpandedSeedMonth = -1;
    protected final SharedPreferences.OnSharedPreferenceChangeListener preferenceListener = (sharedPreferences, key) -> {
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

    protected abstract void refreshUi();

    protected abstract void restartUiTicker();

    protected abstract long tickerDelayMs();

    protected abstract void updateClockText();

    protected abstract void updateStopwatchText();

    protected abstract void renderStopwatchLaps();

    protected abstract void refreshStopwatchControls();

    protected abstract void updateTimerText();

    protected abstract void refreshTimerInputs();

    protected abstract void refreshTimerControls();

    protected abstract void refreshTimerSoundControls();

    protected abstract long currentTimerRemainingMs();

    protected abstract void switchToScreen(int screen, int direction, boolean animate);

    protected void ensureDefaults() {
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

    protected void restoreStopwatchState(Bundle savedInstanceState) {
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

    protected void saveStopwatchToBundle(Bundle outState) {
        outState.putLong(KEY_STOPWATCH_ACCUMULATED, stopwatchAccumulatedMs);
        outState.putLong(KEY_STOPWATCH_STARTED_AT, stopwatchStartedAtMs);
        outState.putLong(KEY_STOPWATCH_STARTED_WALL, stopwatchStartedAtWallMs);
        outState.putString(KEY_STOPWATCH_STARTED_ZONE, stopwatchStartedZoneId);
        outState.putInt(KEY_STOPWATCH_STARTED_OFFSET, stopwatchStartedZoneOffsetMs);
        outState.putBoolean(KEY_STOPWATCH_RUNNING, stopwatchRunning);
        outState.putLongArray(KEY_STOPWATCH_LAPS, stopwatchLapsArray());
    }

    protected void persistStopwatchState() {
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

    protected void normalizeStopwatchState() {
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

    protected long correctedWallElapsedMs(long startedWallMs, String startedZoneId, int startedOffsetMs) {
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

    protected int zoneOffsetFor(String zoneId, long wallMs) {
        if (zoneId == null || zoneId.trim().isEmpty()) {
            return TimeZone.getDefault().getOffset(wallMs);
        }
        return TimeZone.getTimeZone(zoneId).getOffset(wallMs);
    }

    protected void anchorStopwatchToNow() {
        stopwatchStartedAtMs = 0L;
        stopwatchStartedAtWallMs = System.currentTimeMillis();
        TimeZone zone = TimeZone.getDefault();
        stopwatchStartedZoneId = zone.getID();
        stopwatchStartedZoneOffsetMs = zone.getOffset(stopwatchStartedAtWallMs);
        observeStopwatchClockNow();
    }

    protected void clearStopwatchAnchor() {
        stopwatchStartedAtMs = 0L;
        stopwatchStartedAtWallMs = 0L;
        stopwatchStartedZoneId = null;
        stopwatchStartedZoneOffsetMs = UNKNOWN_ZONE_OFFSET_MS;
        clearStopwatchClockObservation();
    }

    protected void observeStopwatchClockNow() {
        stopwatchLastObservedWallMs = System.currentTimeMillis();
        stopwatchLastObservedElapsedMs = SystemClock.elapsedRealtime();
        TimeZone zone = TimeZone.getDefault();
        stopwatchLastObservedZoneId = zone.getID();
        stopwatchLastObservedZoneOffsetMs = zone.getOffset(stopwatchLastObservedWallMs);
    }

    protected void clearStopwatchClockObservation() {
        stopwatchLastObservedWallMs = 0L;
        stopwatchLastObservedElapsedMs = 0L;
        stopwatchLastObservedZoneId = null;
        stopwatchLastObservedZoneOffsetMs = UNKNOWN_ZONE_OFFSET_MS;
    }

    protected void correctStopwatchClockDrift() {
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

    protected boolean sameZone(String leftZoneId, int leftOffset, String rightZoneId, int rightOffset) {
        return leftZoneId != null
                && leftZoneId.equals(rightZoneId)
                && leftOffset == rightOffset;
    }

    protected long[] stopwatchLapsArray() {
        long[] laps = new long[stopwatchLaps.size()];
        for (int index = 0; index < stopwatchLaps.size(); index++) {
            laps[index] = stopwatchLaps.get(index);
        }
        return laps;
    }

    protected String encodeStopwatchLaps() {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < stopwatchLaps.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(stopwatchLaps.get(index));
        }
        return builder.toString();
    }

    protected void decodeStopwatchLaps(String encoded) {
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

    protected boolean isStopwatchPreference(String key) {
        return KEY_STOPWATCH_ACCUMULATED.equals(key)
                || KEY_STOPWATCH_STARTED_AT.equals(key)
                || KEY_STOPWATCH_STARTED_WALL.equals(key)
                || KEY_STOPWATCH_STARTED_ZONE.equals(key)
                || KEY_STOPWATCH_STARTED_OFFSET.equals(key)
                || KEY_STOPWATCH_RUNNING.equals(key)
                || KEY_STOPWATCH_LAPS.equals(key);
    }

    protected void restoreTimerState(Bundle savedInstanceState) {
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

    protected void saveTimerToBundle(Bundle outState) {
        outState.putLong(KEY_TIMER_DURATION, timerDurationMs);
        outState.putLong(KEY_TIMER_REMAINING, timerRemainingMs);
        outState.putLong(KEY_TIMER_STARTED_AT, timerStartedAtMs);
        outState.putLong(KEY_TIMER_STARTED_WALL, timerStartedAtWallMs);
        outState.putString(KEY_TIMER_STARTED_ZONE, timerStartedZoneId);
        outState.putInt(KEY_TIMER_STARTED_OFFSET, timerStartedZoneOffsetMs);
        outState.putBoolean(KEY_TIMER_RUNNING, timerRunning);
    }

    protected void persistTimerState() {
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

    protected void normalizeTimerState() {
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

    protected void anchorTimerToNow() {
        timerStartedAtMs = 0L;
        timerStartedAtWallMs = System.currentTimeMillis();
        TimeZone zone = TimeZone.getDefault();
        timerStartedZoneId = zone.getID();
        timerStartedZoneOffsetMs = zone.getOffset(timerStartedAtWallMs);
        observeTimerClockNow();
    }

    protected void clearTimerAnchor() {
        timerStartedAtMs = 0L;
        timerStartedAtWallMs = 0L;
        timerStartedZoneId = null;
        timerStartedZoneOffsetMs = UNKNOWN_ZONE_OFFSET_MS;
        clearTimerClockObservation();
    }

    protected void observeTimerClockNow() {
        timerLastObservedWallMs = System.currentTimeMillis();
        timerLastObservedElapsedMs = SystemClock.elapsedRealtime();
        TimeZone zone = TimeZone.getDefault();
        timerLastObservedZoneId = zone.getID();
        timerLastObservedZoneOffsetMs = zone.getOffset(timerLastObservedWallMs);
    }

    protected void clearTimerClockObservation() {
        timerLastObservedWallMs = 0L;
        timerLastObservedElapsedMs = 0L;
        timerLastObservedZoneId = null;
        timerLastObservedZoneOffsetMs = UNKNOWN_ZONE_OFFSET_MS;
    }

    protected void correctTimerClockDrift() {
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

    protected boolean isTimerPreference(String key) {
        return KEY_TIMER_DURATION.equals(key)
                || KEY_TIMER_REMAINING.equals(key)
                || KEY_TIMER_STARTED_AT.equals(key)
                || KEY_TIMER_STARTED_WALL.equals(key)
                || KEY_TIMER_STARTED_ZONE.equals(key)
                || KEY_TIMER_STARTED_OFFSET.equals(key)
                || KEY_TIMER_RUNNING.equals(key);
    }

    protected long clampTimerDuration(long durationMs) {
        long max = 99L * 60L * 60L * 1000L + 59L * 60L * 1000L + 59L * 1000L;
        return Math.max(0L, Math.min(durationMs, max));
    }

    protected void syncStopwatchForegroundService(boolean askNotificationPermission) {
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

    protected void syncTimerForegroundService(boolean askNotificationPermission) {
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

    protected void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (checkSelfPermission(PERMISSION_POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        requestPermissions(new String[]{PERMISSION_POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
    }

    protected boolean hasNotificationPermission() {
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

    protected void setScreenContent(View newScreen, boolean animate, int direction) {
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

    protected void switchScreens() {
        switchToNextScreen();
    }

    protected void switchToNextScreen() {
        int target = (activeScreen + 1) % SCREEN_COUNT;
        switchToScreen(target, 1, true);
    }

    protected void switchToPreviousScreen() {
        int target = activeScreen == SCREEN_CALENDAR ? SCREEN_DIARY : activeScreen - 1;
        switchToScreen(target, -1, true);
    }

    protected void attachScreenSwitchGesture(View view) {
        attachSwipeGestures(view, this::switchToNextScreen, this::switchToPreviousScreen, null, false);
    }

    protected void attachClockGestures(View view) {
        attachSwipeGestures(view, this::switchToNextScreen, this::switchToPreviousScreen, this::switchToNextScreen, true);
    }

    protected void attachScreenCycleGesture(View view, boolean consumeTouches) {
        attachSwipeGestures(
                view,
                this::switchToNextScreen,
                this::switchToPreviousScreen,
                null,
                consumeTouches,
                dp(104),
                2.15f,
                dp(72),
                1.25f
        );
    }

    protected void attachSwipeGestures(View view, Runnable swipeLeft, Runnable swipeRight, Runnable verticalSwipe, boolean consumeTouches) {
        attachSwipeGestures(view, swipeLeft, swipeRight, verticalSwipe, consumeTouches, dp(56), 1.35f, dp(72), 1.25f);
    }

    protected void attachSwipeGestures(View view, Runnable swipeLeft, Runnable swipeRight, Runnable verticalSwipe,
                                    boolean consumeTouches, int horizontalThreshold, float horizontalRatio,
                                    int verticalThreshold, float verticalRatio) {
        float[] downX = new float[1];
        float[] downY = new float[1];
        long[] downAt = new long[1];
        boolean[] tracking = new boolean[1];

        view.setClickable(true);
        view.setOnTouchListener((touched, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    tracking[0] = true;
                    downX[0] = event.getRawX();
                    downY[0] = event.getRawY();
                    downAt[0] = event.getEventTime();
                    return consumeTouches;

                case MotionEvent.ACTION_UP:
                    if (!tracking[0]) {
                        return consumeTouches;
                    }

                    tracking[0] = false;

                    if (event.getEventTime() - downAt[0] >= ViewConfiguration.getLongPressTimeout()) {
                        return true;
                    }

                    float dx = event.getRawX() - downX[0];
                    float dy = event.getRawY() - downY[0];
                    float absX = Math.abs(dx);
                    float absY = Math.abs(dy);

                    if (absX > horizontalThreshold && absX > absY * horizontalRatio) {
                        if (dx < 0 && swipeLeft != null) {
                            swipeLeft.run();
                            return true;
                        }
                        if (dx > 0 && swipeRight != null) {
                            swipeRight.run();
                            return true;
                        }
                    }

                    if (absY > verticalThreshold && absY > absX * verticalRatio && verticalSwipe != null) {
                        verticalSwipe.run();
                        return true;
                    }

                    return consumeTouches;

                case MotionEvent.ACTION_CANCEL:
                    tracking[0] = false;
                    downAt[0] = 0L;
                    return consumeTouches;

                default:
                    return false;
            }
        });
    }

    protected void applySystemInsets() {
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

    protected String formatDate(LocalDate date) {
        return formatDate(date, prefs.getInt(KEY_DATE_FORMAT, DATE_FORMAT_DMY_ORDINAL));
    }

    protected String formatDate(LocalDate date, int format) {
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

    protected String dateFormatLabel(int format) {
        return formatDate(LocalDate.of(2026, 6, 3), format);
    }

    protected String ordinal(int day) {
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

    protected String formatClock(LocalTime time) {
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

    protected CheckBox optionCheckBox(String label, boolean checked) {
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

    protected void tintCompoundButton(android.widget.CompoundButton button) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int[][] states = new int[][]{
                    new int[]{android.R.attr.state_checked},
                    new int[]{}
            };
            int[] colors = new int[]{accentColor(), mutedTextColor()};
            button.setButtonTintList(new ColorStateList(states, colors));
        }
    }

    protected TextView plainText(String text, int sizeSp) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(effectiveTextColor());
        view.setTextSize(Math.max(MIN_FONT_SIZE, sizeSp));
        view.setTypeface(Typeface.DEFAULT, boldTypeface());
        view.setPadding(dp(4), dp(3), dp(4), dp(3));
        return view;
    }

    protected TextView dialogText(String text, int sizeSp) {
        TextView view = plainText(text, sizeSp);
        view.setTextColor(dialogTextColor());
        return view;
    }

    protected TextView dialogTitle(String title) {
        TextView view = dialogText(title, fontSize() + 6);
        view.setPadding(dp(24), dp(18), dp(24), dp(8));
        return view;
    }

    protected AlertDialog.Builder dialogBuilder(String title) {
        return new AlertDialog.Builder(this).setCustomTitle(dialogTitle(title));
    }

    protected TextView actionButton(String text) {
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

    protected TextView actionButton(String text, int iconRes) {
        TextView view = actionButton(text);
        setActionButtonContent(view, text, iconRes);
        return view;
    }

    protected LinearLayout topActionButton(String text, int iconRes) {
        LinearLayout button = new LinearLayout(this);
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setGravity(Gravity.CENTER);
        button.setClickable(true);
        button.setFocusable(true);
        button.setBackground(ripple(surfaceColor(), strokeColor(), dp(8)));
        button.setPadding(dp(3), 0, dp(3), 0);
        setButtonTooltip(button, text);
        attachButtonFeedback(button);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(effectiveTextColor());
        button.addView(icon, new LinearLayout.LayoutParams(dp(14), dp(14)));

        TextView label = plainText(text, fontSize());
        label.setIncludeFontPadding(false);
        label.setPadding(0, 0, 0, 0);

        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        labelParams.setMargins(dp(3), 0, 0, 0);
        button.addView(label, labelParams);

        return button;
    }

    protected void setActionButtonContent(TextView view, String text, int iconRes) {
        setActionButtonContent(view, text, iconRes, 22, 5);
    }

    protected void setActionButtonContent(TextView view, String text, int iconRes, int iconSizeDp, int iconPaddingDp) {
        view.setText(text);
        setButtonTooltip(view, text);
        view.setCompoundDrawablePadding(dp(iconPaddingDp));
        Drawable icon = getDrawable(iconRes);
        if (icon != null) {
            int iconSize = dp(iconSizeDp);
            icon = icon.mutate();
            icon.setBounds(0, 0, iconSize, iconSize);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                icon.setTint(effectiveTextColor());
            }
        }
        view.setCompoundDrawables(icon, null, null, null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view.setCompoundDrawableTintList(ColorStateList.valueOf(effectiveTextColor()));
        }
    }

    protected ImageButton iconButton(int drawableRes, String contentDescription) {
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

    protected ImageButton dialogIconButton(int drawableRes, String contentDescription) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(drawableRes);
        button.setColorFilter(dialogTextColor());
        button.setBackground(ripple(dialogButtonColor(), strokeForColor(dialogButtonColor()), dp(8)));
        button.setContentDescription(contentDescription);
        button.setPadding(dp(10), dp(10), dp(10), dp(10));
        button.setScaleType(ImageView.ScaleType.CENTER);
        button.setClickable(true);
        button.setFocusable(true);
        setButtonTooltip(button, contentDescription);
        attachButtonFeedback(button);
        return button;
    }

    protected TextView dialogActionButton(String text) {
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

    protected LinearLayout dialogActionButton(String text, int iconRes) {
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

    protected void setButtonTooltip(View view, String text) {
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

    protected void attachButtonFeedback(View view) {
        applyRaisedSurface(view);
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

    protected void applyRaisedSurface(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setElevation(dp(2));
            view.setTranslationZ(0f);
        }
    }

    protected CheckBox dialogCheckBox(String label, boolean checked) {
        CheckBox box = optionCheckBox(label, checked);
        box.setTextColor(dialogTextColor());
        return box;
    }

    protected LinearLayout dialogLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(18), dp(8), dp(18), dp(8));
        layout.setClipChildren(false);
        layout.setClipToPadding(false);
        return layout;
    }

    protected LinearLayout screenSectionLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(6), dp(6), dp(6), dp(6));
        layout.setClipChildren(false);
        layout.setClipToPadding(false);
        return layout;
    }

    protected void addScrollEndPadding(LinearLayout layout) {
        View spacer = new View(this);
        layout.addView(spacer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(28)
        ));
    }

    protected int fontSize() {
        int value = prefs.getInt(KEY_FONT_SIZE, 12);
        return Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, value));
    }

    protected int boldTypeface() {
        return prefs.getBoolean(KEY_BOLD, false) ? Typeface.BOLD : Typeface.NORMAL;
    }

    protected int accentColor() {
        return prefs.getInt(KEY_ACCENT_COLOR, Color.rgb(106, 148, 255));
    }

    protected int dailyNoteColor() {
        return prefs.getInt(KEY_DAILY_NOTE_COLOR, DEFAULT_DAILY_NOTE_COLOR);
    }

    protected int yearlyNoteColor() {
        return prefs.getInt(KEY_YEARLY_NOTE_COLOR, DEFAULT_YEARLY_NOTE_COLOR);
    }

    protected int selectedTheme() {
        return prefs.getInt(KEY_THEME, THEME_SYSTEM);
    }

    protected int activeTheme() {
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

    protected int backgroundColor() {
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

    protected int surfaceColor() {
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

    protected int strokeColor() {
        if (activeTheme() == THEME_DARK_GRAY) {
            return Color.rgb(42, 53, 80);
        }
        return mix(effectiveTextColor(), backgroundColor(), 0.22f);
    }

    protected int mutedTextColor() {
        if (activeTheme() == THEME_DARK_GRAY) {
            return Color.rgb(168, 179, 209);
        }
        return mix(effectiveTextColor(), backgroundColor(), 0.56f);
    }

    protected int effectiveTextColor() {
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

    protected int dialogTextColor() {
        return activeTheme() == THEME_LIGHT ? Color.WHITE : effectiveTextColor();
    }

    protected int dialogButtonColor() {
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

    protected int menuBackgroundColor() {
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

    protected int menuTextColor() {
        return Color.WHITE;
    }

    protected int strokeForColor(int color) {
        return mix(dialogTextColor(), color, 0.28f);
    }

    protected int mix(int foreground, int background, float amount) {
        int red = Math.round(Color.red(background) + (Color.red(foreground) - Color.red(background)) * amount);
        int green = Math.round(Color.green(background) + (Color.green(foreground) - Color.green(background)) * amount);
        int blue = Math.round(Color.blue(background) + (Color.blue(foreground) - Color.blue(background)) * amount);
        return Color.rgb(red, green, blue);
    }

    protected int contrastText(int color) {
        double luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0;
        return luminance > 0.68 ? Color.BLACK : Color.WHITE;
    }

    protected GradientDrawable rounded(int fill, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        if (Color.alpha(stroke) != 0) {
            drawable.setStroke(Math.max(1, dp(1)), stroke);
        }
        return drawable;
    }

    protected GradientDrawable oval(int fill) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(fill);
        return drawable;
    }

    protected Drawable ripple(int fill, int stroke, int radius) {
        GradientDrawable content = rounded(fill, stroke, radius);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return content;
        }
        GradientDrawable mask = rounded(Color.WHITE, Color.TRANSPARENT, radius);
        int rippleBase = mix(accentColor(), effectiveTextColor(), 0.46f);
        int rippleColor = Color.argb(96, Color.red(rippleBase), Color.green(rippleBase), Color.blue(rippleBase));
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, mask);
    }

    protected void styleSystemBars() {
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

    protected int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    protected static final class NoteListItem {
        final String sortKey;
        final String noteKey;
        final boolean yearly;
        final String title;
        final String type;
        final String note;
        final LocalDate date;
        final int initialTab;
        final int dotColor;

        NoteListItem(String sortKey, String noteKey, boolean yearly, String title, String type,
                     String note, LocalDate date, int initialTab, int dotColor) {
            this.sortKey = sortKey;
            this.noteKey = noteKey;
            this.yearly = yearly;
            this.title = title;
            this.type = type;
            this.note = note;
            this.date = date;
            this.initialTab = initialTab;
            this.dotColor = dotColor;
        }
    }

    protected interface ColorPicked {
        void onPicked(int color);
    }

    protected interface DatePicked {
        void onPicked(LocalDate date);
    }

    protected interface MonthPicked {
        void onPicked(YearMonth month);
    }

    protected interface TextPicked {
        void onPicked(String text);
    }

    protected abstract static class SimpleSeekListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }
}
