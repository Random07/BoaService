package com.mifi.boa;

public class DeviceInfo {
    private static DeviceInfo sInstance;

    public static DeviceInfo getInstance(){
        if (null == sInstance) {
            sInstance = new DeviceInfo();
        }
        return sInstance;
    }

    public String getDeviceInfo(){
        return "";
    }

    public String setReBoot(){
        return "";
    }

    public String setReFactory(){
        return "";
    }
}