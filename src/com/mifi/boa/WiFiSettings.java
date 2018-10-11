package com.mifi.boa;

import android.net.wifi.WifiManager;
import android.content.Context;
import android.provider.Settings.System;
import android.net.wifi.WifiConfiguration;
import android.net.ConnectivityManager;
import static android.net.ConnectivityManager.TETHERING_WIFI;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.os.Handler;
import android.net.wifi.WpsInfo;
import android.text.TextUtils;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.SystemProperties;

public class WiFiSettings {
    private static WiFiSettings sInstance;
    private Context mContext;
    private WifiManager mWifiManager;
    private ConnectivityManager mCm;
    private Handler mHandler = new Handler();
    private OnStartTetheringCallback mStartTetheringCallback;
    static final String TAG = "WiFiSettings";
    public static final String WIFI_HOTSPOT_MAX_CLIENT_NUM = "wifi_hotspot_max_client_num";
    public static final String WIFI_HOTSPOT_AUTO_DISABLE = "wifi_hotspot_auto_disable";
    public static final String MIFI_USERNAME = "persist.sys.user.name";
    public static final int OPEN_INDEX = 0;
    public static final int WPA_INDEX = 1;
    public static final int WPA2_INDEX = 2;
    private String mWifiName;
    private boolean mWifiHide;
    private int mSecurityType;
    private String mPassWord;
    private int mMaxClientNum;
    private boolean mRestartWifiApAfterConfigChange = false;
    private TetherChangeReceiver mTetherChangeReceiver;
    public static WiFiSettings getInstance(Context mCont){
        if (null == sInstance) {
            sInstance = new WiFiSettings(mCont);
        }
        return sInstance;
    }

    public WiFiSettings (Context mCont) {
        mContext = mCont;
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mCm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        System.putInt(mContext.getContentResolver(),WIFI_HOTSPOT_AUTO_DISABLE,0);
        mTetherChangeReceiver = new TetherChangeReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        filter.addAction(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
        mContext.registerReceiver(mTetherChangeReceiver, filter);
    }
    
    public String getWiFiInfo(){
        WifiConfiguration mWifiConfig = mWifiManager.getWifiApConfiguration();
        mWifiName = mWifiConfig.getPrintableSsid();
        mWifiHide = mWifiConfig.hiddenSSID;
        mSecurityType = getSecurityType(mWifiConfig);
        mPassWord = mWifiConfig.preSharedKey;
        mMaxClientNum = System.getInt(mContext.getContentResolver(),WIFI_HOTSPOT_MAX_CLIENT_NUM,5);
        
        return "1|WIFIShow|"+mWifiName+"|"+mWifiHide+"|"+mSecurityType+"|"+mPassWord+"|"+mMaxClientNum;
    }
    
    public String setWiFiInfo(String str){
        analysisString(str);
		String username = SystemProperties.get(MIFI_USERNAME,"Admin"); 
		if(username.equals(mWifiName)) return "0|WIFISetting";
        String  mSetWifiresult= ConfigWifiAp(mWifiName,mWifiHide,mSecurityType,mPassWord,mMaxClientNum);
        return mSetWifiresult+"|WIFISetting" ;
    }

    private int getSecurityType(WifiConfiguration wifiConfig) {
        switch (wifiConfig.getAuthType()) {
            case KeyMgmt.WPA_PSK:
                return 1;
            case KeyMgmt.WPA2_PSK:
                return 2;
            default:
                return 0;
        }
    }

    private void analysisString (String mStr){
        String mArrayStr[] = mStr.split("\\|");
        mWifiName = mArrayStr[2];
        mWifiHide = mArrayStr[3].equals("true")? true : false ;
        mSecurityType = Integer.valueOf(mArrayStr[4]);
        mPassWord = mArrayStr[5];
        mMaxClientNum = Integer.valueOf(mArrayStr[6]);
    }

    public String ConfigWifiAp(String mSSID ,boolean mHidSSID ,int mSecurityType,String  mPasw,int mMaxCl ) {
        if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED) {
            android.util.Log.d(TAG,"Wifi AP config changed while enabled, stop and restart");
             mRestartWifiApAfterConfigChange = true;
            mCm.stopTethering(TETHERING_WIFI);
        }
        WifiConfiguration mWifiConfig = getWifiApConfig(mSSID,mHidSSID,mSecurityType,mPasw,mMaxCl);
        boolean mConfigResultboolean = mWifiManager.setWifiApConfiguration(mWifiConfig);
        String mConfigResult = mConfigResultboolean ? "1" : "0";
        return mConfigResult;
    }

