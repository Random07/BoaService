package com.mifi.boa;

import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;

public class ConnectCustomer {
    private static ConnectCustomer sInstance;

    public static ConnectCustomer getInstance(){
        if (null == sInstance) {
            sInstance = new ConnectCustomer();
        }
        return sInstance;
    }

	/**
     * @parameter ReachableTimeout reach timeout
     * @return
     */
    public String getConnectCustomer(){
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
                            connectedIp.add("|"+splitted[0]+"|"+splitted[3]+"|"+splitted[5]);
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Confirm|"+"Connect_Customer"+connectedIp.toString();
    }
}