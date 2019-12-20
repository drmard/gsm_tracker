package com.hsp.gsm.cell.tracker;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.JsonReader;

import java.io.IOException;
import java.io.StringReader;

public class UpdateCellLocationCallback implements
        JsonPostTask.Callback {

    private static final boolean DEBUG = false;

    private static final String TAG =
            UpdateCellLocationCallback.class.getSimpleName();

    private Cell mCell = null;

    private Handler mHandler = null;

    public UpdateCellLocationCallback (Cell cell, Handler handler) {
        mHandler = handler;
        mCell = cell; 
    }

    @Override
    public void onTaskComplete(String data) {
        if (data == null || data.length() <= 16) {
            mCell = null;
            TrackerHelper.sendMessage(mHandler,
                Constants.MSG_DOWNLOAD_CELLLOCATION_DATA_FINISHED,
                (Object)mCell);
            return;
        }

        if (DEBUG) {
        }

        String address = null;
        String lat = null;
        String lon = null;
        String name;
        StringReader reader = new StringReader(data);

        try {
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                name = jsonReader.nextName();
                if (name.equals("lat")) {
                    lat = jsonReader.nextString();
                } else if (name.equals("lon")) {
                    lon = jsonReader.nextString();
                } else if (name.equals("address")) {
                    address = jsonReader.nextString();
                } else {
                    jsonReader.skipValue();
                }
            }
            jsonReader.endObject();

        } catch (IOException ioe) {
            Log.e(TAG, "IOException: " + ioe);
        }

        try {
            mCell.lat = lat;
            mCell.lon = lon;
            mCell.address = address;
        } catch (NullPointerException ex) {
            Log.e(TAG, "NullPointerException: " + ex);
        }

        mCell.time = Long.toString(System.currentTimeMillis());

        TrackerHelper.sendMessage(mHandler,
                Constants.MSG_DOWNLOAD_CELLLOCATION_DATA_FINISHED,
                (Object)mCell);
    }
}
