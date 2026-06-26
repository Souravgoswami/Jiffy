package com.souravgoswami.jiffy;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

final class JiffyWidgets {
    private static final int REQUEST_TODAY_CALENDAR = 2201;
    private static final int REQUEST_TODAY_DIARY = 2202;
    private static final int REQUEST_TODAY_DISABLED_ROOT = 2203;
    private static final String ACTION_TODAY_DISABLED_ROOT =
            "com.souravgoswami.jiffy.action.TODAY_DISABLED_ROOT";

    private JiffyWidgets() {
    }

    static void updateAll(Context context) {
        updateToday(context);
    }

    static void updateToday(Context context) {
        update(context, JiffyTodayWidgetProvider.class);
    }

    static RemoteViews todayViews(Context context) {
        SharedPreferences prefs = prefs(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.jiffy_today_widget);
        WidgetColors colors = widgetColors(context, prefs);
        LocalDate today = LocalDate.now();

        views.setInt(R.id.widget_today_root, "setBackgroundResource", colors.backgroundRes);
        views.setTextColor(R.id.widget_today_label, colors.accent);
        views.setTextColor(R.id.widget_today_day, colors.text);
        views.setTextColor(R.id.widget_today_month_year, colors.text);
        views.setTextColor(R.id.widget_today_weekday, colors.muted);
        views.setTextColor(R.id.widget_today_time, colors.accent);
        views.setTextColor(R.id.widget_today_full_date, colors.muted);
        views.setTextColor(R.id.widget_today_calendar_button_label, colors.buttonText);
        views.setTextColor(R.id.widget_today_diary_button_label, colors.buttonText);
        views.setInt(R.id.widget_today_calendar_button, "setBackgroundResource", R.drawable.widget_button_background_transparent);
        views.setInt(R.id.widget_today_diary_button, "setBackgroundResource", R.drawable.widget_button_background_transparent);
        applyTextAlignment(views, prefs);
        applyButtonSurface(context, views, colors);
        views.setViewVisibility(
                R.id.widget_today_button_row,
                prefs.getBoolean(JiffyActivityBase.KEY_WIDGET_HIDE_BUTTONS, false) ? View.GONE : View.VISIBLE
        );

        views.setTextViewText(R.id.widget_today_day, String.valueOf(today.getDayOfMonth()));
        views.setTextViewText(
                R.id.widget_today_month_year,
                today.getMonth().getDisplayName(TextStyle.FULL, Locale.US) + " " + today.getYear()
        );
        views.setTextViewText(R.id.widget_today_weekday, today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.US));
        views.setTextViewText(
                R.id.widget_today_full_date,
                formatDate(today, prefs.getInt(JiffyActivityBase.KEY_DATE_FORMAT, JiffyActivityBase.DATE_FORMAT_DMY_ORDINAL))
        );
        applyNoteMarkers(views, prefs, today);
        boolean showSeconds = prefs.getBoolean(JiffyActivityBase.KEY_WIDGET_SHOW_SECONDS, true);
        String timeFormat;
        if (prefs.getBoolean(JiffyActivityBase.KEY_24_HOUR, false)) {
            timeFormat = showSeconds ? "HH:mm:ss" : "HH:mm";
        } else {
            timeFormat = showSeconds ? "hh:mm:ss a" : "hh:mm a";
        }
        views.setCharSequence(R.id.widget_today_time, "setFormat12Hour", timeFormat);
        views.setCharSequence(R.id.widget_today_time, "setFormat24Hour", timeFormat);

