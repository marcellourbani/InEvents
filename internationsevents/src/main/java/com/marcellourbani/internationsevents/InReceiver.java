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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;


public class InReceiver extends WakefulBroadcastReceiver {
    private static final int MYREQUESTCODE = 123;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ( action == Intent.ACTION_POWER_CONNECTED||action==Intent.ACTION_BOOT_COMPLETED) {
            InService.schedule(false,true);
        } else {
            Intent i = new Intent(context, InService.class);
            i.setAction(InService.ACTION_REFRESH_ALL);
            startWakefulService(context, i);
        }
    }

    public static PendingIntent getIntent(boolean scheduled) {
        Intent intent = new Intent(InApp.get(), InReceiver.class);
        PendingIntent pintent = PendingIntent.getBroadcast(InApp.get(), MYREQUESTCODE, intent, scheduled ? PendingIntent.FLAG_NO_CREATE : 0);
        return pintent;
    }
}
