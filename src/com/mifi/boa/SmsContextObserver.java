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
import android.telephony.TelephonyManager;
import android.content.BroadcastReceiver;
import android.app.Activity;
import android.content.IntentFilter;
import android.content.Intent;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import android.os.Handler;
import android.os.Message;
import android.os.AsyncResult;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.SmsParameters;
import java.util.ArrayList;
import android.os.SystemProperties;
import java.util.HashMap;



public class SmsContextObserver extends ContentObserver{
    private static SmsContextObserver sInstance;
    private Uri SMS_INBOX = Uri.parse("content://sms/");
	private Uri SMS_ICC = Uri.parse("content://sms/icc");
    private Context mContext;
    static final String TAG = "BoaService_SmsContextObserver";
    private TelephonyManager telephonyManager;
    private Phone mPhone = null;
    private String mScAddress;
    private boolean mSetSCAresult = false;
    private static final int EVENT_HANDLE_GET_SCA_DONE = 47;
    private static final int EVENT_HANDLE_SET_SCA_DONE = 49;
    private String SENT_SMS_ACTION = "SENT_SMS_ACTION";
    private String DELIVERED_SMS_ACTION = "DELIVERED_SMS_ACTION";
	final String Sms_Report = "persist.sys.sms.report";
    private StringBuilder mCutSIMSmsBuilder = new StringBuilder();
    private HashMap SimSmsMap  = new HashMap<Integer,String>();
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
                        mSetSCAresult = false;
                    } else {
                        mSetSCAresult = true;
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
        mContext.registerReceiver(new BroadcastReceiver() {  
            @Override  
            public void onReceive(Context _context, Intent _intent) {  
                //dealwith receive ok
            }  
        }, new IntentFilter(DELIVERED_SMS_ACTION));

        mContext.registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context content, Intent intent) {    
                int state = telephonyManager.getSimState();    
                if(state == TelephonyManager.SIM_STATE_READY){ 
                    ReadSimSmsThread mReadSimSms = new ReadSimSmsThread();
                    mReadSimSms.start();
                }

            }
        }, new IntentFilter("android.intent.action.SIM_STATE_CHANGED"));
    }

    @Override
    public void onChange(boolean selfChange) {
        //query sms data
        super.onChange(selfChange);
		android.util.Log.d(TAG,"onChange icc");
        ReadSimSmsThread mReadSimSms = new ReadSimSmsThread();
        mReadSimSms.start();
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
        int mCount = 0;

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
                int Mpostion = (mPageNumber -1)*10 + 0;

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
                        mCount++;
                        smsBuilder.append("|");
                        smsBuilder.append(intID+"|");  
                        smsBuilder.append(strAddress + "|");
                        String mCutBody = getCutBody(strbody,20);
                        smsBuilder.append(mCutBody+ "|");  
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
        return "1|GetSmsContent|"+mCount+smsBuilder.toString();
    }    

    public String getOneSmsAndClean(String Str){
        StringBuilder mOneSmsBuilder = new StringBuilder();
        int mSetreadSms = getpageNumber(Str);
        String where = "_id=" + mSetreadSms;
        ContentResolver cr = mContext.getContentResolver();
        ContentValues ct = new ContentValues();
        ct.put("read",1);
        int Result = cr.update(SMS_INBOX,ct,where,null);
        String[] projection = new String[] { "_id", "address", "body", "date"};
        Cursor cur =cr.query(SMS_INBOX, projection,where, null,"date desc");
        try {
            while (cur != null && !cur.isClosed()&& cur.moveToNext()){
                int intID = cur.getInt(cur.getColumnIndex("_id"));
                String strAddress = cur.getString(cur.getColumnIndex("address"));
                String strbody = cur.getString(cur.getColumnIndex("body"));
                long longDate = cur.getLong(cur.getColumnIndex("date"));
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");  
                Date d = new Date(longDate);
                String strDate = dateFormat.format(d);
                mOneSmsBuilder.append(intID+"|");  
                mOneSmsBuilder.append(strAddress + "|");
                mOneSmsBuilder.append(strbody+ "|");  
                mOneSmsBuilder.append(strDate);
            }
        } finally {
            if (cur != null)
            cur.close();
        }
        return Result+"|GetOneSmsAndClean|"+mOneSmsBuilder.toString();
    }

    public String DeleteSmsFromPhone(String Str){
        int mDeleteid = getpageNumber(Str);
        String where = "_id=" + mDeleteid;
        ContentResolver cr = mContext.getContentResolver();
        int result = cr.delete(SMS_INBOX, where, null);

        return result+"|DeleteSms";
    }
    //https://blog.csdn.net/loongshawn/article/details/62215914
    public String getCutBody(String str,int length){
        int count = 0;
        int offset = 0;
        char[] c = str.toCharArray(); 
        int size = c.length;
        if(size >= length){
            for (int i = 0; i < c.length; i++) {
                if (c[i] > 256) {
                    offset = 2;
                    count += 2;
                } else {
                    offset = 1;
                    count++;
                }
                if (count == length) {
                return str.substring(0, i + 1);
                }
                if ((count == length + 1 && offset == 2)) {
                return str.substring(0, i);
                }
            }
        }else{
            return str;
        }
        return "";
    }

    public String SendSms(String Str){
        Intent intent = new Intent();
        intent.setAction("BoaService.Send.SMS");
        intent.putExtra("data",Str);
        intent.putExtra("mScAddress",mScAddress);
        mContext.sendBroadcast(intent);
        return "1|SendSms";
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

    public String getSmsSettings (){
        mPhone.getSmscAddress(mHandler.obtainMessage(EVENT_HANDLE_GET_SCA_DONE));
		 SmsManager smsManager = SmsManager.getDefault();
         int time = smsManager.getSmsParameters().vp;
		 String report = SystemProperties.get(Sms_Report,"0");
        return "0|GetSmsSettings|"+time+"|"+mScAddress+"|"+report;
    }

    public String setSmsSettings(String str){
        String sca = getScAddressFromStr(str);
        mPhone.setSmscAddress(sca, mHandler.obtainMessage(EVENT_HANDLE_SET_SCA_DONE));
		SmsManager smsManager = SmsManager.getDefault();
        SmsParameters mSmsParameters = smsManager.getSmsParameters();
        int time = getSmstime(str);
        mSmsParameters.vp = time ;
        boolean resulttime = smsManager.setSmsParameters(mSmsParameters);
		String mSmsreport = getSmsReprot(str);
		SystemProperties.set(Sms_Report,mSmsreport);
		int result = resulttime == mSetSCAresult ? 1 : 0;
        return result+"|SetSmsSettings";
    }

    /*public String getSMsVaildTime (){
         SmsManager smsManager = SmsManager.getDefault();
         int time = smsManager.getSmsParameters().vp;
        return "1|GetSMsVaildTime|"+time;
    }

    public String setSMsVaildTime (String str){
         SmsManager smsManager = SmsManager.getDefault();
         SmsParameters mSmsParameters = smsManager.getSmsParameters();
         int time = getpageNumber(str);
         mSmsParameters.vp = time ;
         boolean result = smsManager.setSmsParameters(mSmsParameters);
         int resuultint = result ? 1 : 0 ; 
        return resuultint+"|SetSMsVaildTime";
    }*/


	public String getScAddressFromStr (String Str){
		String mArrayStr[] = Str.split("\\|");
		return mArrayStr[3];
	}
	public int getSmstime (String Str){
		String mArrayStr[] = Str.split("\\|");
		return Integer.valueOf(mArrayStr[2]);
	}
	public String getSmsReprot (String Str){
		String mArrayStr[] = Str.split("\\|");
		return mArrayStr[4];
	}

	public int getpageNumber (String Str){
		String mArrayStr[] = Str.split("\\|");
		return Integer.valueOf(mArrayStr[2]);
	}

	public String getSmsFromSIM(){ 
		return "1"+"GetSIMSms"+mCutSIMSmsBuilder.toString();
     }
    public String getOneSmsFromSIM(String str){
        int key = getSimSmsID(str);
        String SimSMSContent=null;
        if(!SimSmsMap.isEmpty()){
            SimSMSContent =(String)SimSmsMap.get(key);
        }
        int result = SimSMSContent == null ? 0 : 1;
    return result+"|GetOneSIMSms"+SimSMSContent;
    }
    
    public int getSimSmsID(String str){
        String mArrayStr[] = str.split("\\|");
    return Integer.valueOf(mArrayStr[2]);

    }

	public class ReadSimSmsThread extends Thread {

		@Override  
		public void run(){
			SmsManager smsManager = SmsManager.getDefault();
			ArrayList<SmsMessage> messages= smsManager.getAllMessagesFromIcc();
			android.util.Log.d(TAG,"ArrayList<SmsMessage>");
			final int count = messages.size();
			android.util.Log.d(TAG,"count ="+count);
			for (int i = 0; i < count; i++) {
				SmsMessage message = messages.get(i);
				if (message != null) {
					android.util.Log.d(TAG,"i ="+i);
					String bodydisply = message.getDisplayMessageBody();
					String body= message.getMessageBody();
					String dislayaddr= message.getDisplayOriginatingAddress();
					String addr= message.getOriginatingAddress();
					long time = message.getTimestampMillis();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");  
                    Date d = new Date(time);
                    String strDate = dateFormat.format(d);
                    SimSmsMap.put(i,addr+"|"+body+"|"+strDate);
                    String cutSmsbody = getCutBody(body,20);
                    mCutSIMSmsBuilder.append("|"); 
                    mCutSIMSmsBuilder.append(i+"|");  
                    mCutSIMSmsBuilder.append(dislayaddr + "|");
                    mCutSIMSmsBuilder.append(cutSmsbody+ "|");  
                    mCutSIMSmsBuilder.append(strDate);
				}
			}
		}

	}

}
