package com.hsp.gsm.cell.tracker;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class TrackerNative {
    static {
        try {
            System.loadLibrary("trackgsm");
        } catch (UnsatisfiedLinkError u) {
            u.printStackTrace();
        }
    };

    private static final String TAG = TrackerNative.class.getSimpleName();

    private static Handler msgHandler;

    private static Context mContext;

    public static void setPath(byte[] pathBytes) {        
        if (msgHandler == null) {
            return; 
        }

        int length = pathBytes.length;

        Message msg = Message.obtain();

        if (length == Constants.EXPATH_ARRAY_LENGTH) {
            msg.what = Constants.MSG_UPDATE_EX_PATH;
        } else if (length == Constants.PUBLIC_KEY_PATH_ARRAY_LENGTH) {
            msg.what = Constants.MSG_UPDATE_PUBLIC_KEY;
        }

        msg.obj = (Object)pathBytes;
        msgHandler.sendMessage(msg);
    }

    public static void setHandler(Context context, Handler handler,
            boolean clear) {
        mContext = context;
        if (true == clear) {
            msgHandler = handler;
        } else {
            msgHandler = null;
        }
    }

    public native static void setAssetManager(AssetManager manager);
    public native static void getPKey();
    public native static void getPrefixPath();
}
