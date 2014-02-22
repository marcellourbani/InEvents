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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.util.ArrayMap;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.cookie.Cookie;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InternationsBot {
    public static final String BASEURL = "http://www.internations.org";
    private static final String MYEVENTSURL = "http://www.internations.org/events/my?ref=he_ev_me",
            SIGNUPURL = "https://www.internations.org/users/signin";
    static final String INTAG = "IN_EVENTS";
    private String mUser;
    private String mPass;
    private final SharedPreferences mPref;
    httpClient mClient;
    boolean mSigned = false;
    ArrayMap<String, InEvent> mEvents = new ArrayMap<String, InEvent>();
    ArrayMap<String, InGroup> mGroups = new ArrayMap<String, InGroup>();

    public enum Refreshkeys {
        GROUPS, EVENTS, MYEVENTS;

        public long getLimit() {
            return 24 * 3600000;
        }

        public Integer getKey() {
            switch (this) {
                case GROUPS:
                    return 1;
                case EVENTS:
                    return 2;
                case MYEVENTS:
                    return 3;
            }
            return 0;
        }
    }

    private class Grouppage {
        ArrayMap<String, InGroup> mGroups = new ArrayMap<String, InGroup>();
        ArrayList<String> mPages = new ArrayList<String>();
    }

    public boolean passIsSet() {
        mUser = mPref.getString("pr_email", "");
        mPass = mPref.getString("pr_password", "");
        return mPass != null && mPass.length() > 0;
    }

    public InternationsBot(SharedPreferences sharedPref) {
        mClient = new httpClient();
        mPref = sharedPref;
        mUser = sharedPref.getString("pr_email", "");
        mPass = sharedPref.getString("pr_password", "");
    }

    //TODO unsubscribe
    public boolean rsvp(InEvent event, boolean going) {
        try {
            if(!sign())return false;
            ArrayList<NameValuePair> parms = new ArrayList<NameValuePair>();
            String url = event.getRsvpUrl(going);
            String result;
            if (event.isEvent()) {
                result = mClient.geturl_string(url);
                if (going)
                    return result.matches("Your attendance to event.*has been saved.*flash__close-button js-no-modal");
                else
                    return result.matches("Your attendance to event.*has been cancelled.*flash__close-button js-no-modal");
            } else {
                String token = InApp.get().getInToken();
                if(token==null||token.length()==0){
                    String evText = mClient.geturl_string(event.mEventUrl);
                    Matcher mat =Pattern.compile("common_base_form__token[^>]*value=\"([^\"]*)").matcher(evText);
                   if(mat.find())token = mat.group(1);
                }
                String method = going ? "PATCH" : "DELETE";
                parms.add(new BasicNameValuePair("_method", method));
                parms.add(new BasicNameValuePair("common_base_form[_token]", token));
                parms.add(new BasicNameValuePair("redirectRoute", "_activity_group_activity_get"));
                parms.add(new BasicNameValuePair("redirectRouteParameters[activityGroupId]", event.mGroupId));
                parms.add(new BasicNameValuePair("redirectRouteParameters[activityId]", event.mEventId));
                result = mClient.posturl_string(url, parms);
                if (going) {
                    return result.matches("You are now attending this.*flash__close-button js-no-modal")
                            || result.matches("You.re on the guest list");
                } else {
                    return result.matches("Your attendance to the Activity has been cancelled.*flash__close-button js-no-modal");
                }
            }
        } catch (Throwable t) {
            return false;
        }
    }

    private Grouppage dlMyGroupsPage(String url, boolean getPageList) {
        Grouppage page = new Grouppage();
        try {
            String text = mClient.geturl_string(url);
            Document doc = Jsoup.parse(extractTable(text, "search-results"));
            Elements elements = doc.select("table#search-results tbody tr");
            for (Element e : elements) {
                InGroup group = new InGroup(e);
                page.mGroups.put(group.mId, group);
            }
            if (getPageList) {
                Matcher mat = Pattern.compile("(<span[^>]*pages.*/span>).*Next page link").matcher(text);
                if (mat.find()) {
                    doc = Jsoup.parse(mat.group(1));
                    elements = doc.select("SPAN.pages a");
                    for (Element e : elements) {
                        URL u = new URL(new URL(BASEURL), e.attr("href"));
                        page.mPages.add(u.toString());
                    }
                }
            }
        } catch (Exception e) {
            Log.d(INTAG, e.getMessage());
        }
        return page;
    }

    public ArrayMap<String, InGroup> readMyGroups() {
        Grouppage page1 = dlMyGroupsPage("http://www.internations.org/activity-group/search/?activity_group_search[userActivityGroups]=1", true);
        for (InGroup g : page1.mGroups.values()) mGroups.put(g.mId, g);
        for (String url : page1.mPages) {
            Grouppage page = dlMyGroupsPage(url, false);
            for (InGroup g : page.mGroups.values()) mGroups.put(g.mId, g);
        }
        return mGroups;
    }

    public ArrayMap<String, InGroup> loadMyGroups() {
        mGroups = InGroup.loadGroups();
        return mGroups;
    }

    public void saveEvents(boolean all) {
        for (InEvent e : mEvents.values()) if (all || e.mMine) e.save();
        writeRefresh(Refreshkeys.MYEVENTS);
        if (all) writeRefresh(Refreshkeys.EVENTS);
    }

    public void saveGroups() {
        ArrayMap<String, InGroup> oldgroups = InGroup.loadGroups();
        for (InGroup g : oldgroups.values()) if (mGroups.get(g.mId) == null) g.delete();
        for (InGroup g : mGroups.values()) g.save();
        writeRefresh(Refreshkeys.GROUPS);
    }

    private void writeRefresh(Refreshkeys refk) {
        SQLiteDatabase db = InApp.get().getDB().getWrdb();
        String[] key = new String[]{ refk.getKey().toString()};
        ContentValues values = new ContentValues();
        values.put("id", refk.getKey());
        values.put("lastrun", (new Date()).getTime());
        db.delete("refreshes", "id = ?", key);
        db.insert("refreshes", null, values);
    }

    public boolean isExpired(Refreshkeys refk) {
        SQLiteDatabase db = InApp.get().getDB().getRodb();
        String[] key = new String[]{refk.getKey().toString()};
        Cursor c = db.query(false, "refreshes", new String[]{"id", "lastrun"}, "id=?", key, null, null, null, null);
        if (c != null && c.moveToNext()) {
            long last = c.getLong(1);
            long now = (new Date()).getTime();
            long limit = refk.getLimit();
            return now - last > limit;
        } else return true;
    }

    public ArrayMap<String, InEvent> loadEvents() {
        mEvents = InEvent.loadEvents();
        return mEvents;
    }

    private String extractTable(String source, String id) {
        Matcher m = Pattern.compile("(<table[^>]*" + id + ".*/table>)").matcher(source);
        if (m.find()) {
            String s = m.group(1);
            return s.substring(0, s.indexOf("/table>") + 7);
        } else return null;
    }

    public void readMyEvents(boolean save) {
        try {
            ArrayMap<String, InEvent> events = new ArrayMap<String, InEvent>();
            String ev = extractTable(mClient.geturl_string(MYEVENTSURL), "my_upcoming_events_table");
            Document doc = Jsoup.parse(ev);
            Elements elements = doc.select("#my_upcoming_events_table tbody tr");
            mEvents.clear();
            for (Element e : elements) {
                InEvent event = new InEvent(e);
                events.put(event.mEventId, event);
                mEvents.put(event.mEventId, event);
            }
            //reset attendance3 if required
            for(InEvent e:mEvents.values()){
                if(e.mMine&&e.mSubscribed&&events.get(e.mEventId)==null){
                    e.reset_attendance();
                    events.put(e.mEventId,e);
                }
            }
            if(save)for(InEvent e:events.values())e.save();
        } catch (IOException e) {
            Log.d(INTAG, e.getMessage());
        }
    }

    public void readGroupsEvents() {
        try {
            for (InGroup group : mGroups.values()) {
                String url = BASEURL + "/activity-group/" + group.mId + "/activity/";
                String activities = mClient.geturl_string(url);
                Matcher m = Pattern.compile("(<ul[^>]*upcoming.*/ul>).*/.upcoming").matcher(activities);
                if (m.find()) {
                    String s = m.group(1);
                    Document doc = Jsoup.parse(m.group(1));
                    Elements elements = doc.select("li.activity");
                    for (Element e : elements) {
                        InEvent event = new InEvent(e, group);
                        InEvent old = mEvents.get(event.mEventId);
                        //the 'my events' view gives more details, do not overwrite if comes from there
                        //perhaps we should copy changed data, like event date and description
                        if (old == null || !old.mMine)
                            mEvents.put(event.mEventId, event);
                    }
                }
            }
        } catch (IOException e) {
            Log.d(INTAG, e.getMessage());
        }
    }

    //returns true if the web transaction was successful, sets mSigned based on the outcome
    public boolean sign() {
        if (!mSigned) {
            if (passIsSet())
                try {
                    List<NameValuePair> parms = new ArrayList<NameValuePair>();
                    parms.add(new BasicNameValuePair("user_email", mUser));
                    parms.add(new BasicNameValuePair("user_password", mPass));
                    mSigned = mClient.posturl_string(SIGNUPURL, parms)
                            .indexOf("Incorrect email or password") <= 0;
                } catch (Throwable e) {
                    mSigned = false;
                    return false;
                }
        }
        return true;
    }

    public ArrayList<InEvent> getEvents() {
        ArrayList<InEvent> events = new ArrayList<InEvent>();
        events.addAll(mEvents.values());
        Collections.sort(events,new Comparator<InEvent>() {
            @Override
            public int compare(InEvent e1, InEvent e2) {
                return e1.mStart.compareTo(e2.mStart);
            }
        });
        return events;
    }
    public Bundle getCookies() {
        Bundle b = new Bundle();
        List<Cookie> cookies;
        if ((cookies = mClient == null ? null : mClient.getCookies()) != null) {
            for (Cookie cookie : cookies)
                b.putString(cookie.getName(), cookie.getValue() + "" + "; domain=" + cookie.getDomain());
        }
        return b;
    }
}