        views.setOnClickPendingIntent(
                R.id.widget_today_root,
                prefs.getBoolean(JiffyActivityBase.KEY_WIDGET_DISABLE_ROOT_LAUNCH, false)
                        ? disabledRootPendingIntent(context)
                        : openScreenPendingIntent(context, JiffyActivityBase.SCREEN_CALENDAR, REQUEST_TODAY_CALENDAR)
        );
        views.setOnClickPendingIntent(
                R.id.widget_today_calendar_button,
                openScreenPendingIntent(context, JiffyActivityBase.SCREEN_CALENDAR, REQUEST_TODAY_CALENDAR)
        );
        views.setOnClickPendingIntent(
                R.id.widget_today_diary_button,
                openScreenPendingIntent(context, JiffyActivityBase.SCREEN_DIARY, REQUEST_TODAY_DIARY)
        );
        return views;
    }

    private static void update(Context context, Class<?> providerClass) {
        if (context == null) {
            return;
        }
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, providerClass));
        if (ids == null || ids.length == 0) {
            return;
        }
        if (providerClass == JiffyTodayWidgetProvider.class) {
            JiffyTodayWidgetProvider.updateWidgets(context, manager, ids);
        }
    }

    private static PendingIntent openScreenPendingIntent(Context context, int screen, int requestCode) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(MainActivity.EXTRA_SCREEN, screen);
        return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent disabledRootPendingIntent(Context context) {
        Intent intent = new Intent(context, JiffyTodayWidgetProvider.class);
        intent.setAction(ACTION_TODAY_DISABLED_ROOT);
        return PendingIntent.getBroadcast(
                context,
                REQUEST_TODAY_DISABLED_ROOT,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(JiffyActivityBase.PREFS, Context.MODE_PRIVATE);
    }

    private static void applyButtonSurface(Context context, RemoteViews views, WidgetColors colors) {
        Bitmap surface = roundedButtonBitmap(
                context,
                colors.buttonFill,
                colors.buttonBorder,
                colors.buttonBorderVisible
        );
        views.setImageViewBitmap(R.id.widget_today_calendar_button_surface, surface);
        views.setImageViewBitmap(R.id.widget_today_diary_button_surface, surface);
    }

    private static void applyTextAlignment(RemoteViews views, SharedPreferences prefs) {
        int alignment = prefs.getInt(
                JiffyActivityBase.KEY_WIDGET_TEXT_ALIGNMENT,
                JiffyActivityBase.WIDGET_TEXT_ALIGNMENT_LEFT
        );
        boolean stickLeft = alignment == JiffyActivityBase.WIDGET_TEXT_ALIGNMENT_LEFT;

        views.setViewVisibility(R.id.widget_today_text_left_spacer, stickLeft ? View.GONE : View.VISIBLE);
        views.setViewVisibility(R.id.widget_today_text_right_spacer, stickLeft ? View.VISIBLE : View.GONE);
        views.setInt(R.id.widget_today_label, "setGravity", Gravity.LEFT);
        views.setInt(R.id.widget_today_month_year, "setGravity", Gravity.LEFT);
        views.setInt(R.id.widget_today_weekday, "setGravity", Gravity.LEFT);
        views.setInt(R.id.widget_today_full_date, "setGravity", Gravity.LEFT);
        views.setInt(R.id.widget_today_time, "setGravity", Gravity.LEFT);
    }

    private static Bitmap roundedButtonBitmap(Context context, int fill, int border, boolean showBorder) {
        float density = context.getResources().getDisplayMetrics().density;
        int width = Math.round(180f * density);
        int height = Math.round(30f * density);
        float stroke = Math.max(1f, density);
        float radius = 8f * density;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        float inset = showBorder ? stroke / 2f : 0f;
        RectF rect = new RectF(inset, inset, width - inset, height - inset);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fill);
        canvas.drawRoundRect(rect, radius, radius, paint);

        if (showBorder) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setColor(border);
            canvas.drawRoundRect(rect, radius, radius, paint);
        }
        return bitmap;
    }

    private static void applyNoteMarkers(RemoteViews views, SharedPreferences prefs, LocalDate date) {
        boolean hasDaily = hasStoredNote(prefs, JiffyActivityBase.KEY_DAILY_NOTES, date.toString());
        boolean hasYearly = hasStoredNote(prefs, JiffyActivityBase.KEY_YEARLY_NOTES, yearlyKey(date));

        views.setViewVisibility(R.id.widget_today_note_markers, hasDaily || hasYearly ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.widget_today_daily_marker, hasDaily ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.widget_today_yearly_marker, hasYearly ? View.VISIBLE : View.GONE);
        views.setInt(
                R.id.widget_today_daily_marker,
                "setColorFilter",
                prefs.getInt(JiffyActivityBase.KEY_DAILY_NOTE_COLOR, JiffyActivityBase.DEFAULT_DAILY_NOTE_COLOR)
        );
        views.setInt(
                R.id.widget_today_yearly_marker,
                "setColorFilter",
                prefs.getInt(JiffyActivityBase.KEY_YEARLY_NOTE_COLOR, JiffyActivityBase.DEFAULT_YEARLY_NOTE_COLOR)
        );
    }

    private static boolean hasStoredNote(SharedPreferences prefs, String prefKey, String noteKey) {
        try {
            String note = new JSONObject(prefs.getString(prefKey, "{}")).optString(noteKey, "").trim();
            return !note.isEmpty();
        } catch (JSONException ignored) {
            return false;
        }
    }

    private static String yearlyKey(LocalDate date) {
        return String.format(Locale.US, "%02d-%02d", date.getMonthValue(), date.getDayOfMonth());
    }

    private static String formatDate(LocalDate date, int format) {
        String weekday = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.US);
        String month = date.getMonth().getDisplayName(TextStyle.FULL, Locale.US);
        String ordinal = ordinal(date.getDayOfMonth());
        String dmy = ordinal + " " + month + ", " + date.getYear();
        switch (format) {
            case JiffyActivityBase.DATE_FORMAT_WEEKDAY_DMY_ORDINAL:
                return weekday + ", " + dmy;
            case JiffyActivityBase.DATE_FORMAT_MDY_ORDINAL:
                return month + " " + ordinal + ", " + date.getYear();
            case JiffyActivityBase.DATE_FORMAT_WEEKDAY_MDY_ORDINAL:
                return weekday + ", " + month + " " + ordinal + " " + date.getYear();
            case JiffyActivityBase.DATE_FORMAT_NUMERIC_DMY:
                return String.format(Locale.US, "%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
            case JiffyActivityBase.DATE_FORMAT_NUMERIC_MDY:
                return String.format(Locale.US, "%02d/%02d/%04d", date.getMonthValue(), date.getDayOfMonth(), date.getYear());
            case JiffyActivityBase.DATE_FORMAT_ISO:
                return date.toString();
            case JiffyActivityBase.DATE_FORMAT_DMY_ORDINAL:
            default:
                return dmy;
        }
    }

    private static String ordinal(int day) {
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

    private static WidgetColors widgetColors(Context context, SharedPreferences prefs) {
        int theme = activeWidgetTheme(context, prefs);
        int text = effectiveTextColor(theme);
        int backgroundRes;
        int muted;
        boolean transparent = prefs.getBoolean(JiffyActivityBase.KEY_WIDGET_TRANSPARENT, false);
        int themeButtonFill;
        int themeButtonBorder;
        if (theme == JiffyActivityBase.THEME_LIGHT) {
            backgroundRes = R.drawable.widget_background_light;
            muted = mix(text, Color.WHITE, 0.58f);
            themeButtonFill = Color.rgb(238, 242, 250);
            themeButtonBorder = Color.rgb(216, 221, 232);
        } else if (theme == JiffyActivityBase.THEME_DARK_GRAY) {
            backgroundRes = R.drawable.widget_background_dark;
            muted = Color.rgb(168, 179, 209);
            themeButtonFill = Color.rgb(31, 42, 58);
            themeButtonBorder = Color.rgb(42, 53, 80);
        } else {
            backgroundRes = R.drawable.widget_background_oled;
            muted = mix(text, Color.BLACK, 0.62f);
            themeButtonFill = Color.rgb(17, 17, 17);
            themeButtonBorder = Color.rgb(51, 51, 51);
        }
        if (transparent) {
            backgroundRes = R.drawable.widget_background_transparent;
            themeButtonFill = Color.TRANSPARENT;
        }
        int accent = Color.rgb(106, 148, 255);
        text = prefs.getInt(JiffyActivityBase.KEY_WIDGET_TEXT_COLOR, text);
        muted = prefs.getInt(JiffyActivityBase.KEY_WIDGET_DETAIL_TEXT_COLOR, muted);
        accent = prefs.getInt(JiffyActivityBase.KEY_WIDGET_ACCENT_COLOR, accent);
        int buttonText = prefs.getInt(JiffyActivityBase.KEY_WIDGET_BUTTON_TEXT_COLOR, accent);
        int buttonFill = prefs.getInt(JiffyActivityBase.KEY_WIDGET_BUTTON_FILL_COLOR, themeButtonFill);
        if (prefs.getBoolean(JiffyActivityBase.KEY_WIDGET_BUTTON_FILL_HIDDEN, false)) {
            buttonFill = Color.TRANSPARENT;
        }
        int defaultBorder = transparent ? buttonText : themeButtonBorder;
        int buttonBorder = prefs.getInt(JiffyActivityBase.KEY_WIDGET_BUTTON_BORDER_COLOR, defaultBorder);
        boolean buttonBorderVisible = prefs.getBoolean(JiffyActivityBase.KEY_WIDGET_BUTTON_BORDER_ENABLED, true);
        return new WidgetColors(
                backgroundRes,
                text,
                muted,
                accent,
                buttonText,
                buttonFill,
                buttonBorder,
                buttonBorderVisible
        );
    }

    private static int activeWidgetTheme(Context context, SharedPreferences prefs) {
        int selected = prefs.getInt(JiffyActivityBase.KEY_WIDGET_THEME, JiffyActivityBase.THEME_SYSTEM);
        if (selected != JiffyActivityBase.THEME_SYSTEM) {
            return selected;
        }
        int nightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightMode == Configuration.UI_MODE_NIGHT_NO) {
            return JiffyActivityBase.THEME_LIGHT;
        }
        if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            return JiffyActivityBase.THEME_DARK_GRAY;
        }
        return JiffyActivityBase.THEME_DARK_GRAY;
    }

    private static int effectiveTextColor(int theme) {
        if (theme == JiffyActivityBase.THEME_LIGHT) {
            return Color.rgb(28, 28, 28);
        }
        if (theme == JiffyActivityBase.THEME_DARK_GRAY) {
            return Color.rgb(233, 237, 246);
        }
        return Color.WHITE;
    }

    private static int mix(int foreground, int background, float amount) {
        int red = Math.round(Color.red(background) + (Color.red(foreground) - Color.red(background)) * amount);
        int green = Math.round(Color.green(background) + (Color.green(foreground) - Color.green(background)) * amount);
        int blue = Math.round(Color.blue(background) + (Color.blue(foreground) - Color.blue(background)) * amount);
        return Color.rgb(red, green, blue);
    }

    private static final class WidgetColors {
        final int backgroundRes;
        final int text;
        final int muted;
        final int accent;
        final int buttonText;
        final int buttonFill;
        final int buttonBorder;
        final boolean buttonBorderVisible;

        WidgetColors(int backgroundRes, int text, int muted, int accent, int buttonText,
                     int buttonFill, int buttonBorder, boolean buttonBorderVisible) {
            this.backgroundRes = backgroundRes;
            this.text = text;
            this.muted = muted;
            this.accent = accent;
            this.buttonText = buttonText;
            this.buttonFill = buttonFill;
            this.buttonBorder = buttonBorder;
            this.buttonBorderVisible = buttonBorderVisible;
        }
    }
}
