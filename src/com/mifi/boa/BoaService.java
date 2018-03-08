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
import static android.net.ConnectivityManager.TETHERING_WIFI;
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
    private Handler mHandler = new Handler();
    private OnStartTetheringCallback mStartTetheringCallback;
    private ConnectivityManager mCm;
    private WifiManager mWifiManager;
    private WifiConfiguration mWifiConfig = null;
    private Context mContext;
    Account mAccount = Account.getInstance();
    ConnectCustomer mConnectCustomer = ConnectCustomer.getInstance();
    DeviceInfo mDeviceInfo = DeviceInfo.getInstance();
    UserData mUserData = UserData.getInstance();
    ApnSettings mApnSettings = ApnSettings.getInstance();
    WiFiSettings mWiFiSettings = WiFiSettings.getInstance();

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        android.util.Log.d(TAG,"Service start");
        startWifiAp();
        new ServerListener().start();		
        return START_STICKY;
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
                            mFlushString = mUserData.setDataLimit(); 
                        break;
                        case "DataStatic":
                            mFlushString = mUserData.getDataStatic(); 
                        break;
                        case "NetworkType":
                            mFlushString = mUserData.setNetworkType(); 
                        break;
                        case "ApnShow":
                            mFlushString = mApnSettings.getApns(); 
                        break;
                        case "ApnChange":
                            mFlushString = ApnSettings.getInstance().setSelectedApn(); 
                        break;
                        case "ApnAdd":
                            mFlushString = ApnSettings.getInstance().addApn(); 
                        break;
                        case "WIFIShow":
                            mFlushString = WiFiSettings.getInstance().getWiFiInfo(); 
                        break;
                        case "WIFISetting":
                            mFlushString = WiFiSettings.getInstance().setWiFiInfo(); 
                        break;
                        case "ReBoot":
                            mFlushString = DeviceInfo.getInstance().setReBoot(); 
                        break;
                        case "ReFactory":
                            mFlushString = DeviceInfo.getInstance().setReFactory(); 
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

      /**
     *    open wifiAp
     * @param mSSID
     * @param mPasswd
     * @param securty
     */
    public void startWifiAp(){
        android.util.Log.d(TAG,"startWifiAp begin");
        mWifiManager = (WifiManager) getApplication().getSystemService(Context.WIFI_SERVICE);
        mCm = (ConnectivityManager) getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
        android.util.Log.d(TAG,"startWifiAp begin ConnectivityManager");
        mStartTetheringCallback = new OnStartTetheringCallback();
        mCm.startTethering(TETHERING_WIFI, true, mStartTetheringCallback, mHandler);
    }

    private String getAction(String mStr){
        String mArrayStr[] = mStr.split("\\|");
        if(mArrayStr.length > 1){
            return mArrayStr[1];
        }
        return "";
    }

    public void ConfigWifiAp(String mSSID ,String mPassword,int mBandIndex ,int mSecurityTypeIndex ) {       
        if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED) {
            android.util.Log.d(TAG,"Wifi AP config changed while enabled, stop and restart");
            mCm.stopTethering(TETHERING_WIFI);
        }
        mWifiConfig = getWifiApConfig(mSSID,mPassword,mBandIndex,mSecurityTypeIndex);
        mWifiManager.setWifiApConfiguration(mWifiConfig);
        startWifiAp();
    }

    public WifiConfiguration getWifiApConfig(String mSSID ,String mPassword,int mBandIndex ,int mSecurityTypeIndex ) {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID =mSSID;
        config.apBand = mBandIndex;
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


     /**
     * @parameter ReachableTimeout reach timeout
     * @return
     */
    public ArrayList<String> getConnectedIP(){
        ArrayList<String> connectedIp=new ArrayList<String>();
        try {
            BufferedReader br=new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line=br.readLine())!=null){
                String[] splitted=line.split(" +");
                if (splitted !=null && splitted.length>=4){
                    String mac=splitted[3];
                    if(mac.matches("..:..:..:..:..:..")){
                        boolean isReachable = InetAddress.getByName(splitted[0]).isReachable(-1);
                        if(isReachable){
                            connectedIp.add(splitted[0]+splitted[3]+splitted[5]);
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return connectedIp;
    }
}
