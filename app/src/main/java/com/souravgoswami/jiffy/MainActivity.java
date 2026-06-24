package com.souravgoswami.jiffy;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.HashSet;

public final class MainActivity extends JiffySettingsActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        ensureDefaults();
        restoreStopwatchState(savedInstanceState);
        restoreTimerState(savedInstanceState);
        restoreCalendarNotes();
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener);
        highlightedDates = new HashSet<>();
        prefs.edit().remove(KEY_HIGHLIGHTED).apply();
        observedDate = LocalDate.now();
        visibleMonth = YearMonth.from(observedDate);
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

    protected void buildShell() {
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

    protected void buildBottomBar() {
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
        diaryTab = bottomTab("Diary", SCREEN_DIARY, R.drawable.ic_note_bookmark, compactTabs);

        tabs.addView(calendarTab);
        tabs.addView(clockTab);
        tabs.addView(calculatorTab);
        tabs.addView(worldTab);
        tabs.addView(stopwatchTab);
        tabs.addView(timerTab);
        tabs.addView(diaryTab);

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

    protected LinearLayout bottomTab(String label, int screen, int iconRes, boolean isCompact) {
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
            text.setTextSize(Math.max(MIN_FONT_SIZE, fontSize()));
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

    protected void showActiveScreen() {
        showActiveScreen(false, 0);
    }

    protected void showActiveScreen(boolean animate, int direction) {
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
            case SCREEN_DIARY:
                showDiary(animate, direction);
                break;
            case SCREEN_CALENDAR:
            default:
                showCalendar(animate, direction);
                break;
        }
        updateTabStyles();
    }

    protected void switchToScreen(int screen, int direction, boolean animate) {
        if (activeScreen == screen) {
            return;
        }
        activeScreen = screen;
        showActiveScreen(animate, direction);
        restartUiTicker();
    }

    protected int screenDirection(int targetScreen) {
        return targetScreen >= activeScreen ? 1 : -1;
    }



    protected void updateTabStyles() {
        if (calendarTab == null || clockTab == null || calculatorTab == null
                || worldTab == null || stopwatchTab == null || timerTab == null || diaryTab == null) {
            return;
        }
        styleTab(calendarTab, activeScreen == SCREEN_CALENDAR);
        styleTab(clockTab, activeScreen == SCREEN_CLOCK);
        styleTab(calculatorTab, activeScreen == SCREEN_CALCULATOR);
        styleTab(worldTab, activeScreen == SCREEN_WORLD);
        styleTab(stopwatchTab, activeScreen == SCREEN_STOPWATCH);
        styleTab(timerTab, activeScreen == SCREEN_TIMER);
        styleTab(diaryTab, activeScreen == SCREEN_DIARY);
    }

    protected void styleTab(LinearLayout tab, boolean active) {
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
                label.setTextSize(Math.max(MIN_FONT_SIZE, fontSize()));
                label.setTypeface(Typeface.DEFAULT, boldTypeface());
            }
        }
        tab.animate()
                .translationY(active ? -dp(1) : 0f)
                .setDuration(140L)
                .start();
    }
















    protected void refreshUi() {
        if (root != null) {
            root.setBackgroundColor(backgroundColor());
        }
        styleSystemBars();
        buildBottomBar();
        showActiveScreen();
    }

    protected void updateClockText() {
        LocalDate today = LocalDate.now();
        if (clockFace != null) {
            clockFace.setText(formatClock(LocalTime.now()));
        }
        if (clockDate != null) {
            clockDate.setText(formatDate(today));
        }
        if (activeScreen == SCREEN_WORLD) {
            renderWorldTimesIfNeeded();
        }
        if (stopwatchFace != null) {
            updateStopwatchText();
        }
        if (timerRunning || timerFace != null) {
            updateTimerText();
        }
        handleDateChange(today);
    }

    protected void handleDateChange(LocalDate today) {
        if (observedDate == null) {
            observedDate = today;
            return;
        }
        if (today.equals(observedDate)) {
            return;
        }

        LocalDate previous = observedDate;
        YearMonth previousMonth = YearMonth.from(previous);
        YearMonth currentMonth = YearMonth.from(today);
        boolean followingCurrentMonth = visibleMonth != null && visibleMonth.equals(previousMonth);
        if (followingCurrentMonth) {
            visibleMonth = currentMonth;
        }

        observedDate = today;
        refreshForDateChange(previous, today, previousMonth, currentMonth, followingCurrentMonth);
    }

    protected void refreshForDateChange(LocalDate previousDate, LocalDate currentDate, YearMonth previousMonth,
                                      YearMonth currentMonth, boolean followingCurrentMonth) {
        if (activeScreen == SCREEN_CALENDAR) {
            if (followingCurrentMonth || currentMonth.equals(visibleMonth) || previousMonth.equals(visibleMonth)) {
                renderCalendar();
            }
            return;
        }

        if (activeScreen == SCREEN_DIARY
                && activeDiaryView == DIARY_VIEW_YEARLY
                && !currentMonth.equals(previousMonth)) {
            showDiary(false, 0);
            return;
        }

        if (activeScreen == SCREEN_CALCULATOR) {
            refreshCalculatorForDateChange(previousDate, currentDate);
        }
    }

    protected void refreshCalculatorForDateChange(LocalDate previousDate, LocalDate currentDate) {
        if (calculatorFromDate == null) {
            calculatorFromDate = previousDate;
            calculatorFromFollowsToday = true;
        }
        if (calculatorToDate == null) {
            calculatorToDate = selectedDateOrToday();
        }
        if (calculatorFromFollowsToday) {
            calculatorFromDate = currentDate;
        }
        if (calculatorOffsetBaseFollowsToday) {
            calculatorOffsetBaseDate = currentDate;
        }
        showCalculatorWithDates(false, 0, calculatorFromDate, calculatorToDate, calculatorFromFollowsToday);
    }

    protected void startUiTicker() {
        if (uiTickerRunning) {
            return;
        }
        uiTickerRunning = true;
        handler.removeCallbacks(clockTicker);
        handler.post(clockTicker);
    }

    protected void stopUiTicker() {
        uiTickerRunning = false;
        handler.removeCallbacks(clockTicker);
    }

    protected void restartUiTicker() {
        if (!uiTickerRunning) {
            return;
        }
        handler.removeCallbacks(clockTicker);
        handler.post(clockTicker);
    }

    protected long tickerDelayMs() {
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
}
