package com.mifi.boa;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.net.NetworkPolicyManager;
import com.android.settingslib.NetworkPolicyEditor;
import com.android.settingslib.net.DataUsageController;
import android.net.NetworkTemplate;
import android.text.format.Time;
import android.text.TextUtils;
import java.util.Date;
import android.content.Intent;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;
import android.content.SharedPreferences;

public class UserDataHandler {
    final String TAG = "BoaService_UserDataHandler";
    final String DATALIMIT_MONTH_TIME = "datalimit.month.time";
    final String DATALIMIT_MONTH_NUMBER = "datalimit.month.number";
    final String DATALIMIT_DAY_TIME = "datalimit.day.time";
    final String DATALIMIT_DAY_NUMBER = "datalimit.day.number";
    final String DATALIMIT_NUMBER = "datalimit_number";
    final String DATALIMIT_NUMBER_CLEAR = "datalimit.clear";
    final String DATALIMIT_NUMBER_CLEAR_TIME = "datalimit.clear.time";
    final String DATALIMIT_NUMBER_RESET = "datalimit.reset";
    final long KB_IN_BYTES = 1000;
    final long MB_IN_BYTES = KB_IN_BYTES * 1000;
    final long GB_IN_BYTES = MB_IN_BYTES * 1000;
    final long MAX_DATA_LIMIT_BYTES = 50000 * GB_IN_BYTES;
    final int DATE_YEAR = 0;
    final int DATE_MONTH = 1;
    final int DATE_DAY = 2;
    final int MODE_PRIVATE = 0;
    final String NAME = "boaservice";
    private static UserDataHandler sInstance;
    private Context mContext;
    NetworkPolicyManager mPolicyManager;
    NetworkPolicyEditor mPolicyEditor;
    int subid;
    TelephonyManager tm;
    NetworkTemplate mTemplate;

    public static UserDataHandler getInstance(Context mCont){
        if (null == sInstance) {
            sInstance = new UserDataHandler(mCont);
        }
        return sInstance;
    }

