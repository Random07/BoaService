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
import android.provider.Settings.System;

public class WiFiSettings {
    private static WiFiSettings sInstance;
    private Context mContext;
    private WifiManager mWifiManager;
    private ConnectivityManager mCm;
    private Handler mHandler = new Handler();
    private OnStartTetheringCallback mStartTetheringCallback;
    static final String TAG = "WiFiSettings";
    public static final String WIFI_HOTSPOT_MAX_CLIENT_NUM = "wifi_hotspot_max_client_num";
	public static final int OPEN_INDEX = 0;
    public static final int WPA_INDEX = 1;
    public static final int WPA2_INDEX = 2;
    private String mWifiName;
    private boolean mWifiHide;
    private int mSecurityType;
    private String mPassWord;
    private int mMaxClientNum;

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
	}
    
    public String getWiFiInfo(){
        WifiConfiguration mWifiConfig = mWifiManager.getWifiApConfiguration();
        mWifiName = mWifiConfig.getPrintableSsid();
        mWifiHide = mWifiConfig.hiddenSSID;
        mSecurityType = getSecurityType(mWifiConfig);
        mPassWord = mWifiConfig.preSharedKey;
        mMaxClientNum = System.getInt(mContext.getContentResolver(),WIFI_HOTSPOT_MAX_CLIENT_NUM,5);
        
        return "Confirm|WIFIShow|"+mWifiName+"|"+mWifiHide+"|"+mSecurityType+"|"+mPassWord+"|"+mMaxClientNum;
    }
    
    public void setWiFiInfo(String str){
       analysisString(str);
       ConfigWifiAp(mWifiName,mWifiHide,mSecurityType,mPassWord,mMaxClientNum);
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

    public void ConfigWifiAp(String mSSID ,boolean mHidSSID ,int mSecurityType,String  mPasw,int mMaxCl ) {
        if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED) {
            android.util.Log.d(TAG,"Wifi AP config changed while enabled, stop and restart");
            mCm.stopTethering(TETHERING_WIFI);
        }
        WifiConfiguration mWifiConfig = getWifiApConfig(mSSID,mHidSSID,mSecurityType,mPasw,mMaxCl);
        mWifiManager.setWifiApConfiguration(mWifiConfig);
        startWifiAp();
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
        android.util.Log.d(TAG,"startWifiAp begin ConnectivityManager");
        mStartTetheringCallback = new OnStartTetheringCallback();
        mCm.startTethering(TETHERING_WIFI, true, mStartTetheringCallback, mHandler);
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