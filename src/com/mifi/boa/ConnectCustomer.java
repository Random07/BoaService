package com.mifi.boa;

public class ConnectCustomer {
    private static ConnectCustomer sInstance;

    public static ConnectCustomer getInstance(){
        if (null == sInstance) {
            sInstance = new ConnectCustomer();
        }
        return sInstance;
    }
    
    public String getConnectCustomer(){
        return "";
    }
}