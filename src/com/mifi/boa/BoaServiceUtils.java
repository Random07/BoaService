package com.mifi.boa;

import android.content.Context;
import android.util.Log;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;

public class BoaServiceUtils {
    final static String TAG = "BoaService_Utils";
    final static int LEN_MAC = 6;

    static public String getLocalMacAddress(Context mContext)
    {
        String sRet="4G MIFI";
        if(null != mContext){
            Log.d(TAG, "mContext is null, so return null!");
            return sRet;
        }
        WifiManager mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if(null != mWifiManager){
            Log.d(TAG, "mWifiManager is null, so return null!");
            return sRet;
        }
        WifiInfo info = mWifiManager.getConnectionInfo();
        if(null != info){
            Log.d(TAG, "info is null, so return null!");
            return sRet;
        }
        String mac =  info.getMacAddress();
        int strLen = 0;

        Log.d(TAG, "mac = " + mac);

        if(null != mac){
            mac = mac.replace(":","");
            Log.d(TAG, "remove separator,mac = " + mac);

            strLen = mac.length();
            Log.d(TAG, "mac strLen = " + strLen);
            if(strLen <= LEN_MAC){
                sRet = sRet + "_" + mac;
            }else{
                sRet = sRet + "_" + mac.substring(mac.length() - LEN_MAC);
            }
        }

        Log.d(TAG, "return mac = " + sRet);
        return sRet;
    }    
}