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
import android.hardware.usb.UsbManager;
import com.mifi.boa.BoaServiceUtils;

public class DeviceInfo {
    private static final int MSG_SET_REBOOT = 1;
    private static final int MSG_SET_REFACTORY = 2;
    private static final long DELAY_MILLIS = 1000;
    private static DeviceInfo sInstance;
	private Context mContext;
    public static final String WIFI_HOTSPOT_MAX_CLIENT_NUM = "wifi_hotspot_max_client_num";
    final String MIFI_LANGUAGE = "persist.sys.user.language";
    final String MIFI_USERNAME = "persist.sys.user.name";
    final String MIFI_PASSWORD = "persist.sys.user.password";
    private ConnectivityManager mCM;
    private TelephonyManager telephonyManager;
    private WifiManager mWifiManager;
    private MyPhoneStateListener mMyPhoneStateListener;
    private SmsContextObserver mSmsContextObserver;
    private ConnectCustomer mConnectCustomer;
	private UsbManager mUsbManager;
	private WiFiSettings mWifisettings;
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
    /** Data connection activity: No traffic. */
    public static final int DATA_ACTIVITY_NONE = 0x00000000;
    /** Data connection activity: Currently receiving IP PPP traffic. */
    public static final int DATA_ACTIVITY_IN = 0x00000001;
    /** Data connection activity: Currently sending IP PPP traffic. */
    public static final int DATA_ACTIVITY_OUT = 0x00000002;
    /** Data connection activity: Currently both sending and receiving
     *  IP PPP traffic. */
    public static final int DATA_ACTIVITY_INOUT = DATA_ACTIVITY_IN | DATA_ACTIVITY_OUT;
    /**
     * Data connection is active, but physical link is down
     */
    public static final int DATA_ACTIVITY_DORMANT = 0x00000004;

    public static DeviceInfo getInstance(Context mCont,SmsContextObserver mSmsObserver,ConnectCustomer mConnectCustomer,WiFiSettings mwificonf){
        if (null == sInstance) {
            sInstance = new DeviceInfo(mCont,mSmsObserver,mConnectCustomer,mwificonf);
        }
        return sInstance;
    }
	
