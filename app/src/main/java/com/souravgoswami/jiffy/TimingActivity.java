package com.souravgoswami.jiffy;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

// Clock, world-time, stopwatch, and timer screens.
abstract class TimingActivity extends ActivityBase {
    protected void showClock(boolean animate, int direction) {
        clockFace = null;
        clockDate = null;
        worldList = null;
        clearStopwatchUi();
        clearTimerUi();

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(backgroundColor());
        attachClockGestures(scrollView);

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

        LinearLayout.LayoutParams keepScreenParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        keepScreenParams.setMargins(0, dp(16), 0, 0);
        layout.addView(clockKeepScreenOnCheckBox(), keepScreenParams);

        TextView hint = plainText("Use the bottom menu for 12/24-hour time and appearance.", fontSize() - 2);
        hint.setGravity(Gravity.CENTER);
        hint.setTextColor(mutedTextColor());
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        hintParams.setMargins(0, dp(18), 0, 0);
        layout.addView(hint, hintParams);
        addScrollEndPadding(layout);

        scrollView.addView(layout, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        setScreenContent(scrollView, animate, direction);
        updateClockText();
    }

    protected void showWorld(boolean animate, int direction) {
        clockFace = null;
        clockDate = null;
        clearStopwatchUi();
        clearTimerUi();
        lastWorldEpochSecond = -1L;

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(backgroundColor());
        screen.setPadding(dp(14), dp(14), dp(14), dp(14));
        allowChildShadows(screen);
        attachScreenCycleGesture(screen, false);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, shadowGutter(), 0, shadowGutter());
        allowChildShadows(header);

        int addButtonWidth = dp(96);
        header.addView(new View(this), new LinearLayout.LayoutParams(addButtonWidth, dp(42)));

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
        header.addView(add, new LinearLayout.LayoutParams(addButtonWidth, dp(42)));
        screen.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        ScrollView scrollView = new ScrollView(this);
        allowChildShadows(scrollView);
        attachScreenCycleGesture(scrollView, false);
        worldList = new LinearLayout(this);
        worldList.setOrientation(LinearLayout.VERTICAL);
        worldList.setPadding(shadowGutter(), dp(10), shadowGutter(), dp(10));
        allowChildShadows(worldList);
        scrollView.addView(worldList);
        screen.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        setScreenContent(screen, animate, direction);
        renderWorldTimes();
    }

    protected void clearStopwatchUi() {
        stopwatchFace = null;
        stopwatchLapList = null;
        stopwatchStartButton = null;
        stopwatchLapButton = null;
        stopwatchResetButton = null;
        keepScreenOnCheckBox = null;
        keepScreenOnPreferenceKey = null;
    }

    protected void clearTimerUi() {
        timerFace = null;
        timerHourInput = null;
        timerMinuteInput = null;
        timerSecondInput = null;
        timerStartButton = null;
        timerStopButton = null;
        timerSoundCheckBox = null;
        timerTuneButton = null;
        timerTuneHint = null;
        keepScreenOnCheckBox = null;
        keepScreenOnPreferenceKey = null;
    }

    protected LinearLayout screenTitle(String text, int iconRes, boolean colorfulIcon) {
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

    protected void showStopwatch(boolean animate, int direction) {
        clockFace = null;
        clockDate = null;
        worldList = null;
        clearTimerUi();

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(backgroundColor());
        allowChildShadows(scrollView);
        attachScreenCycleGesture(scrollView, false);

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setGravity(Gravity.CENTER_HORIZONTAL);
        screen.setBackgroundColor(backgroundColor());
        screen.setPadding(dp(16), dp(18), dp(16), dp(16));
        allowChildShadows(screen);
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
        buttons.setPadding(0, shadowGutter(), 0, shadowGutter());
        allowChildShadows(buttons);

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
                applyKeepScreenOnPreference();
                updateStopwatchText();
                refreshControls[0].run();
            } else {
                anchorStopwatchToNow();
                stopwatchRunning = true;
                persistStopwatchState();
                syncStopwatchForegroundService(true);
                applyKeepScreenOnPreference();
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
                    applyKeepScreenOnPreference();
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

        LinearLayout.LayoutParams keepScreenParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        keepScreenParams.setMargins(0, dp(12), 0, 0);
        screen.addView(keepScreenOnCheckBox(), keepScreenParams);

        stopwatchLapList = new LinearLayout(this);
        stopwatchLapList.setOrientation(LinearLayout.VERTICAL);
        stopwatchLapList.setPadding(0, dp(12), 0, dp(8));
        allowChildShadows(stopwatchLapList);
        LinearLayout.LayoutParams lapParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lapParams.setMargins(0, dp(8), 0, 0);
        screen.addView(stopwatchLapList, lapParams);

        scrollView.addView(screen, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        setScreenContent(scrollView, animate, direction);
        updateStopwatchText();
        renderStopwatchLaps();
        refreshControls[0].run();
    }

    protected void showResetStopwatchDialog(Runnable onReset) {
        TextView message = dialogText("Reset the stopwatch and clear all laps?", fontSize());
        message.setPadding(dp(18), dp(8), dp(18), dp(8));
        dialogBuilder("Reset Stopwatch")
                .setView(message)
                .setPositiveButton("Reset", (dialog, which) -> onReset.run())
                .setNegativeButton("Cancel", null)
                .show();
    }

    protected void addStopwatchButton(LinearLayout row, TextView button) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        row.addView(button, params);
    }

    protected void styleStopwatchButton(TextView button, boolean enabled) {
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.42f);
    }

