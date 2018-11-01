package com.mifi.boa;

import android.content.Context;
import android.util.Log;
import java.util.List;
import java.net.NetworkInterface;
import java.util.Collections;
import com.mifi.boa.WiFiSettings;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;

public class BoaServiceUtils {
    private final String TAG = "BoaService_Utils";
    private final int MSG_RETRY_TIMEOUT = 1;
    private final int MAX_NUM_RETRIES = 5;
    private final long TIME_BETWEEN_RETRIES_MILLIS = 3000;
    private final int LEN_MAC = 6;
    private static BoaServiceUtils sInstance;
    private int mNumRetriesSoFar;
    private WiFiSettings mWiFiSettings;
    private Context mContext;

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_RETRY_TIMEOUT:
                    onRetryTimeout();
                    break;
                default:
                    Log.d(TAG, "handleMessage: unexpected message:" + msg.what);
                    break;
            }
        }
    };

    public static BoaServiceUtils getInstance(Context mCont){
        if (null == sInstance) {
            sInstance = new BoaServiceUtils(mCont);
        }

        return sInstance;
    }

    private BoaServiceUtils(Context mCont){
        mContext = mCont;
    }

    public void ConfigWifiAp(WiFiSettings mwificonf){
        mNumRetriesSoFar = 0;
        mWiFiSettings = mwificonf;
        onRetryTimeout();
    }

    private String getLocalMacAddress(String mac)
    {
        String sRet="4G MIFI";
        int strLen = 0;

        Log.d(TAG, "mac = " + mac);

        mac = mac.replace(":","");
        Log.d(TAG, "remove separator,mac = " + mac);

        strLen = mac.length();
        Log.d(TAG, "mac strLen = " + strLen);
        if(strLen <= LEN_MAC){
            sRet = sRet + "_" + mac;
        }else{
            sRet = sRet + "_" + mac.substring(mac.length() - LEN_MAC);
        }

        Log.d(TAG, "return mac = " + sRet);
        return sRet;
    }

    private String getNewMac() {
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

    private void onRetryTimeout() {
        String mac =  getNewMac();

        Log.d(TAG, "onRetryTimeout,mac = " + mac);
        if((null != mac)&&(!"".equals(mac))){
            cancelRetryTimer();
            mNumRetriesSoFar = 0;
            mWiFiSettings.ConfigWifiAp(getLocalMacAddress(mac),false,2,"12345678",6);
        }else{
            mNumRetriesSoFar++;
            Log.d(TAG, "mNumRetriesSoFar = " + mNumRetriesSoFar);
            if (mNumRetriesSoFar > MAX_NUM_RETRIES) {
                mWiFiSettings.ConfigWifiAp("4G MIFI",false,2,"12345678",6);
            } else {
                startRetryTimer();
            }
        }
    }

    private void startRetryTimer() {
        cancelRetryTimer();
        mHandler.sendEmptyMessageDelayed(MSG_RETRY_TIMEOUT, TIME_BETWEEN_RETRIES_MILLIS);
    }

    private void cancelRetryTimer() {
        mHandler.removeMessages(MSG_RETRY_TIMEOUT);
    }
}
