package com.souravgoswami.jiffy;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Set;

// Menu, customization, startup-screen, and about dialogs.
abstract class JiffySettingsActivity extends JiffyCalendarActivity {
    private static final int REQUEST_BACKUP_EXPORT = 403;
    private static final int REQUEST_BACKUP_IMPORT = 404;
    private static final DateTimeFormatter BACKUP_FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    protected int screenFromPreference() {
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
        if ("diary".equals(screen)) {
            return SCREEN_DIARY;
        }
        return SCREEN_CALENDAR;
    }

    protected String screenPreferenceValue(int screen) {
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
            case SCREEN_DIARY:
                return "diary";
            case SCREEN_CALENDAR:
            default:
                return "calendar";
        }
    }

    protected void showMainMenu(View anchor) {
        PopupWindow popup = new PopupWindow(this);
        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(0, dp(8), 0, dp(8));
        menu.setBackground(rounded(menuBackgroundColor(), strokeForColor(menuBackgroundColor()), dp(8)));

        addPopupMenuItem(menu, popup, "Backup Restore", R.drawable.ic_menu_backup_restore, this::showBackupRestoreDialog);
        addPopupMenuItem(menu, popup, "Customizability", R.drawable.ic_menu_customize, this::showCustomizationDialog);
        addPopupMenuItem(menu, popup, "About", R.drawable.ic_menu_about, this::showAboutDialog);
        addPopupMenuItem(menu, popup, "Exit", R.drawable.ic_menu_exit, this::exitApp);

        popup.setContentView(menu);
        popup.setWidth(dp(270));
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

    protected void addPopupMenuItem(LinearLayout menu, PopupWindow popup, String label, int iconRes, Runnable action) {
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

    protected void showBackupRestoreDialog() {
        LinearLayout layout = dialogLayout();
        final AlertDialog[] dialogRef = new AlertDialog[1];
        addDialogButton(layout, "Export Backup", R.drawable.ic_backup_export, view -> {
            if (dialogRef[0] != null) {
                dialogRef[0].dismiss();
            }
            startBackupExport();
        });
        addDialogButton(layout, "Restore Backup", R.drawable.ic_backup_restore, view -> {
            if (dialogRef[0] != null) {
                dialogRef[0].dismiss();
            }
            confirmBackupRestore();
        });

        dialogRef[0] = dialogBuilder("Backup Restore")
                .setView(layout)
                .setPositiveButton("Close", null)
                .create();
        dialogRef[0].show();
    }

    protected void startBackupExport() {
        persistAppStateForBackup();
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "jiffy-backup-" + LocalDateTime.now().format(BACKUP_FILE_STAMP) + ".json");
        try {
            startActivityForResult(intent, REQUEST_BACKUP_EXPORT);
        } catch (ActivityNotFoundException ignored) {
            Toast.makeText(this, "No file picker found.", Toast.LENGTH_SHORT).show();
        }
    }

    protected void confirmBackupRestore() {
        dialogBuilder("Restore Backup")
                .setMessage("Restore from a backup file? Current local Jiffy data will be replaced.")
                .setPositiveButton("Choose File", (dialog, which) -> startBackupImport())
                .setNegativeButton("Cancel", null)
                .show();
    }

    protected void startBackupImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/json", "text/plain"});
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_BACKUP_IMPORT);
        } catch (ActivityNotFoundException ignored) {
            Toast.makeText(this, "No file picker found.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_BACKUP_EXPORT) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                writeBackup(data.getData());
            }
            return;
        }
        if (requestCode == REQUEST_BACKUP_IMPORT) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                restoreBackup(data.getData());
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    protected void writeBackup(Uri uri) {
        try {
            writeText(uri, JiffyBackup.exportJson(this, prefs));
            Toast.makeText(this, "Backup saved.", Toast.LENGTH_SHORT).show();
        } catch (IOException | JSONException exception) {
            Toast.makeText(this, "Backup export failed.", Toast.LENGTH_LONG).show();
        }
    }

    protected void restoreBackup(Uri uri) {
        try {
            int restored = JiffyBackup.restoreJson(prefs, readText(uri));
            restoreAppAfterBackup(restored);
        } catch (IOException | JSONException exception) {
            Toast.makeText(this, "Backup restore failed.", Toast.LENGTH_LONG).show();
        }
    }

    protected void persistAppStateForBackup() {
        persistStopwatchState();
        persistTimerState();
        persistCalendarNotes();
    }

    protected void restoreAppAfterBackup(int restoredPreferenceCount) {
        TimerAlarmScheduler.cancel(this, prefs);
        stopService(new Intent(this, StopwatchForegroundService.class));
        stopService(new Intent(this, TimerForegroundService.class));
        ensureDefaults();
        restoreStopwatchState(null);
        restoreTimerState(null);
        restoreCalendarNotes();
        if (highlightedDates != null) {
            highlightedDates.clear();
        }
        observedDate = LocalDate.now();
        visibleMonth = YearMonth.from(observedDate);
        activeScreen = screenFromPreference();
        refreshUi();
        syncStopwatchForegroundService(false);
        syncTimerForegroundService(false);
        updateClockText();
        Toast.makeText(this, "Backup restored: " + restoredPreferenceCount + " items.", Toast.LENGTH_SHORT).show();
    }

    protected void writeText(Uri uri, String text) throws IOException {
        try (OutputStream stream = getContentResolver().openOutputStream(uri, "wt")) {
            if (stream == null) {
                throw new IOException("Unable to open backup file");
            }
            stream.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    protected String readText(Uri uri) throws IOException {
        try (InputStream stream = getContentResolver().openInputStream(uri)) {
            if (stream == null) {
                throw new IOException("Unable to open backup file");
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    protected void showCustomizationDialog() {
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

        addDialogButton(layout, "Widget Appearance", R.drawable.ic_tab_calendar, view -> showWidgetAppearanceDialog());

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

        addDialogButton(layout, "Daily Note Colour", R.drawable.ic_dialog_accent_colour, view -> showColorPicker(
                "Daily Note Colour",
                dailyNoteColor(),
                color -> {
                    prefs.edit().putInt(KEY_DAILY_NOTE_COLOR, color).apply();
                    refreshUi();
                }
        ));

        addDialogButton(layout, "Yearly Note Colour", R.drawable.ic_dialog_accent_colour, view -> showColorPicker(
                "Yearly Note Colour",
                yearlyNoteColor(),
                color -> {
                    prefs.edit().putInt(KEY_YEARLY_NOTE_COLOR, color).apply();
                    refreshUi();
                }
        ));

        addDialogButton(layout, "Reset Colours To Default", R.drawable.ic_dialog_reset_colours, view -> confirmResetColours());

        addDialogButton(layout, "Startup Screen", R.drawable.ic_menu_modes, view -> showModesDialog());
        addDialogButton(layout, "Date Format: " + formatDate(LocalDate.now()), R.drawable.ic_dialog_date_format, view -> showDateFormatDialog());

        TextView sizeLabel = dialogText("Font Size: " + fontSize() + "sp", fontSize());
        layout.addView(sizeLabel);
        SeekBar fontSize = new SeekBar(this);
        fontSize.setMax(MAX_FONT_SIZE - MIN_FONT_SIZE);
        fontSize.setProgress(fontSize() - MIN_FONT_SIZE);
        fontSize.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress + MIN_FONT_SIZE;
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
            JiffyWidgets.updateToday(this);
        });
        layout.addView(hour24);

        dialogBuilder("Customizability")
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .show();
    }

    protected void showWidgetAppearanceDialog() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = dialogLayout();
        scrollView.addView(layout);
        populateWidgetAppearanceLayout(layout);

        dialogBuilder("Widget Appearance")
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .show();
    }

    protected void populateWidgetAppearanceLayout(LinearLayout layout) {
        layout.removeAllViews();
        TextView widgetThemeLabel = dialogText("Theme", fontSize());
        widgetThemeLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        layout.addView(widgetThemeLabel);

        RadioGroup widgetThemeGroup = new RadioGroup(this);
        widgetThemeGroup.setOrientation(RadioGroup.VERTICAL);
        RadioButton system = radioButton("System", THEME_SYSTEM);
        RadioButton dark = radioButton("Dark Gray Mode", THEME_DARK_GRAY);
        RadioButton light = radioButton("Light Mode", THEME_LIGHT);
        RadioButton oled = radioButton("OLED Black Mode", THEME_OLED);
        widgetThemeGroup.addView(system);
        widgetThemeGroup.addView(dark);
        widgetThemeGroup.addView(light);
        widgetThemeGroup.addView(oled);
        widgetThemeGroup.check(themeToId(prefs.getInt(KEY_WIDGET_THEME, THEME_SYSTEM), system, dark, light, oled));
        widgetThemeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int theme = THEME_SYSTEM;
            if (checkedId == dark.getId()) {
                theme = THEME_DARK_GRAY;
            } else if (checkedId == light.getId()) {
                theme = THEME_LIGHT;
            } else if (checkedId == oled.getId()) {
                theme = THEME_OLED;
            }
            prefs.edit().putInt(KEY_WIDGET_THEME, theme).apply();
            JiffyWidgets.updateToday(this);
        });
        layout.addView(widgetThemeGroup);

        TextView widgetOptionsLabel = dialogText("Widget Options", fontSize());
        widgetOptionsLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        widgetOptionsLabel.setPadding(dp(4), dp(12), dp(4), dp(4));
        layout.addView(widgetOptionsLabel);

        CheckBox transparentWidget = dialogCheckBox(
                "Transparent Background",
                prefs.getBoolean(KEY_WIDGET_TRANSPARENT, false)
        );
        transparentWidget.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(KEY_WIDGET_TRANSPARENT, checked).apply();
            JiffyWidgets.updateToday(this);
        });
        layout.addView(transparentWidget);

        CheckBox hideWidgetButtons = dialogCheckBox(
                "Hide Buttons",
                prefs.getBoolean(KEY_WIDGET_HIDE_BUTTONS, false)
        );
        hideWidgetButtons.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(KEY_WIDGET_HIDE_BUTTONS, checked).apply();
            JiffyWidgets.updateToday(this);
        });
        layout.addView(hideWidgetButtons);

        CheckBox disableWidgetTapLaunch = dialogCheckBox(
                "Disable Widget Tap Launch",
                prefs.getBoolean(KEY_WIDGET_DISABLE_ROOT_LAUNCH, false)
        );
        disableWidgetTapLaunch.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(KEY_WIDGET_DISABLE_ROOT_LAUNCH, checked).apply();
            JiffyWidgets.updateToday(this);
        });
        layout.addView(disableWidgetTapLaunch);

        CheckBox showSeconds = dialogCheckBox(
                "Show Seconds",
                prefs.getBoolean(KEY_WIDGET_SHOW_SECONDS, true)
        );
        showSeconds.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(KEY_WIDGET_SHOW_SECONDS, checked).apply();
            JiffyWidgets.updateToday(this);
        });
        layout.addView(showSeconds);

        TextView textBlockPositionLabel = dialogText("Text Block Position", fontSize());
        textBlockPositionLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        textBlockPositionLabel.setPadding(dp(4), dp(12), dp(4), dp(4));
        layout.addView(textBlockPositionLabel);

        RadioGroup textDirectionGroup = new RadioGroup(this);
        textDirectionGroup.setOrientation(RadioGroup.VERTICAL);
        RadioButton leftBlock = radioButton("Text Block On Left", WIDGET_TEXT_ALIGNMENT_LEFT);
        RadioButton rightBlock = radioButton("Text Block On Right", WIDGET_TEXT_ALIGNMENT_RIGHT);
        textDirectionGroup.addView(leftBlock);
        textDirectionGroup.addView(rightBlock);
        int textAlignment = prefs.getInt(KEY_WIDGET_TEXT_ALIGNMENT, WIDGET_TEXT_ALIGNMENT_LEFT);
        textDirectionGroup.check(textAlignment == WIDGET_TEXT_ALIGNMENT_LEFT ? leftBlock.getId() : rightBlock.getId());
        textDirectionGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int selected = checkedId == leftBlock.getId()
                    ? WIDGET_TEXT_ALIGNMENT_LEFT
                    : WIDGET_TEXT_ALIGNMENT_RIGHT;
            prefs.edit().putInt(KEY_WIDGET_TEXT_ALIGNMENT, selected).apply();
            JiffyWidgets.updateToday(this);
        });
        layout.addView(textDirectionGroup);

        TextView buttonOptionsLabel = dialogText("Button and Colour Options", fontSize());
        buttonOptionsLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        buttonOptionsLabel.setPadding(dp(4), dp(12), dp(4), dp(4));
        layout.addView(buttonOptionsLabel);

        CheckBox hideButtonFill = dialogCheckBox(
                "Hide Button Fill",
                prefs.getBoolean(KEY_WIDGET_BUTTON_FILL_HIDDEN, false)
        );
        hideButtonFill.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(KEY_WIDGET_BUTTON_FILL_HIDDEN, checked).apply();
            JiffyWidgets.updateToday(this);
        });
        layout.addView(hideButtonFill);

        CheckBox buttonBorders = dialogCheckBox(
                "Show Button Borders",
                prefs.getBoolean(KEY_WIDGET_BUTTON_BORDER_ENABLED, true)
        );
        buttonBorders.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(KEY_WIDGET_BUTTON_BORDER_ENABLED, checked).apply();
            JiffyWidgets.updateToday(this);
        });
        layout.addView(buttonBorders);

        addWidgetColorButton(layout, "Date Text Colour", KEY_WIDGET_TEXT_COLOR, widgetDefaultTextColor());
        addWidgetColorButton(layout, "Detail Text Colour", KEY_WIDGET_DETAIL_TEXT_COLOR, widgetDefaultMutedTextColor());
        addWidgetColorButton(layout, "Accent Text Colour", KEY_WIDGET_ACCENT_COLOR, widgetDefaultAccentColor());
        addWidgetColorButton(layout, "Button Text Colour", KEY_WIDGET_BUTTON_TEXT_COLOR, widgetButtonTextColor());
        addWidgetColorButton(layout, "Button Fill Colour", KEY_WIDGET_BUTTON_FILL_COLOR, widgetButtonFillColor());
        addWidgetColorButton(layout, "Button Border Colour", KEY_WIDGET_BUTTON_BORDER_COLOR, widgetButtonBorderColor());
        addDialogButton(layout, "Reset Widget Appearance", R.drawable.ic_dialog_reset_colours,
                view -> confirmResetWidgetAppearance(() -> populateWidgetAppearanceLayout(layout)));
    }

    protected void addWidgetColorButton(LinearLayout layout, String label, String key, int fallback) {
        addDialogButton(layout, label, R.drawable.ic_dialog_accent_colour, view -> showColorPicker(
                label,
                prefs.getInt(key, fallback),
                color -> {
                    prefs.edit().putInt(key, color).apply();
                    JiffyWidgets.updateToday(this);
                }
        ));
    }

    protected int widgetButtonTextColor() {
        return prefs.getInt(
                KEY_WIDGET_BUTTON_TEXT_COLOR,
                prefs.getInt(KEY_WIDGET_ACCENT_COLOR, widgetDefaultAccentColor())
        );
    }

    protected int widgetButtonFillColor() {
        return prefs.getInt(KEY_WIDGET_BUTTON_FILL_COLOR, widgetDefaultSurfaceColor());
    }

    protected int widgetButtonBorderColor() {
        int fallback = prefs.getBoolean(KEY_WIDGET_TRANSPARENT, false) ? widgetButtonTextColor() : widgetDefaultStrokeColor();
        return prefs.getInt(KEY_WIDGET_BUTTON_BORDER_COLOR, fallback);
    }

    protected void confirmResetWidgetAppearance(Runnable onReset) {
        dialogBuilder("Reset Widget Appearance")
                .setMessage("Reset widget colours and button options to default?")
                .setPositiveButton("Reset", (dialog, which) -> {
                    prefs.edit()
                            .remove(KEY_WIDGET_THEME)
                            .remove(KEY_WIDGET_TRANSPARENT)
                            .remove(KEY_WIDGET_HIDE_BUTTONS)
                            .remove(KEY_WIDGET_DISABLE_ROOT_LAUNCH)
                            .remove(KEY_WIDGET_SHOW_SECONDS)
                            .remove(KEY_WIDGET_TEXT_ALIGNMENT)
                            .remove(KEY_WIDGET_BUTTON_FILL_HIDDEN)
                            .remove(KEY_WIDGET_BUTTON_BORDER_ENABLED)
                            .remove(KEY_WIDGET_TEXT_COLOR)
                            .remove(KEY_WIDGET_DETAIL_TEXT_COLOR)
                            .remove(KEY_WIDGET_ACCENT_COLOR)
                            .remove(KEY_WIDGET_BUTTON_TEXT_COLOR)
                            .remove(KEY_WIDGET_BUTTON_FILL_COLOR)
                            .remove(KEY_WIDGET_BUTTON_BORDER_COLOR)
                            .apply();
                    JiffyWidgets.updateToday(this);
                    onReset.run();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    protected void confirmResetColours() {
        dialogBuilder("Reset Colours")
                .setMessage("Reset text, accent, daily note, and yearly note colours to default?")
                .setPositiveButton("Reset", (dialog, which) -> {
                    prefs.edit()
                            .putBoolean(KEY_CUSTOM_TEXT_COLOR, false)
                            .remove(KEY_TEXT_COLOR)
                            .remove(KEY_ACCENT_COLOR)
                            .remove(KEY_DAILY_NOTE_COLOR)
                            .remove(KEY_YEARLY_NOTE_COLOR)
                            .apply();
                    refreshUi();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    protected void showDateFormatDialog() {
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

    protected int themeToId(int theme, RadioButton system, RadioButton dark, RadioButton light, RadioButton oled) {
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

    protected RadioButton radioButton(String text, int tag) {
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

    protected void addDialogButton(LinearLayout layout, String text, View.OnClickListener listener) {
        addDialogButton(layout, text, 0, listener);
    }

    protected void addDialogButton(LinearLayout layout, String text, int iconRes, View.OnClickListener listener) {
        View button = iconRes == 0 ? dialogActionButton(text) : dialogActionButton(text, iconRes);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44)
        );
        params.setMargins(0, dp(8), 0, 0);
        layout.addView(button, params);
    }

    protected void showColorPicker(String title, int initialColor, ColorPicked callback) {
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

    protected SeekBar colorSeek(int value) {
        SeekBar seek = new SeekBar(this);
        seek.setMax(255);
        seek.setProgress(value);
        return seek;
    }

    protected void addColorRow(LinearLayout layout, String label, SeekBar seekBar) {
        TextView text = dialogText(label, fontSize());
        layout.addView(text);
        layout.addView(seekBar);
    }

    protected void showModesDialog() {
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);
        group.setPadding(dp(18), dp(8), dp(18), dp(8));

        RadioButton calendar = radioButton("Open Calendar First", SCREEN_CALENDAR);
        RadioButton clock = radioButton("Open Clock First", SCREEN_CLOCK);
        RadioButton calculator = radioButton("Open Date Calc First", SCREEN_CALCULATOR);
        RadioButton world = radioButton("Open World First", SCREEN_WORLD);
        RadioButton stopwatch = radioButton("Open Stopwatch First", SCREEN_STOPWATCH);
        RadioButton timer = radioButton("Open Timer First", SCREEN_TIMER);
        RadioButton diary = radioButton("Open Diary First", SCREEN_DIARY);
        group.addView(calendar);
        group.addView(clock);
        group.addView(calculator);
        group.addView(world);
        group.addView(stopwatch);
        group.addView(timer);
        group.addView(diary);
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

    protected void showAboutDialog() {
        String version = "0.0.5";
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

        TextView about = dialogText(
                "Jiffy is an offline calendar, date calculator, clock, stopwatch, timer, and world time app. It supports date-specific diary notes, daily and yearly notes, day counting, lap timing, countdowns, and local/world times with DST-aware time zones.",
                fontSize()
        );

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

    protected void exitApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        } else {
            finish();
        }
    }
}
