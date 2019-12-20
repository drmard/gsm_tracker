package com.hsp.gsm.cell.tracker;

import android.database.Cursor;

public class Cell {
    private static final int ID_INDEX = 0;
    private static final int LAC_INDEX = 1;
    private static final int CID_INDEX = 2;
    private static final int LAT_INDEX = 3;
    private static final int LON_INDEX = 4;
    private static final int ADDRESS_INDEX = 5;
    private static final int MCC_INDEX = 6;
    private static final int MNC_INDEX = 7;

    public Cell() {
            id = -1;
            lac = -1;
            cid = -1;
            mcc = -1;
            mnc = -1;
            lat = null;
            lon = null;
            address = null;
            time = null;       
    }

    public Cell(Cell cell) {
        id = cell.id;
        mcc = cell.mcc;
        mnc = cell.mnc;
        lac = cell.lac;
        cid = cell.cid;

        if (cell.address != null)
            address = new String(cell.address);

        if (cell.lat != null)
            lat = new String(cell.lat);

        if (cell.lon != null)
            lon = new String(cell.lon);

        if (cell.time != null)
            time = new String(cell.time); 
    }

    public Cell(Cursor c) {
        if (c == null) {
            return;
        }
        id = c.getLong(ID_INDEX);
        lac = c.getInt(LAC_INDEX);
        cid = c.getInt(CID_INDEX);
        lat = c.getString(LAT_INDEX);
        lon = c.getString(LON_INDEX);
        address = c.getString(ADDRESS_INDEX);
        mcc = c.getInt(MCC_INDEX);
        mnc = c.getInt(MNC_INDEX);
    }

    public long id;
    public int lac;
    public int cid;
    public int mcc;
    public int mnc;
    public String lat;
    public String lon;
    public String address;
    public String time;
}

