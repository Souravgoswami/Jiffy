package com.souravgoswami.jiffy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class TimerAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !TimerAlarmScheduler.ACTION_TIMER_FINISHED.equals(intent.getAction())) {
            return;
        }
        TimerAlarmScheduler.finishIfDue(context);
    }
}
