package com.mifi.boa;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.text.NumberFormat;
import android.os.BatteryManager;


public class BoaReceiver extends BroadcastReceiver {

    static final String TAG = "BoaReceiver";
    
    public BoaReceiver() {
    }
    @Override
    public void onReceive(Context context, Intent intent) {
       
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                String batterylevl = getBatterylevl(intent);   
                android.util.Log.d(TAG,batterylevl); 
                
            }
    }

    private String getBatterylevl(Intent intent) {
          int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
          int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
          double percentage = (level * 100) / scale;
          return NumberFormat.getPercentInstance().format(percentage);

    }
}