    public UserDataHandler (Context mCont) {
        mContext = mCont;
        tm = TelephonyManager.from(mContext);
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

        if(mData.length < 3 || TextUtils.isEmpty(mData[2])){
            Log.d(TAG, "data error, data is " + data);
            return "0|DataLimit";
        }

        if(!mData[2].matches("[0-9]+")){
            Log.d(TAG, "data error, data is " + data);
            return "0|DataLimit";
        }

        long bytes = Long.parseLong(mData[2]);
        long correctedBytes = bytes; //Math.min(MAX_DATA_LIMIT_BYTES, bytes);

        getNetworkTemplate();
        if(mTemplate != null){
            mPolicyEditor.read();
            if(1 != mPolicyEditor.getPolicyCycleDay(mTemplate)){
                mPolicyEditor.setPolicyCycleDay(mTemplate,1,new Time().timezone);
            }
            SharedPreferences sp = mContext.getSharedPreferences(NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString(DATALIMIT_NUMBER,String.valueOf(correctedBytes));
            editor.commit();
            mPolicyEditor.setPolicyLimitBytes(mTemplate, correctedBytes + getClearedDataForLimit());
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
            mPolicyEditor.read();
            if(1 != mPolicyEditor.getPolicyCycleDay(mTemplate)){
                mPolicyEditor.setPolicyCycleDay(mTemplate,1,new Time().timezone);
                sleep(100);
            }
            DataUsageController.DataUsageInfo usageInfo = controller.getDataUsageInfo(mTemplate);

            long mMonthCleared = getClearedDataForMonth();
            long mDayCleared = getClearedDataForDay();
            long mMonthUsed = usageInfo.usageLevel - mMonthCleared;
            long mDayUsed = getDataForDay();
            SharedPreferences sp = mContext.getSharedPreferences(NAME, MODE_PRIVATE);
            Log.d(TAG, "getDataStatic, mMonthUsed = " + mMonthUsed + ", mDayUsed = "
                + mDayUsed + ", mMonthCleared = " + mMonthCleared + ", mDayCleared = " + mDayCleared);
            mDayUsed -= mDayCleared;
            mStr += mMonthUsed + "|"+ mDayUsed + "|" + sp.getString(DATALIMIT_NUMBER,"-1");
        }else{
            mStr = "0|DataStatic";
            Log.d(TAG, "get network template fail,so used data and limit data is null!");
        }

        return mStr;
    }

    public String clearData(String mCommand){
        String[] mCmd = mCommand.split("\\|");
        String curMouth = getDateForCurrentTime(DATE_MONTH);
        String curDay = getDateForCurrentTime(DATE_DAY);
        String data = getDataStatic();
        String[] mData = data.split("\\|");
        if(mData.length > 2){
            long monthUsedData = Long.parseLong(mData[2]);
            long dayUsedData = Long.parseLong(mData[3]);
            long monthClearedData = getClearedDataForMonth();
            long dayClearedData = getClearedDataForDay();
            SharedPreferences sp = mContext.getSharedPreferences(NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();

            Log.d(TAG, "clear type = " + mCmd[2] + ", monthUsedData = " + monthUsedData + ", dayUsedData = " + dayUsedData + ", monthClearedData = " + monthClearedData + ", dayClearedData = " + dayClearedData);

            // update cleared data
            editor.putString(DATALIMIT_MONTH_TIME, curMouth);
            if("1".equals(mCmd[2])){ // Clear month
                editor.putString(DATALIMIT_MONTH_NUMBER,String.valueOf(monthUsedData + monthClearedData));
                updateLimitData(monthUsedData, curMouth);
            }else if("2".equals(mCmd[2])){// Clear day
                editor.putString(DATALIMIT_MONTH_NUMBER,String.valueOf(dayUsedData + monthClearedData));
                updateLimitData(dayUsedData, curMouth);
            }
            editor.putString(DATALIMIT_DAY_TIME,curDay);
            editor.putString(DATALIMIT_DAY_NUMBER,String.valueOf(dayUsedData + dayClearedData));
            editor.commit();

            startResetTimer();
            return "1|ClearUsedData";
        }else{
            return "0|ClearUsedData";
        }
    }

    public String getDateForCurrentTime(int mType){
        long mCurTime = System.currentTimeMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String[] strDate = dateFormat.format(mCurTime).split("\\-");

        Log.d(TAG, "getDateForCurrentTime = " + strDate[mType]);
        return strDate[mType];
    }

    private long getClearedDataForMonth(){
        SharedPreferences sp = mContext.getSharedPreferences(NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        String curMouth = getDateForCurrentTime(DATE_MONTH);
        String clearMouth = sp.getString(DATALIMIT_MONTH_TIME,"-1");

        Log.d(TAG, "getClearedDataForMonth, curMouth = " + curMouth + ", clearMouth = " + clearMouth);
        if(clearMouth.equals(curMouth)){
            String mRet = sp.getString(DATALIMIT_MONTH_NUMBER,"0");
            return Long.parseLong(mRet);
        }else{
            editor.putString(DATALIMIT_MONTH_TIME,"-1");
            editor.putString(DATALIMIT_MONTH_NUMBER,"0");
            editor.commit();
        }
        return 0L;
    }

    private long getClearedDataForDay(){
        SharedPreferences sp = mContext.getSharedPreferences(NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        String curDay = getDateForCurrentTime(DATE_DAY);
        String clearDay = sp.getString(DATALIMIT_DAY_TIME,"-1");

        Log.d(TAG, "getClearedDataForDay, curDay = " + curDay + ", clearDay = " + clearDay);
        if(clearDay.equals(curDay)){
            String mRet = sp.getString(DATALIMIT_DAY_NUMBER,"0");
            return Long.parseLong(mRet);
        }else{
            editor.putString(DATALIMIT_DAY_TIME,"-1");
            editor.putString(DATALIMIT_DAY_NUMBER,"0");
            editor.commit();
        }
        return 0L;
    }

    public void updateLimitData(long addLimit, String curMouth){
        SharedPreferences sp = mContext.getSharedPreferences(NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        long limitData = Long.parseLong(sp.getString(DATALIMIT_NUMBER,"-1"));
        long limitDataClear = getClearedDataForLimit();
        editor.putString(DATALIMIT_NUMBER_CLEAR,String.valueOf(limitDataClear + addLimit));
        editor.putString(DATALIMIT_NUMBER_CLEAR_TIME,curMouth);
        editor.commit();

        Log.d(TAG, "updateLimitData, limitData = " + limitData + ", limitDataClear = " + limitDataClear + ", addLimit = " + addLimit);
        if(limitData > 0){ // has set limit data
            getNetworkTemplate();
            if(mTemplate != null){
                mPolicyEditor.read();
                mPolicyEditor.setPolicyLimitBytes(mTemplate, limitData + limitDataClear + addLimit);
            }
        }
    }

    public void startResetTimer(){
        SharedPreferences sp = mContext.getSharedPreferences(NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        String setTime = sp.getString(DATALIMIT_NUMBER_RESET,"0");
        Log.d(TAG, "startResetTimer setTime = " + setTime);
        if("0".equals(setTime)){
            editor.putString(DATALIMIT_NUMBER_RESET, getDateForCurrentTime(DATE_MONTH));
            editor.commit();
            setAlarm();
        }
    }

    public void setAlarm() {
        Intent i = new Intent();
        i.setAction("com.mifi.boa.reset.datalimit");
        PendingIntent pi = PendingIntent.getBroadcast(mContext, 1, i, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        int mYear = Integer.parseInt(getDateForCurrentTime(DATE_YEAR));
        int mMonth = Integer.parseInt(getDateForCurrentTime(DATE_MONTH));

        Log.d(TAG, "setAlarm mYear = " + mYear + ", mMonth = " + mMonth);

        if(12 == mMonth){
            mYear++;
            mMonth = 1;
        }else{
            mMonth++;
        }

        Date date = new Date(mYear, mMonth, 1);
        long mCurTime = System.currentTimeMillis();
        long time = date.getTime();

        Log.d(TAG, "setAlarm, time = " + time + ", mCurTime = " + mCurTime);
        am.set(AlarmManager.RTC_WAKEUP, time - mCurTime , pi);
    }

    private long getClearedDataForLimit(){
        SharedPreferences sp = mContext.getSharedPreferences(NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        String curMonth = getDateForCurrentTime(DATE_MONTH);
        String clearMonth = sp.getString(DATALIMIT_NUMBER_CLEAR_TIME,"-1");

        Log.d(TAG, "getClearedDataForDay, curMonth = " + curMonth + ", clearMonth = " + clearMonth);
        if(clearMonth.equals(curMonth)){
            String mRet = sp.getString(DATALIMIT_NUMBER_CLEAR,"0");
            return Long.parseLong(mRet);
        }else{
            editor.putString(DATALIMIT_NUMBER_CLEAR_TIME,"-1");
            editor.putString(DATALIMIT_NUMBER_CLEAR,"0");
            editor.commit();
        }
        return 0L;
    }

    public void checkResetDataLimit(){
        SharedPreferences sp = mContext.getSharedPreferences(NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        String setTime = sp.getString(DATALIMIT_NUMBER_RESET,"0");
        String curMonth = getDateForCurrentTime(DATE_MONTH);

        Log.d(TAG, "checkResetDataLimit setTime = " + setTime + ", curMonth = " + curMonth);
        if(!setTime.equals(curMonth)){
            resetDataLimit();
        }
    }

    public void resetDataLimit(){
        SharedPreferences sp = mContext.getSharedPreferences(NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        long mLimit = Long.parseLong(sp.getString(DATALIMIT_NUMBER,"-1"));

        Log.d(TAG, "resetDataLimit mLimit = " + mLimit);
        getNetworkTemplate();
        if((mLimit > 0) && (mTemplate != null)){
            mPolicyEditor.read();
            mPolicyEditor.setPolicyLimitBytes(mTemplate, mLimit);
        }
        editor.putString(DATALIMIT_NUMBER_RESET,"0");
        editor.commit();
    }

    private long getDataForDay(){
        DataUsageController controller = new DataUsageController(mContext);
        DataUsageController.DataUsageInfo usageInfo;
        long mRet = 0L;
        String day = getDateForCurrentTime(DATE_DAY);
        Log.d(TAG, "getDataForDay day = " + day);
        mPolicyEditor.setPolicyCycleDay(mTemplate,Integer.parseInt(day),new Time().timezone);
        sleep(100);
        usageInfo = controller.getDataUsageInfo(mTemplate);
        mPolicyEditor.setPolicyCycleDay(mTemplate,1,new Time().timezone);
        return usageInfo.usageLevel;
    }

    public void sleep(int mTime){
        try {
            Thread.sleep(mTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
