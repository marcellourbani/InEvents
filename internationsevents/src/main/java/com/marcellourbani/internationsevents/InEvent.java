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
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InEvent {
    private static final Pattern mActPattern = Pattern.compile("activity-group/([0-9]+)/activity/([0-9]+)");
    private static final Pattern mEventPattern = Pattern.compile("/events/.*[^0-9]([0-9]+)$");
    String mGroupId = null;
    String mEventId;
    String mIconUrl, mTitle, mLocation, mEventUrl, mGroup;
    boolean mSubscribed, mMine, mSaved;
    GregorianCalendar mStart, mStop;

    private String getAttr(Elements els, int idx, String name) {
        try {
            String s = els.get(idx).attr(name);
            return s != null ? s : "";
        } catch (Throwable t) {
            return "";
        }
    }

    public String getRsvpUrl(boolean attend) {
        if (mGroupId == null) {
            return "http://www.internations.org/events/" +
                    (attend ? "signin/" : "signout/") + mEventId;
        } else {
            return "http://www.internations.org/activity-group/" + mGroupId + "/activity/" + mEventId +
                    "/attendance/";//+(attend?"accept":"decline");
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
            Matcher mat = mActPattern.matcher(mEventUrl);
            mat.find();
            mEventId = mat.group(2);
            String ts = e.select("p.date").get(0).text();
            DateFormat df = new SimpleDateFormat("dd MMMM yyyy");
            mMine = false;
            mStart = new GregorianCalendar();
            String startdate = ts.substring(0, ts.indexOf("|"));
            mStart.setTime(df.parse(startdate));
        } catch (Exception ex) {
            Log.d(InternationsBot.INTAG, ex.getMessage());
        }
    }

    InEvent(Cursor c) {
        long time;
        mSaved = true;
        mGroup = c.getString(c.getColumnIndex("groupdesc"));
        mGroupId = c.getString(c.getColumnIndex("groupid"));
        mEventId = c.getString(c.getColumnIndex("timelimit"));
        mTitle = c.getString(c.getColumnIndex("title"));
        mIconUrl = c.getString(c.getColumnIndex("iconurl"));
        mLocation = c.getString(c.getColumnIndex("location"));
        mSubscribed = c.getInt(c.getColumnIndex("subscribed")) == 1;
        mEventUrl = c.getString(c.getColumnIndex("eventurl"));
        mMine = c.getInt(c.getColumnIndex("myevent")) == 1;
        time = c.getLong(c.getColumnIndex("starttime"));
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

    InEvent(Element e) {
        try {

            mIconUrl = getAttr(e.select("p.guide-photo img"), 0, "src");
            Elements tmp = e.select("div.guide-entry p");
            if (tmp != null && tmp.size() >= 2 && !tmp.get(1).hasClass("guide-name"))
                mGroup = tmp.get(1).text();
            tmp = e.select("h3.guide-name a");
            mTitle = tmp != null && tmp.size() > 0 ? tmp.get(0).text() : "";
            mEventUrl = getAbsoluteUrl(getAttr(tmp, 0, "href"));
            Matcher mat = mActPattern.matcher(mEventUrl);
            if (mat.find()) {
                mGroupId = mat.group(1);
                mEventId = mat.group(2);
            } else {
                mat = mEventPattern.matcher(mEventUrl);
                if (mat.find()) mEventId = mat.group(1);
            }
            mSubscribed = !(getAttr(e.select("span.already-guest img"), 0, "src").equals(""));
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
            DateFormat df = new SimpleDateFormat("MMM dd,yyyy kk:mm");
            mStart = new GregorianCalendar();
            mStart.getTime().getTime();
            mStart.setTime(df.parse(startd + " " + startt));
            mMine = true;
            if (endd != null) {
                mStop = new GregorianCalendar();
                mStop.setTime(df.parse(endd + " " + endt));
            }
        } catch (Throwable t) {
            Log.d(InternationsBot.INTAG, t.getMessage());
        }
    }

    void save() {
        SQLiteDatabase db = InApp.get().getDB().getWrdb();
        ContentValues values = new ContentValues();
        values.put("groupdesc", mGroup);
        values.put("groupid", mGroupId);
        values.put("timelimit", mEventId);
        values.put("title", mTitle);
        values.put("iconurl", mIconUrl);
        values.put("location", mLocation);
        values.put("subscribed", mSubscribed ? 1 : 0);
        values.put("starttime", mStart.getTimeInMillis());
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

    static ArrayMap<String, InEvent> loadEvents() {
        SQLiteDatabase db = InApp.get().getDB().getWrdb();
        ArrayMap<String, InEvent> events = new ArrayMap<String, InEvent>();
        Cursor c = db.rawQuery("select * from events;", null);
        while (c != null && c.moveToNext()) {
            InEvent event = new InEvent(c);
            events.put(event.mEventId, event);
        }
        return events;
    }

    public void reset_attendance() {
        mSubscribed = false;
    }
}
