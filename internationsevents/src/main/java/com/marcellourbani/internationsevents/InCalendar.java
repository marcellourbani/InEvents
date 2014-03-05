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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.support.v4.util.ArrayMap;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;


public class InCalendar {
    private static final Uri CAL_URI = CalendarContract.Calendars.CONTENT_URI;
    private static final String[] CALCOLUMNS = new String[]{Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME};
    private static final String CALENDARS_WHERE = Calendars.CALENDAR_ACCESS_LEVEL + ">=" + Calendars.CAL_ACCESS_CONTRIBUTOR;
    private static String EVENTID = Events.CUSTOM_APP_URI;// CalendarContract.ExtendedProperties.EVENT_ID;
    private String mId, mName;

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
        values.put(EVENTID, event.mEventUrl);
        return values;
    }

    public static void addEvent(Context context, InEvent event) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = cr.insert(Events.CONTENT_URI, getEventValues(context, event));
        String eventID = uri.getLastPathSegment();
    }

    public static void modifyEvent(Context context, InEvent event) {
        String cal = getDefaultCalendar(context);
        if (cal == null || cal.length() == 0)
            return;
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(Events.CONTENT_URI, new String[]{Events._ID, Events.DESCRIPTION, EVENTID},
                Events.CALENDAR_ID + " = ? AND " + EVENTID + " = ?",
                new String[]{cal, event.mEventUrl}, null);
        boolean updated = false;
        while (cursor.moveToNext()) {
            final int APPURL = cursor.getColumnIndex(EVENTID);
            if (cursor.getString(APPURL) != null && cursor.getString(APPURL).equals(event.mEventUrl)) {
                //TODO:only update if needed...
                updated = contentResolver.update(Events.CONTENT_URI, getEventValues(context, event),
                        "(" + Events._ID + " = ?)", new String[]{cursor.getString(0)}) > 0;
                break;
            }
        }
        if (!updated) addEvent(context, event);
    }

    private static String getDefaultCalendar(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("pr_calendar", "");
    }

    public static List<InCalendar> getCalendars(Context context) {
        ArrayList<InCalendar> calendars = null;
        ContentResolver cr = context.getContentResolver();
        Resources r = context.getResources();
        Cursor c = cr.query(CAL_URI, CALCOLUMNS,
                CALENDARS_WHERE, null, Calendars.DEFAULT_SORT_ORDER);
        try {
            if (c != null && c.getCount() > 0) {
                calendars = new ArrayList<InCalendar>();
                while (c.moveToNext()) {
                    calendars.add(new InCalendar(c));
                }
                return calendars;
            }
        } finally {
            if (c != null) {
                c.close();
            }
            return calendars;
        }
    }

    public static boolean addCalendarsPreferences(Context context, ListPreference calendarPref) {
        List<InCalendar> calendars = getCalendars(context);
        if (calendars == null || calendars.size() == 0) return false;
        String calendar = getDefaultCalendar(context);
        int idx = 0;
        CharSequence[] entries = new CharSequence[calendars.size()];
        CharSequence[] entryvalues = new CharSequence[calendars.size()];
        for (int i = 0; i < calendars.size(); i++) {
            entryvalues[i] = calendars.get(i).getId();
            entries[i] = calendars.get(i).getName();
            if (calendar.equals(calendars.get(i).getId())) idx = i;
        }
        calendarPref.setEntries(entries);
        calendarPref.setEntryValues(entryvalues);
        calendarPref.setValueIndex(idx);
        return true;
    }

    public static ArrayList<String> syncEvents(ArrayMap<String, InEvent> events) {
        ArrayList<String> removed = new ArrayList<String>();
        ArrayList<String> toremove = new ArrayList<String>();
        if (events == null) return removed;
        ContentResolver contentResolver = InApp.get().getContentResolver();
        Cursor cursor = contentResolver.query(Events.CONTENT_URI, new String[]{Events._ID, Events.DESCRIPTION, EVENTID},
                EVENTID + " like ?",
                new String[]{InternationsBot.BASEURL + "%"}, null);
        while (cursor.moveToNext()) {
            final int APPURL = cursor.getColumnIndex(EVENTID);
            String url = cursor.getString(APPURL);
            String eventId = InEvent.idFromurl(url);
            if (eventId != null) {
                InEvent old = events.get(eventId);
                if (old == null || !old.imGoing())
                    toremove.add(url);//event needs to be deleted
            }
        }
        for (String url : toremove) {
            if (contentResolver.delete(Events.CONTENT_URI, EVENTID + " = ?", new String[]{url}) > 0) {
                String eventId = InEvent.idFromurl(url);
                if (eventId != null) removed.add(eventId);
            }
        }
        for (InEvent event : events.values()) {
            if (event.imGoing())
                InCalendar.modifyEvent(InApp.get(), event);
        }
        return removed;
    }
}
