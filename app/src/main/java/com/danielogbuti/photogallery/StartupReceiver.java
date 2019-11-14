package com.danielogbuti.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.JobIntentService;
import android.util.Log;

public class StartupReceiver extends BroadcastReceiver {

    private static final String TAG = "StartupReceiver";


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG,"Received broadcast intent"+intent.getAction());

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isOn = preferences.getBoolean(PollService.PREF_IS_ALARM_ON,false);
        intent.setClass(context,PollService.class);
        if (PollService.isServiceAlarmOn(context)){
            PollService.enqueueWork(context,PollService.class,1,intent);
        }else {
            PollService.setServiceAlarm(context,isOn);
        }
    }
}
