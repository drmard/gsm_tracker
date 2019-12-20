package com.hsp.gsm.cell.tracker;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class JsonPostTask extends AsyncTask<Void, Void, String> {

    private static final boolean DEBUG = false;

    private static final String TAG = JsonPostTask.class.getSimpleName();

    public static interface Callback {
        public void onTaskComplete(String data);
    }

    private Callback callback;
    public  Cell cell = null;
    private OutputStream out = null;
    private int responseCode = -1;
    private InputStream response = null;
    private String account_key = null;
 
    public JsonPostTask(Cell queryCell, String key,
            Callback taskCallback) {
        callback = taskCallback;
        account_key = new String(key);
        cell = queryCell;
    }

    private byte[] setPostData(Cell cell, String openCellIDToken) {
        StringBuilder data = new StringBuilder();
        data.append("{\"token\": \"");
        data.append(openCellIDToken);
        data.append("\", \"radio\": \"gsm\", \"mcc\": ");
        data.append("" + cell.mcc);
        data.append(", \"mnc\": ");
        data.append("" + cell.mnc);
        data.append(", \"cells\": [{\"lac\": ");
        data.append("" + cell.lac);
        data.append(", \"cid\": ");
        data.append("" + cell.cid);
        data.append("}], ");
        data.append("\"address\": 1}");

        String stringPostData = data.toString();

        return stringPostData.getBytes();
    }

    @Override
    protected String doInBackground(Void... params) {
        String str = null;
        try {
            str = loadData();
        } catch (IOException ioe) {
            Log.e(TAG, "IOException: " + ioe);
        }
        return str;
    }

    @Override
    protected void onPostExecute(String result) {
        if(result != null)
        Log.d(TAG, "-onPostExecute-  result - " + result);
        callback.onTaskComplete(result);
    }

    private String loadData() throws IOException {
        return downloadData();
    }

    private String downloadData() throws IOException, NullPointerException {
        BufferedReader breader = null;
        StringBuilder res = new StringBuilder();
        Reader reader = null;
        URL url = new URL(Constants.OPEN_CELL_ID_PROCESS_URL);

        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setReadTimeout(10000);
        conn.setUseCaches(true);
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");

        out = conn.getOutputStream();

        // add JSON POST data
        out.write(setPostData(cell, account_key));
        out.flush();

        conn.connect();

        responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            response = conn.getInputStream();
            reader = new InputStreamReader(response, "UTF-8");
            breader = new BufferedReader(reader);
            while (true) {
                String line = breader.readLine();
                if (line != null) {
                    res.append(line);
                } else {
                    break;
                }
            }
        } else {
            return "connection error";
        }

        out.close();
        response.close(); 
        conn.disconnect();

        if (DEBUG) {
            Log.d(TAG, "response - " + res.toString());
        }

        return res.toString();
    }
}

