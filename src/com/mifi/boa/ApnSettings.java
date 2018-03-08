package com.mifi.boa;

public class ApnSettings {
    private static ApnSettings sInstance;

    public static ApnSettings getInstance(){
        if (null == sInstance) {
            sInstance = new ApnSettings();
        }
        return sInstance;
    }
    
    public String getApns(){
        return "";
    }
    
    public String setSelectedApn(){
        return "";
    }
    
    public String addApn(){
        return "";
    }
}