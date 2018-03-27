package com.mifi.boa;

import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import android.os.FileObserver;
import android.os.Environment; 

public class ConnectCustomer {
    private static ConnectCustomer sInstance;
    private BufferedReader br;
    private String connectedIp = "";
     static final String TAG = "BoaService";

    public static ConnectCustomer getInstance(){
        if (null == sInstance) {
            sInstance = new ConnectCustomer();
            
        }
        return sInstance;
    }

    public ConnectCustomer (){
           

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
    
         CustomerThread mCustomerThread = new CustomerThread();
         mCustomerThread.start(); 
          try {  
            mCustomerThread.join();  
          } catch (InterruptedException e) {  
            e.printStackTrace();  
          } 
             
           return "1|"+"Connect_Customer"+connectedIp;
     }

    public class CustomerThread extends Thread {

        @Override  
        public void run(){  
        try {
            br=new BufferedReader(new FileReader("/proc/net/arp"));
            connectedIp = "";
            String line;
            while ((line=br.readLine())!=null){
                String[] splitted=line.split(" +");
                if (splitted !=null && splitted.length>=4){
                    String mac=splitted[3];
                    if(mac.matches("..:..:..:..:..:..")){
                        boolean isReachable = InetAddress.getByName(splitted[0]).isReachable(500);
                        android.util.Log.d("BoaService","customName"+splitted[0]);
                        if(isReachable){
                            connectedIp +=("|"+splitted[0]+"|"+splitted[3]+"|"+splitted[5]);
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