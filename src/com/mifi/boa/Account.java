package com.mifi.boa;

import android.content.Context;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import java.util.Date;
import java.text.SimpleDateFormat;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

public class Account {
    final String TAG = "BoaService_Account";
    final String MIFI_USERNAME = "persist.sys.user.name";
    final String MIFI_PASSWORD = "persist.sys.user.password";
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
        Date date = new Date(2018, 8, 1);
        long endTime = date.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String strDate = dateFormat.format(mCurTime);
        
        if(mCurTime > endTime){
            bRet = false;
        }

        Log.d(TAG,"strDate = " + strDate + ", isAccountValid = " + bRet);
        return true; // bRet;
    }

    public void setUserName(String mUserName){
        SystemProperties.set(MIFI_USERNAME,mUserName);
    }

    public String getUserName(){
        return SystemProperties.get(MIFI_USERNAME,"admin");
    }

    public void setPassWord(String mPassWord){
        SystemProperties.set(MIFI_PASSWORD,mPassWord);
    }

    public String getPassWord(){
        return SystemProperties.get(MIFI_PASSWORD,"admin");
    }

    public String getPasswordAndUserName(){
        return ("1|Login|" + getUserName() + "|" + getPassWord());
    }

    public String setPasswordAndUserName(String data){
        String[] mData = data.split("\\|");

        if(mData.length < 4 || TextUtils.isEmpty(mData[2]) || TextUtils.isEmpty(mData[3])){
            return ("0|SetAccountInfo");
        }
		WifiManager mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration mWifiConfig = mWifiManager.getWifiApConfiguration();
        String mWifiName = mWifiConfig.getPrintableSsid();
		if(mWifiName.equals(mData[2])) return "0|SetAccountInfo";
        setUserName(mData[2]);
        setPassWord(mData[3]);
        if(getUserName().equals(mData[2]) && getPassWord().equals(mData[3])){
            return ("1|SetAccountInfo");
        }else{
            return ("0|SetAccountInfo");
        }
    }

    
}