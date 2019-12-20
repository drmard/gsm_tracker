package com.hsp.gsm.cell.tracker;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo; 

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import android.util.Log;

public class TrackerHelper {
    private static final String TAG = TrackerHelper.class.getSimpleName();
    private Context mContext;

    public TrackerHelper(Context context) {
        mContext = context;
    }

    public static String convertTime() {
        int h = 0, m = 0, s = 0, y = 0, mo = 0, da = 0;
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        java.util.Date date = calendar.getTime();
        y = calendar.get(java.util.Calendar.YEAR);
        mo = calendar.get(java.util.Calendar.MONTH);
        da = calendar.get(java.util.Calendar.DAY_OF_MONTH);
        h = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        m = calendar.get(java.util.Calendar.MINUTE);
        s = calendar.get(java.util.Calendar.SECOND);
        String rez = y + "." +
                ((mo > 9) ? mo : ("0" + mo)) + "." +
                ((da > 9) ? da : ("0" + da)) + "." +
                ((h > 9) ? h : ("0" + h)) + "." +
                ((m > 9) ? m : ("0" + m)) + "." +
                ((s > 9) ? s : ("0" + s));

        return rez;
    }

    public static void sendMessage(Handler handler, int what,
            Object obj) {
        if (handler != null) {
            Message msg = Message.obtain();
            msg.what = what;
            msg.obj = obj;
            handler.sendMessage(msg);
        }
    }

    public static boolean isServiceRunning(Class<GsmTrackerService>
            serviceClass, Context context) {

        ActivityManager manager = (ActivityManager)context.getSystemService(
            Context.ACTIVITY_SERVICE);

        for (RunningServiceInfo service : manager.
                getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }

    public static boolean startService(Activity context) {
        ContextWrapper contextWrapper = null;

        if (!isServiceRunning(GsmTrackerService.class, context)) {
            contextWrapper = new ContextWrapper(context);
            contextWrapper.startService (new Intent(contextWrapper,
                GsmTrackerService.class));
            return true;
        }

        return false;
    }
}
