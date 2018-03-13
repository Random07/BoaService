package com.mifi.boa;
import android.net.wifi.WifiManager;
import android.content.Context;
import android.provider.Settings;
import android.net.wifi.WifiConfiguration;
import static android.net.ConnectivityManager.TETHERING_WIFI;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.os.Handler;

public class WiFiSettings {
    private static WiFiSettings sInstance;
    private Context mContext;
    private WifiManager mWifiManager;
    private Handler mHandler = new Handler();
    private OnStartTetheringCallback mStartTetheringCallback;
    static final String TAG = "WiFiSettings";
    public static final String WIFI_HOTSPOT_MAX_CLIENT_NUM = "wifi_hotspot_max_client_num";
    private String mWifiName;
    private boolean mWifiHide;
    private String mSecurityType;
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
	}
    
    public String getWiFiInfo(){
        
		WifiConfiguration mWifiConfig = mWifiManager.getWifiApConfiguration();
		 mWifiName = mWifiConfig.getPrintableSsid();
         mWifiHide = mWifiConfig.hiddenSSID;
         mSecurityType = getSecurityType(mWifiConfig); 
         mPassWord = mWifiConfig.preSharedKey;
         mMaxClientNum = Settings.System.getInt(mContext.getContentResolver(),WIFI_HOTSPOT_MAX_CLIENT_NUM,5);
        
        return "Confirm|WIFIShow|"+mWifiName+"|"+mWifiHide+"|"+mSecurityType+"|"+mPassWord+"|"+mMaxClientNum;
    }
    
    public void setWiFiInfo(String string){
           resoverString(string);
           ConfigWifiAp(mWifiName,mWifiHide,mSecurityType,mPassWord,mMaxClientNum);
        
    }
    

     private String getSecurityType(WifiConfiguration wifiConfig) {
        switch (wifiConfig.getAuthType()) {
            case KeyMgmt.WPA_PSK:
                return "wpa-psk";
            case KeyMgmt.WPA2_PSK:
                return "wpa2-psk";
            default:
                return "open";
         }
        }
     private void resoverString (String Str){
          String mArrayStr[] = mStr.split("\\|");
          mWifiName = mArrayStr[2];
          mWifiHide = true == mArrayStr[3].equals("true")? true : false ;
          mSecurityType = mArrayStr[4];
          mPassWord = mArrayStr[5];
          mMaxClientNum = Integer.valueOf(mArrayStr[6]);
     }
     public void ConfigWifiAp(String mSSID ,boolean mHiddenSSID ,String  mPasw,int mSecurityTypeIndex ) {       
        if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED) {
            android.util.Log.d(TAG,"Wifi AP config changed while enabled, stop and restart");
            mCm.stopTethering(TETHERING_WIFI);
        }
            WifiConfiguration mWifiConfig = getWifiApConfig(mSSID,mPasw,mHiddenSSID,mSecurityTypeIndex);
            mWifiManager.setWifiApConfiguration(mWifiConfig);
            startWifiAp();
    }

    public WifiConfiguration getWifiApConfig(String mSSID ,String mPasw,int mHiddenSSID ,int mSecurityTypeIndex ) {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID =mSSID;
        config.hiddenSSID = mHiddenSSID;
        switch (mSecurityTypeIndex) {
            case OPEN_INDEX:
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                return config;
            case WPA_INDEX:
                config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
                config.preSharedKey = mPassword;
                return config;
            case WPA2_INDEX:
                config.allowedKeyManagement.set(KeyMgmt.WPA2_PSK);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
                config.preSharedKey = mPassword;
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
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mCm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
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