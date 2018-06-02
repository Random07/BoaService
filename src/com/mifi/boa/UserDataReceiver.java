package com.mifi.boa;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class UserDataReceiver extends BroadcastReceiver {
    static final String TAG = "BoaService_UserDataReceiver";
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
        }else if("com.mifi.boa.clear.data".equals(action)){
            String data=intent.getStringExtra("data");
            Log.d(TAG, "clear data : " + data);
            mRet = mUserDataHandler.clearData(data);
            resultIntent.setAction("com.mifi.boa.confirm.ClearData");
        }else if("com.mifi.boa.reset.datalimit".equals(action)){
            mUserDataHandler.resetDataLimit();
        }else if("android.intent.action.BOOT_COMPLETED".equals(action)){
            mUserDataHandler.checkResetDataLimit();
        }

        Log.d(TAG, "mRet: " + mRet);
        if(!"".equals(mRet)){
            resultIntent.putExtra("data",mRet);
            context.sendBroadcast(resultIntent);
        }else{
            Log.d(TAG, "Do not send result!");
        }
    }
}
