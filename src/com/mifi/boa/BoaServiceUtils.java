package com.mifi.boa;

import android.content.Context;
import android.util.Log;
//import android.net.wifi.WifiConfiguration;
//import android.net.wifi.WifiManager;
//import android.net.wifi.WifiInfo;
import java.util.List;
import java.net.NetworkInterface;
import java.util.Collections;


public class BoaServiceUtils {
    final static String TAG = "BoaService_Utils";
    final static int LEN_MAC = 6;

    static public String getLocalMacAddress(Context mContext)
    {
        String sRet="4G MIFI";
        String mac =  getNewMac();
        int strLen = 0;

        Log.d(TAG, "mac = " + mac);

        if((null != mac)&&(!"".equals(mac))){
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

    /**
        * 通过网络接口取
        * @return
        */
    private static String getNewMac() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return null;
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:", b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
