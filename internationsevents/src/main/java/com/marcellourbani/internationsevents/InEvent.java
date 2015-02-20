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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.util.ArrayMap;
import android.util.Log;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InEvent {
    private static final Pattern MACTPATTERN = Pattern.compile("activity-group/([0-9]+)/activity/([0-9]+)");
    private static final Pattern MEVENTPATTERN = Pattern.compile("/events/.*[^0-9]([0-9]+)$");
    private static final DateFormat MYEVENTDF = new SimpleDateFormat("MMM dd,yyyy kk:mm", Locale.US);
    private static final DateFormat MYEVENTDF_NOTIME = new SimpleDateFormat("MMM dd,yyyy", Locale.US);
    private static final DateFormat GROPUEVENTDF = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
    String mGroupId = null;
    String mEventId;
    String mIconUrl, mTitle, mLocation, mEventUrl, mGroup;

    private SubscStatus mRsvp = SubscStatus.NOTGOING;
    boolean mMine, mSaved, mAllDay;
    GregorianCalendar mStart, mStop;
    public boolean mNew;
    long mTimelimit = 0L;

    public static String idFromurl(String url) {
        if (url == null) return null;
        Matcher m = MACTPATTERN.matcher(url);
        if (m.find()) return m.group(2);
        m = MEVENTPATTERN.matcher(url);
        if (m.find()) return m.group(1);
        return null;
    }

    public boolean equals(InEvent event) {
        return mRsvp == event.mRsvp &&
                mMine == event.mMine &&
                mAllDay == event.mAllDay &&
                (mGroupId == null ? (event.mGroupId == null) : mGroupId.equals(event.mGroupId)) &&
                (mEventId == null ? (event.mEventId == null) : mEventId.equals(event.mEventId)) &&
                (mIconUrl == null ? (event.mIconUrl == null) : mIconUrl.equals(event.mIconUrl)) &&
                (mTitle == null ? (event.mTitle == null) : mTitle.equals(event.mTitle)) &&
                (mLocation == null ? (event.mLocation == null) : mLocation.equals(event.mLocation)) &&
                (mEventUrl == null ? (event.mEventUrl == null) : mEventUrl.equals(event.mEventUrl)) &&
                (mGroup == null ? (event.mGroup == null) : mGroup.equals(event.mGroup)) &&
                (mStart == null ? (event.mStart == null) : mStart.equals(event.mStart)) &&
                (mStop == null ? (event.mStop == null) : mStop.equals(event.mStop));
        //mNew and mTimelimit are ignored
    }

    public boolean merge(InEvent event) {
        if (mEventId == null || mEventId.equals("")) {
            mEventId = event.mEventId;
            mAllDay = event.mAllDay;
            mNew = event.mNew;
            mTimelimit = event.mTimelimit;
        } else if (!mEventId.equals(event.mEventId)) return false;
        if (mSaved && equals(event)) return true;
        mGroupId = event.mGroupId;
        mIconUrl = event.mIconUrl;
        mTitle = event.mTitle;
        mLocation = event.mLocation;
        mEventUrl = event.mEventUrl;
        mGroup = event.mGroup;
        mRsvp = event.mRsvp;
        mMine = event.mMine;
        mSaved = event.mSaved;
        mAllDay = event.mAllDay;
        mStart = event.mStart;
        mStop = event.mStop;
        mNew = event.mNew;
        mTimelimit = event.mTimelimit;
        return true;
    }

    private enum SubscStatus {
        INVITED, GOING, NOTGOING;

        public int toInt() {
            switch (this) {
                case GOING:
                    return 11;
                case INVITED:
                    return 12;
                case NOTGOING:
                    return 10;
                default:
                    return 10;
            }
        }

        public static SubscStatus FromDb(int i, boolean mine) {
            switch (i) {
                case 0:
                    return mine ? INVITED : NOTGOING;//old values in DB
                case 1:
                case 11:
                    return GOING;
                case 12:
                    return INVITED;
                default:
                    return NOTGOING;
            }
        }
    }

    private String getAttr(Elements els, int idx, String name) {
        try {
            String s = els.get(idx).attr(name);
            return s != null ? s : "";
        } catch (Throwable t) {
            return "";
        }
    }

    public String getRsvpUrl(boolean attend) {
        return getRsvpUrl(attend, beenInvited());
    }

    public String getRsvpUrl(boolean attend, boolean invited) {
        if (mGroupId == null) {
            return "http://www.internations.org/events/" +
                    (attend ? "signin/" : "signout/") + mEventId;
        } else {
            String url = "http://www.internations.org/activity-group/" + mGroupId + "/activity/" + mEventId +
                    "/attendance/";
            if (invited) url = url + (attend ? "accept" : "decline");
            return url;
        }
    }

    public boolean isEvent() {
        return mGroupId == null;
    }

    public InEvent(Element e, InGroup group) {
        try {
            mGroupId = group.mId;
            mGroup = group.mDesc;
            mIconUrl = getAttr(e.select("div.image img"), 0, "src");
            Elements tmp = e.select("div.info a");
            mEventUrl = getAbsoluteUrl(getAttr(tmp, 0, "href"));
            mTitle = tmp.get(0).text();
            Matcher mat = MACTPATTERN.matcher(mEventUrl);
            if (mat.find())
                mEventId = mat.group(2);
            String ts = e.select("p.date").get(0).text();
            mMine = false;
            mStart = new GregorianCalendar();
            String startdate = ts.substring(0, ts.indexOf("|"));
            mStart.setTime(GROPUEVENTDF.parse(startdate));
        } catch (Exception ex) {
            Log.d(InternationsBot.INTAG, ex.getMessage());
        }
    }

    InEvent(Cursor c) {
        long time;
        mSaved = true;
        mEventId = c.getString(c.getColumnIndex("id"));
        mGroup = c.getString(c.getColumnIndex("groupdesc"));
        mGroupId = c.getString(c.getColumnIndex("groupid"));
        mTimelimit = c.getLong(c.getColumnIndex("timelimit"));
        mTitle = c.getString(c.getColumnIndex("title"));
        mIconUrl = c.getString(c.getColumnIndex("iconurl"));
        mLocation = c.getString(c.getColumnIndex("location"));
        mMine = c.getInt(c.getColumnIndex("myevent")) == 1;
        mRsvp = SubscStatus.FromDb(c.getInt(c.getColumnIndex("subscribed")), mMine);
        mEventUrl = c.getString(c.getColumnIndex("eventurl"));
        time = c.getLong(c.getColumnIndex("starttime"));
        mAllDay = time % 1000 == 1;
        time = time - time % 1000;
        mStart = new GregorianCalendar();
        mStart.setTimeInMillis(time);
        time = c.getLong(c.getColumnIndex("endtime"));
        if (time > 0) {
            mStop = new GregorianCalendar();
            mStop.setTimeInMillis(time);
        }
    }

    public String getAbsoluteUrl(String url) throws MalformedURLException {
        final URL INURL = new URL(InternationsBot.BASEURL);
        return (new URL(INURL, url)).toString();
    }

    InEvent(Element e, boolean newformat) throws MalformedURLException, ParseException {
        if (newformat) {
            mIconUrl = getAttr(e.select("img.teaserRow__image"), 0, "src");
            mTitle = getAttr(e.select("a.teaserRow__titleLink"), 0, "title");
            mEventUrl = getAbsoluteUrl(getAttr(e.select("a.teaserRow__titleLink"), 0, "href"));
            Matcher mat = MACTPATTERN.matcher(mEventUrl);
            if (mat.find()) {
                mGroupId = mat.group(1);
                mEventId = mat.group(2);
            } else {
                mat = MEVENTPATTERN.matcher(mEventUrl);
                if (mat.find()) mEventId = mat.group(1);
            }
            Elements tmp = e.select("a.t-calendar-entry-activity-group");
            if (tmp != null && tmp.size() > 0) {
                mGroup = tmp.get(0).text();
            }
            tmp = e.select("span.t-attending-activity-message");
            mRsvp = tmp != null && tmp.size() > 0 ? SubscStatus.GOING : SubscStatus.NOTGOING;
            mLocation = "";
            tmp = e.select("span.teaserRow__date");
            String startd = tmp.get(0).text();
            tmp = e.select("span.teaserRow__time");
            String startt = tmp.get(0).text();
            mAllDay = startt.equals("");
            mStart = new GregorianCalendar();
            mStart.getTime().getTime();
            mStart.setTime(mAllDay ? MYEVENTDF_NOTIME.parse(startd) : MYEVENTDF.parse(startd + " " + startt));
            mMine = true;
        } else {
            mIconUrl = getAttr(e.select("p.guide-photo img"), 0, "src");
            Elements tmp = e.select("div.guide-entry p");
            if (tmp != null && tmp.size() >= 2 && !tmp.get(1).hasClass("guide-name"))
                mGroup = tmp.get(1).text();
            tmp = e.select("h3.guide-name a");
            mTitle = tmp != null && tmp.size() > 0 ? tmp.get(0).text() : "";
            mEventUrl = getAbsoluteUrl(getAttr(tmp, 0, "href"));
            Matcher mat = MACTPATTERN.matcher(mEventUrl);
            if (mat.find()) {
                mGroupId = mat.group(1);
                mEventId = mat.group(2);
            } else {
                mat = MEVENTPATTERN.matcher(mEventUrl);
                if (mat.find()) mEventId = mat.group(1);
            }
            mRsvp = (getAttr(e.select("span.already-guest img"), 0, "src").equals("")) ? SubscStatus.INVITED : SubscStatus.GOING;
            tmp = e.select("td.col_city");
            mLocation = tmp.text();
            if (mLocation.length() > 4 && mLocation.substring(0, 3).equals("At "))
                mLocation = mLocation.substring(3);
            tmp = e.select("td.col_attend input#common_base_form__token");
            String token = null;
            if (tmp != null && tmp.size() > 0) token = tmp.get(0).attr("value");
            if (token != null && token.length() > 0)
                InApp.get().setInToken(token);
            tmp = e.select("td.col_datetime p.date");
            String startd = tmp.get(0).text();
            String endd = tmp.size() > 1 ? tmp.get(1).text() : startd;
            endd = endd.equals("") ? startd : null;
            tmp = e.select("td.col_datetime p.time");
            String startt = tmp.get(0).text();
            String endt = tmp.size() > 1 ? tmp.get(1).text() : null;
            if (endt != null && endd == null) endd = startd;
            DateFormat df = new SimpleDateFormat("MMM dd,yyyy kk:mm", Locale.US);
            //DateFormat df = new SimpleDateFormat("MMM dd,yyyy kk:mm");
            DateFormat dfdo = new SimpleDateFormat("MMM dd,yyyy", Locale.US);
            mStart = new GregorianCalendar();
            mStart.getTime().getTime();
            mStart.setTime(startt.equals("")?dfdo.parse(startd): df.parse(startd + " " + startt));
            mMine = true;
            if (endd != null) {
                mStop = new GregorianCalendar();
                mStop.setTime(endt!=null&&endt.equals("")?dfdo.parse(endd): df.parse(endd + " " + endt));
            }
        }
    }

    void save() {
        SQLiteDatabase db = InApp.get().getDB().getWrdb();
        ContentValues values = new ContentValues();
        long time = mStart.getTimeInMillis();
        time = time - time % 1000 + (mAllDay ? 1L : 0L);
        values.put("groupdesc", mGroup);
        values.put("groupid", mGroupId);
        values.put("timelimit", mTimelimit);
        values.put("title", mTitle);
        values.put("iconurl", mIconUrl);
        values.put("location", mLocation);
        values.put("subscribed", mRsvp.toInt());
        values.put("starttime", time);
        values.put("endtime", mStop == null ? 0 : mStop.getTimeInMillis());
        values.put("eventurl", mEventUrl);
        values.put("myevent", mMine ? 1 : 0);
        if (isSaved()) {
            db.update("events", values, "id=?", new String[]{mEventId});
        } else {
            values.put("id", mEventId);
            if (db.insert("events", null, values) > 0) mSaved = true;
        }
    }

    private boolean isSaved() {
        if (!mSaved) {
            SQLiteDatabase db = InApp.get().getDB().getRodb();
            Cursor c = db.rawQuery("select * from events where id = ?;", new String[]{mEventId});
            mSaved = c != null && c.getCount() > 0;
        }
        return mSaved;
    }

    private static Long getMinTime() {
        Calendar c = Calendar.getInstance();
        if (c.get(Calendar.HOUR_OF_DAY) < 10)
            c.add(Calendar.HOUR_OF_DAY, -10);//10 hours ago
        else {
            c.add(Calendar.DATE, -1);
            c.set(Calendar.HOUR_OF_DAY, 23);
            c.set(Calendar.MINUTE, 59);
            c.set(Calendar.MINUTE, 59);
        }
        return (c).getTime().getTime();
    }

    static ArrayMap<String, InEvent> loadEvents() {
        SQLiteDatabase db = InApp.get().getDB().getWrdb();
        ArrayMap<String, InEvent> events;
        events = new ArrayMap<>();
        Cursor c = db.query(false, "events", new String[]{"*"}, "starttime >= ?", new String[]{getMinTime().toString()}, null, null, null, null);
        while (c != null && c.moveToNext()) {
            InEvent event = new InEvent(c);
            events.put(event.mEventId, event);
        }
        return events;
    }

    public static void clearold() {
        SQLiteDatabase db = InApp.get().getDB().getWrdb();
        db.delete("events", "starttime < ?", new String[]{getMinTime().toString()});
    }

    public void set_attendance(boolean going) {
        mRsvp = going ? SubscStatus.GOING : SubscStatus.NOTGOING;
    }

    public boolean imGoing() {
        return mRsvp == SubscStatus.GOING;
    }

    public boolean beenInvited() {
        return mRsvp == SubscStatus.INVITED;
    }

    public boolean addedrecently() {
        return (new Date()).getTime() - mTimelimit < 24 * 3600000;
    }
}
