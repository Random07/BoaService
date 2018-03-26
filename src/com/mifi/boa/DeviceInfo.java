package com.mifi.boa;

import android.telephony.TelephonyManager;
import android.net.wifi.WifiManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.provider.Settings.System;
import android.net.LinkProperties;
import java.net.InetAddress;
import java.util.Iterator;
import android.os.PowerManager;
import android.telephony.SignalStrength;
import android.telephony.PhoneStateListener;
import android.content.Intent;

public class DeviceInfo {
    private static DeviceInfo sInstance;
	private Context mContext;
    public static final String WIFI_HOTSPOT_MAX_CLIENT_NUM = "wifi_hotspot_max_client_num";
    private ConnectivityManager mCM;
    private TelephonyManager telephonyManager;
    private WifiManager mWifiManager;
    private BoaReceiver mBoaReceiver;
    private MyPhoneStateListener mMyPhoneStateListener;

    public static DeviceInfo getInstance(Context mCont,BoaReceiver mBoaReceiver){
        if (null == sInstance) {
            sInstance = new DeviceInfo(mCont,mBoaReceiver);
        }
        return sInstance;
    }
	
    private DeviceInfo(Context mCont,BoaReceiver mBoaReceiver){
        mContext = mCont;
        mBoaReceiver = mBoaReceiver;
        mCM = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        telephonyManager = TelephonyManager.from(mContext);
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mMyPhoneStateListener = new MyPhoneStateListener();
    }

    public String getDeviceInfo(){
        String mSimNumber = telephonyManager.getLine1Number();
        String mDeviceIMEI = telephonyManager.getDeviceId();
        String imsi = telephonyManager.getSubscriberId();
        String mSSID = mWifiManager.getWifiApConfiguration().getPrintableSsid();
        //String mSSID = mWifiManager.getWifiApConfiguration().SSID;
        int mMaxConnect = System.getInt(mContext.getContentResolver(),WIFI_HOTSPOT_MAX_CLIENT_NUM,5);
        String mIpAddress = getIpAddresses();
        String mMacAddress = getMacAddress();
        String mWanIpAddress = SystemProperties.get("net.wimax.mac.address", "Unavailable");;
        String mSwVersion = "Android 7.0";
        String mFirmwareVersion = "Not Know";
        String mHwVersion ="Not Know";
		
        return "Confirm|"+"DeviceInfo|"+mSimNumber+"|"+mDeviceIMEI+"|"+imsi+"|"+mSSID+"|"+mMaxConnect+"|"+mWanIpAddress+"|"+mSwVersion+"|"+mFirmwareVersion+"|"+mHwVersion;
    }

    private String getIpAddresses() {
        LinkProperties prop = mCM.getActiveLinkProperties();
        if (prop == null) return null;
        Iterator<InetAddress> iter = prop.getAllAddresses().iterator();
        // If there are no entries, return null
        if (!iter.hasNext()) return null;
        // Concatenate all available addresses, comma separated
        String addresses = "";
        while (iter.hasNext()) {
            addresses += iter.next().getHostAddress();
            if (iter.hasNext()) addresses += "\n";
        }
        return addresses;
    }

     public String getMacAddress(){
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        boolean hasMacAddress = wifiInfo != null && wifiInfo.hasRealMacAddress();
        String macAddress = hasMacAddress ? wifiInfo.getMacAddress() : null;
        
        return !TextUtils.isEmpty(macAddress) ? macAddress : "Unavailable";
    }

    public String setReBoot(){
		PowerManager mPM = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
        mPM.reboot("");
        return "1|ReBoot";
    }

    public String getCommon(){
	   String mBatteryLevl = mBoaReceiver.getBatterylevl();
       int networkType = telephonyManager.getNetworkType();
       String mSpn = telephonyManager.getSimOperatorName();
       int mRsrp = mMyPhoneStateListener.getSignalStrength();
        
       return "Confirm|Common|"+mBatteryLevl+"|"+networkType+"|"+mSpn+"|"+mRsrp;
    }

    public void setDataClose(){
        telephonyManager.setDataEnabled(false);
    }

    public String setReFactory(){
        /*Intent intent = new Intent(Intent.ACTION_FACTORY_RESET);
        intent.setPackage("android");
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.putExtra(Intent.EXTRA_REASON, "CryptKeeper.MAX_FAILED_ATTEMPTS");
        mContext.sendBroadcast(intent);*/
        Intent intent = new Intent(Intent.ACTION_MASTER_CLEAR);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.putExtra(Intent.EXTRA_REASON, "MasterClearConfirm");
        intent.putExtra(Intent.EXTRA_WIPE_EXTERNAL_STORAGE, true);
        mContext.sendBroadcast(intent);
        return "1|ReFactory";
    }

    private class MyPhoneStateListener extends PhoneStateListener  {
        public int mRsrp;

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            // TODO Auto-generated method stub  
            super.onSignalStrengthsChanged(signalStrength);
            mRsrp = signalStrength.getLevel();
        }

        private int getSignalStrength(){
            return mRsrp;
        }
    }
}