package com.mifi.boa;

import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

public class UserData {
    private static UserData sInstance;
	private Context mContext;
    
    public static UserData getInstance(Context mCont){
        if (null == sInstance) {
            sInstance = new UserData(mCont);
        }
        return sInstance;
    }

	public UserData (Context mCont) {
        mContext = mCont;
	}

    public String setDataLimit(String mData){
        return "";
    }

    public String getDataStatic(){
        return "";
    }

    public void  setNetworkType(String mType){
        int subid = SubscriptionManager.getDefaultVoiceSubscriptionId();
        TelephonyManager tm = TelephonyManager.from(mContext);
        tm.setPreferredNetworkType(subid, Integer.parseInt(mType));
    }

    public int getNetworkType(){
        int subid = SubscriptionManager.getDefaultVoiceSubscriptionId();
        TelephonyManager tm = TelephonyManager.from(mContext);
        return tm.getPreferredNetworkType(subid);
    }
}