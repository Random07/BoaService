package com.mifi.boa;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.os.Build;
import android.text.TextUtils;
import android.os.Handler;
import android.os.IBinder;
import android.content.IntentFilter;
import android.net.Uri;
import android.content.ContentResolver;
import android.os.SystemProperties;



public class BoaService extends Service {
    static final String TAG = "BoaService";
    private ConnectivityManager mCm;
    private WifiManager mWifiManager;
    private WifiConfiguration mWifiConfig = null;
    private Context mContext;
    private Account mAccount;
    private ConnectCustomer mConnectCustomer;
    private DeviceInfo mDeviceInfo;
    private UserData mUserData;
    private ApnSettings mApnSettings;
    private WiFiSettings mWiFiSettings;
    private BoaReceiver mBoaReceiver;
    private SmsContextObserver mSmsContextObserver;
	final String USER_WIFI = "persist.sys.user.wifi";

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        android.util.Log.d(TAG,"Service start");
		initInstance();
        if (mBoaReceiver != null) {
        mContext.registerReceiver(mBoaReceiver,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            }
         if (mSmsContextObserver != null) {
            getContentResolver().registerContentObserver(Uri.parse("content://sms/icc"), true, mSmsContextObserver);
        }
		if(SystemProperties.get(USER_WIFI,"true").equals("true")){
			mWiFiSettings.ConfigWifiAp("4G_MIFI",false,2,"12345678",6);
			SystemProperties.set(USER_WIFI,"false");
		}
		mWiFiSettings.startWifiAp();
        new ServerListener().start();		
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBoaReceiver != null) {
        mContext.unregisterReceiver(mBoaReceiver);
            }
        if (mSmsContextObserver != null) {
            getContentResolver().unregisterContentObserver(mSmsContextObserver);
        }
    }

    private void initInstance() {
        mContext = getApplication();
        mBoaReceiver = new BoaReceiver();
        mAccount = Account.getInstance(mContext);
        mConnectCustomer = ConnectCustomer.getInstance(mContext);
        mUserData = UserData.getInstance(mContext);
        mApnSettings = ApnSettings.getInstance(mContext);
        mWiFiSettings = WiFiSettings.getInstance(mContext);
        mSmsContextObserver = SmsContextObserver.getInstance(mContext);
        mDeviceInfo = DeviceInfo.getInstance(mContext,mBoaReceiver,mSmsContextObserver,mConnectCustomer);
    }

    /**
      *   Socket connet with gci 
      *
      */
    public class ServerListener extends Thread {
        @Override
        public void run() {
            try {
                ServerSocket serverSocket = new ServerSocket(30000);
                android.util.Log.d(TAG,"serverSocket start");

                while (true) {
                    Socket socket = serverSocket.accept();
                    android.util.Log.d(TAG,"serverSocket accept");
                    TransportSocket ts = new TransportSocket(socket);
                    ts.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

     /**
          * deal with gci message
          *
          *
          */
    public class TransportSocket extends Thread {
        Socket socket;
        String msg;

        public TransportSocket(Socket s) {
            this.socket = s;
        }

        @Override
        public void run() {
            if(!mAccount.isAccountValid()){
                return ;
            }

            try {
                android.util.Log.d(TAG,"serverSocket BufferedReader start");

                BufferedReader br=new BufferedReader(new InputStreamReader(socket.getInputStream()));//br.readLine();// success...  
                /*socket.setSoTimeout(5000);  
                        socket.setTcpNoDelay(true);   
                       socket.setSoLinger(true, 30);  
                       socket.setSendBufferSize(4096);  
                       socket.setReceiveBufferSize(4096);  
                       socket.setKeepAlive(true);  (*/
                OutputStream osSend = socket.getOutputStream();  
                OutputStreamWriter osWrite = new OutputStreamWriter(osSend);  
                BufferedWriter bufWrite = new BufferedWriter(osWrite);  
                //socket.setOOBInline(true);  
                //socket.sendUrgentData(0x44);//"D"  
                //bufWrite.write("HiIamLichuan \r\n\r\n");
                //bufWrite.flush();  
                boolean goon=true;
                while(goon){        
                    String string = br.readLine();
                    if(string == null)continue;
                    android.util.Log.d(TAG,"S:receive data:("+string+")");
                    String mAction = getAction(string);
                    if(mAction == null || "".equals(mAction))continue;
                    if(mAction != null) goon = false;
                    android.util.Log.d(TAG,"action = " + mAction);
                    String  mFlushString = "";
                    switch (mAction){
                        case "Login":
                            mFlushString = mAccount.getPasswordAndUserName();
                        break;
                        case "SetAccountInfo":
                            mFlushString = mAccount.setPasswordAndUserName(string);
                            break;
                        case "Connect_Customer":
                            mFlushString = mConnectCustomer.getConnectCustomer();
                        break;
						case "BlockClient":
                            mFlushString = mConnectCustomer.SetWhetherblockClient(string);
                        break;
						case "GetBlockList":
                            mFlushString = mConnectCustomer.getBlockCustomerList();
                        break;
                        case "DeviceInfo":
                            mFlushString = mDeviceInfo.getDeviceInfo();
                        break;
                        case "DataLimit":
                            mFlushString = mUserData.setDataLimit(string);
                        break;
                        case "DataStatic":
                            mFlushString = mUserData.getDataStatic();
                        break;
                        case "ClearUsedData":
                            mFlushString = mUserData.clearData(string);
                        break;
                        case "SetNetworkType":
                             mFlushString = mUserData.setNetworkType(string);
                        break;
                        case "GetNetworkType":
                            mFlushString = mUserData.getNetworkType();
                        break;
                        case "ApnShow":
                            mFlushString = mApnSettings.getApns();
                        break;
                        case "ApnChange":
                            mFlushString = mApnSettings.setSelectedApn(string);
                        break;
                        case "ApnAdd":
                            mFlushString = mApnSettings.addApn(string);
                        break;
                        case "WIFIShow":
                            mFlushString = mWiFiSettings.getWiFiInfo();
                        break;
                        case "WIFISetting":
                            mFlushString = mWiFiSettings.setWiFiInfo(string);
                        break;
                        case "ReBoot":
                            mFlushString = mDeviceInfo.setReBoot();
                        break;
                        case "ReFactory":
                            mFlushString = mDeviceInfo.setReFactory();
                        break;
                        case "Common":
                            mFlushString = mDeviceInfo.getCommon();
                        break;
                        case "setDataEnabled":
                            mDeviceInfo.setDataEnabled(string);
                        break;
                        case "SetLanguage":
                           mFlushString = mDeviceInfo.SetLanguage(string);
                        break;
                        case "GetSmsContent":
                           mFlushString = mSmsContextObserver.getSmsFromPhone(string);
                        break;
                        case "GetOneSmsAndClean":
                           mFlushString = mSmsContextObserver.getOneSmsAndClean(string); 
                        break;
                        case "DeleteSms":
                           mFlushString = mSmsContextObserver.DeleteSmsFromPhone(string);
                        break;
                        case "GetSIMSms":
                            mFlushString = mSmsContextObserver.getSmsFromSIM(string);
                        break;
                        case "GetOneSIMSms":
                            mFlushString = mSmsContextObserver.getOneSmsFromSIM(string);
                        break;
                        case "SendSms":
                           mFlushString = mSmsContextObserver.SendSms(string); 
                        break;
                        case "GetSmsSettings":
                           mFlushString = mSmsContextObserver.getSmsSettings();
                        break;
                        case "SetSmsSettings":
                           mFlushString = mSmsContextObserver.setSmsSettings(string);
                        break;
                        case "SetWPSConnectMode":
                            mFlushString = mWiFiSettings.setWPSConnectMode(string);
                        break;
						case "UsbFunction":
                            mFlushString = mDeviceInfo.setUsbFunction(string);
                        break;
                        case "GetUsbMode":
                            mFlushString = mDeviceInfo.getUsbFunction();
                        break;
						case "FotaUpdate":
                            mFlushString = mDeviceInfo.setFotaupdate();
                        break;
                        default:
                            android.util.Log.d(TAG,mAction+" not support!");
                        break;
                    }
                    android.util.Log.d(TAG,"ReturnMessage="+mFlushString);
                    if(TextUtils.isEmpty(mFlushString)){
                        mFlushString = 0 + "|" + mAction;
                    }
                    bufWrite.write(mFlushString+"\r\n\r\n");
                    bufWrite.flush();
                }
                bufWrite.close();
                br.close();
                socket.close();
                
                

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String getAction(String mStr){
        String mArrayStr[] = mStr.split("\\|");
        if(mArrayStr.length > 1){
            return mArrayStr[1];
        }
        return "";
    }
}
