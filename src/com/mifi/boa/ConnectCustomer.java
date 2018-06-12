package com.mifi.boa;

import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import android.os.FileObserver;
import android.os.Environment;
import android.net.wifi.WifiManager;
import android.content.Context;
import android.net.wifi.HotspotClient;
import java.util.List;





public class ConnectCustomer {
	private static ConnectCustomer sInstance;
	private BufferedReader br;
	private String connectedIp = "";
	private int connectNumber = 0;
	static final String TAG = "BoaService";
	private WifiManager mWifiManager;
	private Context mContext;
	private List<HotspotClient> mClientList;


    public static ConnectCustomer getInstance(Context contex){
        if (null == sInstance) {
            sInstance = new ConnectCustomer(contex);
            
        }
        return sInstance;
    }

	public ConnectCustomer (Context contex ){
		mContext = contex;
		mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
	}

	public void init(){
		android.util.Log.d(TAG,"mObserver");
		CustomerFileObserver mObserver = new CustomerFileObserver("/proc/net/arp");
		mObserver.startWatching(); 
	}

    private class CustomerFileObserver extends FileObserver{


         public CustomerFileObserver(String path) {  
            super(path,FileObserver.ALL_EVENTS);  
        }  


          @Override
        public void startWatching() {
        	android.util.Log.d(TAG,"startWatching");
            super.startWatching();
         
        }

           @Override
        public void stopWatching() {
        	android.util.Log.d(TAG,"stopWatching");
            super.stopWatching();
         
        }

          @Override
         public void onEvent(int event, String path) {
            android.util.Log.d(TAG,"event" + event+"path"+path);
            int el = event & FileObserver.ALL_EVENTS;
            switch (el) {
            case FileObserver.MODIFY:
              android.util.Log.d(TAG,"Change");
              CustomerThread mCustomerThread = new CustomerThread();
             mCustomerThread.start(); 
            break;  
          }

    }
        }

	/**
     * @parameter ReachableTimeout reach timeout
     * @return
     */
	public String getConnectCustomer(){

		/*CustomerThread mCustomerThread = new CustomerThread();
		mCustomerThread.start(); 
		try {  
		mCustomerThread.join();  
		} catch (InterruptedException e) {  
		e.printStackTrace();  
		}*/ 
		String ConnectCustom = "";
		mClientList = mWifiManager.getHotspotClients();
		int mSize = getconnectNumber();
		android.util.Log.d(TAG,"getConnectCustomer.size()"+mClientList.size());
		for (HotspotClient client : mClientList) {
			if(client.isBlocked == false){
			String mDevicesName = mWifiManager.getClientDeviceName(client.deviceAddress);
			String Macaddress = client.deviceAddress;
			String mIpaddress = mWifiManager.getClientIp(client.deviceAddress);
			android.util.Log.d(TAG,"getConnectCustomer.client.deviceAddress"+client.deviceAddress);
			ConnectCustom += "|"+mDevicesName+"|"+Macaddress+"|"+mIpaddress;
			}
		}


	return "1|"+"Connect_Customer|"+mSize+ConnectCustom;
	}
	
	public String getBlockCustomerList(){
		mClientList = mWifiManager.getHotspotClients();
        int	BlockNumber = 0;
		String BlockMacAddressList="";
		for (HotspotClient client : mClientList) {
            if(client.isBlocked == true){
                BlockNumber++;
		        String mDevicesName = mWifiManager.getClientDeviceName(client.deviceAddress);
		        String mIpaddress = mWifiManager.getClientIp(client.deviceAddress);
		        String Macaddress = client.deviceAddress;
				android.util.Log.d(TAG,"saveBlockList"+Macaddress);
		        BlockMacAddressList += "|"+mDevicesName+"|"+Macaddress+"|"+mIpaddress;
				
			} 

		}

	return "1|GetBlockList|"+BlockNumber+BlockMacAddressList;
	}

