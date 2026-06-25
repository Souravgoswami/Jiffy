package com.souravgoswami.jiffy;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
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
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.time.YearMonth;
import java.time.DateTimeException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

// Calendar, date calculator, and diary screens.
abstract class JiffyCalendarActivity extends JiffyTimingActivity {
    protected void showCalculator(boolean animate, int direction) {
        showCalculatorWithDates(animate, direction, LocalDate.now(), selectedDateOrToday(), true);
    }

    protected void showCalculatorWithDates(boolean animate, int direction, LocalDate fromDate, LocalDate toDate,
                                         boolean fromFollowsToday) {
        calculatorFromDate = fromDate;
        calculatorToDate = toDate;
        calculatorFromFollowsToday = fromFollowsToday;
        if (calculatorOffsetBaseDate == null || calculatorOffsetBaseFollowsToday) {
            calculatorOffsetBaseDate = fromDate;
            calculatorOffsetBaseFollowsToday = fromFollowsToday;
        }

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
        LinearLayout calculator = buildDateCalculatorView(calculatorFromDate, calculatorToDate, false);
        addScrollEndPadding(calculator);
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

    protected void showCalendar(boolean animate, int direction) {
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
        nav.setPadding(dp(6), dp(6), dp(6), dp(3));
        screen.addView(nav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        ImageButton previous = iconButton(R.drawable.ic_chevron_left, "Previous month");
        previous.setContentDescription("Previous month");
        previous.setOnClickListener(view -> navigateMonth(-1));
        nav.addView(previous, new LinearLayout.LayoutParams(dp(38), dp(42)));

        monthTitle = plainText("", fontSize() + 2);
        monthTitle.setGravity(Gravity.CENTER);
        monthTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        nav.addView(monthTitle, new LinearLayout.LayoutParams(0, dp(42), 1f));

        ImageButton next = iconButton(R.drawable.ic_chevron_right, "Next month");
        next.setContentDescription("Next month");
        next.setOnClickListener(view -> navigateMonth(1));
        nav.addView(next, new LinearLayout.LayoutParams(dp(38), dp(42)));

        LinearLayout today = topActionButton("Today", R.drawable.ic_current_time);
        today.setOnClickListener(view -> {
            LocalDate todayDate = LocalDate.now();
            visibleMonth = YearMonth.from(todayDate);
            selectDate(todayDate);
        });
        LinearLayout.LayoutParams todayParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(42)
        );
        todayParams.setMargins(dp(3), 0, 0, 0);
        nav.addView(today, todayParams);

        LinearLayout jump = topActionButton("Pick Date", R.drawable.ic_dialog_date_format);
        jump.setOnClickListener(view -> showJumpDialog());
        LinearLayout.LayoutParams jumpParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(42)
        );
        jumpParams.setMargins(dp(3), 0, 0, 0);
        nav.addView(jump, jumpParams);

        LinearLayout notes = topActionButton("Notes", R.drawable.ic_note_bookmark);
        notes.setOnClickListener(view -> showNoteDialog(selectedDateOrToday(), defaultNoteTab(selectedDateOrToday())));
        LinearLayout.LayoutParams notesParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(42)
        );
        notesParams.setMargins(dp(3), 0, 0, 0);
        nav.addView(notes, notesParams);

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
        calendarTable.setPadding(dp(6), dp(6), dp(6), calendarTableBottomPadding());
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
                calendarBottomGestureHeight(),
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

    protected void renderCalendar() {
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

    protected int daysBetween(DayOfWeek start, DayOfWeek current) {
        int diff = current.getValue() - start.getValue();
        return diff < 0 ? diff + 7 : diff;
    }

    protected boolean isLandscape() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    protected int calendarHeaderCellHeight() {
        return isLandscape() ? dp(28) : dp(34);
    }

    protected int calendarDayCellHeight() {
        return isLandscape() ? dp(38) : dp(50);
    }

    protected int calendarTableBottomPadding() {
        return isLandscape() ? dp(4) : dp(8);
    }

    protected int calendarBottomGestureHeight() {
        return isLandscape() ? dp(36) : 0;
    }

    protected View headerCell(String text, float weight) {
        TextView label = plainText(text, Math.max(11, fontSize() - 1));
        label.setGravity(Gravity.CENTER);
        label.setTextColor(mutedTextColor());
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setLayoutParams(new TableRow.LayoutParams(0, calendarHeaderCellHeight(), weight));
        return label;
    }

    protected View weekNumberCell(String text) {
        TextView label = plainText(text, Math.max(11, fontSize() - 2));
        label.setGravity(Gravity.CENTER);
        label.setTextColor(mutedTextColor());
        label.setLayoutParams(new TableRow.LayoutParams(0, calendarDayCellHeight(), 0.65f));
        return label;
    }

    protected View dayCell(LocalDate date, boolean inMonth) {
        FrameLayout frame = new FrameLayout(this);
        TableRow.LayoutParams params = new TableRow.LayoutParams(0, calendarDayCellHeight(), 1f);
        params.setMargins(dp(2), dp(2), dp(2), dp(2));
        frame.setLayoutParams(params);
        frame.setClickable(true);
        frame.setLongClickable(true);

        boolean highlighted = highlightedDates.contains(date.toString());
        boolean hasDailyNote = hasDailyNote(date);
        boolean hasYearlyNote = hasYearlyNote(date);
        boolean today = LocalDate.now().equals(date);
        int fill = today ? accentColor() : backgroundColor();
        int stroke = highlighted ? (today ? contrastText(accentColor()) : accentColor()) : Color.TRANSPARENT;
        frame.setBackground(rounded(fill, stroke, dp(7)));

        TextView label = plainText(String.valueOf(date.getDayOfMonth()), fontSize());
        label.setGravity(Gravity.CENTER);
        label.setTextColor(today ? contrastText(accentColor()) : (inMonth ? effectiveTextColor() : mutedTextColor()));
        label.setTypeface(Typeface.DEFAULT, highlighted || today ? Typeface.BOLD : boldTypeface());
        label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        frame.addView(label, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        if (hasDailyNote || hasYearlyNote) {
            addNoteDots(frame, hasDailyNote, hasYearlyNote);
        }
        frame.setContentDescription(calendarDayDescription(date, inMonth, today, highlighted, hasDailyNote, hasYearlyNote));
        frame.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

        frame.setOnLongClickListener(view -> {
            LocalDate from = selectedDate();
            selectDate(date);

            showDateCalculator(
                from != null && !from.equals(date) ? from : LocalDate.now(),
                date
            );

            return true;
        });

        attachCalendarDayGestures(frame, date);
        return frame;
    }

    protected void addNoteDots(FrameLayout frame, boolean hasDailyNote, boolean hasYearlyNote) {
        LinearLayout dots = new LinearLayout(this);
        dots.setOrientation(LinearLayout.HORIZONTAL);
        dots.setGravity(Gravity.CENTER);
        dots.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        if (hasDailyNote) {
            addNoteDot(dots, dailyNoteColor());
        }
        if (hasYearlyNote) {
            addNoteDot(dots, yearlyNoteColor());
        }
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(8),
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL
        );

        params.setMargins(0, 0, 0, dp(5));
        frame.addView(dots, params);
    }

    protected void addNoteDot(LinearLayout parent, int color) {
        View dot = new View(this);
        dot.setBackground(oval(color));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(6), dp(6));
        params.setMargins(dp(2), 0, dp(2), 0);
        parent.addView(dot, params);
    }

    protected void addNoteMark(LinearLayout parent, int color) {
        addNoteMark(parent, color, dp(12), dp(15));
    }

