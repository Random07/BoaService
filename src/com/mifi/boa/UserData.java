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
    List<SubscriptionInfo> subscriptions;
    SubscriptionInfo subInfo;

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

    public void getNetworkTemplate(){
        mTemplate = null;
        subscriptions = mSPM.getActiveSubscriptionInfoList();
        if(subscriptions != null){
            Log.d(TAG, "subscriptions.size = " + subscriptions.size());
            subInfo = subscriptions.get(0);
            subid = subInfo.getSubscriptionId();
            Log.d(TAG, "subid = " + subid);
            mTemplate = getNetworkTemplate(subid);
        }else{
            Log.d(TAG, "subscriptions is null!");
        }
    }

    public NetworkTemplate getNetworkTemplate(int subscriptionId) {
        NetworkTemplate mobileAll = NetworkTemplate.buildTemplateMobileAll(
                tm.getSubscriberId(subscriptionId));
        Log.d(TAG, "getNetworkTemplate with subID: " + subscriptionId);
        return NetworkTemplate.normalize(mobileAll,
                tm.getMergedSubscriberIds());
    }

    public void setDataLimit(String data){
        String[] mData = data.split("\\|");
        long bytes = Long.parseLong(mData[2]);
        long correctedBytes = Math.min(MAX_DATA_LIMIT_BYTES, bytes);

        getNetworkTemplate();
        if(mTemplate != null){
            mPolicyEditor.read();
            mPolicyEditor.setPolicyLimitBytes(mTemplate, correctedBytes);
        }else{
            Log.d(TAG, "get network template fail,do not set data limit!");
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
        String mStr = "Confirm|DataStatic|";

        getNetworkTemplate();
        if(mTemplate != null){
            DataUsageController.DataUsageInfo usageInfo = controller.getDataUsageInfo(mTemplate);
            mPolicyEditor.read();
            long mLimit = mPolicyEditor.getPolicyLimitBytes(mTemplate);
            mStr += usageInfo.usageLevel+"|"+mLimit;
        }else{
            mStr += "|";
            Log.d(TAG, "get network template fail,so used data and limit data is null!");
        }

        return mStr;
    }

    public void  setNetworkType(String data){
        String[] mData = data.split("\\|");
        tm.setPreferredNetworkType(subid, Integer.parseInt(mData[2]));
    }

    public String getNetworkType(){
        return ("Confirm|GetNetworkType|" + String.valueOf(tm.getPreferredNetworkType(subid)));
    }
}