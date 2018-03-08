package com.mifi.boa;

public class WiFiSettings {
    private static WiFiSettings sInstance;

    public static WiFiSettings getInstance(){
        if (null == sInstance) {
            sInstance = new WiFiSettings();
        }
        return sInstance;
    }
    
    public String getWiFiInfo(){
        return "";
    }
    
    public String setWiFiInfo(){
        return "";
    }
}