package com.mifi.boa;
import android.net.wifi.WifiManager;
import android.content.Context;
public class WiFiSettings {
    private static WiFiSettings sInstance;
    private Context mContext;
    public static WiFiSettings getInstance(Context mCont){
        if (null == sInstance) {
            sInstance = new WiFiSettings(mCont);
        }
        return sInstance;
    }
		public WiFiSettings (Context mCont) {
             mContext = mCont;
	}
    
    public String getWiFiInfo(){
        WifiManager mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		WifiConfiguration mWifiConfig = mWifiManager.getWifiApConfiguration();
		String mWifiName = mWifiConfig.getPrintableSsid();
	
        
        return "";
    }
    
    public String setWiFiInfo(){
        return "";
    }
}