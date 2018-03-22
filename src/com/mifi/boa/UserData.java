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
        mSPM = SubscriptionManager.from(mContext);
        subscriptions = mSPM.getActiveSubscriptionInfoList();
        Log.d(TAG, "subscriptions.size = " + subscriptions.size());
        subInfo = subscriptions.get(0);
        mPolicyManager = NetworkPolicyManager.from(mContext);
        mPolicyEditor = new NetworkPolicyEditor(mPolicyManager);
        mPolicyEditor.read();
        subid = subInfo.getSubscriptionId();
        Log.d(TAG, "subid = " + subid);
        tm = TelephonyManager.from(mContext);
        mTemplate = getNetworkTemplate(subid);
        mPolicyEditor.setPolicyCycleDay(mTemplate,31,"Asia/Shanghai");
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
        mPolicyEditor.read();
        mPolicyEditor.setPolicyLimitBytes(mTemplate, correctedBytes);
    }

    public String getDataStatic(){
        DataUsageController controller = new DataUsageController(mContext);
        DataUsageController.DataUsageInfo usageInfo = controller.getDataUsageInfo(mTemplate);
        mPolicyEditor.read();
        long mLimit = mPolicyEditor.getPolicyLimitBytes(mTemplate);
        return ("Confirm|DataStatic|"+usageInfo.usageLevel+"|"+mLimit);
    }

    public void  setNetworkType(String data){
        String[] mData = data.split("\\|");
        tm.setPreferredNetworkType(subid, Integer.parseInt(mData[2]));
    }

    public String getNetworkType(){
        return ("Confirm|GetNetworkType|" + String.valueOf(tm.getPreferredNetworkType(subid)));
    }

    public NetworkTemplate getNetworkTemplate() {
        return NetworkTemplate.normalize(mTemplate,
                tm.getMergedSubscriberIds());
    }
}