    protected void addNoteMark(LinearLayout parent, int color, int width, int height) {
        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.ic_note_bookmark);
        mark.setColorFilter(color);
        mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.setMargins(dp(4), 0, dp(6), 0);
        parent.addView(mark, params);
    }

    protected String calendarDayDescription(LocalDate date, boolean inMonth, boolean today, boolean highlighted,
                                          boolean hasDailyNote, boolean hasYearlyNote) {
        StringBuilder description = new StringBuilder(formatDate(date));
        if (today) {
            description.append(", today");
        }
        if (highlighted) {
            description.append(", selected");
        }
        if (hasDailyNote) {
            description.append(", daily note");
        }
        if (hasYearlyNote) {
            description.append(", yearly note");
        }
        if (!inMonth) {
            description.append(", outside this month");
        }
        description.append(". Tap to select. Long press for date details.");
        return description.toString();
    }

    protected void navigateMonth(int months) {
        visibleMonth = visibleMonth.plusMonths(months);
        renderCalendar();
        animateMonthTransition(months);
    }

    protected void animateMonthTransition(int direction) {
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

    protected void attachCalendarMonthSwipe(View view) {
        attachSwipeGestures(view, () -> navigateMonth(1), () -> navigateMonth(-1), null, false);
    }

    protected void attachCalendarDayGestures(View view, LocalDate date) {
        float[] downX = new float[1];
        float[] downY = new float[1];
        long[] downAt = new long[1];
        boolean[] tracking = new boolean[1];

        int tapSlop = Math.max(ViewConfiguration.get(this).getScaledTouchSlop() * 2, dp(18));

        view.setOnTouchListener((touched, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    tracking[0] = true;
                    downX[0] = event.getRawX();
                    downY[0] = event.getRawY();
                    downAt[0] = event.getEventTime();
                    return false;

                case MotionEvent.ACTION_UP:
                    // Very important:
                    // Ignore stray ACTION_UP events that did not begin on this exact cell.
                    if (!tracking[0]) {
                        return true;
                    }

                    tracking[0] = false;

                    float dx = event.getRawX() - downX[0];
                    float dy = event.getRawY() - downY[0];
                    float absX = Math.abs(dx);
                    float absY = Math.abs(dy);
                    long heldFor = event.getEventTime() - downAt[0];

                    // Long-click already handled this gesture.
                    if (heldFor >= ViewConfiguration.getLongPressTimeout()) {
                        return true;
                    }

                    if (absX > dp(56) && absX > absY * 1.35f) {
                        if (dx < 0) {
                            navigateMonth(1);
                            return true;
                        }
                        navigateMonth(-1);
                        return true;
                    }

                    if (absX <= tapSlop && absY <= tapSlop) {
                        handleDateTap(date);
                        return true;
                    }

                    return false;

                case MotionEvent.ACTION_CANCEL:
                    tracking[0] = false;
                    downAt[0] = 0L;
                    return false;

                default:
                    return false;
            }
        });
    }

    protected void attachCalendarEmptyGestures(View view) {
        attachSwipeGestures(view, this::switchToNextScreen, this::switchToPreviousScreen, this::switchToNextScreen, true);
    }

    protected void handleDateTap(LocalDate date) {
        selectDate(date);
    }

    protected void selectDate(LocalDate date) {
        highlightedDates.clear();
        highlightedDates.add(date.toString());
        renderCalendar();
    }

    protected LocalDate selectedDate() {
        if (highlightedDates.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(highlightedDates.iterator().next());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    protected LocalDate selectedDateOrToday() {
        LocalDate date = selectedDate();
        return date == null ? LocalDate.now() : date;
    }

    protected int defaultNoteTab(LocalDate date) {
        if (hasDailyNote(date)) {
            return NOTE_TAB_DAILY;
        }
        if (hasYearlyNote(date)) {
            return NOTE_TAB_YEARLY;
        }
        return NOTE_TAB_DAILY;
    }

    protected void showNoteDialog(LocalDate date, int initialTab) {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = dialogLayout();
        scrollView.addView(layout);

        TextView dateLabel = dialogText(formatDate(date), Math.max(12, fontSize() - 1));
        dateLabel.setGravity(Gravity.CENTER);
        dateLabel.setTextColor(accentColor());
        dateLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        layout.addView(dateLabel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tabsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tabsParams.setMargins(0, dp(8), 0, dp(10));
        layout.addView(tabs, tabsParams);

        TextView dailyTab = noteTabButton("Daily Note");
        TextView yearlyTab = noteTabButton("Yearly Note");
        TextView allTab = noteTabButton("All Notes");
        LinearLayout.LayoutParams dailyTabParams = new LinearLayout.LayoutParams(0, dp(42), 1f);
        dailyTabParams.setMargins(0, 0, dp(4), 0);
        tabs.addView(dailyTab, dailyTabParams);
        LinearLayout.LayoutParams yearlyTabParams = new LinearLayout.LayoutParams(0, dp(42), 1f);
        yearlyTabParams.setMargins(dp(4), 0, dp(4), 0);
        tabs.addView(yearlyTab, yearlyTabParams);
        LinearLayout.LayoutParams allTabParams = new LinearLayout.LayoutParams(0, dp(42), 1f);
        allTabParams.setMargins(dp(4), 0, 0, 0);
        tabs.addView(allTab, allTabParams);

        LinearLayout contentArea = new LinearLayout(this);
        contentArea.setOrientation(LinearLayout.VERTICAL);
        layout.addView(contentArea, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        EditText dailyInput = noteInput(dailyNotes.get(dailyKey(date)), "Daily Note");
        EditText yearlyInput = noteInput(yearlyNotes.get(yearlyKey(date)), "Yearly Note");
        int[] activeTab = new int[]{initialTab};

        Runnable[] renderTabs = new Runnable[1];
        renderTabs[0] = () -> {
            styleNoteTab(dailyTab, activeTab[0] == NOTE_TAB_DAILY);
            styleNoteTab(yearlyTab, activeTab[0] == NOTE_TAB_YEARLY);
            styleNoteTab(allTab, activeTab[0] == NOTE_TAB_ALL);
            contentArea.removeAllViews();
            if (activeTab[0] == NOTE_TAB_DAILY) {
                addNoteEditor(contentArea, dailyInput, "Daily Note", dailyNoteColor());
            } else if (activeTab[0] == NOTE_TAB_YEARLY) {
                addNoteEditor(contentArea, yearlyInput, yearlyDateLabel(date), yearlyNoteColor());
            } else {
                addAllNotes(
                        contentArea,
                        date,
                        dailyInput.getText().toString(),
                        yearlyInput.getText().toString(),
                        () -> {
                            dailyInput.setText("");
                            saveNote(date, false, "");
                            persistCalendarNotes();
                            refreshNoteViews();
                            renderTabs[0].run();
                        },
                        () -> {
                            yearlyInput.setText("");
                            saveNote(date, true, "");
                            persistCalendarNotes();
                            refreshNoteViews();
                            renderTabs[0].run();
                        }
                );
            }
        };

        dailyTab.setOnClickListener(view -> {
            activeTab[0] = NOTE_TAB_DAILY;
            renderTabs[0].run();
        });
        yearlyTab.setOnClickListener(view -> {
            activeTab[0] = NOTE_TAB_YEARLY;
            renderTabs[0].run();
        });
        allTab.setOnClickListener(view -> {
            activeTab[0] = NOTE_TAB_ALL;
            renderTabs[0].run();
        });
        renderTabs[0].run();

        dialogBuilder("Date Notes")
                .setView(scrollView)
                .setPositiveButton("Save", (dialog, which) -> {
                    saveNote(date, false, dailyInput.getText().toString());
                    saveNote(date, true, yearlyInput.getText().toString());
                    persistCalendarNotes();
                    refreshNoteViews();
                    Toast.makeText(this, "Notes saved", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("All Notes", (dialog, which) -> showAllNotesView())
                .setNegativeButton("Close", null)
                .show();
    }

    protected void showAllNotesView() {
        diaryDailyFilter = null;
        diaryDailySearchQuery = "";
        diaryYearlySearchQuery = "";
        activeDiaryView = DIARY_VIEW_DAILY;
        if (activeScreen == SCREEN_DIARY) {
            showDiary(false, 0);
            return;
        }
        switchToScreen(SCREEN_DIARY, 1, true);
    }

    protected TextView noteTabButton(String text) {
        TextView tab = dialogText(text, Math.max(MIN_FONT_SIZE, fontSize() - 1));
        tab.setGravity(Gravity.CENTER);
        tab.setClickable(true);
        tab.setFocusable(true);
        tab.setSingleLine(false);
        tab.setPadding(dp(4), 0, dp(4), 0);
        attachButtonFeedback(tab);
        setButtonTooltip(tab, text);
        return tab;
    }

    protected void styleNoteTab(TextView tab, boolean active) {
        int fill = active ? accentColor() : dialogButtonColor();
        tab.setTextColor(active ? contrastText(accentColor()) : dialogTextColor());
        tab.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : boldTypeface());
        tab.setBackground(ripple(fill, strokeForColor(fill), dp(8)));
    }

    protected EditText noteInput(String text, String hint) {
        EditText input = new EditText(this);
        input.setText(text == null ? "" : text);
        input.setHint(hint);
        input.setHintTextColor(mix(dialogTextColor(), dialogButtonColor(), 0.46f));
        input.setTextColor(dialogTextColor());
        input.setTextSize(fontSize());
        input.setTypeface(Typeface.DEFAULT, boldTypeface());
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setSingleLine(false);
        input.setMinLines(4);
        input.setMaxLines(7);
        input.setBackground(ripple(dialogButtonColor(), strokeForColor(dialogButtonColor()), dp(8)));
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        return input;
    }

    protected void addNoteEditor(LinearLayout contentArea, EditText input, String label, int dotColor) {
        LinearLayout labelRow = new LinearLayout(this);
        labelRow.setOrientation(LinearLayout.HORIZONTAL);
        labelRow.setGravity(Gravity.CENTER_VERTICAL);
        addNoteMark(labelRow, dotColor);
        TextView labelView = dialogText(label, fontSize());
        labelView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        labelRow.addView(labelView, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));
        contentArea.addView(labelRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        if (input.getParent() instanceof ViewGroup) {
            ((ViewGroup) input.getParent()).removeView(input);
        }
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        inputParams.setMargins(0, dp(6), 0, dp(4));
        contentArea.addView(input, inputParams);
    }

    protected void addAllNotes(LinearLayout contentArea, LocalDate date, String dailyText, String yearlyText,
                             Runnable onDeleteDaily, Runnable onDeleteYearly) {
        boolean hasDaily = !dailyText.trim().isEmpty();
        boolean hasYearly = !yearlyText.trim().isEmpty();
        if (!hasDaily && !hasYearly) {
            TextView empty = dialogText("No notes for this date yet.", fontSize());
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(8), dp(18), dp(8), dp(18));
            contentArea.addView(empty, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            return;
        }
        if (hasDaily) {
            addNoteSummary(
                    contentArea,
                    new NoteListItem(
                            "0-" + dailyKey(date),
                            dailyKey(date),
                            false,
                            formatDate(date),
                            "Daily Note",
                            dailyText,
                            date,
                            NOTE_TAB_DAILY,
                            dailyNoteColor()
                    ),
                    onDeleteDaily
            );
        }
        if (hasYearly) {
            addNoteSummary(
                    contentArea,
                    new NoteListItem(
                            "1-" + yearlyKey(date),
                            yearlyKey(date),
                            true,
                            yearlyDateLabel(date),
                            "Yearly Note",
                            yearlyText,
                            date,
                            NOTE_TAB_YEARLY,
                            yearlyNoteColor()
                    ),
                    onDeleteYearly
            );
        }
    }

    protected void addNoteSummary(LinearLayout contentArea, NoteListItem item, Runnable onDelete) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(8), dp(8), dp(8));
        row.setBackground(ripple(dialogButtonColor(), strokeForColor(dialogButtonColor()), dp(8)));

        LinearLayout markColumn = new LinearLayout(this);
        markColumn.setGravity(Gravity.CENTER);
        addNoteMark(markColumn, item.dotColor);
        row.addView(markColumn, new LinearLayout.LayoutParams(dp(28), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        TextView title = dialogText(item.type + " - " + item.title, Math.max(11, fontSize() - 1));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        textColumn.addView(title);

        TextView body = dialogText(item.note.trim(), fontSize());
        body.setPadding(0, dp(3), 0, 0);
        textColumn.addView(body);
        row.addView(textColumn, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        ImageButton delete = dialogIconButton(R.drawable.ic_delete, "Delete " + item.type);
        delete.setOnClickListener(view -> confirmDeleteNote(item, onDelete));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        deleteParams.setMargins(dp(8), 0, 0, 0);
        row.addView(delete, deleteParams);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, dp(8));
        contentArea.addView(row, rowParams);
    }

    protected void saveNote(LocalDate date, boolean yearly, String value) {
        Map<String, String> target = yearly ? yearlyNotes : dailyNotes;
        String key = yearly ? yearlyKey(date) : dailyKey(date);
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            target.remove(key);
        } else {
            target.put(key, trimmed);
        }
    }

    protected void confirmDeleteNote(NoteListItem item, Runnable onDelete) {
        String message = "Delete " + item.type.toLowerCase(Locale.US) + " for " + item.title + "?";
        dialogBuilder("Delete Note")
                .setMessage(message)
                .setPositiveButton("Delete", (dialog, which) -> onDelete.run())
                .setNegativeButton("Cancel", null)
                .show();
    }

    protected void deleteNote(NoteListItem item) {
        if (item.yearly) {
            yearlyNotes.remove(item.noteKey);
        } else {
            dailyNotes.remove(item.noteKey);
        }
        persistCalendarNotes();
        refreshNoteViews();
    }

    protected void refreshNoteViews() {
        renderCalendar();
        if (activeScreen == SCREEN_DIARY) {
            showDiary(false, 0);
        }
    }

    protected boolean hasDailyNote(LocalDate date) {
        return dailyNotes.containsKey(dailyKey(date));
    }

    protected boolean hasYearlyNote(LocalDate date) {
        return yearlyNotes.containsKey(yearlyKey(date));
    }

    protected String dailyKey(LocalDate date) {
        return date.toString();
    }

    protected String yearlyKey(LocalDate date) {
        return String.format(Locale.US, "%02d-%02d", date.getMonthValue(), date.getDayOfMonth());
    }

    protected String yearlyDateLabel(LocalDate date) {
        return "Every "
                + date.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault())
                + " "
                + ordinal(date.getDayOfMonth());
    }

    protected void restoreCalendarNotes() {
        dailyNotes.clear();
        dailyNotes.putAll(readNoteMap(KEY_DAILY_NOTES));
        yearlyNotes.clear();
        yearlyNotes.putAll(readNoteMap(KEY_YEARLY_NOTES));
    }

    protected Map<String, String> readNoteMap(String key) {
        Map<String, String> notes = new HashMap<>();
        String stored = prefs.getString(key, "{}");
        try {
            JSONObject object = new JSONObject(stored);
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String noteKey = keys.next();
                String note = object.optString(noteKey, "").trim();
                if (!note.isEmpty()) {
                    notes.put(noteKey, note);
                }
            }
        } catch (JSONException ignored) {
        }
        return notes;
    }

    protected void persistCalendarNotes() {
        prefs.edit()
                .putString(KEY_DAILY_NOTES, writeNoteMap(dailyNotes))
                .putString(KEY_YEARLY_NOTES, writeNoteMap(yearlyNotes))
                .apply();
        JiffyWidgets.updateToday(this);
    }

    protected String writeNoteMap(Map<String, String> notes) {
        JSONObject object = new JSONObject();
        List<String> keys = new ArrayList<>(notes.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            try {
                object.put(key, notes.get(key));
            } catch (JSONException ignored) {
            }
        }
        return object.toString();
    }

    protected void showDateCalculator(LocalDate fromDate, LocalDate toDate) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(buildDateCalculatorView(fromDate, toDate, true));
        dialogBuilder("Calculate Dates")
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .show();
    }

    protected LinearLayout buildDateCalculatorView(LocalDate fromDate, LocalDate toDate, boolean inDialog) {
        LinearLayout layout = inDialog ? dialogLayout() : screenSectionLayout();
        TextView distanceTitle = calculatorSectionLabel("Distance", inDialog);
        TextView result = calculatorText("", fontSize() + 5, inDialog);
        result.setGravity(Gravity.CENTER);
        result.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        result.setPadding(dp(8), dp(10), dp(8), dp(10));
        result.setSingleLine(false);

        LocalDate[] start = new LocalDate[]{fromDate};
        LocalDate[] end = new LocalDate[]{toDate};
        LocalDate[] offsetBase = new LocalDate[]{inDialog || calculatorOffsetBaseDate == null ? fromDate : calculatorOffsetBaseDate};
        int[] offsetAmount = new int[]{inDialog ? 1 : Math.max(0, calculatorOffsetAmount)};
        int[] offsetUnit = new int[]{
                inDialog ? CALCULATOR_OFFSET_DAYS : normalizedCalculatorOffsetUnit(calculatorOffsetUnit)
        };
        int[] offsetDirection = new int[]{inDialog ? 1 : calculatorOffsetDirection};
        if (!inDialog) {
            calculatorFromDate = start[0];
            calculatorToDate = end[0];
            calculatorOffsetBaseDate = offsetBase[0];
        }

        CheckBox includeStart = calculatorCheckBox("Include Start Date", prefs.getBoolean(KEY_INCLUDE_START_DATE, false), inDialog);
        CheckBox excludeWeekends = calculatorCheckBox("Exclude Saturdays & Sundays", prefs.getBoolean(KEY_EXCLUDE_WEEKENDS, false), inDialog);
        TextView fromLabel = datePickerLabel("", inDialog);
        TextView toLabel = datePickerLabel("", inDialog);
        TextView offsetTitle = calculatorSectionLabel("Add / Subtract", inDialog);
        TextView offsetBaseLabel = datePickerLabel("", inDialog);
        EditText offsetAmountInput = calculatorNumberInput(String.valueOf(offsetAmount[0]), inDialog);
        TextView beforeButton = calculatorChoiceButton("Before", inDialog);
        TextView afterButton = calculatorChoiceButton("After", inDialog);
        TextView daysButton = calculatorChoiceButton("Days", inDialog);
        TextView weeksButton = calculatorChoiceButton("Weeks", inDialog);
        TextView monthsButton = calculatorChoiceButton("Months", inDialog);
        TextView offsetResult = calculatorText("", fontSize() + 2, inDialog);
        offsetResult.setGravity(Gravity.CENTER);
        offsetResult.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        offsetResult.setPadding(dp(8), dp(10), dp(8), dp(10));
        offsetResult.setSingleLine(false);

        layout.addView(distanceTitle);
        layout.addView(includeStart);
        layout.addView(excludeWeekends);
        layout.addView(result);
        layout.addView(fromLabel);
        layout.addView(toLabel);
        layout.addView(offsetTitle);
        layout.addView(offsetBaseLabel);

        LinearLayout amountRow = new LinearLayout(this);
        amountRow.setOrientation(LinearLayout.HORIZONTAL);
        amountRow.setGravity(Gravity.CENTER_VERTICAL);
        amountRow.setClipChildren(false);
        amountRow.setClipToPadding(false);
        amountRow.setPadding(0, dp(3), 0, dp(3));
        LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(0, dp(42), 0.9f);
        amountRow.addView(offsetAmountInput, amountParams);
        LinearLayout.LayoutParams beforeParams = new LinearLayout.LayoutParams(0, dp(42), 1f);
        beforeParams.setMargins(dp(6), 0, 0, 0);
        amountRow.addView(beforeButton, beforeParams);
        LinearLayout.LayoutParams afterParams = new LinearLayout.LayoutParams(0, dp(42), 1f);
        afterParams.setMargins(dp(6), 0, 0, 0);
        amountRow.addView(afterButton, afterParams);
        LinearLayout.LayoutParams amountRowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        amountRowParams.setMargins(0, dp(8), 0, dp(4));
        layout.addView(amountRow, amountRowParams);

        LinearLayout unitRow = new LinearLayout(this);
        unitRow.setOrientation(LinearLayout.HORIZONTAL);
        unitRow.setGravity(Gravity.CENTER_VERTICAL);
        unitRow.setClipChildren(false);
        unitRow.setClipToPadding(false);
        unitRow.setPadding(0, dp(3), 0, dp(3));
        TextView[] unitButtons = new TextView[]{daysButton, weeksButton, monthsButton};
        for (int index = 0; index < unitButtons.length; index++) {
            LinearLayout.LayoutParams unitParams = new LinearLayout.LayoutParams(0, dp(42), 1f);
            if (index > 0) {
                unitParams.setMargins(dp(6), 0, 0, 0);
            }
            unitRow.addView(unitButtons[index], unitParams);
        }
        LinearLayout.LayoutParams unitRowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        unitRowParams.setMargins(0, dp(4), 0, dp(4));
        layout.addView(unitRow, unitRowParams);
        layout.addView(offsetResult);

        Runnable[] update = new Runnable[1];
        Runnable[] updateOffset = new Runnable[1];
        Runnable[] styleOffsetControls = new Runnable[1];
        update[0] = () -> {
            boolean exclude = excludeWeekends.isChecked();
            boolean include = includeStart.isChecked();
            long days = countDaysBetween(start[0], end[0], exclude, include);
            fromLabel.setText("From: " + formatDate(start[0]));
            toLabel.setText("To: " + formatDate(end[0]));
            result.setText(formatDateDistance(days, exclude, start[0]));
        };
        updateOffset[0] = () -> {
            boolean exclude = excludeWeekends.isChecked();
            boolean include = includeStart.isChecked();
            offsetAmount[0] = calculatorOffsetAmountValue(offsetAmountInput.getText().toString());
            offsetUnit[0] = normalizedCalculatorOffsetUnit(offsetUnit[0]);
            long days = offsetAmountDays(offsetBase[0], offsetAmount[0], offsetUnit[0], offsetDirection[0]);
            LocalDate target = offsetTargetDate(offsetBase[0], offsetAmount[0], offsetUnit[0],
                    offsetDirection[0], exclude, include);
            offsetBaseLabel.setText("From: " + formatDate(offsetBase[0]));
            offsetResult.setText("Target: " + formatDate(target)
                    + "\n" + formatDateDistance(days, exclude, offsetBase[0]));
            if (!inDialog) {
                calculatorOffsetBaseDate = offsetBase[0];
                calculatorOffsetAmount = offsetAmount[0];
                calculatorOffsetUnit = offsetUnit[0];
                calculatorOffsetDirection = offsetDirection[0];
            }
        };
        styleOffsetControls[0] = () -> {
            styleCalculatorChoice(beforeButton, offsetDirection[0] < 0, inDialog);
            styleCalculatorChoice(afterButton, offsetDirection[0] > 0, inDialog);
            styleCalculatorChoice(daysButton, offsetUnit[0] == CALCULATOR_OFFSET_DAYS, inDialog);
            styleCalculatorChoice(weeksButton, offsetUnit[0] == CALCULATOR_OFFSET_WEEKS, inDialog);
            styleCalculatorChoice(monthsButton, offsetUnit[0] == CALCULATOR_OFFSET_MONTHS, inDialog);
        };

        includeStart.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(KEY_INCLUDE_START_DATE, checked).apply();
            update[0].run();
            updateOffset[0].run();
        });
        excludeWeekends.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(KEY_EXCLUDE_WEEKENDS, checked).apply();
            update[0].run();
            updateOffset[0].run();
        });
        fromLabel.setOnClickListener(view -> showDatePickerDialog("Pick From Date", start[0], date -> {
            start[0] = date;
            if (!inDialog) {
                calculatorFromDate = date;
                calculatorFromFollowsToday = false;
            }
            update[0].run();
        }));
        toLabel.setOnClickListener(view -> showDatePickerDialog("Pick To Date", end[0], date -> {
            end[0] = date;
            if (!inDialog) {
                calculatorToDate = date;
            }
            update[0].run();
        }));
        offsetBaseLabel.setOnClickListener(view -> showDatePickerDialog("Pick From Date", offsetBase[0], date -> {
            offsetBase[0] = date;
            if (!inDialog) {
                calculatorOffsetBaseDate = date;
                calculatorOffsetBaseFollowsToday = false;
            }
            updateOffset[0].run();
        }));
        offsetAmountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                updateOffset[0].run();
            }
        });
        beforeButton.setOnClickListener(view -> {
            offsetDirection[0] = -1;
            styleOffsetControls[0].run();
            updateOffset[0].run();
        });
        afterButton.setOnClickListener(view -> {
            offsetDirection[0] = 1;
            styleOffsetControls[0].run();
            updateOffset[0].run();
        });
        daysButton.setOnClickListener(view -> {
            offsetUnit[0] = CALCULATOR_OFFSET_DAYS;
            styleOffsetControls[0].run();
            updateOffset[0].run();
        });
        weeksButton.setOnClickListener(view -> {
            offsetUnit[0] = CALCULATOR_OFFSET_WEEKS;
            styleOffsetControls[0].run();
            updateOffset[0].run();
        });
        monthsButton.setOnClickListener(view -> {
            offsetUnit[0] = CALCULATOR_OFFSET_MONTHS;
            styleOffsetControls[0].run();
            updateOffset[0].run();
        });
        styleOffsetControls[0].run();
        update[0].run();
        updateOffset[0].run();

        return layout;
    }

    protected TextView calculatorSectionLabel(String text, boolean inDialog) {
        TextView label = calculatorText(text, fontSize() + 2, inDialog);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setGravity(Gravity.CENTER_VERTICAL);
        label.setPadding(dp(4), dp(14), dp(4), dp(4));
        return label;
    }

    protected EditText calculatorNumberInput(String text, boolean inDialog) {
        EditText input = new EditText(this);
        input.setText(text == null ? "" : text);
        input.setSelectAllOnFocus(true);
        input.setTextColor(inDialog ? dialogTextColor() : effectiveTextColor());
        input.setTextSize(fontSize());
        input.setTypeface(Typeface.DEFAULT, boldTypeface());
        input.setGravity(Gravity.CENTER);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setSingleLine(true);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});
        int background = inDialog ? dialogButtonColor() : surfaceColor();
        input.setBackground(ripple(background, strokeForColor(background), dp(8)));
        input.setPadding(dp(8), 0, dp(8), 0);
        applyRaisedSurface(input);
        return input;
    }

    protected TextView calculatorChoiceButton(String text, boolean inDialog) {
        TextView button = calculatorText(text, Math.max(MIN_FONT_SIZE, fontSize() - 2), inDialog);
        button.setGravity(Gravity.CENTER);
        button.setClickable(true);
        button.setFocusable(true);
        button.setSingleLine(false);
        button.setPadding(dp(4), 0, dp(4), 0);
        setButtonTooltip(button, text);
        attachButtonFeedback(button);
        return button;
    }

    protected void styleCalculatorChoice(TextView button, boolean active, boolean inDialog) {
        int fill = active ? accentColor() : inDialog ? dialogButtonColor() : surfaceColor();
        int textColor = active ? contrastText(accentColor()) : inDialog ? dialogTextColor() : effectiveTextColor();
        button.setTextColor(textColor);
        button.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : boldTypeface());
        button.setBackground(ripple(fill, strokeForColor(fill), dp(8)));
    }

    protected int calculatorOffsetAmountValue(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return 0;
        }
        try {
            return Math.max(0, Math.min(99999, Integer.parseInt(trimmed)));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    protected int normalizedCalculatorOffsetUnit(int unit) {
        switch (unit) {
            case CALCULATOR_OFFSET_WEEKS:
            case CALCULATOR_OFFSET_MONTHS:
                return unit;
            case CALCULATOR_OFFSET_DAYS:
            default:
                return CALCULATOR_OFFSET_DAYS;
        }
    }

    protected LocalDate offsetTargetDate(LocalDate baseDate, int amount, int unit, int direction,
                                       boolean excludeWeekends, boolean includeStartDate) {
        long days = offsetAmountDays(baseDate, amount, unit, direction);
        return plusCountedDays(baseDate, days, excludeWeekends, includeStartDate);
    }

    protected long offsetAmountDays(LocalDate baseDate, int amount, int unit, int direction) {
        long signedAmount = (long) amount * (long) direction;
        switch (unit) {
            case CALCULATOR_OFFSET_WEEKS:
                return signedAmount * 7L;
            case CALCULATOR_OFFSET_MONTHS:
                return ChronoUnit.DAYS.between(baseDate, baseDate.plusMonths(signedAmount));
            case CALCULATOR_OFFSET_DAYS:
            default:
                return signedAmount;
        }
    }

    protected LocalDate plusCountedDays(LocalDate date, long days, boolean excludeWeekends,
                                      boolean includeStartDate) {
        if (days == 0L) {
            return date;
        }
        int direction = days < 0L ? -1 : 1;
        long remaining = Math.abs(days);
        LocalDate result = date;

        if (includeStartDate && (!excludeWeekends || !isWeekend(result))) {
            remaining--;
        }

        while (remaining > 0L) {
            result = result.plusDays(direction);
            if (!excludeWeekends || !isWeekend(result)) {
                remaining--;
            }
        }
        return result;
    }

    protected long countDaysBetween(LocalDate from, LocalDate to, boolean excludeWeekends, boolean includeStartDate) {
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

    protected boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    protected String formatDateDistance(long days, boolean weekdaysOnly) {
        String unit = weekdaysOnly ? "weekday" : "day";
        long absolute = Math.abs(days);
        String units = absolute == 1 ? unit : unit + "s";
        if (days < 0) {
            return absolute + " " + units + " ago";
        }
        return absolute + " " + units + " remaining";
    }

    protected String formatDateDistance(long days, boolean weekdaysOnly, LocalDate from) {
        return formatDateDistance(days, weekdaysOnly) + " (" + formatCalendarDistance(from, days) + ")";
    }

    protected String formatCalendarDistance(LocalDate from, long days) {
        LocalDate to = from.plusDays(days);
        LocalDate earlier = days < 0 ? to : from;
        LocalDate later = days < 0 ? from : to;
        Period period = Period.between(earlier, later);
        return calendarPart(period.getYears(), "year")
                + " " + calendarPart(period.getMonths(), "month")
                + ", " + calendarPart(period.getDays(), "day");
    }

    protected String calendarPart(int value, String unit) {
        return value + " " + unit + (value == 1 ? "" : "s");
    }

    protected LocalDate pickerDate(DatePicker picker) {
        return LocalDate.of(picker.getYear(), picker.getMonth() + 1, picker.getDayOfMonth());
    }

    protected TextView calculatorText(String text, int sizeSp, boolean inDialog) {
        return inDialog ? dialogText(text, sizeSp) : plainText(text, sizeSp);
    }

    protected CheckBox calculatorCheckBox(String label, boolean checked, boolean inDialog) {
        return inDialog ? dialogCheckBox(label, checked) : optionCheckBox(label, checked);
    }

    protected TextView datePickerLabel(String text, boolean inDialog) {
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

    protected void showDatePickerDialog(String title, LocalDate initialDate, DatePicked onPicked) {
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

    protected void showJumpDialog() {
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

    protected List<NoteListItem> dailyNoteListItems(YearMonth monthFilter) {
        List<NoteListItem> items = new ArrayList<>();
        for (Map.Entry<String, String> entry : dailyNotes.entrySet()) {
            try {
                LocalDate date = LocalDate.parse(entry.getKey());
                if (monthFilter != null && !YearMonth.from(date).equals(monthFilter)) {
                    continue;
                }
                items.add(new NoteListItem(
                        date.toString(),
                        entry.getKey(),
                        false,
                        formatDate(date),
                        "Daily Note",
                        entry.getValue(),
                        date,
                        NOTE_TAB_DAILY,
                        dailyNoteColor()
                ));
            } catch (DateTimeException ignored) {
            }
        }
        Collections.sort(items, (left, right) -> left.sortKey.compareTo(right.sortKey));
        return items;
    }

    protected List<NoteListItem> filterNoteListItems(List<NoteListItem> items, String searchQuery) {
        List<String> tokens = searchTokens(searchQuery);
        if (tokens.isEmpty()) {
            return items;
        }
        List<NoteListItem> filtered = new ArrayList<>();
        for (NoteListItem item : items) {
            if (noteMatchesSearch(item, tokens)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    protected boolean noteMatchesSearch(NoteListItem item, List<String> tokens) {
        String haystack = normalizeSearchText(item.title + " " + item.note);
        for (String token : tokens) {
            if (!haystack.contains(token)) {
                return false;
            }
        }
        return true;
    }

    protected List<String> searchTokens(String searchQuery) {
        String normalized = normalizeSearchText(searchQuery);
        List<String> tokens = new ArrayList<>();
        if (normalized.isEmpty()) {
            return tokens;
        }
        String[] parts = normalized.split(" ");
        for (String part : parts) {
            if (!part.isEmpty()) {
                tokens.add(part);
            }
        }
        return tokens;
    }

    protected String normalizeSearchText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        String lower = text.toLowerCase(Locale.US);
        StringBuilder normalized = new StringBuilder();
        boolean previousSpace = true;
        for (int index = 0; index < lower.length(); ) {
            int value = lower.codePointAt(index);
            if (Character.isLetterOrDigit(value)) {
                normalized.appendCodePoint(value);
                previousSpace = false;
            } else if (isSearchableSymbol(value)) {
                if (!previousSpace) {
                    normalized.append(' ');
                }
                normalized.appendCodePoint(value);
                normalized.append(' ');
                previousSpace = true;
            } else if (!previousSpace) {
                normalized.append(' ');
                previousSpace = true;
            }
            index += Character.charCount(value);
        }
        return normalized.toString().trim();
    }

    protected boolean isSearchableSymbol(int value) {
        int type = Character.getType(value);
        return type == Character.MATH_SYMBOL
                || type == Character.CURRENCY_SYMBOL
                || type == Character.MODIFIER_SYMBOL
                || type == Character.OTHER_SYMBOL;
    }

    protected List<NoteListItem> yearlyNoteListItems() {
        List<NoteListItem> items = new ArrayList<>();
        for (Map.Entry<String, String> entry : yearlyNotes.entrySet()) {
            LocalDate date = dateFromYearlyKey(entry.getKey());
            String label = yearlyLabelFromKey(entry.getKey());
            if (date != null && label != null) {
                items.add(new NoteListItem(
                        entry.getKey(),
                        entry.getKey(),
                        true,
                        label,
                        "Yearly Note",
                        entry.getValue(),
                        date,
                        NOTE_TAB_YEARLY,
                        yearlyNoteColor()
                ));
            }
        }
        Collections.sort(items, (left, right) -> left.sortKey.compareTo(right.sortKey));
        return items;
    }

    protected void showDiary(boolean animate, int direction) {
        clockFace = null;
        clockDate = null;
        worldList = null;
        clearStopwatchUi();
        clearTimerUi();

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(backgroundColor());
        screen.setPadding(dp(14), dp(14), dp(14), dp(14));
        attachScreenCycleGesture(screen, false);

        screen.addView(screenTitle("Diary", R.drawable.ic_note_bookmark, false), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout diaryTabs = new LinearLayout(this);
        diaryTabs.setOrientation(LinearLayout.HORIZONTAL);
        diaryTabs.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams diaryTabsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        diaryTabsParams.setMargins(0, dp(10), 0, dp(8));
        screen.addView(diaryTabs, diaryTabsParams);

        LinearLayout dailyTab = diaryTabButton("Daily Notes", dailyNoteColor());
        LinearLayout yearlyTab = diaryTabButton("Yearly Notes", yearlyNoteColor());
        LinearLayout.LayoutParams dailyTabParams = new LinearLayout.LayoutParams(0, dp(42), 1f);
        dailyTabParams.setMargins(0, 0, dp(4), 0);
        diaryTabs.addView(dailyTab, dailyTabParams);
        LinearLayout.LayoutParams yearlyTabParams = new LinearLayout.LayoutParams(0, dp(42), 1f);
        yearlyTabParams.setMargins(dp(4), 0, 0, 0);
        diaryTabs.addView(yearlyTab, yearlyTabParams);

        ScrollView scrollView = new ScrollView(this);
        attachScreenCycleGesture(scrollView, false);
        LinearLayout list = screenSectionLayout();
        list.setPadding(0, dp(8), 0, dp(4));
        scrollView.addView(list);
        screen.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        Runnable[] renderDiary = new Runnable[1];
        renderDiary[0] = () -> {
            styleDiaryTab(dailyTab, activeDiaryView == DIARY_VIEW_DAILY);
            styleDiaryTab(yearlyTab, activeDiaryView == DIARY_VIEW_YEARLY);
            list.removeAllViews();

            if (activeDiaryView == DIARY_VIEW_YEARLY) {
                View currentMonthTarget = renderYearlyDiary(list, renderDiary[0]);
                if (currentMonthTarget != null && normalizeSearchQuery(diaryYearlySearchQuery).isEmpty()) {
                    scrollView.post(() -> scrollView.scrollTo(0, Math.max(0, currentMonthTarget.getTop() - dp(8))));
                } else {
                    scrollView.post(() -> scrollView.scrollTo(0, 0));
                }
            } else {
                renderDailyDiary(list, renderDiary[0]);
                scrollView.post(() -> scrollView.scrollTo(0, 0));
            }
            addScrollEndPadding(list);
        };

        dailyTab.setOnClickListener(view -> {
            activeDiaryView = DIARY_VIEW_DAILY;
            renderDiary[0].run();
        });
        yearlyTab.setOnClickListener(view -> {
            activeDiaryView = DIARY_VIEW_YEARLY;
            renderDiary[0].run();
        });
        renderDiary[0].run();

        setScreenContent(screen, animate, direction);
    }

    protected LinearLayout diaryTabButton(String text, int iconColor) {
        LinearLayout tab = new LinearLayout(this);
        tab.setOrientation(LinearLayout.HORIZONTAL);
        tab.setGravity(Gravity.CENTER);
        tab.setClickable(true);
        tab.setFocusable(true);
        tab.setPadding(dp(3), 0, dp(3), 0);
        attachButtonFeedback(tab);
        setButtonTooltip(tab, text);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_note_bookmark);
        icon.setColorFilter(iconColor);
        icon.setTag(iconColor);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        tab.addView(icon, new LinearLayout.LayoutParams(dp(16), dp(16)));

        TextView label = plainText(text, Math.max(11, fontSize() - 1));
        label.setIncludeFontPadding(false);
        label.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        labelParams.setMargins(dp(3), 0, 0, 0);
        tab.addView(label, labelParams);

        return tab;
    }

    protected void styleDiaryTab(LinearLayout tab, boolean active) {
        int fill = active ? accentColor() : surfaceColor();
        tab.setBackground(ripple(fill, active ? accentColor() : strokeColor(), dp(8)));
        int textColor = active ? contrastText(accentColor()) : effectiveTextColor();
        for (int i = 0; i < tab.getChildCount(); i++) {
            View child = tab.getChildAt(i);
            if (child instanceof TextView) {
                TextView label = (TextView) child;
                label.setTextColor(textColor);
                label.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : boldTypeface());
            } else if (child instanceof ImageView) {
                ImageView icon = (ImageView) child;
                Object iconColor = icon.getTag();
                icon.setColorFilter(active ? textColor : (Integer) iconColor);
            }
        }
    }

    protected void renderDailyDiary(LinearLayout list, Runnable onFilterChanged) {
        addDailyDiaryFilterBar(list, onFilterChanged);

        List<NoteListItem> unsearchedItems = dailyNoteListItems(diaryDailyFilter);
        List<NoteListItem> items = filterNoteListItems(unsearchedItems, diaryDailySearchQuery);
        if (unsearchedItems.isEmpty()) {
            String message = diaryDailyFilter == null
                    ? "No daily notes yet."
                    : "No daily notes in " + monthYearLabel(diaryDailyFilter) + ".";
            addDiaryEmptyMessage(list, message);
            return;
        }
        if (items.isEmpty()) {
            String message = dailySearchEmptyMessage(diaryDailySearchQuery, diaryDailyFilter);
            addDiaryEmptyMessage(list, message);
            return;
        }
        for (NoteListItem item : items) {
            addDiaryNoteRow(list, item);
        }
    }

    protected void addDailyDiaryFilterBar(LinearLayout list, Runnable onFilterChanged) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView pickMonth = actionButton(diaryDailyFilter == null ? "Jump to Month/Year" : monthYearLabel(diaryDailyFilter));
        pickMonth.setOnClickListener(view -> showDiaryMonthFilterDialog(
                diaryDailyFilter == null ? YearMonth.now() : diaryDailyFilter,
                month -> {
                    diaryDailyFilter = month;
                    onFilterChanged.run();
                }
        ));
        LinearLayout.LayoutParams pickParams = new LinearLayout.LayoutParams(0, dp(42), 1f);
        row.addView(pickMonth, pickParams);

        TextView search = actionButton("Search");
        if (!normalizeSearchQuery(diaryDailySearchQuery).isEmpty()) {
            setButtonTooltip(search, "Search: " + normalizeSearchQuery(diaryDailySearchQuery));
        }
        search.setOnClickListener(view -> showNoteSearchDialog(
                "Search Daily Notes",
                "Search Daily Notes",
                diaryDailySearchQuery,
                query -> {
                    diaryDailySearchQuery = query;
                    onFilterChanged.run();
                }));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(dp(92), dp(42));
        searchParams.setMargins(dp(8), 0, 0, 0);
        row.addView(search, searchParams);

        if (diaryDailyFilter != null || !normalizeSearchQuery(diaryDailySearchQuery).isEmpty()) {
            TextView clear = actionButton("Clear");
            clear.setOnClickListener(view -> {
                diaryDailyFilter = null;
                diaryDailySearchQuery = "";
                onFilterChanged.run();
            });
            LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(dp(82), dp(42));
            clearParams.setMargins(dp(8), 0, 0, 0);
            row.addView(clear, clearParams);
        }

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, dp(10));
        list.addView(row, rowParams);

        String query = normalizeSearchQuery(diaryDailySearchQuery);
        if (!query.isEmpty()) {
            addDiarySearchStatus(list, query);
        }
    }

    protected void addDiarySearchStatus(LinearLayout list, String searchQuery) {
        String query = normalizeSearchQuery(searchQuery);
        if (query.isEmpty()) {
            return;
        }
        TextView searchState = plainText("Search: " + query, Math.max(11, fontSize() - 1));
        searchState.setTextColor(mutedTextColor());
        searchState.setSingleLine(false);
        LinearLayout.LayoutParams stateParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        stateParams.setMargins(0, 0, 0, dp(10));
        list.addView(searchState, stateParams);
    }

    protected void showNoteSearchDialog(String title, String hint, String initialQuery, TextPicked onPicked) {
        LinearLayout layout = dialogLayout();
        EditText input = new EditText(this);
        input.setText(normalizeSearchQuery(initialQuery));
        input.setSelectAllOnFocus(true);
        input.setSingleLine(true);
        input.setHint(hint);
        input.setHintTextColor(mix(dialogTextColor(), dialogButtonColor(), 0.46f));
        input.setTextColor(dialogTextColor());
        input.setTextSize(fontSize());
        input.setTypeface(Typeface.DEFAULT, boldTypeface());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setBackground(ripple(dialogButtonColor(), strokeForColor(dialogButtonColor()), dp(8)));
        input.setPadding(dp(12), 0, dp(12), 0);
        layout.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44)
        ));

        dialogBuilder(title)
                .setView(layout)
                .setPositiveButton("Search", (dialog, which) -> onPicked.onPicked(normalizeSearchQuery(input.getText().toString())))
                .setNeutralButton("Clear", (dialog, which) -> onPicked.onPicked(""))
                .setNegativeButton("Cancel", null)
                .show();
    }

    protected String dailySearchEmptyMessage(String searchQuery, YearMonth monthFilter) {
        String query = normalizeSearchQuery(searchQuery);
        if (monthFilter == null) {
            return "No daily notes matching \"" + query + "\".";
        }
        return "No daily notes matching \"" + query + "\" in " + monthYearLabel(monthFilter) + ".";
    }

    protected String normalizeSearchQuery(String query) {
        return query == null ? "" : query.trim();
    }

    protected void showDiaryMonthFilterDialog(YearMonth initialMonth, MonthPicked onPicked) {
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
        month.setValue(initialMonth.getMonthValue());

        year.setMinValue(1900);
        year.setMaxValue(2200);
        year.setValue(initialMonth.getYear());

        layout.addView(dialogText("Month", fontSize()));
        layout.addView(month);
        layout.addView(dialogText("Year", fontSize()));
        layout.addView(year);

        dialogBuilder("Filter Daily Notes")
                .setView(layout)
                .setPositiveButton("Show", (dialog, which) -> onPicked.onPicked(YearMonth.of(year.getValue(), month.getValue())))
                .setNegativeButton("Cancel", null)
                .show();
    }

    protected View renderYearlyDiary(LinearLayout list, Runnable onSearchChanged) {
        addYearlyDiarySearchBar(list, onSearchChanged);

        List<NoteListItem> unsearchedItems = yearlyNoteListItems();
        List<NoteListItem> items = filterNoteListItems(unsearchedItems, diaryYearlySearchQuery);
        String query = normalizeSearchQuery(diaryYearlySearchQuery);
        boolean searching = !query.isEmpty();
        if (unsearchedItems.isEmpty()) {
            addDiaryEmptyMessage(list, "No yearly notes yet.");
            return null;
        }
        if (items.isEmpty()) {
            addDiaryEmptyMessage(list, yearlySearchEmptyMessage(diaryYearlySearchQuery));
            return null;
        }

        Map<Integer, List<NoteListItem>> itemsByMonth = new HashMap<>();
        for (NoteListItem item : items) {
            int[] monthDay = parseYearlyKey(item.noteKey);
            if (monthDay == null) {
                continue;
            }
            List<NoteListItem> monthItems = itemsByMonth.get(monthDay[0]);
            if (monthItems == null) {
                monthItems = new ArrayList<>();
                itemsByMonth.put(monthDay[0], monthItems);
            }
            monthItems.add(item);
        }

        int currentMonth = LocalDate.now().getMonthValue();
        ensureCurrentYearlyMonthExpanded(currentMonth);
        View currentMonthTarget = null;
        boolean renderedAnyNote = false;
        for (int monthValue = 1; monthValue <= 12; monthValue++) {
            List<NoteListItem> monthItems = itemsByMonth.get(monthValue);
            boolean isCurrentMonth = monthValue == currentMonth;
            if ((monthItems == null || monthItems.isEmpty()) && (!isCurrentMonth || searching)) {
                continue;
            }

            boolean expanded = searching || expandedYearlyMonths.contains(monthValue);
            int noteCount = monthItems == null ? 0 : monthItems.size();
            LinearLayout section = new LinearLayout(this);
            section.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(40)
            );
            headingParams.setMargins(0, list.getChildCount() == 0 ? 0 : dp(8), 0, dp(8));
            LinearLayout heading = yearlyMonthHeading(Month.of(monthValue), isCurrentMonth, noteCount, expanded);
            section.addView(heading, headingParams);

            LinearLayout monthNotes = new LinearLayout(this);
            monthNotes.setOrientation(LinearLayout.VERTICAL);
            monthNotes.setVisibility(expanded ? View.VISIBLE : View.GONE);
            section.addView(monthNotes, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            int toggledMonth = monthValue;
            heading.setOnClickListener(view -> {
                boolean showMonth = monthNotes.getVisibility() != View.VISIBLE;
                if (showMonth) {
                    expandedYearlyMonths.add(toggledMonth);
                } else {
                    expandedYearlyMonths.remove(toggledMonth);
                }
                monthNotes.setVisibility(showMonth ? View.VISIBLE : View.GONE);
                styleYearlyMonthHeading(heading, Month.of(toggledMonth), toggledMonth == currentMonth,
                        noteCount, showMonth);
            });

            list.addView(section, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            if (isCurrentMonth) {
                currentMonthTarget = section;
            }

            if (monthItems == null || monthItems.isEmpty()) {
                addDiaryEmptyMessage(monthNotes, items.isEmpty() ? "No yearly notes yet." : "No yearly notes this month.");
                continue;
            }
            renderedAnyNote = true;
            for (NoteListItem item : monthItems) {
                addDiaryNoteRow(monthNotes, item);
            }
        }

        if (!renderedAnyNote && currentMonthTarget == null) {
            addDiaryEmptyMessage(list, "No yearly notes yet.");
        }
        return currentMonthTarget;
    }

    protected void addYearlyDiarySearchBar(LinearLayout list, Runnable onSearchChanged) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView search = actionButton("Search");
        if (!normalizeSearchQuery(diaryYearlySearchQuery).isEmpty()) {
            setButtonTooltip(search, "Search: " + normalizeSearchQuery(diaryYearlySearchQuery));
        }
        search.setOnClickListener(view -> showNoteSearchDialog(
                "Search Yearly Notes",
                "Search Yearly Notes",
                diaryYearlySearchQuery,
                query -> {
                    diaryYearlySearchQuery = query;
                    onSearchChanged.run();
                }));
        row.addView(search, new LinearLayout.LayoutParams(0, dp(42), 1f));

        if (!normalizeSearchQuery(diaryYearlySearchQuery).isEmpty()) {
            TextView clear = actionButton("Clear");
            clear.setOnClickListener(view -> {
                diaryYearlySearchQuery = "";
                onSearchChanged.run();
            });
            LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(dp(82), dp(42));
            clearParams.setMargins(dp(8), 0, 0, 0);
            row.addView(clear, clearParams);
        }

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, dp(10));
        list.addView(row, rowParams);

        addDiarySearchStatus(list, diaryYearlySearchQuery);
    }

    protected String yearlySearchEmptyMessage(String searchQuery) {
        return "No yearly notes matching \"" + normalizeSearchQuery(searchQuery) + "\".";
    }

    protected void ensureCurrentYearlyMonthExpanded(int currentMonth) {
        if (yearlyExpandedSeedMonth == currentMonth) {
            return;
        }
        expandedYearlyMonths.clear();
        expandedYearlyMonths.add(currentMonth);
        yearlyExpandedSeedMonth = currentMonth;
    }

    protected LinearLayout yearlyMonthHeading(Month month, boolean current, int noteCount, boolean expanded) {
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setClickable(true);
        heading.setFocusable(true);
        heading.setPadding(dp(12), 0, dp(12), 0);
        attachButtonFeedback(heading);

        TextView label = plainText("", fontSize() + 2);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setPadding(0, 0, 0, 0);
        heading.addView(label, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        ImageView chevron = new ImageView(this);
        chevron.setImageResource(R.drawable.ic_chevron_right);
        heading.addView(chevron, new LinearLayout.LayoutParams(dp(22), dp(22)));

        styleYearlyMonthHeading(heading, month, current, noteCount, expanded);
        return heading;
    }

    protected void styleYearlyMonthHeading(LinearLayout heading, Month month, boolean current, int noteCount,
                                         boolean expanded) {
        String monthName = month.getDisplayName(TextStyle.FULL, Locale.getDefault());
        String labelText = noteCount > 0 ? monthName + " (" + noteCount + ")" : monthName;
        int fill = current ? accentColor() : backgroundColor();
        int color = current ? contrastText(accentColor()) : effectiveTextColor();
        heading.setBackground(ripple(fill, current ? accentColor() : strokeColor(), dp(8)));
        setButtonTooltip(heading, (expanded ? "Collapse " : "Expand ") + monthName);
        heading.setContentDescription((expanded ? "Collapse " : "Expand ") + monthName);
        for (int i = 0; i < heading.getChildCount(); i++) {
            View child = heading.getChildAt(i);
            if (child instanceof TextView) {
                TextView label = (TextView) child;
                label.setText(labelText);
                label.setTextColor(color);
                label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                label.setTextSize(Math.max(MIN_FONT_SIZE, fontSize() + 2));
            } else if (child instanceof ImageView) {
                ImageView chevron = (ImageView) child;
                chevron.setColorFilter(color);
                chevron.setRotation(expanded ? 90f : 0f);
            }
        }
    }

    protected void addDiaryEmptyMessage(LinearLayout list, String message) {
        TextView empty = plainText(message, fontSize());
        empty.setGravity(Gravity.CENTER);
        empty.setTextColor(mutedTextColor());
        empty.setPadding(dp(8), dp(28), dp(8), dp(28));
        list.addView(empty, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
    }

    protected String monthYearLabel(YearMonth month) {
        return month.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + month.getYear();
    }

    protected void addDiaryNoteRow(LinearLayout layout, NoteListItem item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClickable(true);
        row.setFocusable(true);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.setBackground(ripple(surfaceColor(), strokeColor(), dp(8)));
        setButtonTooltip(row, item.title);
        attachButtonFeedback(row);

        LinearLayout markColumn = new LinearLayout(this);
        markColumn.setGravity(Gravity.CENTER);
        addNoteMark(markColumn, item.dotColor);
        row.addView(markColumn, new LinearLayout.LayoutParams(dp(28), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        TextView title = plainText(item.title, Math.max(12, fontSize() - 1));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        textColumn.addView(title);

        TextView note = plainText(item.type + " - " + item.note, Math.max(11, fontSize() - 1));
        note.setTextColor(mutedTextColor());
        note.setPadding(0, dp(2), 0, 0);
        note.setMaxLines(2);
        textColumn.addView(note);
        row.addView(textColumn, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        ImageButton delete = iconButton(R.drawable.ic_delete, "Delete " + item.type);
        delete.setOnClickListener(view -> confirmDeleteNote(item, () -> deleteNote(item)));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        deleteParams.setMargins(dp(8), 0, 0, 0);
        row.addView(delete, deleteParams);

        row.setOnClickListener(view -> {
            visibleMonth = YearMonth.from(item.date);
            showNoteDialog(item.date, item.initialTab);
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(8));
        layout.addView(row, params);
    }

    protected LocalDate dateFromYearlyKey(String key) {
        int[] monthDay = parseYearlyKey(key);
        if (monthDay == null) {
            return null;
        }
        int year = visibleMonth == null ? LocalDate.now().getYear() : visibleMonth.getYear();
        YearMonth targetMonth = YearMonth.of(year, monthDay[0]);
        if (!targetMonth.isValidDay(monthDay[1])) {
            year = 2024;
            targetMonth = YearMonth.of(year, monthDay[0]);
        }
        if (!targetMonth.isValidDay(monthDay[1])) {
            return null;
        }
        return LocalDate.of(year, monthDay[0], monthDay[1]);
    }

    protected String yearlyLabelFromKey(String key) {
        int[] monthDay = parseYearlyKey(key);
        if (monthDay == null) {
            return null;
        }
        return "Every "
                + Month.of(monthDay[0]).getDisplayName(TextStyle.FULL, Locale.getDefault())
                + " "
                + ordinal(monthDay[1]);
    }

    protected int[] parseYearlyKey(String key) {
        String[] parts = key == null ? new String[0] : key.split("-");
        if (parts.length != 2) {
            return null;
        }
        try {
            int month = Integer.parseInt(parts[0]);
            int day = Integer.parseInt(parts[1]);
            if (month < 1 || month > 12 || day < 1 || day > 31) {
                return null;
            }
            YearMonth leapSafeMonth = YearMonth.of(2024, month);
            if (!leapSafeMonth.isValidDay(day)) {
                return null;
            }
            return new int[]{month, day};
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
