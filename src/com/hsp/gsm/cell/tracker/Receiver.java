package com.hsp.gsm.cell.tracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import java.util.Calendar;

public class Receiver extends BroadcastReceiver {
  
    public static final String ACTION_ON_HALF_HOUR =
            "com.hsp.gsm.cell.tracker.ACTION_ON_HALF_HOUR";

    private static final int ALARM_ON_HALF_HOUR_INTERVAL = 30;

    private AlarmManager am = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (ACTION_ON_HALF_HOUR.equals(action)) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");

            wl.acquire();
            setAlarm(context, ACTION_ON_HALF_HOUR);
            wl.release();

        } else if (action.equals("android.intent.action.BOOT_COMPLETED")) {
            Intent serviceIntent = new Intent(context, GsmTrackerService.class);
            context.startService(serviceIntent);

        } else {
            return;
        }
    }

    private void setAlarm(Context context, String action) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.SECOND, 1);
        int minute = c.get(Calendar.MINUTE);
        c.add(Calendar.MINUTE, ALARM_ON_HALF_HOUR_INTERVAL -
                (minute % ALARM_ON_HALF_HOUR_INTERVAL));

        long onHalfHour = c.getTimeInMillis();

        Intent intent = new Intent(ACTION_ON_HALF_HOUR);

        PendingIntent halfHourIntent = PendingIntent.getBroadcast(context, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (am == null)
            am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (Build.VERSION.SDK_INT >= 19 && Build.VERSION.SDK_INT < 23) {
            am.setExact(AlarmManager.RTC_WAKEUP, onHalfHour, halfHourIntent);
        } else if (Build.VERSION.SDK_INT < 19){
            am.set(AlarmManager.RTC_WAKEUP, onHalfHour, halfHourIntent);
        } else {
            // for 6.0 and much newer
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, onHalfHour,
                    halfHourIntent);
        }
    }
}

