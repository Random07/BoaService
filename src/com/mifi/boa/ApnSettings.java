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
import android.text.TextUtils;

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
    ApnInfo mSeletectedApn;
    public static final String APN_ID = "apn_id";
    public static final String PREFERRED_APN_URI =
            "content://telephony/carriers/preferapn";
    public int subid;
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
        subid = SubscriptionManager.getDefaultSubscriptionId();
        Log.d(TAG, "subid = " + subid);
    }

    public void createAllApnList(){
        final TelephonyManager tm = TelephonyManager.from(mContext);
        final String mccmnc = tm.getSimOperatorNumericForPhone(0);
        Log.d(TAG, "mccmnc = " + mccmnc);
        String where = "numeric=\"" + mccmnc + "\"";
        String mSelectedApnName = getSelectedApnName();

        Log.d(TAG, "mSelectedApnName= " + mSelectedApnName);
        Cursor mCursor = mContext.getContentResolver().query(
                            Telephony.Carriers.CONTENT_URI,
                            sProjection, where, null, Telephony.Carriers.DEFAULT_SORT_ORDER);
        if (mCursor != null) {
            mCursor.moveToFirst();
            while (!mCursor.isAfterLast()) {
                if(mSelectedApnName != null && mSelectedApnName.equals(mCursor.getString(NAME_INDEX))){
                    mSeletectedApn = new ApnInfo(mCursor.getInt(ID_INDEX),
                                                mCursor.getString(NAME_INDEX),
                                                mCursor.getString(APN_INDEX),
                                                mCursor.getString(USER_INDEX),
                                                mCursor.getString(PASSWORD_INDEX),
                                                mCursor.getString(MCC_INDEX),
                                                mCursor.getString(MNC_INDEX),
                                                mCursor.getInt(AUTH_TYPE_INDEX));
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
            Log.d(TAG, "getApns, mCursor is null");
        }
    }

    public String getApns(){
        String mRet = "Confirm|ApnShow|";

        createAllApnList();
        mRet += mSeletectedApn.toString();
        for(ApnInfo mApn:mApnList){
            mRet += "|";
            mRet += mApn.toString();
        }
        mApnList.clear();
        Log.d(TAG, "getApns = " + mRet);

        return mRet;
    }

    public String convertApnToApnId(String apn){
        String mApnId = "";

        if(!TextUtils.isEmpty(apn)){
            if(apn.equals(mSeletectedApn.getApn())){
                mApnId = String.valueOf(mSeletectedApn.getApnIndex());
            }else{
                for(ApnInfo mApn:mApnList){
                    if(apn.equals(mApn.getApn())){
                        mApnId = String.valueOf(mApn.getApnIndex());
                        break;
                    }
                }
            }
        }

        return mApnId;
    }

    public void setSelectedApn(String data){
        ContentResolver resolver = mContext.getContentResolver();
        ContentValues values = new ContentValues();
        String[] mData = data.split("\\|");
        String mApnId = "";

        createAllApnList();
        mApnId = convertApnToApnId(mData[2]);
        Log.d(TAG, "APN_ID = " + mApnId);
        if(!TextUtils.isEmpty(mApnId)){
            values.put(APN_ID, mApnId);
            resolver.update(getPreferApnUri(subid), values, null, null);
        }else{
            Log.d(TAG, "mApnId is null, ignore it!");
        }
        mApnList.clear();
    }

    public String getArrayContent(String[] mData, int index){
        return ((mData.length > index)?mData[index]:"");
    }

    public void addApn(String data){
        Log.d(TAG, "addApn = " + data);
        String[] mData = data.split("\\|");
        Log.d(TAG, "addApn,arry length = " + mData.length);
        addApn(getArrayContent(mData,2), getArrayContent(mData,3),
                getArrayContent(mData,4), getArrayContent(mData,5),
                getArrayContent(mData,6), getArrayContent(mData,7),
                getArrayContent(mData,8));
    }
        
    public void addApn(String name, String apn, String mcc, String mnc,
                            String userName, String password, String authType){
        Log.d(TAG, "name = " + name + "; apn = " + apn + "; mcc = " + mcc + "; mnc = " + mnc
                + "; userName = " + userName + "; password = " + password + "; authType = " + authType);
        Uri mUri = mContext.getContentResolver().insert(Telephony.Carriers.CONTENT_URI, new ContentValues());
        ContentValues values = new ContentValues();
        Log.d(TAG, "mUri = " + mUri);

        values.put(Telephony.Carriers.NAME, checkNotSet(name)); // Can not be null
        values.put(Telephony.Carriers.APN, checkNotSet(apn)); // Can not be null
        values.put(Telephony.Carriers.USER, checkNotSet(userName)); // default value is null
        values.put(Telephony.Carriers.PASSWORD, checkNotSet(password)); // default value is null
        values.put(Telephony.Carriers.MCC, checkNotSet(mcc)); // Can not be null
        values.put(Telephony.Carriers.MNC, checkNotSet(mnc)); // Can not be null
        if(!TextUtils.isEmpty(authType)){
            values.put(Telephony.Carriers.AUTH_TYPE, Integer.parseInt(authType)); // Can not be -1
        }
        values.put(Telephony.Carriers.PROTOCOL, "IP"); // default value is IP
        values.put(Telephony.Carriers.ROAMING_PROTOCOL, "IP"); // default value is IP
        values.put(Telephony.Carriers.PROXY, ""); // default value is null
        values.put(Telephony.Carriers.PORT, ""); //  default value is null
        values.put(Telephony.Carriers.MMSPROXY, ""); // default value is null
        values.put(Telephony.Carriers.MMSPORT, ""); // default value is null
        values.put(Telephony.Carriers.SERVER, ""); // default value is null
        values.put(Telephony.Carriers.MMSC, ""); // default value is null
        values.put(Telephony.Carriers.TYPE, "default,mms,supl,dun,hipri,fota,cbs,dm,wap,net,cmmail,tethering,rcse,xcap,rcs,bip,vsim");
        // default value is default,mms,supl,dun,hipri,fota,cbs,dm,wap,net,cmmail,tethering,rcse,xcap,rcs,bip,vsim
        values.put(Telephony.Carriers.NUMERIC, mcc+mnc);
        values.put(Telephony.Carriers.CURRENT, 1); // default value is 1
        values.put(Telephony.Carriers.BEARER_BITMASK, 0); // default value is 0
        values.put(Telephony.Carriers.BEARER, 0); // default value is 0
        values.put(Telephony.Carriers.MVNO_TYPE, ""); // default value is null
        values.put(Telephony.Carriers.MVNO_MATCH_DATA, ""); // default value is null
        values.put(Telephony.Carriers.CARRIER_ENABLED, 1); // default value is 1

        mContext.getContentResolver().update(mUri, values, null, null);
    }

    public String getSelectedApnName() {
        String name = null;
        Cursor cursor = mContext.getContentResolver().query(getPreferApnUri(subid), new String[] { "name" },
                null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);

        if(cursor != null){
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                name = cursor.getString(0);
            }
            cursor.close();
        }else{
            Log.d(TAG,"getSelectedApnName(), cursor is null");
        }
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
