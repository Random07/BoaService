package com.mifi.boa;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.text.NumberFormat;
import android.os.BatteryManager;

public class BoaReceiver extends BroadcastReceiver {
    static final String TAG = "BoaReceiver";
    public String batterylevl="";
	private DeviceInfo mDeviceInfo;
    
    public BoaReceiver(DeviceInfo mDeInfo) {
        mDeviceInfo =mDeInfo;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals("android.intent.action.CLEAR_MIFI_DATA")) {
            mDeviceInfo.setReFactory();
        }
    }

    private String getBatterylevlFromIntent(Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        double percentage = (level * 100) / scale;
        return NumberFormat.getPercentInstance().format(percentage);
    }

    public String getBatterylevl(){
        return batterylevl;
    }
}

