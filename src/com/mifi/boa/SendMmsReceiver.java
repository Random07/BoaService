package com.mifi.boa;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.telephony.SmsManager;
import java.util.List;

public class SendMmsReceiver extends BroadcastReceiver {
    static final String TAG = "Boservice_SendMms";
    private String SENT_SMS_ACTION = "SENT_SMS_ACTION";
    private String DELIVERED_SMS_ACTION = "DELIVERED_SMS_ACTION";
    private PendingIntent sentPI;
    private PendingIntent deliverPI;

    public SendMmsReceiver() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        
        String action = intent.getAction();
		android.util.Log.d(TAG,"onReceive"+action);
		if (action.equals("android.intent.action.BOOT_COMPLETED")){
			Intent startIntent = new Intent(context, BoaService.class);
			 startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startService(startIntent);
		}else if (action.equals("BoaService.Send.SMS")){
			initPendingIntent(context);
			String mscAdd = intent.getStringExtra("mScAddress");
			String data=intent.getStringExtra("data");
			SendSms(mscAdd,data);
		}
    }

    public void SendSms(String mScAddress,String data){
        String phoneNumber = getphoneNumber(data);
        String message = getSmsContent(data);
        SmsManager smsManager = SmsManager.getDefault();  
        List<String> divideContents = smsManager.divideMessage(message);   
        for (String text : divideContents) {    
            smsManager.sendTextMessage(phoneNumber, mScAddress, text, sentPI, deliverPI);    
        }  
    }
    public String getphoneNumber (String Str){
        String mArrayStr[] = Str.split("\\|");
        return mArrayStr[2];
    }

    public String getSmsContent (String Str){
        String mArrayStr[] = Str.split("\\|");
        return mArrayStr[3];
    }
    public void initPendingIntent(Context mContext){
        //send intent
        Intent sentIntent = new Intent(SENT_SMS_ACTION);  
        sentPI = PendingIntent.getBroadcast(mContext, 0, sentIntent,0);
        //deilverIntent      
        Intent deliverIntent = new Intent(DELIVERED_SMS_ACTION);  
        deliverPI = PendingIntent.getBroadcast(mContext, 0,deliverIntent, 0);  
    }
   }

