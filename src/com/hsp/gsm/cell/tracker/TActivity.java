package com.hsp.gsm.cell.tracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;

public class TActivity extends Activity {
    @Override public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        requestWindowFeature (Window.FEATURE_NO_TITLE);
        setContentView(R.layout.tactivity);
    }

    @Override public void onResume() {
        super.onResume();
        TrackerHelper.startService(this);

        Intent intent = new Intent("gsm.tracker.MAIN_VIEW");
        startActivity(intent);

        finish();
    }

    @Override public void onDestroy() {
        super.onDestroy();
    }
}
