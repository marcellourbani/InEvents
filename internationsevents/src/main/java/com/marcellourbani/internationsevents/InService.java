/*
 * In Events for Android
 *
 * Copyright (C) 2014 Marcello Urbani.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.marcellourbani.internationsevents;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class InService extends IntentService implements InAsyncTask.Listener {
    public static final String RELOAD_EVENTS = "INEVENTS_RELOAD";
    public static final String ACTION_REFRESH="INEVENTS_REFRESH_MINE";
    public static final String ACTION_SUBSCRIBE="INEVENTS_SUBSCRIBE";
    public static final String ACTION_UNSUBSCRIBE="INEVENTS_UNSUBSCRIBE";
    public static final String ACTION_REFRESH_ALL="INEVENTS_REFRESH_ALL";
    public static final String EVENTID = "INEVENTS_EVENTID";
    public static final String ACTION_LOAD = "INEVENTS_LOAD";

    final static DateFormat DATEFORMAT = new SimpleDateFormat("dd.MM.yy kk:mm"),
            ALLLDAYDF = new SimpleDateFormat("dd.MM.yy");

    SharedPreferences prefs;
    private InternationsBot bot;

    public InService() {
        super("InService");
    }

    @SuppressWarnings("ResourceType")
    @Override
    protected void onHandleIntent(Intent intent) {
        if (InApp.get().isConnected())
            new InAsyncTask(intent).setListener(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else if(intent.getAction()==ACTION_REFRESH_ALL){
            InReceiver.completeWakefulIntent(intent);
            retry();
        }
    }

    private void retry() {
        AlarmManager alarm = (AlarmManager) InApp.get().getSystemService(Context.ALARM_SERVICE);
        PendingIntent pintent = InReceiver.getIntent(false);
        alarm.set(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis() + 600000, pintent);//10 minutes
    }

    static void schedule(boolean reschedule,boolean startNow) {
        AlarmManager alarm = (AlarmManager) InApp.get().getSystemService(Context.ALARM_SERVICE);

        PendingIntent pintent = InReceiver.getIntent(true);

        if (pintent == null) {
            pintent = InReceiver.getIntent(false);
        } else {
            if (reschedule)
                alarm.cancel(pintent);
            else //already scheduled, no reschedule request
                return;
        }
        long period = getPeriod();
        long start = Calendar.getInstance().getTimeInMillis() + (startNow?0:period);
        if(period>0)
            alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, start, period, pintent);
    }

    private static long getPeriod() {
        SharedPreferences sprefs = PreferenceManager.getDefaultSharedPreferences(InApp.get());
        return 3600000 * (sprefs == null ? 6 : Integer.parseInt(sprefs.getString("pr_refresh_interval", "6")));
    }

    private void sendNotifications() {
        if (!prefs.getBoolean("pr_notify_new", true)) return;
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        for (InEvent e : bot.getEvents()) {
            if (e.isNew() && e.mEventId != null) {
                String title = (e.isEvent() ? "New Event" : "New Activity") + e.mTitle;
                String text = e.mAllDay ? ALLLDAYDF.format(e.mStart.getTime()) : DATEFORMAT.format(e.mStart.getTime());
                if (e.mGroup != null) text = "from:" + e.mGroup + "\n" + text;
                if (e.mLocation != null) text = text + " at " + e.mLocation;
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.ic_launcher)
                                .setContentTitle(title)
                                .setContentText(text)
                                .setAutoCancel(true)
                                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE);
                Intent resultIntent = new Intent(this, EventList.class);
                resultIntent.setData(Uri.parse("content://" + e.mEventId));
                resultIntent.putExtra(InApp.NOTIFIEDEVENT, e.mEventId);
                resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                stackBuilder.addParentStack(EventList.class);
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(
                                0,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );
                mBuilder.setContentIntent(resultPendingIntent);
                mNotificationManager.notify(Integer.parseInt(e.mEventId), mBuilder.build());
            }
        }
    }

    @Override
    public void onCompleted(Intent intent) {
        sendNotifications();
        InReceiver.completeWakefulIntent(intent);
        sendBroadcast(new Intent(InService.RELOAD_EVENTS));
    }

    @Override
    public void onFailed(Intent intent) {
        retry();
    }
}