    public WifiConfiguration getWifiApConfig(String mSSID ,boolean mHidSSID,int mSecurityType ,String mPasw,int mMaxCl) {
        WifiConfiguration config = new WifiConfiguration();

        config.SSID =mSSID;
        config.hiddenSSID = mHidSSID;
		System.putInt(mContext.getContentResolver(),WIFI_HOTSPOT_MAX_CLIENT_NUM,mMaxCl);

        switch (mSecurityType) {
            case OPEN_INDEX:
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                return config;
            case WPA_INDEX:
                config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
                config.preSharedKey = mPasw;
                return config;
            case WPA2_INDEX:
                config.allowedKeyManagement.set(KeyMgmt.WPA2_PSK);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
                config.preSharedKey = mPasw;
                return config;
        }
        return null;
    }

    /**
     *    open wifiAp
     * @param mSSID
     * @param mPasswd
     * @param securty
     */
    public void startWifiAp(){
        android.util.Log.d(TAG,"startWifiAp begin");
        mStartTetheringCallback = new OnStartTetheringCallback();
        mCm.startTethering(TETHERING_WIFI, true, mStartTetheringCallback, mHandler);
    }

    public String setWPSConnectMode(String mData){
        String mArrayStr[] = mData.split("\\|");
        WpsInfo config = new WpsInfo();
        int mWpsMode = Integer.parseInt(mArrayStr[2]);

        config.setup = Integer.parseInt(mArrayStr[2]);
        if(mWpsMode == 0){
            config.setup = WpsInfo.PBC;
            config.BSSID = "any";
        }else if(mWpsMode == 1){
            if(mArrayStr.length < 4 || TextUtils.isEmpty(mArrayStr[3])){
                return "0|SetWPSConnectMode";
            }
            config.setup = WpsInfo.DISPLAY;
            config.pin = mArrayStr[3];
        }

        if(mWifiManager.startApWps(config)){
            return "1|SetWPSConnectMode";
        }else{
            return "0|SetWPSConnectMode";
        }
    }

    private class TetherChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            android.util.Log.d(TAG,"TetherChangeReceiver action"+action);
            if (action.equals(ConnectivityManager.ACTION_TETHER_STATE_CHANGED)) {
                if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_DISABLED&& mRestartWifiApAfterConfigChange) {
                    mRestartWifiApAfterConfigChange = false;
                    android.util.Log.d(TAG,"Restarting WifiAp due to prior config change.");
                    startWifiAp();
                }
            } else if (action.equals(WifiManager.WIFI_AP_STATE_CHANGED_ACTION)) {
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_AP_STATE, 0);
                if (state == WifiManager.WIFI_AP_STATE_DISABLED && mRestartWifiApAfterConfigChange) {
                    mRestartWifiApAfterConfigChange = false;
                    android.util.Log.d(TAG,"Restarting WifiAp due to prior config change");
                    startWifiAp();
                }
            }

        }
    }

    private static final class OnStartTetheringCallback extends
        ConnectivityManager.OnStartTetheringCallback {
        @Override
        public void onTetheringStarted() {
            android.util.Log.d(TAG,"onTetheringStarted");
        }

        @Override
        public void onTetheringFailed() {
            android.util.Log.d(TAG,"onTetheringFailed");
        }
    }
}