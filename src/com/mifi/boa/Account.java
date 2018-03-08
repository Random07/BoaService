package com.mifi.boa;

public class Account {
    private static Account sInstance;

    public static Account getInstance(){
        if (null == sInstance) {
            sInstance = new Account();
        }
        return sInstance;
    }
    
    public String getPasswordAndAccount(){
        return "";
    }
}