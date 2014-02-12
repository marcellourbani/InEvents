package com.marcellourbani.internationsevents;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.prefs.Preferences;


/**
 * Created by Marcello on 11/02/14.
 */
public class InCalendar {
    final static String[] CALCOLUMNS = new String[]{Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME};
    final static String CALENDARS_WHERE = Calendars.CALENDAR_ACCESS_LEVEL + ">=" + Calendars.CAL_ACCESS_EDITOR;
    private String mId,mName;
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
    //TODO:this is a template
    public static void addEvent(Context context,InEvent event ){
        Calendar beginTime = Calendar.getInstance();
        ContentResolver cr = context.getContentResolver();
        ContentValues values = new ContentValues();
        TimeZone timeZone = TimeZone.getDefault();
        values.put(CalendarContract.Events.DTSTART, event.mStart.getTimeInMillis());
        values.put(CalendarContract.Events.DTEND,event.mStop.getTimeInMillis() );
        values.put(CalendarContract.Events.EVENT_TIMEZONE, timeZone.getID());
        values.put(CalendarContract.Events.TITLE, event.mTitle);
        values.put(CalendarContract.Events.DESCRIPTION, event.mGroup +" "+ event.mTitle);
        values.put(CalendarContract.Events.CALENDAR_ID, getDefaultCalendar(context));
        values.put(CalendarContract.Events.CUSTOM_APP_URI, event.mEventUrl);
        Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);

        String eventID = uri.getLastPathSegment();
    }
    public void modifyEvent(InEvent event){

    }
    public void deleteEvent(InEvent event){
//        Uri eventUri = Uri.parse("content://calendar/events");  // or "content://com.android.calendar/events"
//        Cursor cursor = contentResolver.query(eventUri, new String[]{"_id"}, "calendar_id = " + calendarId, null, null); // calendar_id can change in new versions
//        while(cursor.moveToNext()) {
//            Uri deleteUri = ContentUris.withAppendedId(eventUri, cursor.getInt(0));
//            contentResolver.delete(deleteUri, null, null);
//        }
    }
    private static String getDefaultCalendar(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context).getString("pr_calendar", "");
    }
    public static List<InCalendar> getCalendars(Context context) {
        ArrayList<InCalendar> calendars = null;
        ContentResolver cr = context.getContentResolver();
        Resources r = context.getResources();
        Cursor c = cr.query(CalendarContract.Calendars.CONTENT_URI, CALCOLUMNS,
                CALENDARS_WHERE, null, Calendars.DEFAULT_SORT_ORDER);
        try {
            String defaultSetting = getDefaultCalendar(context);
            if (c != null && c.getCount() > 0) {
                calendars = new ArrayList<InCalendar>();
                int calendarCount = c.getCount();
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

    public static void addCalendarsPreferences(Context context, ListPreference calendarPref) {
        List<InCalendar> calendars = getCalendars(context);
        String calendar = getDefaultCalendar(context);
        int idx=0;
        CharSequence[] entries = new CharSequence[calendars.size()];
        CharSequence[] entryvalues = new CharSequence[calendars.size()];
        for(int i = 0;i<calendars.size();i++){
            entryvalues[i]=calendars.get(i).getId();
            entries[i]=calendars.get(i).getName();
            if(calendar.equals(calendars.get(i).getId())) idx = i;
        }
        calendarPref.setEntries(entries);
        calendarPref.setEntryValues(entryvalues);
        calendarPref.setValueIndex(idx);
    }
}
