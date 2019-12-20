package com.hsp.gsm.cell.tracker;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class CellDataCursorLoader extends CursorLoader {

    private static final String TAG = CellDataCursorLoader.class.
            getSimpleName();

    protected static final boolean DEBUG = false;

    private Cell mCell = null;
    private Context mContext = null;
    private Cursor cursor = null;
    private SQLiteDatabase mDBase = null;

    public CellDataCursorLoader(Context context, Cell cell) {
        super(context);

        mContext = context;

        if (cell == null) {
            mCell = new Cell();
        } else {
            mCell = new Cell(cell);
        }
    }

    public void setCell (Cell cell) {
        mCell.lac = cell.lac;
        mCell.cid = cell.cid;
        mCell.mcc = cell.mcc;
        mCell.mnc = cell.mnc;
    }

    public Cell getCell() {
        return mCell;
    }

    public SQLiteDatabase getDBase () {
        return mDBase;
    }

    /**
     * Queries the Gsm cell in the local database and loads
     * results in background.
     *
     * @return Cursor of the cell matches the query.
     */
    @Override
    public Cursor loadInBackground() {
        if (DEBUG) {
            Log.d (TAG, "loadInBackground()");
        }

        Cursor cursor = null;
        if (mCell == null || mCell.lac <= 0 || mCell.cid <= 0)
            return null;

        mDBase = mContext.openOrCreateDatabase(DBase.DB_NAME,
                Context.MODE_PRIVATE, null);
        if (mDBase == null) {
            return null;
        }

        String where = DBase.COLUMN_LAC + " = " + mCell.lac + " AND " +
                DBase.COLUMN_CID + " = " + mCell.cid;

        try {
            cursor = mDBase.query(DBase.DB_TABLE, null, where, null, null,
                null, null);
        } catch (SQLException e) {
            Log.e(TAG, "SQLException: " + e);
        }

        return cursor; 
    }
}   

