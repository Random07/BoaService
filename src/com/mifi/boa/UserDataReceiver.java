package com.mifi.boa;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class UserDataReceiver extends BroadcastReceiver {
    static final String TAG = "Boservice_UserDataReceiver";
    private UserDataHandler mUserDataHandler;

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Intent resultIntent = new Intent();
        String mRet = "";

        mUserDataHandler = UserDataHandler.getInstance(context);
        Log.d(TAG, "Received broadcast: " + action);
        if("com.mifi.boa.set.DataLimit".equals(action)){
            String data=intent.getStringExtra("data");
            Log.d(TAG, "data : " + data);
            mRet = mUserDataHandler.setDataLimit(data);
            resultIntent.setAction("com.mifi.boa.confirm.DataLimit"); 
        }else if("com.mifi.boa.get.DataStatic".equals(action)){
            mRet = mUserDataHandler.getDataStatic();
            resultIntent.setAction("com.mifi.boa.confirm.DataStatic");
        }
        Log.d(TAG, "mRet: " + mRet);
        resultIntent.putExtra("data",mRet);
        context.sendBroadcast(resultIntent);
    }
}
