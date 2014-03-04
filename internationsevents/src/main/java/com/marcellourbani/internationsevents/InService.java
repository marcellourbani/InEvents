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
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Calendar;

public class InService extends IntentService {
    SharedPreferences prefs;

    public InService() {
        super("InService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        prefs = PreferenceManager.getDefaultSharedPreferences(InApp.get());
        Boolean usemobile = prefs.getBoolean("pr_refresh_mobile", false);
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNI = connectivityManager.getActiveNetworkInfo();
        if (activeNI != null && activeNI.isConnected() && (usemobile || activeNI.getType() == ConnectivityManager.TYPE_WIFI))
            new refreshTask().doInBackground("");
            else retry();
    }

    private void retry() {
        AlarmManager alarm = (AlarmManager) InApp.get().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(InApp.get(), InReceiver.class);
        PendingIntent pintent =  PendingIntent.getService(InApp.get(), 0, intent, 0);
        alarm.set(AlarmManager.RTC_WAKEUP,Calendar.getInstance().getTimeInMillis() +600000,pintent);//10 minutes
    }

    static void schedule(boolean reschedule) {
        long period = getPeriod();
        long start = Calendar.getInstance().getTimeInMillis() + period;
        AlarmManager alarm = (AlarmManager) InApp.get().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(InApp.get(), InReceiver.class);
        PendingIntent pintent = PendingIntent.getBroadcast(InApp.get(), 0, intent, PendingIntent.FLAG_NO_CREATE);
        if (pintent == null) {
            pintent = PendingIntent.getBroadcast(InApp.get(), 0, intent,0);//PendingIntent.getService(InApp.get(), 0, intent, 0);
        } else {
            if (reschedule)
                alarm.cancel(pintent);
            else return;
        }
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, start + period, period, pintent);
    }

    private static long getPeriod() {
        SharedPreferences sprefs = PreferenceManager.getDefaultSharedPreferences(InApp.get());
        return 3600000 * (sprefs == null ? 6 : Integer.parseInt(sprefs.getString("pr_refresh_interval", "6")));
    }

    private class refreshTask extends AsyncTask<String, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(String... strings) {
            InternationsBot bot = new InternationsBot(prefs);
            bot.clearold();
            bot.loadEvents();
            bot.loadMyGroups();
            InError.get().clear();
            bot.readMyEvents(true);
            if (InError.isOk() && bot.isExpired(InternationsBot.Refreshkeys.GROUPS)) {
                bot.readMyGroups();
                if (InError.isOk()) bot.saveGroups();
            }
            if (InError.isOk()) bot.readGroupsEvents();
            if (InError.isOk()) bot.saveEvents(true);
            return InError.isOk();
        }
    }
}
