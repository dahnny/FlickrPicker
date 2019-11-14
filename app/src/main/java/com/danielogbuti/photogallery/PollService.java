package com.danielogbuti.photogallery;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.text.PrecomputedText;
import android.util.Log;

import java.lang.annotation.Target;
import java.util.ArrayList;

public class PollService extends JobIntentService {
    private static final String TAG = "ogbuti.photogallery";

    private static final int POLL_INTERVAL = 1000 * 60 * 5;//5 mins
    private static final String CHANNEL_ID = "id";
    public static final String PREF_IS_ALARM_ON = "isAlarmOn";
    public static final String ACTION_SHOW_NOTIFICATION = "com.danielogbuti.photogallery.SHOW_NOTIFICATION";
    public static final String PERM_PRIVATE = "com.danielogbuti.photogallery.PRIVATE";




    @Override
    public void onHandleWork(Intent intent) {

        createNotificationChannel();
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        //checking if the network is available to run background application
        boolean isNetworkAvailable = cm.getBackgroundDataSetting() && cm.getActiveNetworkInfo()!=null;

        if (!isNetworkAvailable) return;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String query = pref.getString(FlickrFetcher.PREF_SEARCH_QUERY,null);
        String lastResultId = pref.getString(FlickrFetcher.PREF_LAST_RESULT_ID,null);

        ArrayList<GalleryItem> items;

        if (query != null){
            items = new FlickrFetcher().search(query,this);
        }else {
            items = new FlickrFetcher().fetchItems(1,this);
        }


        if (items.size() == 0)
            return;

        String resultId = items.get(0).getId();

        if (!resultId.equals(lastResultId)){
            Log.i(TAG,"Got a new Result "+resultId);

            Resources r = getResources();
            PendingIntent pi = PendingIntent
                    .getActivity(this,0,new Intent(this,PhotoGalleryActivity.class),0);

            Notification notification = new NotificationCompat.Builder(this,CHANNEL_ID)
                    .setTicker(r.getString(R.string.new_pictures_title))
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentTitle(r.getString(R.string.new_pictures_title))
                    .setContentText(r.getString(R.string.new_pictures_text))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build();

            showBackgroundNotification(0,notification);

        }else {
            Log.i(TAG,"Got an old result "+resultId);
        }

        pref.edit()
                .putString(FlickrFetcher.PREF_LAST_RESULT_ID,resultId)
                .apply();

    }

    void showBackgroundNotification(int requestCode, Notification notification){
        Intent i = new Intent(ACTION_SHOW_NOTIFICATION);
        i.putExtra("REQUEST_CODE",requestCode);
        i.putExtra("NOTIFICATiON",notification);

        sendOrderedBroadcast(i,PERM_PRIVATE,null,null,Activity.RESULT_OK,null,null);
    }


    @TargetApi(26)
    public static void setServiceAlarm(Context c, boolean isOn){

         Intent i = new Intent(c,StartupReceiver.class);


            //Pending intent carries a message to start an intent when called
            PendingIntent pendingIntent = PendingIntent.getBroadcast(c, 0, i, 0);
            //This starts the alarm manager
            AlarmManager alarmManager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            // this repeats the alarm when true and cancels it when false
            if (isOn) {
                alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), POLL_INTERVAL, pendingIntent);

            } else {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }

        PreferenceManager.getDefaultSharedPreferences(c)
                .edit()
                .putBoolean(PREF_IS_ALARM_ON,isOn)
                .apply();




    }

    public static boolean isServiceAlarmOn(Context context) {
        Intent i = new Intent(context, StartupReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_NO_CREATE);
        //check if pending intent already has a message
        return pendingIntent != null;
    }

    public void createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            CharSequence name = "Service Channel";
            String description = "Channel for services";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,name,importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

    }
    }


