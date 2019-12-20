package com.hsp.gsm.cell.tracker;

public class GsmOperator {

    private String serviceOperatorNumeric;

    private int mcc;

    private int mnc;

    public GsmOperator (String operator) {
        mcc = -1;
        mnc = -1;
        if (operator != null && operator.length() > 3) {
            serviceOperatorNumeric = new String(operator);
            update (serviceOperatorNumeric);
        }
    }

    private void update (String data) {
        try {
            mcc = Integer.parseInt(data.substring(0, 3));
            mnc = Integer.parseInt(data.substring(3));
        } catch (NumberFormatException e) {
            reset ();
        }
    }

    private void reset () {
        serviceOperatorNumeric = new String("");
        mcc = -1;
        mnc = -1;
    }

    public static interface OnChangeServiceOperator {
        public void onChange(String operator);
    }
}
