package com.mifi.boa;
import com.android.internal.telephony.Phone;
import android.content.Context;

public class UserData {
    private static UserData sInstance;
	private Context mContext;
    
    public static UserData getInstance(Context mCont){
		mContext =mContext;
        if (null == sInstance) {
            sInstance = new UserData(mCont);
        }
        return sInstance;
    }
	public UserData (Context mCont) {
             mContext = mCont;
	}

	

    public String setDataLimit(){
        return "";
    }

    public String getDataStatic(){
        return "";
    }

    public void  setNetworkType(String mString){
        
      
    }
}