    protected void refreshStopwatchControls() {
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

    protected long stopwatchElapsedMs() {
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

    protected void updateStopwatchText() {
        if (stopwatchFace != null) {
            stopwatchFace.setText(formatStopwatchDuration(stopwatchElapsedMs()));
        }
    }

    protected String formatStopwatchDuration(long elapsedMs) {
        long totalCentiseconds = Math.max(0L, elapsedMs) / 10L;
        long centiseconds = totalCentiseconds % 100L;
        long totalSeconds = totalCentiseconds / 100L;
        long seconds = totalSeconds % 60L;
        long totalMinutes = totalSeconds / 60L;
        long minutes = totalMinutes % 60L;
        long hours = totalMinutes / 60L;
        return String.format(Locale.US, "%02d:%02d:%02d:%02d", hours, minutes, seconds, centiseconds);
    }

    protected void renderStopwatchLaps() {
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
            addScrollEndPadding(stopwatchLapList);
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
        addScrollEndPadding(stopwatchLapList);
    }

    protected void showTimer(boolean animate, int direction) {
        clockFace = null;
        clockDate = null;
        worldList = null;
        clearStopwatchUi();
        ensureTimerSoundChannels();

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(backgroundColor());
        allowChildShadows(scrollView);
        attachScreenCycleGesture(scrollView, false);

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setGravity(Gravity.CENTER_HORIZONTAL);
        screen.setBackgroundColor(backgroundColor());
        screen.setPadding(dp(16), dp(18), dp(16), dp(16));
        allowChildShadows(screen);
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
        buttons.setPadding(0, shadowGutter(), 0, shadowGutter());
        allowChildShadows(buttons);
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

        LinearLayout.LayoutParams keepScreenParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        keepScreenParams.setMargins(0, dp(12), 0, 0);
        screen.addView(keepScreenOnCheckBox(), keepScreenParams);

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
        addScrollEndPadding(screen);

        scrollView.addView(screen, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        setScreenContent(scrollView, animate, direction);
        refreshTimerInputs();
        updateTimerText();
        refreshTimerControls();
        refreshTimerSoundControls();
    }

    protected void ensureTimerSoundChannels() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            TimerAlert.ensureChannels(this, manager, prefs);
        }
    }

    protected void showTimerTunePicker() {
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

    protected Uri timerTuneUri() {
        String stored = prefs.getString(KEY_TIMER_SOUND_URI, null);
        if (stored == null || stored.trim().isEmpty()) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        return Uri.parse(stored);
    }

    protected String timerTuneButtonText() {
        return "Timer Tune: " + timerTuneLabel();
    }

    protected String timerTuneLabel() {
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

    protected void refreshTimerSoundControls() {
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

    protected EditText timerInput() {
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

    protected void addTimerInput(LinearLayout row, String label, EditText input) {
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

    protected void addTimerButton(LinearLayout row, TextView button) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        row.addView(button, params);
    }

    protected void startTimer() {
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
        TimerAlarmScheduler.schedule(this, prefs);
        syncTimerForegroundService(true);
        applyKeepScreenOnPreference();
        updateTimerText();
        refreshTimerInputs();
        refreshTimerControls();
    }

    protected void pauseTimer() {
        if (!timerRunning) {
            return;
        }
        timerRemainingMs = currentTimerRemainingMs();
        timerRunning = false;
        clearTimerAnchor();
        persistTimerState();
        TimerAlarmScheduler.cancel(this, prefs);
        syncTimerForegroundService(false);
        applyKeepScreenOnPreference();
        updateTimerText();
        refreshTimerInputs();
        refreshTimerControls();
    }

    protected void stopTimer() {
        timerRunning = false;
        clearTimerAnchor();
        timerRemainingMs = timerDurationMs;
        persistTimerState();
        TimerAlarmScheduler.cancel(this, prefs);
        syncTimerForegroundService(false);
        applyKeepScreenOnPreference();
        updateTimerText();
        refreshTimerInputs();
        refreshTimerControls();
    }

    protected long currentTimerRemainingMs() {
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

    protected void updateTimerText() {
        if (timerRunning && currentTimerRemainingMs() <= 0L) {
            finishTimer();
        }
        if (timerFace != null) {
            timerFace.setText(formatTimerDuration(currentTimerRemainingMs()));
        }
    }

    protected void finishTimer() {
        timerRunning = false;
        clearTimerAnchor();
        timerRemainingMs = 0L;
        persistTimerState();
        TimerAlarmScheduler.cancel(this, prefs);
        TimerAlert.show(this, prefs);
        syncTimerForegroundService(false);
        applyKeepScreenOnPreference();
        refreshTimerInputs();
        refreshTimerControls();
    }

    protected String formatTimerDuration(long remainingMs) {
        long totalCentiseconds = (Math.max(0L, remainingMs) + 9L) / 10L;
        long centiseconds = totalCentiseconds % 100L;
        long totalSeconds = totalCentiseconds / 100L;
        long seconds = totalSeconds % 60L;
        long totalMinutes = totalSeconds / 60L;
        long minutes = totalMinutes % 60L;
        long hours = totalMinutes / 60L;
        return String.format(Locale.US, "%02d:%02d:%02d:%02d", hours, minutes, seconds, centiseconds);
    }

    protected void refreshTimerInputs() {
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

    protected void refreshTimerControls() {
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

    protected void syncTimerDurationFromInputs() {
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

    protected long parseTimerInput(EditText input, long max) {
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

    protected void renderWorldTimesIfNeeded() {
        if (activeScreen != SCREEN_WORLD || worldList == null) {
            return;
        }
        long epochSecond = Instant.now().getEpochSecond();
        if (epochSecond != lastWorldEpochSecond) {
            lastWorldEpochSecond = epochSecond;
            renderWorldTimes();
        }
    }

    protected void renderWorldTimes() {
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
        addScrollEndPadding(worldList);
    }

    protected void addWorldRow(String zoneId, Instant now, ZoneId localZone, boolean pinned) {
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
        allowChildShadows(row);
        applyRaisedSurface(row);

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);

        TextView name = plainText(zoneDisplayName(zoneId), fontSize());
        name.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        textColumn.addView(name);

        TextView relation = plainText(zoneRelationText(zone, localZone, now), Math.max(MIN_FONT_SIZE, fontSize() - 3));
        relation.setTextColor(mutedTextColor());
        textColumn.addView(relation);

        TextView dst = plainText(dstStatusText(zone, now), Math.max(MIN_FONT_SIZE, fontSize() - 3));
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

    protected void showAddWorldZoneDialog() {
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

    protected void renderWorldZoneSearchResults(LinearLayout results, List<String> zones, String query, AlertDialog dialog) {
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

    protected void addWorldZoneSearchResult(LinearLayout results, String label, String zone, AlertDialog dialog) {
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

    protected List<String> worldZoneIds() {
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

    protected void saveWorldZoneIds(List<String> zones) {
        StringBuilder builder = new StringBuilder();
        for (String zone : zones) {
            if (builder.length() > 0) {
                builder.append('|');
            }
            builder.append(zone);
        }
        prefs.edit().putString(KEY_WORLD_ZONES, builder.toString()).apply();
    }

    protected String zonePickerLabel(String zoneId) {
        return zoneDisplayName(zoneId) + " (" + zoneId + ")";
    }

    protected String zoneDisplayName(String zoneId) {
        int slash = zoneId.lastIndexOf('/');
        String name = slash >= 0 ? zoneId.substring(slash + 1) : zoneId;
        return name.replace('_', ' ');
    }

    protected String zoneRelationText(ZoneId zone, ZoneId localZone, Instant now) {
        int targetOffset = zone.getRules().getOffset(now).getTotalSeconds();
        int localOffset = localZone.getRules().getOffset(now).getTotalSeconds();
        int diffMinutes = Math.round((targetOffset - localOffset) / 60f);
        if (diffMinutes == 0) {
            return "Current timezone";
        }
        String direction = diffMinutes > 0 ? "ahead" : "behind";
        return formatHourDifference(Math.abs(diffMinutes)) + " " + direction;
    }

    protected String formatHourDifference(int minutes) {
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

    protected String dstStatusText(ZoneId zone, Instant now) {
        TimeZone timeZone = TimeZone.getTimeZone(zone);
        if (!timeZone.useDaylightTime()) {
            return "DST not applicable";
        }
        return zone.getRules().isDaylightSavings(now) ? "DST active" : "Standard time";
    }

    protected String formatWorldTime(ZonedDateTime time) {
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
}
