package com.hsp.gsm.cell.tracker;

public interface UiEventController {

    static final int EVENT_SERVICE_CONNECTION_COMPLETE           = 20001;
    static final int EVENT_TELEPHONY_SERVICE_START_LISTEN        = 20002;
    static final int EVENT_CELL_LOCATION_NOT_FOUND_IN_LOCAL_DB   = 20003;

    void onUiEvent(int event, Object context);

    void setRequestListener(ActivityRequestListener listener);

    String getSimOperator();

    String getServiceOperator();

    Cell getCurrentCell ();

    boolean getPhoneServiceState();
    
    public interface ActivityRequestListener {
        static final int EVENT_REQUEST_SERVICE_NONE              = 10004;
        static final int EVENT_REQUEST_CELL_DATA_UPDATE          = 10005;
        static final int EVENT_REQUEST_SERVICE_OPERATOR_CHANGED  = 10006;
        static final int EVENT_REQUEST_CELL_DATA_FROM_DB         = 10007;

        void onRequest(int requestCode);
        void onRequestObject(int requestCode, Object obj);
    }
}
