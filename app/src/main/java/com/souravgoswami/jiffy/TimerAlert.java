package com.souravgoswami.jiffy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;

final class TimerAlert {
    static final String SOUND_CHANNEL_ID = "jiffy_timer_finished_sound";

    private static final String KEY_ACCENT_COLOR = "accent_color";
    private static final String KEY_TIMER_SOUND = "timer_sound";
    private static final String KEY_TIMER_SOUND_URI = "timer_sound_uri";
    private static final String KEY_TIMER_FINISH_ALERTED = "timer_finish_alerted";
    private static final String SILENT_CHANNEL_ID = "jiffy_timer_finished_silent";
    private static final int NOTIFICATION_ID = 1803;

    private TimerAlert() {
    }

    static synchronized void show(Context context, SharedPreferences prefs) {
        if (prefs.getBoolean(KEY_TIMER_FINISH_ALERTED, false)) {
            return;
        }
        prefs.edit().putBoolean(KEY_TIMER_FINISH_ALERTED, true).commit();

        boolean soundEnabled = prefs.getBoolean(KEY_TIMER_SOUND, true);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        ensureChannels(context, manager, prefs);

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String channelId = soundEnabled ? soundChannelId(prefs) : SILENT_CHANNEL_ID;
        Notification.Builder builder = new Notification.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_tab_timer)
                .setContentTitle("Jiffy Timer")
                .setContentText("Timer finished.")
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setShowWhen(true)
                .setCategory(Notification.CATEGORY_ALARM)
                .setColor(prefs.getInt(KEY_ACCENT_COLOR, Color.rgb(106, 148, 255)));
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    static void ensureChannels(Context context, NotificationManager manager, SharedPreferences prefs) {
        Uri sound = soundUri(prefs);
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        NotificationChannel soundChannel = new NotificationChannel(
                soundChannelId(prefs),
                soundChannelName(context, prefs),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        soundChannel.setDescription("Plays the selected timer tune when a Jiffy timer finishes.");
        soundChannel.setSound(sound, attributes);
        manager.createNotificationChannel(soundChannel);

        NotificationChannel silentChannel = new NotificationChannel(
                SILENT_CHANNEL_ID,
                "Timer Finished Silent",
                NotificationManager.IMPORTANCE_LOW
        );
        silentChannel.setDescription("Shows a silent notification when a Jiffy timer finishes.");
        silentChannel.setSound(null, null);
        silentChannel.enableVibration(false);
        manager.createNotificationChannel(silentChannel);
    }

    private static String soundChannelId(SharedPreferences prefs) {
        String stored = prefs.getString(KEY_TIMER_SOUND_URI, null);
        if (stored == null || stored.trim().isEmpty()) {
            return SOUND_CHANNEL_ID;
        }
        return "jiffy_timer_finished_sound_" + Long.toHexString(stored.hashCode() & 0xffffffffL);
    }

    private static Uri soundUri(SharedPreferences prefs) {
        String stored = prefs.getString(KEY_TIMER_SOUND_URI, null);
        if (stored == null || stored.trim().isEmpty()) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        return Uri.parse(stored);
    }

    private static String soundChannelName(Context context, SharedPreferences prefs) {
        String stored = prefs.getString(KEY_TIMER_SOUND_URI, null);
        if (stored == null || stored.trim().isEmpty()) {
            return "Timer Finished";
        }
        try {
            android.media.Ringtone ringtone = RingtoneManager.getRingtone(context, Uri.parse(stored));
            if (ringtone != null) {
                String title = ringtone.getTitle(context);
                if (title != null && !title.trim().isEmpty()) {
                    return "Timer Finished: " + title;
                }
            }
        } catch (RuntimeException ignored) {
            return "Timer Finished: Selected Tune";
        }
        return "Timer Finished: Selected Tune";
    }
}
