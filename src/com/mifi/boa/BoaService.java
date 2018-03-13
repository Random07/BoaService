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
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.os.Build;
import android.text.TextUtils;
import android.os.Handler;
import android.os.IBinder;
import java.net.InetAddress;

public class BoaService extends Service {
    static final String TAG = "BoaService";
    public static final int OPEN_INDEX = 0;
    public static final int WPA_INDEX = 1;
    public static final int WPA2_INDEX = 2;
    private ArrayList<Hotspot> result = null;
    private String hotspotString="";
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
        mWiFiSettings.startWifiAp();
        new ServerListener().start();		
        return START_STICKY;
    }

	private void initInstance() {
        mContext = getApplication();
	    mAccount = Account.getInstance(mContext);
        mConnectCustomer = ConnectCustomer.getInstance();
        mDeviceInfo = DeviceInfo.getInstance(mContext);
        mUserData = UserData.getInstance(mContext);
        mApnSettings = ApnSettings.getInstance(mContext);
        mWiFiSettings = WiFiSettings.getInstance(mContext);
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
            try {
                android.util.Log.d(TAG,"serverSocket BufferedReader start");

                BufferedReader br=new BufferedReader(new InputStreamReader(socket.getInputStream()));//br.readLine();// success...  
                socket.setSoTimeout(5000);  
                socket.setTcpNoDelay(true);   
                socket.setSoLinger(true, 30);  
                socket.setSendBufferSize(4096);  
                socket.setReceiveBufferSize(4096);  
                socket.setKeepAlive(true);  
                OutputStream osSend = socket.getOutputStream();  
                OutputStreamWriter osWrite = new OutputStreamWriter(osSend);  
                final BufferedWriter bufWrite = new BufferedWriter(osWrite);  
                socket.setOOBInline(true);  
                socket.sendUrgentData(0x44);//"D"  
                bufWrite.write("HiIamLichuan \r\n\r\n");
                bufWrite.flush();  
                android.util.Log.d(TAG,"write ok"); 
                boolean goon=true;
                while(goon){        
                    String string = br.readLine();
                    if(string == null)continue;
                    android.util.Log.d(TAG,"S:receive data:("+string+")");
                    String mAction = getAction(string);
                    if(mAction == null || "".equals(mAction))continue;
                    android.util.Log.d(TAG,"action = " + mAction);
                    String  mFlushString = "";
                    switch (mAction){
                        case "Login":
                            mFlushString = mAccount.getPasswordAndAccount();
                        break;
                        case "Connect_Customer":
                            mFlushString = mConnectCustomer.getConnectCustomer();
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
                        case "NetworkType":
                             mUserData.setNetworkType(string); 
                        break;
                        case "ApnShow":
                            mFlushString = mApnSettings.getApns();
                        break;
                        case "ApnChange":
                            mApnSettings.setSelectedApn(string);
                        break;
                        case "ApnAdd":
                            mApnSettings.addApn(string);
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
                        default:
                            android.util.Log.d(TAG,mAction+" not support!");
                        break;
                    }
                    if(!TextUtils.isEmpty(mFlushString)){
                        bufWrite.write(mFlushString+"\r\n\r\n");
                        bufWrite.flush(); 
                    }
                }
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
