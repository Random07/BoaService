package com.mifi.boa;

public class UserData {
    private static UserData sInstance;
    
    public static UserData getInstance(){
        if (null == sInstance) {
            sInstance = new UserData();
        }
        return sInstance;
    }

    public String setDataLimit(){
        return "";
    }

    public String getDataStatic(){
        return "";
    }

    public String setNetworkType(){
        return "";
    }
}