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

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.support.v4.util.ArrayMap;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class InCalendar {
    private static final Uri CAL_URI = CalendarContract.Calendars.CONTENT_URI;
    private static final String CALENDARS_WHERE = Calendars.CALENDAR_ACCESS_LEVEL + ">=" + Calendars.CAL_ACCESS_CONTRIBUTOR;
    private static String CUSTOMURL = Events.CUSTOM_APP_URI;// CalendarContract.ExtendedProperties.EVENT_ID;
    private String mId, mName;
    private static boolean hasurl(){
        return Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        this.mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }


    private InCalendar(Cursor c) {
        setId(c.getString(c.getColumnIndex(Calendars._ID)));
        setName(c.getString(c.getColumnIndex(Calendars.CALENDAR_DISPLAY_NAME)));
    }

    private static ContentValues getEventValues(Context context, InEvent event) {
        ContentValues values = new ContentValues();
        TimeZone timeZone = TimeZone.getDefault();
        values.put(CalendarContract.Events.DTSTART, event.mStart.getTimeInMillis());
        values.put(CalendarContract.Events.DTEND, event.mStop != null ? event.mStop.getTimeInMillis() : (event.mStart.getTimeInMillis() + 3600000));
        values.put(CalendarContract.Events.EVENT_TIMEZONE, timeZone.getID());
        values.put(CalendarContract.Events.TITLE, event.mTitle);
        values.put(Events.EVENT_LOCATION, event.mLocation);
        values.put(CalendarContract.Events.DESCRIPTION, event.mGroup + " " + event.mTitle + "\n" + event.mEventUrl);
        values.put(CalendarContract.Events.CALENDAR_ID, getDefaultCalendar(context));
        if(hasurl())
          values.put(CUSTOMURL, event.mEventUrl);
        return values;
    }

    public static void addEvent(Context context, InEvent event) {
        ContentResolver cr = context.getContentResolver();
        cr.insert(Events.CONTENT_URI, getEventValues(context, event));
    }
    private static String[] calendarColumns(){
        if(hasurl())return new String[]{Events._ID, Events.DESCRIPTION, CUSTOMURL};
        return new String[]{Events._ID, Events.DESCRIPTION};
    }
    private static String getUrlcondition(boolean strict){
        if(hasurl())return CUSTOMURL + (strict?" = ?":" like ?");
        return Events.DESCRIPTION +" like ?";
    }
    public static void modifyEvent(Context context, InEvent event) {
        String cal = getDefaultCalendar(context);
        if (cal == null || cal.length() == 0)
            return;
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(Events.CONTENT_URI, calendarColumns(),
                Events.CALENDAR_ID + " = ? AND " + getUrlcondition(true),
                new String[]{cal, hasurl()? event.mEventUrl:"%"+event.mEventUrl+"%"}, null);
        boolean updated = false;
        if(cursor.getCount()>1) return;
        while (cursor.moveToNext()) {
            updated = contentResolver.update(Events.CONTENT_URI, getEventValues(context, event),
                        "(" + Events._ID + " = ?)", new String[]{cursor.getString(0)}) > 0;
        }
        if (!updated) addEvent(context, event);
    }

    private static String getDefaultCalendar(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("pr_calendar", "");
    }

    public static List<InCalendar> getCalendars(Context context) {
        ArrayList<InCalendar> calendars = null;
        ContentResolver cr = context.getContentResolver();
        context.getResources();
        Cursor c = cr.query(CAL_URI, new String[]{Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME},
                CALENDARS_WHERE, null, Calendars.DEFAULT_SORT_ORDER);
        try {
            if (c != null && c.getCount() > 0) {
                calendars = new ArrayList<>();
                while (c.moveToNext()) {
                    calendars.add(new InCalendar(c));
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return calendars;
    }

    public static boolean addCalendarsPreferences(Context context, ListPreference calendarPref) {
        List<InCalendar> calendars = getCalendars(context);
        if (calendars == null || calendars.size() == 0) return false;
        String calendar = getDefaultCalendar(context);
        int idx = 0;
        CharSequence[] entries = new CharSequence[calendars.size()+1];
        CharSequence[] entryvalues = new CharSequence[calendars.size()+1];
        entryvalues[0] = "";
        entries[0] = "None - do not sync events";
        for (int i = 1; i < calendars.size()+1; i++) {
            InCalendar cal =calendars.get(i-1);
            entryvalues[i] = cal.getId();
            entries[i] = cal.getName();
            if (calendar!=null && calendar.equals(cal.getId())) idx = i;
        }
        calendarPref.setEntries(entries);
        calendarPref.setEntryValues(entryvalues);
        calendarPref.setValueIndex(idx);
        return true;
    }
    private static class EvCal{
        String mId, mEventId;
        public EvCal(String id, String eventId) {
            mId = id;
            mEventId=eventId;
        }
    }
    public static ArrayList<String> syncEvents(ArrayMap<String, InEvent> events) {
        ArrayList<String> removed = new ArrayList<>();
        ArrayList<EvCal> toremove = new ArrayList<>();
        if (events == null) return removed;
        ContentResolver contentResolver = InApp.get().getContentResolver();
        Cursor cursor = contentResolver.query(Events.CONTENT_URI, calendarColumns(),
                getUrlcondition(false),
                new String[]{hasurl()? InternationsBot.BASEURL+"%":"%"+InternationsBot.BASEURL+"%"}, null);
        if(cursor==null)return removed;
        while (cursor.moveToNext()) {
            String eventId;
            if(hasurl()){
                final int APPURL = cursor.getColumnIndex(CUSTOMURL);
                String url = cursor.getString(APPURL);
                eventId = InEvent.idFromurl(url);
            }else{
                final int DESCCOL = cursor.getColumnIndex(Events.DESCRIPTION);
                String url = cursor.getString(DESCCOL);
                eventId = InEvent.idFromurl(url);
            }
            if (eventId != null) {
                InEvent old = events.get(eventId);
                if (old == null || !old.imGoing())
                    toremove.add(new EvCal(cursor.getString(cursor.getColumnIndex(Events._ID)),eventId));//event needs to be deleted
            }
        }
        for (EvCal evCal : toremove) {
            if (contentResolver.delete(Events.CONTENT_URI, Events._ID + " = ?", new String[]{evCal.mId}) > 0) {
                removed.add(evCal.mEventId);
            }
        }
        for (InEvent event : events.values()) {
            if (event.imGoing())
                InCalendar.modifyEvent(InApp.get(), event);
        }
        return removed;
    }
}