    private DeviceInfo(Context mCont,SmsContextObserver mSmsObserver,ConnectCustomer mCoutomer,WiFiSettings mwificonf){
        mContext = mCont;
        mSmsContextObserver = mSmsObserver;
        mConnectCustomer= mCoutomer;
        mWifisettings = mwificonf;
        mCM = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        telephonyManager = TelephonyManager.from(mContext);
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mMyPhoneStateListener = new MyPhoneStateListener();
        telephonyManager.listen(mMyPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		mUsbManager = mContext.getSystemService(UsbManager.class);
    }

    public String getDeviceInfo(){
        String mSimNumber = telephonyManager.getLine1Number();
        String mDeviceIMEI = telephonyManager.getDeviceId();
        String iccid = telephonyManager.getSimSerialNumber();
        String mSSID = mWifiManager.getWifiApConfiguration().getPrintableSsid();
        //String mSSID = mWifiManager.getWifiApConfiguration().SSID;
        int mMaxConnect = System.getInt(mContext.getContentResolver(),WIFI_HOTSPOT_MAX_CLIENT_NUM,5);
        String mIpAddress = getIpAddresses();
        String mMacAddress = getMacAddress();
        //String mWanIpAddress = SystemProperties.get("net.wimax.mac.address", "Unavailable");;
        //String mSwVersion = "Android 7.0";
        String mFirmwareVersion = "1.1";
        String mHwVersion ="1.0";
		
        return "1|"+"DeviceInfo|"+mSimNumber+"|"+mDeviceIMEI+"|"+iccid+"|"+mSSID+"|"+mMaxConnect+"|"+mIpAddress+"|"+mMacAddress+"|"+mFirmwareVersion+"|"+mHwVersion;
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
            if (iter.hasNext()) addresses += "/";
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
	   BatteryManager battMgr = (BatteryManager) mContext.getSystemService(Context.BATTERY_SERVICE);
       int mPercent = battMgr.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
       int networkType = telephonyManager.getNetworkType();
       String mSpn = telephonyManager.getSimOperatorName();
       int mRsrp = mMyPhoneStateListener.getSignalStrength();
       //int mRsrp =telephonyManager.getSignalStrength().getLevel();
       int mMaxConnect = mConnectCustomer.getconnectNumber();
       int mUnreadSms =mSmsContextObserver.getUnreadSmsCount();
       String mLanguage = SystemProperties.get(MIFI_LANGUAGE,"1");
       String mDataActivity = getDataString(telephonyManager.getDataActivity());
       return "1|Common"+"|"+mMaxConnect+"|"+mUnreadSms+"|"+mPercent+"|"+networkType+"|"+mSpn+"|"+mRsrp+"|"+mLanguage+"|"+mDataActivity;
    }

     public String getDataString(int data){
        if(!telephonyManager.getDataEnabled()) return "OFF";
            // is not available.
            switch (data) {
                case DATA_ACTIVITY_NONE:
                    return "NONE";
                case  DATA_ACTIVITY_IN:
                    return "UPLOAD";
                case DATA_ACTIVITY_OUT:
                    return "DOWNLOAD";
                case DATA_ACTIVITY_INOUT:
                    return "UPDOWNLOAD";
                case  DATA_ACTIVITY_DORMANT:
                    return "NONE";
                default:
                    return "unknow";
            }
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

	public String setUsbFunction(String mode) {
		String fuction = getFuctionFromMode(mode);
		switch (fuction) {
			case "MTP":
				mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_MTP);
				mUsbManager.setUsbDataUnlocked(true);
			break;
			case "PTP":
				mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_PTP);
				mUsbManager.setUsbDataUnlocked(true);
			break;
			case "MIDI":
				mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_MIDI);
				mUsbManager.setUsbDataUnlocked(true);
			break;
			/// M: Add for Built-in CD-ROM and USB Mass Storage @{
			case "STORAGE":
				mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_MASS_STORAGE);
				mUsbManager.setUsbDataUnlocked(true);
			break;
			case "BICR":
				mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_BICR);
				mUsbManager.setUsbDataUnlocked(true);
			break;
			/// M: @}
			default:			
				mUsbManager.setUsbDataUnlocked(false);
				mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_NONE);
			break;
		}
	return "1|UsbFunction";
	}

	public String getFuctionFromMode(String str){
        String[] mode = str.split("\\|");
        return mode[2];
    }

	public String getUsbFunction(){
        if (mUsbManager.isFunctionEnabled(UsbManager.USB_FUNCTION_MTP)) {
            return "1|GetUsbMode|MTP";
        } else if (mUsbManager.isFunctionEnabled(UsbManager.USB_FUNCTION_PTP)) {
            return "1|GetUsbMode|PTP";
        } else if (mUsbManager.isFunctionEnabled(UsbManager.USB_FUNCTION_MIDI)) {
            return "1|GetUsbMode|MIDI";
        /// M: Add for Built-in CD-ROM and USB Mass Storage @{
        } else if (mUsbManager.isFunctionEnabled(UsbManager.USB_FUNCTION_MASS_STORAGE)) {
            return "1|GetUsbMode|STORAGE";
        } else if (mUsbManager.isFunctionEnabled(UsbManager.USB_FUNCTION_BICR)) {
            return "1|GetUsbMode|BICR";
        /// M: @}
        }
        return "1|GetUsbMode|NONE"; // ...
    }

    public String setReFactory(){
        SystemProperties.set(MIFI_USERNAME,"admin");
        SystemProperties.set(MIFI_PASSWORD,"admin");
        SystemProperties.set(MIFI_LANGUAGE,"1");
        //mWifisettings.ConfigWifiAp("4G_MIFI",false,2,"12345678",6);
        BoaServiceUtils.getInstance(mContext).ConfigWifiAp(mWifisettings);
        return "1|ReFactory";
    }

	public String setFotaupdate(){
        Intent intent = new Intent();
        intent.setAction("4GMIFI.FotaUpdate");
        mContext.sendBroadcast(intent);
        return "1|Fotaupdate";
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
