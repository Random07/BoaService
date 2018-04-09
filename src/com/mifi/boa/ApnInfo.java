package com.mifi.boa;

import android.util.Log;

public class ApnInfo {
    final String TAG = "BoaService_apn";
    private int mIndex;
    private String mName;
    private String mApn;
    private String mMcc;
    private String mMnc;
    private int mAuthTypeIndex;
    private String mUserName;
    private String mPassword;

    public ApnInfo(int index,
    					String name,
    					String apn,
    					String userName,
    					String password,
    					String mcc,
    					String mnc,
    					int authTypeIndex){
        mIndex = index;
        mName = name;
        mApn = apn;
        mMcc = mcc;
        mMnc = mnc;
        mAuthTypeIndex = authTypeIndex;
        mUserName = userName;
        mPassword = password;
    }

    public String getApn(){
        return mApn;
    }

    public int getApnIndex(){
        return mIndex;
    }

    public String toString(){
        return (mIndex + "|" + mName + "|" + mApn + "|" +mMcc
    			+ "|" +mMnc + "|" +mAuthTypeIndex
    			+ "|" +mUserName + "|" +mPassword);
    }
}