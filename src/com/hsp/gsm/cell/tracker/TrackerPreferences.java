package com.hsp.gsm.cell.tracker;

import android.content.Context;
import android.content.SharedPreferences;

public class TrackerPreferences {
    Context mContext = null;

    private static final String _PACKAGE = "com.hsp.gsm.cell.tracker";
    private static final String PREF_RSA_PRIVATE_KEY = "pkey";
    private static final String PREF_RSA_PUBLIC_KEY = "key";
    private static final String PREF_CRASH = "crash";

    public TrackerPreferences (Context context) {
        mContext = context;
    }

    public void setPKey(String key) {
        SharedPreferences preferences = mContext.getSharedPreferences(_PACKAGE,
                Context.MODE_PRIVATE);
        preferences.edit().putString(PREF_RSA_PRIVATE_KEY, key).commit();
    }

    public void setKey(String key) {
        SharedPreferences preferences = mContext.getSharedPreferences(_PACKAGE,
                Context.MODE_PRIVATE);
        preferences.edit().putString(PREF_RSA_PUBLIC_KEY, key).commit();
    }

    public String getPKey (String checkString) {
        SharedPreferences preferences = mContext.getSharedPreferences(_PACKAGE,
                Context.MODE_PRIVATE);
        return preferences.getString(PREF_RSA_PRIVATE_KEY, checkString);
    }

    public String getKey (String checkString) {
        SharedPreferences preferences = mContext.getSharedPreferences(_PACKAGE,
                Context.MODE_PRIVATE);
        return preferences.getString(PREF_RSA_PUBLIC_KEY, checkString);
    }
}
