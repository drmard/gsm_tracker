package com.hsp.gsm.cell.tracker;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;

import android.util.Log;
import android.widget.RemoteViews;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import java.util.Calendar;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class GsmTrackerService extends Service implements UiEventController,
        GsmOperator.OnChangeServiceOperator {

    public static final int TRACKER_NOTIFICATION_ID = 20001;

    public static final int MSG_DOWNLOAD_XTRA_DATA_FINISHED            = 30004;
    public static final int MSG_UPDATE_LOCATION                        = 30005;
    public static final int MSG_UPDATE_COORDINATE_FOR_CURRENT_CELL     = 30006;
    public static final int MSG_UPDATE_PREFIX_PATH                     = 30007;
    public static final int MSG_INIT_NATIVE                            = 30008;
    public static final int MSG_GET_PUBLIC_KEY                         = 30009;

    private static final int VIEW_CELL_REQUEST                         =  0x11;

    private static final String TAG = GsmTrackerService.class.getSimpleName();

    protected static final boolean DEBUG = false;

    private Context activityContext; 
    private Context mContext;

    private TelephonyManager mTelephonyManager;
    private NotificationManager mNotificationManager = null;
    private PhoneListener mPL = null;

    private IBinder mBinder = new TrackerBinder();

    private DBase mDBase = null;
    private SQLiteDatabase db = null;

    private Cell mCurrent;
    private Cell mNewCell;

    private boolean nativeInitComplete = false;

    private String mICCOperatorNumeric = null;
    private String mServiceOperatorNumeric = "";
    private int mServiceOperatorMcc = -1;
    private int mServiceOperatorMnc = -1;

    private static String PROPERTY_SIM_OPERATOR_NUMERIC =
            "gsm.sim.operator.numeric";

    private GsmOperator.OnChangeServiceOperator callback = null;

    private boolean mIsNationalRoaming = false;

    private ServiceHandler mHandler = new ServiceHandler();

    private boolean mServiceState = false;

    private byte[] encodedExPathKey = null;
    private byte[] encodedPKey = null;
    private byte[] authToken = null;

    UiEventController.ActivityRequestListener activityRequestListener = null;

    public class TrackerBinder extends Binder {
        public UiEventController getUiEventController() {
            return GsmTrackerService.this;
        } 
    }

    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        mPL = new PhoneListener();
        mTelephonyManager = (TelephonyManager)getSystemService(
                TELEPHONY_SERVICE);

        super.onCreate();
        callback = this;

        mICCOperatorNumeric = mTelephonyManager.getSimOperator();

        if (mCurrent == null) {
            mCurrent = new Cell();
        }

        nativeInitComplete = false;

        mDBase = new DBase(this);
        mDBase.open();
        mDBase.addSomeData();

        if (DEBUG) {
            Log.d (TAG, "Sim Operator Numeric: " + mICCOperatorNumeric);
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (DEBUG) {
            Log.d(TAG, "onStartCommand()");
        }

        mContext = getApplicationContext();
        mNotificationManager = (NotificationManager)this.getSystemService(
                Context.NOTIFICATION_SERVICE);

        setAlarmOnHalfHour(mContext);
        startNotification(mContext);
        
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mTelephonyManager.listen(mPL, PhoneStateListener.LISTEN_NONE);

        super.onDestroy();
        mNotificationManager.cancel(TRACKER_NOTIFICATION_ID);

        stopSelf();
    }

    private void initNative() {
        if (mHandler == null) {
            mHandler = new ServiceHandler();
        }
         
        TrackerNative.setHandler(mContext, mHandler, true);
        TrackerNative.setAssetManager(mContext.getAssets());    
    }

    @Override
    public void onTaskRemoved(Intent intent) {
    }

    @Override
    public void onChange(String operator) {
        if (operator.length() > 3) {
            synchronized(mServiceOperatorNumeric) {
                mServiceOperatorNumeric = new String(operator);
                mServiceOperatorMcc = Integer.parseInt(
                        mServiceOperatorNumeric.substring(0, 3));
                mServiceOperatorMnc = Integer.parseInt(
                        mServiceOperatorNumeric.substring(3));
            }
        }
    }

    public String getServiceOperator () {
        return mServiceOperatorNumeric;
    }

    public String getSimOperator () {
        return mICCOperatorNumeric;
    }

    public Cell getCurrentCell () {
        Cell cell = null;
        synchronized(mCurrent) {
            cell = new Cell(mCurrent);
        }

        return cell;
    }

    public boolean getPhoneServiceState() {
        return mServiceState;
    }

    public boolean isBinded() {
        return (activityRequestListener != null) ? true : false;
    }

    private Cursor executeCallable(Callable<Cursor> callable) {
        Cursor cursor = null;
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Cursor> future = executor.submit(callable);
        try {
            cursor = future.get();
        } catch (InterruptedException ie) {
            Log.e(TAG, "InterruptedException: " + ie);
            return null;
        } catch (ExecutionException ee) {
            throw new RuntimeException(ee);
        }

        return cursor;      
    }

    private class GetCellLocation implements Callable<Cursor> {
        private Cell cell;
        private Context ctx = null;

        public GetCellLocation(Context context, Cell queryCell) {
            ctx = context;
            cell = queryCell;
        }

        @Override
        public Cursor call() throws Exception {
            Cursor cursor = null;
            if (cell.lac <= 0 || cell.cid <= 0) {
                return null;
            }

            String where = DBase.COLUMN_LAC + " = " + cell.lac + " AND " +
                    DBase.COLUMN_CID + " = " + cell.cid;

            db = mContext.openOrCreateDatabase (
                DBase.DB_NAME, Context.MODE_PRIVATE, null);
            if (db == null) {
                return null;
            }

            try {
                cursor = db.query(DBase.DB_TABLE, null, where, null, null,
                        null, null);
            } catch (SQLException e) {
                Log.e(TAG, "SQLException: " + e);
            }

            return cursor;       
        }
    }

    private Cursor QueryCellLocationAsync (Cell cell) throws
            InterruptedException, ExecutionException {
        Cursor c =
            executeCallable(new GetCellLocation(mContext, cell));
        return c;
    }

    public void getCellLocationFromDB (boolean isBinded, Cell cell)
            throws ExecutionException, InterruptedException,
            NullPointerException {
        if (cell == null || cell.lac <= 0 || cell.cid <= 0)
            return;

        if (DEBUG) {
            Log.d(TAG, "getCellLocationFromDB: service binded - " + isBinded);
            Log.d(TAG, "cell: lac - " + cell.lac +
                    " cid - " + cell.cid + " mcc - " +
                    cell.mcc + " mnc - " + cell.mnc);
        }

        Cursor cursor = null;

        // We will use Callable interface for query to DB if
        // service is unbinded or will use CursorLoader
        // in the main Activity binded to service
        String lat = null;
        String lon = null;
        String address = null;

        if (isBinded == true) {
            if (activityRequestListener != null)
                activityRequestListener.onRequestObject(
                        UiEventController.ActivityRequestListener.
                        EVENT_REQUEST_CELL_DATA_FROM_DB,
                        (Object)cell);
            return;
        }

        cursor = QueryCellLocationAsync(cell);
        if (cursor != null && cursor.getCount() > 0) {
            try {
                cursor.moveToPosition(-1);
                cursor.moveToFirst();
                cell = new Cell(cursor);

                Message msg = Message.obtain();
                msg.what = Constants.MSG_CELL_LOCATION_DATA_OBTAINED;
                msg.obj = (Object)cell;
                mHandler.sendMessage(msg);
            } finally {
                cursor.close();
                db.close();
            }
        } else {
            // Cell location data was not found in the local DB
            // so we should init Post request to opencellid.org      
            if (cell.mcc < 0 || cell.mnc < 0) {
                cell.mcc = mServiceOperatorMcc;
                cell.mnc = mServiceOperatorMnc;
            }
            Message msg = Message.obtain();
            msg.what = Constants.MSG_GET_CELL_DATA_FROM_OPEN_CELL_ID;
            msg.obj = (Object)cell;
            mHandler.sendMessage(msg);
        }
    }

    private byte[] decodeExPath() throws Exception {
        byte[] decoded = null;

        KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedPKey);
        RSAPublicKey publicKey = (RSAPublicKey)rsaKeyFactory.generatePublic(keySpec);

        Cipher cph = Cipher.getInstance("RSA");
        cph.init(Cipher.DECRYPT_MODE, publicKey);
        decoded = cph.doFinal(encodedExPathKey);

        if (DEBUG) {
        }

        return decoded;
    }

    private void setAlarmOnHalfHour(Context context) {
        Calendar nextHalf = Calendar.getInstance();

        // take one second to ensure half-hour threshold
        // passed
        nextHalf.set(Calendar.SECOND, 1);

        int min = nextHalf.get(Calendar.MINUTE);
        nextHalf.add(Calendar.MINUTE, 30 - (min % 30));

        long onHalfHour = nextHalf.getTimeInMillis();

        PendingIntent halfHourIntent = PendingIntent.getBroadcast(context, 0,
                new Intent(Receiver.ACTION_ON_HALF_HOUR),
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = ((AlarmManager) context.getSystemService(
                Context.ALARM_SERVICE));
        if (Build.VERSION.SDK_INT >= 19 && Build.VERSION.SDK_INT < 23) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, onHalfHour,
            halfHourIntent);
        } else if (Build.VERSION.SDK_INT < 19){
            alarmManager.set(AlarmManager.RTC_WAKEUP, onHalfHour, halfHourIntent);
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                    onHalfHour, halfHourIntent);
        }

        if (DEBUG) {
            Log.d(TAG, "alarm on half hour was set\n");
        }     
    }

    public void startNotification(Context context) {

        RemoteViews views = new RemoteViews(getPackageName(),
                R.layout.notification_layout);
        views.setImageViewResource(R.id.ic_not, R.drawable.ic_not);
        views.setTextViewText(R.id.tr_notif, " GSM Cell Tracking Data ");

        Intent restartintent = new Intent("gsm.tracker.MAIN_VIEW");
    	restartintent.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this,
                VIEW_CELL_REQUEST, restartintent, 0);

        Notification status = new Notification();
        status.contentView = views;
        status.flags |= Notification.FLAG_AUTO_CANCEL;
        status.icon = R.drawable.not_icon;
        status.contentIntent = contentIntent;

        startForeground(TRACKER_NOTIFICATION_ID, status);

    }

    @Override
    public void setRequestListener(UiEventController.ActivityRequestListener
            listener) {
        activityRequestListener = listener;
    }

    @Override
    public void onUiEvent(int event, Object context) {
        String activity = null;

        if (context instanceof Activity) {
            activityContext = (Context)context;
            activity = ((Activity)context).getClass().getSimpleName();
        }

        switch (event) {
            case UiEventController.EVENT_SERVICE_CONNECTION_COMPLETE:
                if (activity.equals("StartTracker")) {
                    if (mTelephonyManager == null) {
                        mTelephonyManager = (TelephonyManager)getSystemService(
                                TELEPHONY_SERVICE);
                    }

                    mTelephonyManager.listen(mPL,
                        PhoneStateListener.LISTEN_CELL_LOCATION |
                        PhoneStateListener.LISTEN_SERVICE_STATE);

                    if (nativeInitComplete == false) {
                        Message m = Message.obtain();
                        m.what = MSG_INIT_NATIVE;
                        mHandler.sendMessage(m);
                        nativeInitComplete = true;
                    }
                }
                break;

            case UiEventController.EVENT_CELL_LOCATION_NOT_FOUND_IN_LOCAL_DB:
                if (activity.equals("StartTracker")) {
                    if (mNewCell == null || mNewCell.lac <= 0 ||
                            mNewCell.cid <= 0 || mServiceOperatorMcc <= 0 ||
                            mServiceOperatorMnc <= 0) {
                        break;
                    }
                  
                    if (mNewCell.mcc < 0 || mNewCell.mnc < 0) {
                        mNewCell.mcc = mServiceOperatorMcc;
                        mNewCell.mnc = mServiceOperatorMnc;
                    }

                    Message msg = Message.obtain();
                    msg.what = Constants.MSG_GET_CELL_DATA_FROM_OPEN_CELL_ID;
                    msg.obj = (Object)mNewCell;
                    mHandler.sendMessage(msg);
                }
                break;

            default:
                break;
        }
    }
 
    class PhoneListener extends PhoneStateListener {
        String operatorMcc = null;
        String operatorMnc = null;

        @Override
        public void onCellLocationChanged(CellLocation location)
        {
            IddCellLocChanged(location);
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {

            if (serviceState.getState() == ServiceState.STATE_IN_SERVICE) {
                mServiceState = true;

                // check service operator data
                String operatorNumeric = serviceState.getOperatorNumeric();
                if (operatorNumeric != null && operatorNumeric.equals(
                        mServiceOperatorNumeric) == false) {
                    callback.onChange(operatorNumeric);

                    Bundle bundle = new Bundle();
                    bundle.putInt("mcc", mServiceOperatorMcc);
                    bundle.putInt("mnc", mServiceOperatorMnc);
                    if (mServiceOperatorNumeric.substring(0, 3).equals(
                            mICCOperatorNumeric.substring(0, 3)) == true)
                        bundle.putInt("roaming", 0);
                    else
                        bundle.putInt("roaming", 1); 

                    if (activityRequestListener != null)
                        activityRequestListener.onRequestObject(UiEventController.
                            ActivityRequestListener.
                            EVENT_REQUEST_SERVICE_OPERATOR_CHANGED,
                            (Object)bundle);
                }

            } else {
                mServiceState = false;
                synchronized(mCurrent) {
                    mCurrent = new Cell();
                }

                if (activityRequestListener != null)
                    activityRequestListener.onRequest(
                        UiEventController.ActivityRequestListener.
                        EVENT_REQUEST_SERVICE_NONE);
            }
        }
    }

    private void addRecordToFile(String out) throws IOException,
            NullPointerException {
        File dir = new File(android.os.Environment.
                getExternalStorageDirectory().getPath() + "/tracker/");
        boolean mkd = false;
        if(!(dir.exists())) {
            mkd = dir.mkdirs();
        }

        BufferedWriter bwList = null;
        FileWriter fw = new FileWriter(
                android.os.Environment.getExternalStorageDirectory().getPath()
                + "/tracker/cells.ibg", true);
        bwList = new BufferedWriter(fw);
        try {
            bwList.append(out);
            bwList.flush();
        } catch (Exception ex){
            Log.d(TAG, "IOException: " + ex);
        }

        // close file
        try {
            bwList.close();
        } catch (Exception e) {
            Log.e(TAG, "IOException: " + e);
        }
        try {
            fw.close();
        } catch (Exception e){
            Log.e(TAG, "IOException: " + e);
        }
        bwList = null;
        fw = null;
    }

    public void IddCellLocChanged(CellLocation location) {
        int lac = -1;
        int cid = -1;
        GsmCellLocation mGCL = null;
        boolean updated = false;

        if (location instanceof GsmCellLocation) {
                mGCL = (GsmCellLocation) location;
                lac = mGCL.getLac(); 
                cid = mGCL.getCid();
                if (lac == cid || lac == -1 || cid == -1)
                    return;

                if (mNewCell == null)
                    mNewCell = new Cell();

                if (mCurrent.lac != lac || mCurrent.cid != cid) {
                    mNewCell.lac = lac;
                    mNewCell.cid = cid;
                    mNewCell.time = Long.toString(System.currentTimeMillis());

                    Message msg = Message.obtain();
                    msg.what = MSG_UPDATE_COORDINATE_FOR_CURRENT_CELL;
                    mHandler.sendMessage(msg);
                }
        }
    }

    private final class ServiceHandler extends Handler {
        Cell data = null;

        @Override
        public void handleMessage(Message msg) {
            int message = msg.what;

            switch (message) {

                case Constants.MSG_GET_CELL_DATA_FROM_OPEN_CELL_ID:
                    if (mServiceState == false)
                        break;

                    Cell cell = (Cell)msg.obj;
                    if (cell == null || authToken == null || cell.mcc <  0 ||
                        cell.mnc < 0) {
                        break;
                    }
                    
                    UpdateCellLocationCallback uc = new
                            UpdateCellLocationCallback(cell, this);

                    new JsonPostTask(cell, new String(authToken), uc).
                            execute();
                    break;

                case MSG_UPDATE_COORDINATE_FOR_CURRENT_CELL:
                    data = new Cell(mNewCell);
                    try {
                        getCellLocationFromDB(isBinded(), data);
                    } catch (ExecutionException e) {
                        Log.e(TAG, "Exception: " + e);
                    } catch (InterruptedException ie) {
                        Log.e(TAG, "Exception: " + ie);
                    }
                    break;

                case Constants.MSG_CELL_LOCATION_DATA_OBTAINED:
                case Constants.MSG_DOWNLOAD_CELLLOCATION_DATA_FINISHED:
                    if (msg.obj == null)
                        break;

                    data = (Cell)msg.obj;
                    synchronized(mCurrent) {
                        if (data.cid != mCurrent.cid || data.lac != mCurrent.lac) {
                            mCurrent = new Cell(data);
                            if (activityRequestListener != null)
                                activityRequestListener.onRequestObject(
                                    UiEventController.ActivityRequestListener.
                                    EVENT_REQUEST_CELL_DATA_UPDATE,
                                    (Object)mCurrent);
                        }
                    }

                    if (message == Constants.MSG_DOWNLOAD_CELLLOCATION_DATA_FINISHED) {

                        final SQLiteDatabase db = mContext.openOrCreateDatabase(
                                DBase.DB_NAME, Context.MODE_PRIVATE, null);
                        try {
                            DBase.addRecord(data, db);
                        } catch (SQLException ex) {
                            Log.e(TAG, "SQLException: " + ex);
                        }

                        db.close();

                        String strCellData = "mcc:" + data.mcc + " mnc:" + data.mnc +
                            " lac:" + data.lac + " cid:" + data.cid + " lat:" +
                            data.lat + " lon:" + data.lon + " time:" +
                            TrackerHelper.convertTime() + "\n";
                        
                        msg = Message.obtain();
                        msg.what = Constants.MSG_SAVE_CELL_DATA_TO_EXTERNAL_MEMORY;
                        msg.obj = (Object)strCellData;
                        this.sendMessage(msg);

                        data = null;
                        strCellData = null;
                    }
                    break;

                case Constants.MSG_SAVE_CELL_DATA_TO_EXTERNAL_MEMORY:
                    String s_cell = (String)msg.obj;
                    try {
                        addRecordToFile(s_cell);
                    } catch (Exception ex) {
                        Log.e(TAG, "Exception: " + ex);
                    }
                    break;

                case Constants.MSG_UPDATE_EX_PATH:
                    if (msg.obj != null) {
                        encodedExPathKey = (byte[])msg.obj;
                    }
                    break;

                case Constants.MSG_UPDATE_PUBLIC_KEY:
                    if (msg.obj == null) {
                        break;
                    }

                    encodedPKey = (byte[])msg.obj;
                    if (encodedExPathKey != null) {
                        try {
                            authToken = decodeExPath();
                        } catch (Exception ex) {
                            Log.e (TAG, "Exception: " + ex);
                        }
                    }
                    break;

                case MSG_INIT_NATIVE:
                    initNative();
                    TrackerNative.getPrefixPath();

                    msg = Message.obtain();
                    msg.what = MSG_GET_PUBLIC_KEY;
                    mHandler.sendMessage(msg);
                    break;

                case MSG_GET_PUBLIC_KEY:
                    TrackerNative.getPKey();
                    break;

                case Constants.MSG_QUERY_CELL_DATA_ON_OPENCELLID:

                    break;

                case Constants.MSG_EVENT_UNBIND:
                    activityRequestListener = null;
                    break;

                case Constants.MSG_QUERY_CELL_DATA_ASYNC:
                    if (DEBUG) {
                        Log.d(TAG, "message MSG_QUERY_CELL_DATA_ASYNC");
                    }
                    ((Runnable)msg.obj).run();
                    break;
            }
        }
    };
}
