package com.mifi.boa;
import android.telephony.TelephonyManager;
import android.net.wifi.WifiManager;
import android.content.Context;
public class DeviceInfo {
    private static DeviceInfo sInstance;
	private Context mContext;

    public static DeviceInfo getInstance(Context mCont){
        if (null == sInstance) {
            sInstance = new DeviceInfo(mContext);
        }
        return sInstance;
    }
	
    private DeviceInfo(Context     mCont){
             mContext = mCont;
	}
    public String getDeviceInfo(){
        TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(TELEPHONY_SERVICE);
		WifiManager mWifiManager = (WifiManager) getApplication().getSystemService(Context.WIFI_SERVICE);
        String mSimNumber = telephonyManager.getLine1Number();
		String mDeviceIMEI = telephonyManager.getDeviceId(); 
		String imsi = telephonyManager.getSubscriberId();
		String mSSID = mWifiManager.getWifiApConfiguration().getPrintableSsid();
		//String mSSID = mWifiManager.getWifiApConfiguration().SSID;
		String mMaxConnect = "";
		String mIpAddress = "";
		String mMacAddress = "";
		String mWanIpAddress = "";
		String mSwVersion = "";
		String mFirmwareVersion = "";
		String mHwVersion ="";
		
		
        return "";
    }

    public String setReBoot(){
        return "";
    }

    public String setReFactory(){
        return "";
    }
}