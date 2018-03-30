package com.mifi.boa;

import android.content.Context;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

public class Account {
    final String TAG = "BoaService_Account";
    final String MIFI_USERNAME = "persist.sys.user.name";
    final String MIFI_PASSWORD = "persist.sys.user.password";
    final String MIFI_LANGUAGE = "persist.sys.user.language";
    final long MIFI_END_TIME = 1527782400000L;
    private static Account sInstance;
    private Context mContext;
    public static Account getInstance(Context mCont){
        if (null == sInstance) {
            sInstance = new Account(mCont);
        }
        return sInstance;
    }

    private Account (Context mCont){
        mContext = mCont;
    }

    public boolean isAccountValid(){
        long mCurTime = System.currentTimeMillis();
        boolean bRet = true;

        if(mCurTime > MIFI_END_TIME){
            bRet = false;
        }

        Log.d(TAG,"mCurTime = " + mCurTime + ", isAccountValid = " + bRet);
        return bRet;
    }

    public void setUserName(String mUserName){
        SystemProperties.set(MIFI_USERNAME,mUserName);
    }

    public String getUserName(){
        return SystemProperties.get(MIFI_USERNAME,"Admin");
    }

    public void setPassWord(String mPassWord){
        SystemProperties.set(MIFI_PASSWORD,mPassWord);
    }

    public String getPassWord(){
        return SystemProperties.get(MIFI_PASSWORD,"123321");
    }

    public String getPasswordAndUserName(){
        return ("1|Login|" + getUserName() + "|" + getPassWord());
    }

    public String setPasswordAndUserName(String data){
        String[] mData = data.split("\\|");

        if(mData.length < 4 || TextUtils.isEmpty(mData[2]) || TextUtils.isEmpty(mData[3])){
            return ("0|SetAccountInfo");
        }

        setUserName(mData[2]);
        setPassWord(mData[3]);
        if(getUserName().equals(mData[2]) && getPassWord().equals(mData[3])){
            return ("1|SetAccountInfo");
        }else{
            return ("0|SetAccountInfo");
        }
    }
 
    public String getLanguage(){
        return ("1|GetLanguage|" + SystemProperties.get(MIFI_LANGUAGE,"1"));
    }

    public String SetLanguage(String data){
        String[] mData = data.split("\\|");
        SystemProperties.set(MIFI_LANGUAGE,mData[2]);
        return "1|GetLanguage";
    }
}