package com.mifi.boa;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.os.Handler;
import static android.net.ConnectivityManager.TETHERING_USB;
import android.os.SystemProperties;

public class UserDataReceiver extends BroadcastReceiver {
    static final String TAG = "BoaService_UserDataReceiver";
    static final String USB_CONFIG_PROPERTY = "sys.usb.config";
    static final int MODE_DATA_NONE   = 0x00 << 1;
    static final int MODE_DATA_MTP    = 0x01 << 1;
    static final int MODE_DATA_PTP    = 0x02 << 1;
    static final int MODE_DATA_MIDI   = 0x03 << 1;
    static final int MODE_DATA_MASS_STORAGE = 0x04 << 1;
    static final int MODE_DATA_BICR   = 0x05 << 1;
    static boolean bUsbInsert = false;
    private UserDataHandler mUserDataHandler;
    private Handler mHandler = new Handler();
    private OnStartTetheringCallback mStartTetheringCallback = new OnStartTetheringCallback();

    private static final class OnStartTetheringCallback extends
        ConnectivityManager.OnStartTetheringCallback {
        @Override
        public void onTetheringStarted() {
            Log.d(TAG,"onTetheringStarted");
        }

        @Override
        public void onTetheringFailed() {
            Log.d(TAG,"onTetheringFailed");
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Intent resultIntent = new Intent();
        String mRet = "";

        mUserDataHandler = UserDataHandler.getInstance(context);
        Log.d(TAG, "Received broadcast: " + action);
        if("com.mifi.boa.set.DataLimit".equals(action)){
            String data=intent.getStringExtra("data");
            Log.d(TAG, "data : " + data);
            mRet = mUserDataHandler.setDataLimit(data);
            resultIntent.setAction("com.mifi.boa.confirm.DataLimit"); 
        }else if("com.mifi.boa.get.DataStatic".equals(action)){
            mRet = mUserDataHandler.getDataStatic();
            resultIntent.setAction("com.mifi.boa.confirm.DataStatic");
        }else if("com.mifi.boa.clear.data".equals(action)){
            String data=intent.getStringExtra("data");
            Log.d(TAG, "clear data : " + data);
            mRet = mUserDataHandler.clearData(data);
            resultIntent.setAction("com.mifi.boa.confirm.ClearData");
        }else if("com.mifi.boa.reset.datalimit".equals(action)){
            mUserDataHandler.resetDataLimit();
        }else if("android.intent.action.BOOT_COMPLETED".equals(action)){
            mUserDataHandler.checkResetDataLimit();
        }else if("android.hardware.usb.action.USB_STATE".equals(action)){
            ConnectivityManager mCm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            boolean mUsbConnected = intent.getBooleanExtra(UsbManager.USB_CONNECTED, false);
            int  mUsbMode = getUsbDataMode();
            Log.d(TAG, "mUsbConnected : " + mUsbConnected + ", bUsbInsert = " + bUsbInsert + ", mUsbMode = " + mUsbMode);

            if(mUsbConnected){
                if(!bUsbInsert || mUsbMode == MODE_DATA_NONE){
                    Log.d(TAG, "startTethering - usb");
                    mCm.startTethering(TETHERING_USB, true, mStartTetheringCallback, mHandler);
                }
            }else{
                Log.d(TAG, "stopTethering - usb");
                mCm.stopTethering(TETHERING_USB);
            }
            bUsbInsert = mUsbConnected;
        }

        Log.d(TAG, "mRet: " + mRet);
        if(!"".equals(mRet)){
            resultIntent.putExtra("data",mRet);
            context.sendBroadcast(resultIntent);
        }else{
            Log.d(TAG, "Do not send result!");
        }
    }

    public int getUsbDataMode() {
        String functions = SystemProperties.get(USB_CONFIG_PROPERTY);
        if (UsbManager.containsFunction(functions, UsbManager.USB_FUNCTION_MTP)) {
            return MODE_DATA_MTP;
        } else if (UsbManager.containsFunction(functions, UsbManager.USB_FUNCTION_PTP)) {
            return MODE_DATA_PTP;
        } else if (UsbManager.containsFunction(functions, UsbManager.USB_FUNCTION_MIDI)) {
            return MODE_DATA_MIDI;
        } else if (UsbManager.containsFunction(functions, UsbManager.USB_FUNCTION_MASS_STORAGE)) {
            return MODE_DATA_MASS_STORAGE;
        } else if (UsbManager.containsFunction(functions, UsbManager.USB_FUNCTION_BICR)) {
            return MODE_DATA_BICR;
        }
        return MODE_DATA_NONE;
    }
}
