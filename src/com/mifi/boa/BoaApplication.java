package com.mifi.boa;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BoaApplication extends Application {

    public BoaApplication() {
    }

    @Override
    public void onCreate() {
        Log.d("BoaApplication", "Oncreate...");
        //Intent startIntent = new Intent(this, BoaService.class);
        //startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //this.startService(startIntent);
    }
}