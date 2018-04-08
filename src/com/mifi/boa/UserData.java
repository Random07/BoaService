package com.mifi.boa;

import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import com.android.internal.telephony.RILConstants;

public class UserData {
    final String TAG = "BoaService_UserData";
    static final int preferredNetworkMode = RILConstants.PREFERRED_NETWORK_MODE;
    private static UserData sInstance;
    private Context mContext;
    int subid;
    TelephonyManager tm;
    private UserDataConfirmReceiver mUserDataConfirmReceiver;
    String mUserDataResult;
    IntentFilter intentFilter;

    public static UserData getInstance(Context mCont){
        if (null == sInstance) {
            sInstance = new UserData(mCont);
        }
        return sInstance;
    }

    public UserData (Context mCont) {
        mContext = mCont;
        tm = TelephonyManager.from(mContext);
        mUserDataConfirmReceiver = new UserDataConfirmReceiver();
        intentFilter = new IntentFilter();
        intentFilter.addAction("com.mifi.boa.confirm.DataStatic");
        intentFilter.addAction("com.mifi.boa.confirm.DataLimit");
        mContext.registerReceiver(mUserDataConfirmReceiver, intentFilter);
    }

    public void updateSubId(){
        int[] subId = SubscriptionManager.getSubId(0);
        if (subId == null || subId.length == 0
            || !SubscriptionManager.isValidSubscriptionId(subId[0])) {
            subid = -1;
        } else {
            subid = subId[0];
        }
        Log.d(TAG, "updateSubId subid = " + subid);
    }

    public String setDataLimit(String data){
        mUserDataResult = "";
        Intent intent = new Intent();
        intent.setAction("com.mifi.boa.set.DataLimit");
        intent.putExtra("data",data);
        mContext.sendBroadcast(intent);
        while("".equals(mUserDataResult)){
            Log.d("xjhe", "setDataLimit, mUserDataResult = "+mUserDataResult);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Do nothing
        }
        Log.d(TAG, "setDataLimit, mUserDataResult = "+mUserDataResult);
        return mUserDataResult;
    }

    public String getDataStatic(){
        mUserDataResult = "";
        Intent intent = new Intent();
        intent.setAction("com.mifi.boa.get.DataStatic");
        mContext.sendBroadcast(intent);
        while("".equals(mUserDataResult)){
            Log.d("xjhe", "getDataStatic, mUserDataResult = "+mUserDataResult);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Do nothing
        }
        Log.d(TAG, "getDataStatic, mUserDataResult = "+mUserDataResult);
        return mUserDataResult;
    }

    public String  setNetworkType(String data){
        String[] mData = data.split("\\|");

        updateSubId();
        if(tm.setPreferredNetworkType(subid, Integer.parseInt(mData[2]))){
            android.provider.Settings.Global.putInt(mContext.getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE + subid,
                        Integer.parseInt(mData[2]));
            return ("1|SetNetworkType");
        }else{
            return ("0|SetNetworkType");
        }
    }

    public String getNetworkType(){
        int mNetworkType = -1;
        updateSubId();
        //mNetworkType = tm.getPreferredNetworkType(subid);

        mNetworkType = android.provider.Settings.Global.getInt(mContext.
                    getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE + subid,
                    preferredNetworkMode);

        if(-1 == mNetworkType){
            return ("0|GetNetworkType");
        }else{
            return ("1|GetNetworkType|" + String.valueOf(mNetworkType));
        }
    }

    private class UserDataConfirmReceiver extends BroadcastReceiver {
        private boolean listeningToManagedProfileEvents;

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.v(TAG, "Received broadcast: " + action);
            if (action.equals("com.mifi.boa.confirm.DataLimit")
                    || action.equals("com.mifi.boa.confirm.DataStatic")) {
                mUserDataResult = intent.getStringExtra("data");
                Log.d(TAG, "confirm data = " + mUserDataResult);
            }
        }
    }
}