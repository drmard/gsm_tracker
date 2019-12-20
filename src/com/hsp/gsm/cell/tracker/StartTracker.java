package com.hsp.gsm.cell.tracker;

import android.app.ActionBar;
import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import android.telephony.PhoneStateListener;
import android.telephony.CellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.telephony.TelephonyManager;

import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.util.Log;
import android.widget.TextView;

public class StartTracker extends Activity implements
        LoaderCallbacks<Cursor> {

    protected static final boolean DEBUG = false;
    private Context ctx;
    private TelephonyManager mTM;

    private CellLocation mCL;
    private TextView t0, t1, t2, t3, t4, encoded,
        op, opmcc, opmnc, oproam;

    private TrackerPreferences prefs;

    private GsmTrackerService mService;

    private UiEventController mUiEventController;

    private GsmTrackerService.TrackerBinder binder;

    private DBase mDBase = null;

    private CellDataCursorLoader mLoader = null;

    private boolean mServiceIsBound = false;

    private boolean keyBackPressed = false;

    private enum StorageType {
        DATA, INTERNAL, EXTERNAL, USB
    };

    private static final String TAG = "StartTracker";

    private static final int CELL_DATA_LOADER_ID = 0;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        return new CellDataCursorLoader(ctx, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        CellDataCursorLoader mLoader = null;
        
        switch (loader.getId())  {
            case CELL_DATA_LOADER_ID:
                mLoader = (CellDataCursorLoader)loader;
                Cell cell = mLoader.getCell();
                SQLiteDatabase db = mLoader.getDBase();

                if (cursor == null) {
                    queryCurrentCellData();
                    return;
                }
                if (cursor.getCount() == 0) {
                    // send event to Service
                    if (mUiEventController != null)
                        mUiEventController.onUiEvent(UiEventController.
                            EVENT_CELL_LOCATION_NOT_FOUND_IN_LOCAL_DB, ctx);
                    if (db != null) {
                        db.close();
                    }
                    return;
                }
          
                cursor.moveToPosition(-1);
                try {
                    if (cursor.moveToFirst()) {
                        cell = new Cell(cursor);
                        updateGsmData(cell);
                    }
                } finally {
                    cursor.close();
                }
                if (db != null) {
                    db.close();
                }
                break;

            default:
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int message = msg.what;
            switch (message) {

                case Constants.MSG_UPDATE_UI_OPERATOR_DATA:
                    if (mUiEventController != null &&
                            mUiEventController.getPhoneServiceState()
                            == false) {
                        updateServiceData(false, -1, -1);  
                        updateGsmData(null);                      
                        break;
                    }

                    if (msg.obj == null) {
                        msg = Message.obtain();
                        msg.what = Constants.MSG_UPDATE_UI_OPERATOR_DATA;
                        if (mUiEventController == null) {
                            this.sendMessageDelayed(msg, 20);
                        } else {
                            boolean isRoaming = false;
                            String serviceOperator =
                                    mUiEventController.getServiceOperator();
                            String simOperator =
                                    mUiEventController.getSimOperator();
                            String opMcc = serviceOperator.substring(0, 3);
                            String simMcc = simOperator.substring(0, 3);
                            if (opMcc != null && simMcc != null)
                                isRoaming = !opMcc.equals(simMcc);
                            else
                                break;
                            String opMnc = serviceOperator.substring(3);
                            if (opMnc == null) {
                                break;
                            }
                            updateServiceData(isRoaming,
                                    Integer.parseInt(opMcc),
                                    Integer.parseInt(opMnc));
                        }
                    }
                    break;

                case Constants.MSG_UPDATE_CURRENT_CELL:
                    if (msg.obj != null) {
                        updateGsmData((Cell)msg.obj);
                    }
                    break;

                case Constants.MSG_UPDATE_UI_LOCATION_DATA:
                    break;

            }
        }
    };

    UiEventController.ActivityRequestListener requestListener =
            new UiEventController.ActivityRequestListener() {
        @Override
        public void onRequest(int event) {
            switch(event) {
                case UiEventController.ActivityRequestListener.
                        EVENT_REQUEST_SERVICE_NONE:
                    updateGsmData(null);
                    break;
            }
        }

        @Override
        public void onRequestObject(int event, Object obj) {
            switch(event) {
                case UiEventController.ActivityRequestListener.
                        EVENT_REQUEST_CELL_DATA_UPDATE:
                    if (obj == null)
                        break;

                    updateGsmData((Cell)obj);
                    break;

                case UiEventController.ActivityRequestListener.
                        EVENT_REQUEST_SERVICE_OPERATOR_CHANGED:
                    Bundle bundle = null;
                    if (obj != null) {
                        bundle = (Bundle)obj;
                    }
                    int isRoaming = bundle.getInt("roaming", -1);
                    int op_mcc = bundle.getInt("mcc", -1);
                    int op_mnc = bundle.getInt("mnc", -1);
                    updateServiceData((isRoaming == 0) ? false : true,
                            op_mcc, op_mnc);
                    break;

                case UiEventController.ActivityRequestListener.
                        EVENT_REQUEST_CELL_DATA_FROM_DB:
                    if (mLoader != null && obj != null) {
                        mLoader.setCell((Cell)obj);
                        mLoader.forceLoad();
                    }
                    break;

                default:
                    break;
            }
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            binder = (GsmTrackerService.TrackerBinder)service;

            mUiEventController = binder.getUiEventController();
            mUiEventController.setRequestListener(requestListener);

            // send event to service for init telephony events listening
            mUiEventController.onUiEvent(
                mUiEventController.EVENT_SERVICE_CONNECTION_COMPLETE, ctx);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            binder = null;
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.main);
        ctx = this;

        if (DEBUG){
        }

        op = (TextView) findViewById(R.id.op_data);
        op.setTypeface (Typeface.createFromAsset(getAssets(),
                "fonts/RobotoMono-Medium.ttf"));
        opmcc = (TextView) findViewById(R.id.mcc);
        opmcc.setTypeface (Typeface.createFromAsset(getAssets(),
                "fonts/RobotoMono-Medium.ttf")); 
        opmnc = (TextView) findViewById(R.id.mnc);
        opmnc.setTypeface (Typeface.createFromAsset(getAssets(),
                "fonts/RobotoMono-Medium.ttf"));
        oproam = (TextView) findViewById(R.id.nat_roaming);
        oproam.setTypeface (Typeface.createFromAsset(getAssets(),
                "fonts/RobotoMono-Medium.ttf"));

        t0 = (TextView) findViewById(R.id.text0);
        t0.setTypeface (Typeface.createFromAsset(getAssets(),
                "fonts/RobotoMono-Medium.ttf"));   
        t1 = (TextView) findViewById(R.id.text1);
        t1.setTypeface (Typeface.createFromAsset(getAssets(),
                "fonts/RobotoMono-Medium.ttf"));
        t2 = (TextView) findViewById(R.id.text2);
        t2.setTypeface (Typeface.createFromAsset(getAssets(),
                "fonts/RobotoMono-Medium.ttf"));
        t3 = (TextView) findViewById(R.id.text3);
        t3.setTypeface (Typeface.createFromAsset(getAssets(),
                "fonts/RobotoMono-Medium.ttf"));
        t4 = (TextView) findViewById(R.id.text4);
        t4.setTypeface (Typeface.createFromAsset(getAssets(),
                "fonts/RobotoMono-Medium.ttf"));

        prefs = new TrackerPreferences(ctx);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        bindToGsmTrackerService(this, mServiceConnection);

        Message message = Message.obtain();
        message.what = Constants.MSG_UPDATE_UI_OPERATOR_DATA;
        mHandler.sendMessageDelayed(message, 200);

        keyBackPressed = false;

        mLoader = (CellDataCursorLoader)getLoaderManager().initLoader(
                CELL_DATA_LOADER_ID, null, this);
    }
 
    @Override
    public void onPause() {
        super.onPause();

        // close the activity in case user pressed
        // the 'home' key
        if (keyBackPressed == false) {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        keyBackPressed = true;
        super.onBackPressed();
    }

    @Override
    public void onDestroy() {
        if (mHandler != null) {
            Message message = Message.obtain();
            message.what = Constants.MSG_EVENT_UNBIND;
            mHandler.sendMessage(message);
        }
        doUnbindService();

        if (mLoader != null) {
            mLoader.reset();
            mLoader = null;
        }
        super.onDestroy();
    }

    private void queryCurrentCellData () {
        if (mUiEventController == null) {
            return;
        }

        Cell cell = mUiEventController.getCurrentCell();
        if (cell != null) {
            Message msg = Message.obtain();
            msg.what = Constants.MSG_UPDATE_CURRENT_CELL;
            msg.obj = (Object)cell;
            mHandler.sendMessage(msg);
        }
    }

    private void doUnbindService() {
        if (mServiceIsBound) {
            // Detach our existing connection.
            unbindService(mServiceConnection);

            mServiceIsBound = false;
        }
    }

    public void updateServiceData(boolean isNationalRoaming, int mcc,
            int mnc) {

        opmcc.setText ("      mcc:  " + mcc);
        opmnc.setText ("      mnc:  " + mnc);
        oproam.setText("      national roaming:  " +
            ((isNationalRoaming == false) ? "no" : "yes"));

    }

    public void updateGsmData(Cell data) {
        if (data == null) {
            t1.setText("      lac: out of service");
            t2.setText("      cid: out of service");
            return;
        }
        t1.setText("      lac:  " + data.lac);
        t2.setText("      cid:  " + data.cid);
        if (data.lat != null)
            t3.setText("      lat:  " + data.lat);
        else
            t3.setText("      lat:  " + "(null)");
        if (data.lon != null)
            t4.setText("      lon:  " + data.lon);
        else
            t4.setText("      lon:  " + "(null)");
    }

    public void bindToGsmTrackerService(Activity context,
            ServiceConnection connection) {

        Intent intent = new Intent(context, GsmTrackerService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        mServiceIsBound = true;

    }
}
