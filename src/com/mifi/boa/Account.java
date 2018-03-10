package com.mifi.boa;
import android.content.Context;
public class Account {
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
        return "";
    }
}