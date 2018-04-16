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
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;

public class DeviceInfo {
    private static final int MSG_SET_REBOOT = 1;
    private static final int MSG_SET_REFACTORY = 2;
    private static final long DELAY_MILLIS = 1000;
    private static DeviceInfo sInstance;
	private Context mContext;
    public static final String WIFI_HOTSPOT_MAX_CLIENT_NUM = "wifi_hotspot_max_client_num";
    final String MIFI_LANGUAGE = "persist.sys.user.language";
    private ConnectivityManager mCM;
    private TelephonyManager telephonyManager;
    private WifiManager mWifiManager;
    private BoaReceiver mBoaReceiver;
    private MyPhoneStateListener mMyPhoneStateListener;
    private SmsContextObserver mSmsContextObserver;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            android.util.Log.d("BoaService_DeviceInfo","message.what = "+msg.what);
            switch (msg.what) {
                case MSG_SET_REBOOT:
                    PowerManager mPM = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
                    mPM.reboot("");
                    break;
                case MSG_SET_REFACTORY:
                    Intent intent = new Intent(Intent.ACTION_MASTER_CLEAR);
                    intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                    intent.putExtra(Intent.EXTRA_REASON, "MasterClearConfirm");
                    intent.putExtra(Intent.EXTRA_WIPE_EXTERNAL_STORAGE, true);
                    mContext.sendBroadcast(intent);
                    break;
            }
        }
    };

    public static DeviceInfo getInstance(Context mCont,BoaReceiver mBoaReceiver,SmsContextObserver mSmsObserver){
        if (null == sInstance) {
            sInstance = new DeviceInfo(mCont,mBoaReceiver,mSmsObserver);
        }
        return sInstance;
    }
	
    private DeviceInfo(Context mCont,BoaReceiver mBoaSer,SmsContextObserver mSmsObserver){
        mContext = mCont;
        mBoaReceiver = mBoaSer;
        mSmsContextObserver = mSmsObserver;
        mCM = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        telephonyManager = TelephonyManager.from(mContext);
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mMyPhoneStateListener = new MyPhoneStateListener();
        telephonyManager.listen(mMyPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
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
        //String mWanIpAddress = SystemProperties.get("net.wimax.mac.address", "Unavailable");;
        //String mSwVersion = "Android 7.0";
        String mFirmwareVersion = "1.1";
        String mHwVersion ="1.0";
		
        return "1|"+"DeviceInfo|"+mSimNumber+"|"+mDeviceIMEI+"|"+imsi+"|"+mSSID+"|"+mMaxConnect+"|"+mIpAddress+"|"+mMacAddress+"|"+mFirmwareVersion+"|"+mHwVersion;
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
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SET_REBOOT), DELAY_MILLIS);
        return "1|ReBoot";
    }

    public String getCommon(){
	   //String mBatteryLevl = mBoaReceiver.getBatterylevl();
	   BatteryManager battMgr = (BatteryManager) mContext.getSystemService(Context.BATTERY_SERVICE);
       int mPercent = battMgr.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
       int networkType = telephonyManager.getNetworkType();
       String mSpn = telephonyManager.getSimOperatorName();
       int mRsrp = mMyPhoneStateListener.getSignalStrength();
       //int mRsrp =telephonyManager.getSignalStrength().getLevel();
       int mMaxConnect = System.getInt(mContext.getContentResolver(),WIFI_HOTSPOT_MAX_CLIENT_NUM,5);
       int mUnreadSms =mSmsContextObserver.getUnreadSmsCount();
       String mLanguage = SystemProperties.get(MIFI_LANGUAGE,"1");
       return "1|Common"+"|"+mMaxConnect+"|"+mUnreadSms+"|"+mPercent+"|"+networkType+"|"+mSpn+"|"+mRsrp+"|"+mLanguage;
    }
    public String SetLanguage(String data){
        String[] mData = data.split("\\|");
        SystemProperties.set(MIFI_LANGUAGE,mData[2]);
        return "1|SetLanguage";
    }
    public void setDataEnabled(String data){
        String[] mData = data.split("\\|");
        int enable = Integer.parseInt(mData[2]);
        telephonyManager.setDataEnabled((enable == 1)?true:false);
    }

    public String setReFactory(){
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SET_REFACTORY), DELAY_MILLIS);
        return "1|ReFactory";
    }

    private class MyPhoneStateListener extends PhoneStateListener  {
        public int mRsrp;

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            // TODO Auto-generated method stub  
            super.onSignalStrengthsChanged(signalStrength);
            mRsrp = signalStrength.getLevel();
            android.util.Log.d("testtest","mRsrp = "+mRsrp);
        }

        private int getSignalStrength(){
            return mRsrp;
        }
    }
}