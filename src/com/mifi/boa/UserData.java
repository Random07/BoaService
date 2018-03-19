package com.mifi.boa;

import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.net.NetworkPolicyManager;
import com.android.settingslib.NetworkPolicyEditor;
import com.android.settingslib.net.DataUsageController;
import android.net.NetworkTemplate;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;
import android.text.format.Formatter;

public class UserData {
    final String TAG = "BoaService_UserData";
    public static final long KB_IN_BYTES = 1000;
    public static final long MB_IN_BYTES = KB_IN_BYTES * 1000;
    public static final long GB_IN_BYTES = MB_IN_BYTES * 1000;
    private static final long MAX_DATA_LIMIT_BYTES = 50000 * GB_IN_BYTES;
    private static UserData sInstance;
	private Context mContext;
    NetworkPolicyManager mPolicyManager;
    NetworkPolicyEditor mPolicyEditor;
    int subid;
    TelephonyManager tm;
    NetworkTemplate mTemplate;

    public static UserData getInstance(Context mCont){
        if (null == sInstance) {
            sInstance = new UserData(mCont);
        }
        return sInstance;
    }

	public UserData (Context mCont) {
        mContext = mCont;
        mPolicyManager = NetworkPolicyManager.from(mContext);
        mPolicyEditor = new NetworkPolicyEditor(mPolicyManager);
        subid = SubscriptionManager.getDefaultVoiceSubscriptionId();
        tm = TelephonyManager.from(mContext);
        mTemplate = NetworkTemplate.buildTemplateMobileAll(
                tm.getSubscriberId(subid));
        mPolicyEditor.setPolicyCycleDay(mTemplate,31,"Asia/Shanghai");
	}

    public String setDataLimit(String data){
        String[] mData = data.split("\\|");
        long bytes = Long.parseLong(mData[2]);
        // Long.ValueOf("String")
        //long bytes = (long) (Float.valueOf(mData[2])
                        //* (mData[3].equals("MB") ? MB_IN_BYTES : GB_IN_BYTES));
        long correctedBytes = Math.min(MAX_DATA_LIMIT_BYTES, bytes);
        mPolicyEditor.setPolicyLimitBytes(mTemplate, correctedBytes);
        return "";
    }

    public String getDataStatic(){
        DataUsageController controller = new DataUsageController(mContext);
        DataUsageController.DataUsageInfo usageInfo = controller.getDataUsageInfo(mTemplate);
        long mLimit = mPolicyEditor.getPolicyLimitBytes(mTemplate);
        Log.d(TAG, "xjhe Total = "+Formatter.formatFileSize(mContext, usageInfo.usageLevel));
        return ("Confirm|DataStatic|"+usageInfo.usageLevel+"|"+mLimit);
    }

    public void  setNetworkType(String mType){
        tm.setPreferredNetworkType(subid, Integer.parseInt(mType));
    }

    public int getNetworkType(){
        return tm.getPreferredNetworkType(subid);
    }

    public NetworkTemplate getNetworkTemplate() {
        return NetworkTemplate.normalize(mTemplate,
                tm.getMergedSubscriberIds());
    }
}