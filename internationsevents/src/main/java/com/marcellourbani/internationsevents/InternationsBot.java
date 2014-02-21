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


import android.content.SharedPreferences;
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
import java.util.List;

public class InternationsBot {
    public static final String BASEURL = "http://www.internations.org";
    private static final String MYEVENTSURL = "http://www.internations.org/events/my?ref=he_ev_me",
            SIGNUPURL = "https://www.internations.org/users/signin";
    private static final String INTAG = "IN_EVENTS";
    private final String mUser;
    private final String mPass;
    httpClient mClient;
    boolean mSigned = false;
    ArrayList<InEvent> mEvents = new ArrayList<InEvent>();
    ArrayMap<String,InGroup> mGroups = new ArrayMap<String,InGroup>();
    private class Grouppage{
        ArrayMap<String,InGroup> mGroups = new ArrayMap<String,InGroup>();
        ArrayList<String> mPages = new ArrayList<String>();
    }
    public boolean passIsSet() {
        return mPass != null && mPass.length() > 0;
    }

    public InternationsBot(SharedPreferences sharedPref) {
        mClient = new httpClient();
        mUser = sharedPref.getString("pr_email", "");
        mPass = sharedPref.getString("pr_password", "");
    }

    //TODO unsubscribe
    public boolean rsvp(InEvent event, boolean going) {
        try {
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
                String method = going ? "PATCH" : "DELETE";
                parms.add(new BasicNameValuePair("_method", "PATCH"));
                parms.add(new BasicNameValuePair("common_base_form[_token]", InApp.get().getInToken()));
                parms.add(new BasicNameValuePair("redirectRoute", "_activity_group_activity_get"));
                parms.add(new BasicNameValuePair("redirectRouteParameters[activityGroupId]", event.mGroup));
                parms.add(new BasicNameValuePair("redirectRouteParameters[activityId]", event.mEventId));
                result = mClient.posturl_string(url, parms);
                if (going) {
                    return result.matches("You are now attending this.*flash__close-button js-no-modal")
                                         ||result.matches("You're on the guest list");
                } else {
                    return result.matches("Your attendance to the Activity has been cancelled.*flash__close-button js-no-modal");
                }
            }
        } catch (Throwable t) {
            return false;
        }
    }
    private Grouppage dlMyGroupsPage(String url, boolean getPageList){
        Grouppage page = new Grouppage();
        try {
            String text = mClient.geturl_string(url);
            Document doc = Jsoup.parse(text);
            Elements elements = doc.select("table#search-results tbody tr");
            for (Element e : elements) {
                InGroup group = new InGroup(e);
                page.mGroups.put(group.mId,group);
            }
            if (getPageList){
                elements = doc.select("SPAN.pages a");
                for (Element e : elements) {
                    URL u= new URL( new URL(BASEURL), e.attr("href"));
                    page.mPages.add(u.toString());
                }
            }
        } catch (Exception e) {
            Log.d(INTAG, e.getMessage());
        }
        return page;
    }
    public ArrayMap<String, InGroup> dlMyGroups(){
        Grouppage page1 = dlMyGroupsPage("http://www.internations.org/activity-group/search/?activity_group_search[userActivityGroups]=1", true);
        for(InGroup g:page1.mGroups.values())mGroups.put(g.mId,g);
        for(String url:page1.mPages){
            Grouppage page = dlMyGroupsPage(url, false);
            for(InGroup g:page.mGroups.values())mGroups.put(g.mId,g);
        }
        return  mGroups;
    }
    public ArrayMap<String, InGroup> loadMyGroups(){
        mGroups = InGroup.loadGroups();
        //TODO:also load when too old, remove unsubscribed groups
        if (mGroups.isEmpty()){
            dlMyGroups();
            for(InGroup g:mGroups.values())g.save();
        }
        return mGroups;
    }
    public void readMyEvents() {
        try {
            String ev = mClient.geturl_string(MYEVENTSURL);
            Document doc = Jsoup.parse(ev);
            Elements elements = doc.select("#my_upcoming_events_table tbody tr");
            mEvents.clear();
            for (Element e : elements) {
                InEvent event = new InEvent(e);
                mEvents.add(event);
            }
//            elements = doc.select("#my_contacts_events_table tbody tr");
//            for (Element e : elements) {
//                InEvent event = new InEvent(e);
//                mEvents.add(event);
//            }
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
        return mEvents;
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
