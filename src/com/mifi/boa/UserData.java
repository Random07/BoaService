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
import android.text.format.Formatter;
import android.telephony.SubscriptionInfo;
import java.util.List;
import android.text.format.Time;

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
    SubscriptionManager mSPM;

    public static UserData getInstance(Context mCont){
        if (null == sInstance) {
            sInstance = new UserData(mCont);
        }
        return sInstance;
    }

    public UserData (Context mCont) {
        mContext = mCont;
        tm = TelephonyManager.from(mContext);
        mSPM = SubscriptionManager.from(mContext);
        mPolicyManager = NetworkPolicyManager.from(mContext);
        mPolicyEditor = new NetworkPolicyEditor(mPolicyManager);
    }

    public void updateSubId(){
        int[] subId = SubscriptionManager.getSubId(0);
        if (subId == null || subId.length == 0
            || !SubscriptionManager.isValidSubscriptionId(subId[0])) {
            subid = -1;
        } else {
            subid = subId[0];
        }
        Log.d(TAG, "updateSubId subid = " + subid);
    }

    public void getNetworkTemplate(){
        mTemplate = null;
        updateSubId();
        if(SubscriptionManager.isValidSubscriptionId(subid)){
            mTemplate = getNetworkTemplate(subid);
        }else{
            Log.d(TAG, "subid is invalid");
        }
    }

    public NetworkTemplate getNetworkTemplate(int subscriptionId) {
        NetworkTemplate mobileAll = NetworkTemplate.buildTemplateMobileAll(
                tm.getSubscriberId(subscriptionId));
        Log.d(TAG, "getNetworkTemplate with subID: " + subscriptionId);
        return NetworkTemplate.normalize(mobileAll,
                tm.getMergedSubscriberIds());
    }

    public String setDataLimit(String data){
        String[] mData = data.split("\\|");
        long bytes = Long.parseLong(mData[2]);
        long correctedBytes = Math.min(MAX_DATA_LIMIT_BYTES, bytes);

        getNetworkTemplate();
        if(mTemplate != null){
            mPolicyEditor.read();
            if(1 != mPolicyEditor.getPolicyCycleDay(mTemplate)){
                mPolicyEditor.setPolicyCycleDay(mTemplate,1,new Time().timezone);
            }
            mPolicyEditor.setPolicyLimitBytes(mTemplate, correctedBytes);
            return "1|DataLimit";
        }else{
            Log.d(TAG, "get network template fail,do not set data limit!");
            return "0|DataLimit";
        }
    }

    public void setDataCycle(String data){
        String[] mData = data.split("\\|");
        int day = Integer.parseInt(mData[2]);
        String cycleTimezone = new Time().timezone;

        getNetworkTemplate();
        if(mTemplate != null){
            mPolicyEditor.read();
            Log.d(TAG, "day = "+day+"; cycleTimezone = " + cycleTimezone);
            mPolicyEditor.setPolicyCycleDay(mTemplate,day,cycleTimezone);
        }else{
            Log.d(TAG, "get network template fail,do not set data cycle!");
        }
    }

    public int getDataCycle(){
        int day = -1;

        getNetworkTemplate();
        if(mTemplate != null){
            mPolicyEditor.read();
            day = mPolicyEditor.getPolicyCycleDay(mTemplate);
        }else{
            Log.d(TAG, "get network template fail,do not set data cycle!");
        }

        return day;
    }
    public String getDataStatic(){
        DataUsageController controller = new DataUsageController(mContext);
        String mStr = "1|DataStatic|";

        getNetworkTemplate();
        if(mTemplate != null){
            DataUsageController.DataUsageInfo usageInfo = controller.getDataUsageInfo(mTemplate);
            mPolicyEditor.read();
            if(1 != mPolicyEditor.getPolicyCycleDay(mTemplate)){
                mPolicyEditor.setPolicyCycleDay(mTemplate,1,new Time().timezone);
            }
            long mLimit = mPolicyEditor.getPolicyLimitBytes(mTemplate);
            mStr += usageInfo.usageLevel+"|"+mLimit;
        }else{
            mStr = "0|DataStatic";
            Log.d(TAG, "get network template fail,so used data and limit data is null!");
        }

        return mStr;
    }

    public String  setNetworkType(String data){
        String[] mData = data.split("\\|");
        updateSubId();
        if(tm.setPreferredNetworkType(subid, Integer.parseInt(mData[2]))){
            return ("1|SetNetworkType");
        }else{
            return ("0|SetNetworkType");
        }
    }

    public String getNetworkType(){
        int mNetworkType = -1;
        updateSubId();
        mNetworkType = tm.getPreferredNetworkType(subid);
        if(-1 == mNetworkType){
            return ("0|GetNetworkType");
        }else{
            return ("1|GetNetworkType|" + String.valueOf(mNetworkType));
        }
    }
}