	/*public void saveBlockList(String mblockMacAddress){
		String mDevicesName = mWifiManager.getClientDeviceName(mblockMacAddress);
		String mIpaddress = mWifiManager.getClientIp(mblockMacAddress);
		android.util.Log.d(TAG,"saveBlockList"+mblockMacAddress);
		BlockMacAddressList += "|"+mDevicesName+"|"+mblockMacAddress+"|"+mIpaddress;
		android.util.Log.d(TAG,"saveBlockList BlockMacAddressList"+BlockMacAddressList);

	}

	public void removeBlockList(String mblockMacAddress){
		String mDevicesName = mWifiManager.getClientDeviceName(mblockMacAddress);
		String mIpaddress = mWifiManager.getClientIp(mblockMacAddress);
		android.util.Log.d(TAG,"removeBlockList"+mblockMacAddress);
        BlockMacAddressList.replace("|"+mDevicesName+"|"+mblockMacAddress+"|"+mIpaddress,"");
		android.util.Log.d(TAG,"removeBlockList BlockMacAddressList "+BlockMacAddressList);

	}*/
	
	public int getconnectNumber(){
        int	connectNumber = 0;
		mClientList = mWifiManager.getHotspotClients();
		for (HotspotClient client : mClientList) {
             if(client.isBlocked == false){
                     connectNumber++;
			 }
		}
	return connectNumber;
	}
	
	public String  SetWhetherblockClient(String str){
		String MacAddress = getClientMacAddress(str);
		boolean whether = getNeedblock(str);
		boolean resultboolean = false;

		mClientList = mWifiManager.getHotspotClients();
		android.util.Log.d(TAG,"mClientList.size()"+mClientList.size());
		for (HotspotClient client : mClientList) {
		
			if(MacAddress.equals(client.deviceAddress)){
			    android.util.Log.d(TAG,"enter blockClient");
				if(whether == true){
					android.util.Log.d(TAG,"blockClient");
					resultboolean = mWifiManager.blockClient(client);
				}else if (whether == false){
				 	android.util.Log.d(TAG,"unblockClient");
					resultboolean = mWifiManager.unblockClient(client);
				}
			}
		}
	    String Result = resultboolean ? "1" : "0";
	return Result+"|BlockClient|";
	}
	
	public String getClientMacAddress(String str){
		String[] mData = str.split("\\|");
		android.util.Log.d(TAG,"need block or unbolck  macAddress"+mData[2] );
	return mData[2];
	}


	public boolean  getNeedblock(String str){
		String[] mData = str.split("\\|");     
	return mData[3].equals("true")? true : false ;
	}
	

	public boolean getBooleanBlock(String MacAddress){
		mClientList = mWifiManager.getHotspotClients();
		android.util.Log.d(TAG,"mClientList.size()"+mClientList.size());
		for (HotspotClient client : mClientList) {
			if (MacAddress.equals(client.deviceAddress) ){
				return client.isBlocked;
			}			 	
		}
		android.util.Log.d(TAG,"no found client from macAddress");
	return false;
	}

	
    public class CustomerThread extends Thread {

        @Override  
        public void run(){  
        try {
            br=new BufferedReader(new FileReader("/proc/net/arp"));
            connectedIp = "";
            connectNumber = 0;
            String line;
            while ((line=br.readLine())!=null){
                String[] splitted=line.split(" +");
                if (splitted !=null && splitted.length>=4){
                    String mac=splitted[3];
                    if(mac.matches("..:..:..:..:..:..")){
                        boolean isReachable = InetAddress.getByName(splitted[0]).isReachable(500);
                        android.util.Log.d("BoaService","customName"+splitted[0]);
                        if(isReachable){
                            connectNumber++; 
							boolean ifblock = getBooleanBlock(splitted[3]);
                            connectedIp +=("|"+splitted[0]+"|"+splitted[3]+"|"+splitted[5]+"|"+ifblock);
                        }
                    }
                }
            }
            android.util.Log.d(TAG,"connectedIp" + connectedIp);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {  
                try {  
                    br.close();  
                } catch (IOException e) {  
                    e.printStackTrace();  
             }  
        }
      }

    }
    
}
