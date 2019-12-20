package com.hsp.gsm.cell.tracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
 
public class DBase {

    private static final String TAG = DBase.class.getSimpleName();

    private static final boolean DEBUG = false;

    public static final String DB_NAME = "db_cells";
    public static final int DB_VERSION = 1;
    public static final String DB_TABLE = "table_cells";
   
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_LAC = "lac";
    public static final String COLUMN_CID = "cid";
    public static final String COLUMN_LAT = "lat";
    public static final String COLUMN_LON = "lon";
    public static final String COLUMN_ADDRESS = "address";
    public static final String COLUMN_MCC = "mcc";
    public static final String COLUMN_MNC = "mnc";
    public static final String COLUMN_TIME = "time";

    public static final int COLUMN_ID_N = 0;
    public static final int COLUMN_LAC_N = 1;
    public static final int COLUMN_CID_N = 2;
    public static final int COLUMN_LAT_N = 3;
    public static final int COLUMN_LON_N = 4;
    public static final int COLUMN_ADDRESS_N = 5;
    public static final int COLUMN_MCC_N = 6;
    public static final int COLUMN_MNC_N = 7;
    public static final int COLUMN_TIME_N = 8;

    private static final String DB_CREATE = "create table " + DB_TABLE + "(" +
        COLUMN_ID + " integer primary key autoincrement, " +
        COLUMN_LAC + " integer, " +
        COLUMN_CID + " integer, " +
        COLUMN_LAT + " text, " +
        COLUMN_LON + " text, " +
        COLUMN_ADDRESS + " text, " +
        COLUMN_MCC + " integer, " +
        COLUMN_MNC + " integer, " +
        COLUMN_TIME + " text" +
        ");";
   
    private final Context mContext;
    private DBHelper mDBHelper;
    public SQLiteDatabase mDBase;
   
    public DBase(Context context) {
        mContext = context;
    }

    public void open() {
        if (DEBUG) {
            Log.d(TAG, "DBase#open()");
        }
        mDBHelper = new DBHelper(mContext, DB_NAME, null, DB_VERSION);
        mDBase = mDBHelper.getWritableDatabase();
    }

    public void close() {
        if (mDBHelper != null) {
            mDBHelper.close();
        }
    }

    public Cursor getCell(Cell cell) {
      if (mDBase == null)
          return null;

      String where = COLUMN_LAC + " = " + cell.lac + " AND " +
          COLUMN_CID + " = " + cell.cid;

      return mDBase.query(DB_TABLE, null, where, null, null, null, null);
    }

    public void addRecord(int lac, int cid, String lat, String lon,
            String addr, int mcc, int mnc, String time) {
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_LAC, lac);
        cv.put(COLUMN_CID, cid);
        cv.put(COLUMN_LAT, lat);
        cv.put(COLUMN_LON, lon);
        cv.put(COLUMN_ADDRESS, addr);
        cv.put(COLUMN_MCC, mcc);
        cv.put(COLUMN_MNC, mnc);
        cv.put(COLUMN_TIME, time);

        if (mDBase != null) {
            mDBase.insert(DB_TABLE, null, cv);
        }
    }

    public static void addRecord(Cell cell, SQLiteDatabase dB) {
        SQLiteDatabase db = dB;

        ContentValues cv = new ContentValues();

        cv.put(COLUMN_LAC, cell.lac);
        cv.put(COLUMN_CID, cell.cid);
        cv.put(COLUMN_LAT, cell.lat);
        cv.put(COLUMN_LON, cell.lon);
        cv.put(COLUMN_ADDRESS, cell.address);
        cv.put(COLUMN_MCC, cell.mcc);
        cv.put(COLUMN_MNC, cell.mnc);
        cv.put(COLUMN_TIME, cell.time);

        db.insert(DB_TABLE, null, cv);
    }

    public void delRecord(long id) {
        if (mDBase != null) {
            mDBase.delete(DB_TABLE, COLUMN_ID + " = " + id, null);
        }
    }

    public void addSomeData() {
            String stime = Long.toString(System.currentTimeMillis());

            addRecord(19680,199745813,"59.981367","30.33989","Vyborgsky District, Saint Petersburg, Northwestern Federal District, 194100, Russia",
                250,20,stime);
            addRecord(19681,200166668,"59.935911","30.32546","Saint Petersburg, Northwestern Federal District, 190000, Russia",
                250,20,stime);
            addRecord(19656,200175627,"59.835653","30.36409","Saint Petersburg, Northwestern Federal District, 190000, Russia",
                250,20,stime);
            addRecord(19686,199975693,"59.847914","30.30036","Saint Petersburg, Northwestern Federal District, 190000, Russia",
                250,20,stime);
            addRecord(19680,2409681,"59.97973","30.328037","Saint Petersburg, Northwestern Federal District, 190000, Russia",
                250,20,stime);
            addRecord(19680,200201740,"59.978184","30.32949","Vyborgsky District, Saint Petersburg, Northwestern Federal District, 190000, Russia",
                250,20,stime);
            addRecord(19686,2073172,"59.84588","30.300282","Saint Petersburg, Northwestern Federal District, 190000, Russia",
                250,20,stime);
            addRecord(19710,200167948,"59.893496","30.33005","Saint Petersburg, Northwestern Federal District, 190000, Russia",
                250,20,stime);
            addRecord(19671,200170507,"59.829486","30.38338","Saint Petersburg, Northwestern Federal District, 190000, Russia",
                250,20,stime);
            addRecord(19686,199975691,"59.848278","30.30846","Saint Petersburg, Northwestern Federal District, 190000, Russia",
                250,20,stime);
            addRecord(19703,4505925,"59.974825","30.34526","Vyborgsky District, Saint Petersburg, Northwestern Federal District, 190000, Russia",
                250,20,stime);
            addRecord(19680,200164621,"59.974196","30.31814","Petrogradsky District, Saint Petersburg, Northwestern Federal District, 190000, Russia",
                250,20,stime);
            addRecord(19681,200172299,"59.956242","30.35569","Saint Petersburg, Northwestern Federal District, 190000, Russia",
                250,20,stime);
            addRecord(19680,200253453,"59.977114","30.32140","Petrogradsky District, Saint Petersburg, Northwestern Federal District, 197022, Russia",
                250,20,stime);
            addRecord(19710,200167179,"59.84588","30.300282","Saint Petersburg, Northwestern Federal District, 190000, Russia",
                250,20,stime);
    }

    private class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context, String name, CursorFactory factory,
                int version) {
            super(context, name, factory, version);
        }
 
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DB_CREATE);
        }
 
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion,
                int newVersion) {
        }

    }
}
