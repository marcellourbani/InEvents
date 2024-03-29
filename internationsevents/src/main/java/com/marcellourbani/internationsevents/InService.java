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
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class InService extends IntentService {
    public static final String RELOAD_EVENTS = "INEVENTS_RELOAD";
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
            new RefreshTask(intent).execute();
        else {
            InReceiver.completeWakefulIntent(intent);
            retry();
        }
    }

    private void retry() {
        AlarmManager alarm = (AlarmManager) InApp.get().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(InApp.get(), InReceiver.class);
        PendingIntent pintent = PendingIntent.getService(InApp.get(), 0, intent, 0);
        alarm.set(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis() + 600000, pintent);//10 minutes
    }

    static void schedule(boolean reschedule) {
        long period = getPeriod();
        long start = Calendar.getInstance().getTimeInMillis() + period;
        AlarmManager alarm = (AlarmManager) InApp.get().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(InApp.get(), InReceiver.class);
        PendingIntent pintent = PendingIntent.getBroadcast(InApp.get(), 0, intent, PendingIntent.FLAG_NO_CREATE);
        if (pintent == null) {
            pintent = PendingIntent.getBroadcast(InApp.get(), 0, intent, 0);//PendingIntent.getService(InApp.get(), 0, intent, 0);
        } else {
            if (reschedule)
                alarm.cancel(pintent);
            else return;
        }
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, start, period, pintent);
    }

    private static long getPeriod() {
        SharedPreferences sprefs = PreferenceManager.getDefaultSharedPreferences(InApp.get());
        return 3600000 * (sprefs == null ? 6 : Integer.parseInt(sprefs.getString("pr_refresh_interval", "6")));
    }

    private class RefreshTask extends AsyncTask<String, Integer, Boolean> {
        private Intent mIntent;
        public RefreshTask(Intent i){
            mIntent = i;
        }
        @Override
        protected Boolean doInBackground(String... strings) {
            if (prefs == null) prefs = PreferenceManager.getDefaultSharedPreferences(InApp.get());
            bot = new InternationsBot(prefs);
            bot.clearold();
            bot.loadEvents();
            bot.loadMyGroups();
            if (!bot.sign()) {
                if (bot.passIsSet()) retry();
                return false;
            }
            InError.get().clear();
            bot.readMyEvents(true,"");
            if (InError.isOk()) InCalendar.syncEvents(bot.mEvents);
            if (InError.isOk() && bot.isExpired(InternationsBot.Refreshkeys.GROUPS)) {
                bot.readMyGroups();
                if (InError.isOk()) bot.saveGroups();
            }
            if (InError.isOk()) bot.readGroupsEvents();
            if (InError.isOk()) bot.saveEvents(true);
            return InError.isOk();
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            sendNotifications();
            InReceiver.completeWakefulIntent(mIntent);
            sendBroadcast(new Intent(RELOAD_EVENTS));
        }
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
}
