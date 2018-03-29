package com.mifi.boa;

import android.database.ContentObserver;
import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.content.Context;
import android.database.sqlite.SQLiteException;
import java.sql.Date;  
import java.text.SimpleDateFormat;
import android.content.ContentValues;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import android.app.Activity;
import android.content.IntentFilter;
import java.util.List;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import android.os.Handler;
import android.os.Message;
import android.os.AsyncResult;

public class SmsContextObserver extends ContentObserver{
    private static SmsContextObserver sInstance;
    private Uri SMS_INBOX = Uri.parse("content://sms/");
    private Context mContext;
    static final String TAG = "BoaService";
    private TelephonyManager telephonyManager;
    private String SENT_SMS_ACTION = "SENT_SMS_ACTION";
    private String DELIVERED_SMS_ACTION = "DELIVERED_SMS_ACTION";
    private PendingIntent sentPI;
    private PendingIntent deliverPI;
    private Phone mPhone = null;
    private String mScAddress;
    private int mSetSCAresult = -1;
    private static final int EVENT_HANDLE_GET_SCA_DONE = 47;
    private static final int EVENT_HANDLE_SET_SCA_DONE = 49;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            switch (msg.what) {
                case EVENT_HANDLE_GET_SCA_DONE:
                    if (ar.exception != null) {
                    } else {
                        mScAddress = (String)ar.result;
                    }
                    break;
                case EVENT_HANDLE_SET_SCA_DONE:
                    if (ar.exception != null) {
                        mSetSCAresult = 0;
                    } else {
                        mSetSCAresult = 1;
                    }  
                    break;
                default:
                    break;
            }
        }
    };

    public static SmsContextObserver getInstance(Context mCont ){
        if (null == sInstance) {
            sInstance = new SmsContextObserver(mCont);
        }
        return sInstance;
    }

    public SmsContextObserver (Context mCont){
        super(null);
        mContext = mCont;
        telephonyManager = TelephonyManager.from(mContext);
        initBroadReceiver();
        mPhone = PhoneFactory.getDefaultPhone();
        mPhone.getSmscAddress(mHandler.obtainMessage(EVENT_HANDLE_GET_SCA_DONE)); 
    }

    public void initBroadReceiver(){
        //dealwith send intent
        Intent sentIntent = new Intent(SENT_SMS_ACTION);  
        sentPI = PendingIntent.getBroadcast(mContext, 0, sentIntent,0);
        mContext.registerReceiver(new BroadcastReceiver() {  
        @Override  
            public void onReceive(Context _context, Intent _intent) {  
                switch (getResultCode()) {  
                    case Activity.RESULT_OK:  
                     // deal send ok
                    break;  
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:  
                    break;  
                    case SmsManager.RESULT_ERROR_RADIO_OFF:  
                    break;  
                    case SmsManager.RESULT_ERROR_NULL_PDU:  
                    break;  
                }  
            }  
        }, new IntentFilter(SENT_SMS_ACTION));

        //dealwith deilverIntent      
        Intent deliverIntent = new Intent(DELIVERED_SMS_ACTION);  
        deliverPI = PendingIntent.getBroadcast(mContext, 0,deliverIntent, 0);  
        mContext.registerReceiver(new BroadcastReceiver() {  
                @Override  
                public void onReceive(Context _context, Intent _intent) {  
                    //dealwith receive ok
                }  
            }, new IntentFilter(DELIVERED_SMS_ACTION));  
    }

    @Override
    public void onChange(boolean selfChange) {
        //query sms data
        super.onChange(selfChange);
    }

    /*  quey Sms database
    sms database
        _id: the sms id
        thread_id£ºconversation id
        address:  Sender's address
        person: Sender 's name in contact
        date: time ,the type is long
        protocol: Protocol , 0 means sms , 1 means Mms
        read:  the sms whether read , 0 means unread , 1 means read
        status: the sms status , 0 means complete , 64 means Pending , 128 means fail
        type: sms type , 1 means receive , 2 means send
        body: sms Content
        service_center: sms Center
    */
    public String getSmsFromPhone(String Str){
        int mPageNumber =getpageNumber(Str) ;
        StringBuilder smsBuilder = new StringBuilder();

        try {
            ContentResolver cr =mContext.getContentResolver();
            String[] projection = new String[] { "_id", "address", "body", "date", "type","read","protocol" };
            Cursor cur =mContext.getContentResolver().query(SMS_INBOX, projection, null, null, "date desc");  

            if (null == cur)  
                return "0|GetSmsContent"; 

            if (cur.moveToFirst()) {
                int index_id = cur.getColumnIndex("_id");
                int index_Address = cur.getColumnIndex("address");    
                int index_Body = cur.getColumnIndex("body");  
                int index_Date = cur.getColumnIndex("date");  
                int index_Type = cur.getColumnIndex("type");
                int index_Read = cur.getColumnIndex("read");
                int index_protocol = cur.getColumnIndex("protocol");
                int mCount = cur.getCount();
                int Mpostion = (mPageNumber -1)*10 + 1;

                smsBuilder.append(mCount+"|");
                cur.moveToPosition(Mpostion); 
                for(int i = 0; i < 10; i++){ 
                    int intID = cur.getInt(index_id);
                    String strAddress = cur.getString(index_Address);   
                    String strbody = cur.getString(index_Body);  
                    long longDate = cur.getLong(index_Date);  
                    int intType = cur.getInt(index_Type);
                    int intRead = cur.getInt(index_Read);
                    int intprotocol = cur.getInt(index_protocol);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");  
                    Date d = new Date(longDate);  
                    String strDate = dateFormat.format(d);  

                    if (intType == 1 && intprotocol == 0) {   
                        smsBuilder.append(intID+"|");  
                        smsBuilder.append(strAddress + "|");
                        smsBuilder.append(strbody + "|");  
                        smsBuilder.append(strDate + "|");
                        smsBuilder.append(intRead);
                    } else if (intType == 2) {  
                        // sender sms database
                    } 

                    if(cur.moveToNext()==false)break;
                } 

                if (!cur.isClosed()) {  
                    cur.close();  
                    cur = null;  
                }  
            }
        } catch (SQLiteException ex) {  
            android.util.Log.d(TAG,"fail curor");
        }  
        return "1|GetSmsContent|"+smsBuilder.toString();
    }    

    public String CleanSmsUnread(String Str){
        int mSetreadSms = getpageNumber(Str);
        String where = "_id=" + mSetreadSms;
        //String query = WhereBuilder.b("address", "in", targets).and("protocol", "=", "0").toString();
        ContentResolver cr = mContext.getContentResolver();
        ContentValues ct = new ContentValues();
        ct.put("read",1);
        int Result = cr.update(SMS_INBOX,ct,where,null);
        return Result+"|CleanSmsUnread";
    }

    public String DeleteSmsFromPhone(String Str){
        int mDeleteid = getpageNumber(Str);
        String where = "_id=" + mDeleteid;
        ContentResolver cr = mContext.getContentResolver();
        int result = cr.delete(SMS_INBOX, where, null);

        return result+"|DeleteSms";
    }

    public String SendSms(String Str){
        String phoneNumber = getphoneNumber(Str);
        String message = getSmsContent(Str);
        SmsManager smsManager = SmsManager.getDefault();  
        List<String> divideContents = smsManager.divideMessage(message);   
        for (String text : divideContents) {    
            smsManager.sendTextMessage(phoneNumber, mScAddress, text, sentPI, deliverPI);    
        }  
        return "Result|SendSms";
    }

    public int getUnreadSmsCount() { 
        int result = 0; 
        Cursor csr = mContext.getContentResolver().query(SMS_INBOX, null, "type = 1 and read = 0", null, null); 
        if (csr != null) { 
            result = csr.getCount(); 
            csr.close(); 
        } 
        return result; 
    } 

    public String getScAddress (){
        mPhone.getSmscAddress(mHandler.obtainMessage(EVENT_HANDLE_GET_SCA_DONE));
        return "GetScAddress|"+mScAddress;
    }

    public String SetScAddress(String str){
        String sca = getScAddressFromStr(str);
        mPhone.setSmscAddress(sca, mHandler.obtainMessage(EVENT_HANDLE_SET_SCA_DONE));
        return mSetSCAresult+"|SetScAddress";
    }

    public String getSMsVaildTime (){
        return "1|GetSMsVaildTime|12";
    }

    public String setSMsVaildTime (String str){
        return "1|SetSMsVaildTime|";
    }

    public String setSMsReport (String str){
        return "1|SetSMsReport";
    }

    public String getSMsReport (){
        return "1|GetSMsReport|0";
    }

    public String getScAddressFromStr (String Str){
        String mArrayStr[] = Str.split("\\|");
        return mArrayStr[2];
    }

    public int getpageNumber (String Str){
        String mArrayStr[] = Str.split("\\|");
        return Integer.valueOf(mArrayStr[2]);
    }

    public String getphoneNumber (String Str){
        String mArrayStr[] = Str.split("\\|");
        return mArrayStr[2];
    }

    public String getSmsContent (String Str){
        String mArrayStr[] = Str.split("\\|");
        return mArrayStr[3];
    }
}
