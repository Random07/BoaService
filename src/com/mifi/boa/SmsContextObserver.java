package com.mifi.boa;

import android.database.ContentObserver;
import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.content.Context;
import android.database.sqlite.SQLiteException;
import java.sql.Date;  
import java.text.SimpleDateFormat;  

public class SmsContextObserver extends ContentObserver{
   
   private static SmsContextObserver sInstance;
   private Uri SMS_INBOX = Uri.parse("content://sms/");
   private Context mContext;
   static final String TAG = "BoaService";
   public static SmsContextObserver getInstance(Context mCont ){
        if (null == sInstance) {
            sInstance = new SmsContextObserver(mCont);
            
        }
        return sInstance;
    }

    public SmsContextObserver (Context mCont){
           super(null);
           mContext = mCont;

    }

     @Override
    public void onChange(boolean selfChange) {
           //query sms data
           super.onChange(selfChange);
           getSmsFromPhone();  

     }

     public void getSmsFromPhone(){
        try {
            StringBuilder smsBuilder = new StringBuilder();
            ContentResolver cr =mContext.getContentResolver();
            String[] projection = new String[] { "_id", "address", "person", "body", "date", "type" };    
            //String where =  " date > "  + (System.currentTimeMillis() - 60*1000); 
            Cursor cur =mContext.getContentResolver().query(SMS_INBOX, projection, null, null, "date desc");   
            if (null == cur)  
            return; 
            if (cur.moveToFirst()) {  
                int index_Address = cur.getColumnIndex("address");  
                int index_Person = cur.getColumnIndex("person");  
                int index_Body = cur.getColumnIndex("body");  
                int index_Date = cur.getColumnIndex("date");  
                int index_Type = cur.getColumnIndex("type");  
                do {  
                    String strAddress = cur.getString(index_Address);  
                    int intPerson = cur.getInt(index_Person);  
                    String strbody = cur.getString(index_Body);  
                    long longDate = cur.getLong(index_Date);  
                    int intType = cur.getInt(index_Type);  

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");  
                    Date d = new Date(longDate);  
                    String strDate = dateFormat.format(d);  

                    String strType = "";  
                    if (intType == 1) {  
                        strType = "Ω” ’";  
                    } else if (intType == 2) {  
                        strType = "∑¢ÀÕ";  
                    } else {  
                        strType = "null";  
                    }  

                    smsBuilder.append("[ ");  
                    smsBuilder.append(strAddress + ", ");  
                    smsBuilder.append(intPerson + ", ");  
                    smsBuilder.append(strbody + ", ");  
                    smsBuilder.append(strDate + ", ");  
                    smsBuilder.append(strType);  
                    smsBuilder.append(" ]\n\n");
                    android.util.Log.d(TAG,"smsBuilder"+smsBuilder.toString());
                } while (cur.moveToNext()); 
                if (!cur.isClosed()) {  
                    cur.close();  
                    cur = null;  
                }  
            }
        } catch (SQLiteException ex) {  
            android.util.Log.d(TAG,"fail curor");
        }  
     }



    
}