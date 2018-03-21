package com.mifi.boa;

import android.content.Context;
import android.os.SystemProperties;

public class Account {
    final String MIFI_USERNAME = "persist.sys.user.name";
    final String MIFI_PASSWORD = "persist.sys.user.password";
    final String MIFI_LANGUAGE = "persist.sys.user.language";
    private static Account sInstance;
    private Context mContext;
    public static Account getInstance(Context mCont){
        if (null == sInstance) {
            sInstance = new Account(mCont);
        }
        return sInstance;
    }

    private Account (Context mCont){
        mContext = mCont;
    }

    public String getPasswordAndAccount(){
        return ("Confirm|Login|" + SystemProperties.get(MIFI_USERNAME,"Admin") + "|"
                + SystemProperties.get(MIFI_PASSWORD,"123321"));
    }

    public void setPasswordAndAccount(String data){
        String[] mData = data.split("\\|");
        SystemProperties.set(MIFI_USERNAME,mData[2]);
        SystemProperties.set(MIFI_PASSWORD,mData[3]);
    }
 
    public String getLanguage(){
        return ("Confirm|GetLanguage|" + SystemProperties.get(MIFI_LANGUAGE,"1"));
    }

    public void SetLanguage(String data){
        String[] mData = data.split("\\|");
        SystemProperties.set(MIFI_LANGUAGE,mData[2]);
    }
}