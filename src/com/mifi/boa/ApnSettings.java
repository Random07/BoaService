package com.mifi.boa;

import java.util.ArrayList;
import android.telephony.SubscriptionInfo;
import android.provider.Telephony;
import android.content.Context;
import android.telephony.SubscriptionManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.telephony.TelephonyManager;

public class ApnSettings {
    final String TAG = "BoaService_apn";
    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int APN_INDEX = 2;
    private static final int USER_INDEX = 3;
    private static final int PASSWORD_INDEX = 4;
    private static final int MCC_INDEX = 5;
    private static final int MNC_INDEX = 6;
    private static final int AUTH_TYPE_INDEX = 7;
    private static ApnSettings sInstance;
    ArrayList<ApnInfo> mApnList = new ArrayList<ApnInfo>();
    public static final String APN_ID = "apn_id";
    public static final String PREFERRED_APN_URI =
            "content://telephony/carriers/preferapn";
    public int subid;
    private SubscriptionInfo mSubscriptionInfo;
    private static String[] sProjection = new String[] {
            Telephony.Carriers._ID,     // 0
            Telephony.Carriers.NAME,    // 1
            Telephony.Carriers.APN,     // 2
            Telephony.Carriers.USER,    // 3
            Telephony.Carriers.PASSWORD, // 4
            Telephony.Carriers.MCC, // 5
            Telephony.Carriers.MNC, // 6
            Telephony.Carriers.AUTH_TYPE, // 7
    };
    private Context mContext;

    public static ApnSettings getInstance(Context mCont){
        if (null == sInstance) {
            sInstance = new ApnSettings(mCont);
        }

        return sInstance;
    }

    public ApnSettings(Context mCont){
        mContext = mCont;
        subid = SubscriptionManager.getDefaultVoiceSubscriptionId();
        mSubscriptionInfo = SubscriptionManager.from(mContext).getActiveSubscriptionInfo(subid);
    }
    
    public String getApns(){
        final TelephonyManager tm = TelephonyManager.from(mContext);
        final String mccmnc = tm.getSimOperatorNumericForPhone(0);
        Log.d(TAG, "mccmnc = " + mccmnc);
        String where = "numeric=\"" + mccmnc + "\"";
        String mRet = "Confirm|ApnShow";
        String mSelectedApnName = getSelectedApnName();

        Log.d(TAG, "mSelectedApnName= " + mSelectedApnName);
        Cursor mCursor = mContext.getContentResolver().query(
                            Telephony.Carriers.CONTENT_URI,
                            sProjection, where, null, Telephony.Carriers.DEFAULT_SORT_ORDER);
        if (mCursor != null) {
            mCursor.moveToFirst();
            while (!mCursor.isAfterLast()) {
                if(mSelectedApnName != null && mSelectedApnName.equals(mCursor.getString(NAME_INDEX))){
                    ApnInfo mSeletectedApn = new ApnInfo(mCursor.getInt(ID_INDEX),
                                                mCursor.getString(NAME_INDEX),
                                                mCursor.getString(APN_INDEX),
                                                mCursor.getString(USER_INDEX),
                                                mCursor.getString(PASSWORD_INDEX),
                                                mCursor.getString(MCC_INDEX),
                                                mCursor.getString(MNC_INDEX),
                                                mCursor.getInt(AUTH_TYPE_INDEX));
                    mRet += "|";
                    mRet += mSeletectedApn.toString();
                }else{
                    mApnList.add(new ApnInfo(mCursor.getInt(ID_INDEX),
                                            mCursor.getString(NAME_INDEX),
                                            mCursor.getString(APN_INDEX),
                                            mCursor.getString(USER_INDEX),
                                            mCursor.getString(PASSWORD_INDEX),
                                            mCursor.getString(MCC_INDEX),
                                            mCursor.getString(MNC_INDEX),
                                            mCursor.getInt(AUTH_TYPE_INDEX)));
                }
                mCursor.moveToNext();
            }
            mCursor.close();
        }else{
            Log.d(TAG, "mCursor is null");
        }

        for(ApnInfo mApn:mApnList){
            mRet += "|";
            mRet += mApn.toString();
        }
        Log.d(TAG, "getApns = " + mRet);

        return mRet;
    }
    
    public void setSelectedApn(String key){
        ContentResolver resolver = mContext.getContentResolver();

        ContentValues values = new ContentValues();
        values.put(APN_ID, key);

        /// M: add sub id for prefer APN
        // resolver.update(PREFERAPN_URI, values, null, null);
        resolver.update(getPreferApnUri(mSubscriptionInfo.getSubscriptionId()), values,
        null, null);
    }
    
    public void addApn(String data){
        String[] mApnContent = data.split("\\|");
        addApn(mApnContent[0], mApnContent[1], mApnContent[2], mApnContent[3]
                , mApnContent[4], mApnContent[5], mApnContent[6]);
    }
        
    public void addApn(String name, String apn, String mcc, String mnc,
                            String userName, String password, String authType){
        //Uri mUri = mContext.getContentResolver().insert(Telephony.Carriers.CONTENT_URI, new ContentValues());
        ContentValues values = new ContentValues();

        //Log.d(TAG, "mUri = " + mUri);
        values.put(Telephony.Carriers.NAME, checkNotSet(name));
        values.put(Telephony.Carriers.APN, checkNotSet(apn));
        values.put(Telephony.Carriers.USER, checkNotSet(userName));
        values.put(Telephony.Carriers.PASSWORD, checkNotSet(password));
        values.put(Telephony.Carriers.MCC, checkNotSet(mcc));
        values.put(Telephony.Carriers.MNC, checkNotSet(mnc));
        values.put(Telephony.Carriers.AUTH_TYPE, Integer.parseInt(authType));
        mContext.getContentResolver().insert(Telephony.Carriers.CONTENT_URI, values);
        //mContext.getContentResolver().update(mUri, values, null, null);
    }

    public String getSelectedApnName() {
        String name = null;

        int subId = mSubscriptionInfo.getSubscriptionId();
        Cursor cursor = mContext.getContentResolver().query(getPreferApnUri(subId), new String[] { "name" },
                null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            name = cursor.getString(0);
        }
        cursor.close();
        Log.d(TAG,"getSelectedApnName(), name = " + name);
        return name;
    }

    public Uri getPreferApnUri(int subId) {
        Uri preferredUri = Uri.withAppendedPath(Uri.parse(PREFERRED_APN_URI), "/subId/" + subId);
        Log.d(TAG, "getPreferredApnUri: " + preferredUri);
        return preferredUri;
    }

    public String checkNotSet(String value) {
        if (value == null || value.equals("Not set")) {
            return "";
        } else {
            return value;
        }
    }
}
