package com.marcellourbani.internationsevents;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InternationsBot {
    public static final String BASEURL ="http://www.internations.org";
    private static final String MYEVENTSURL="http://www.internations.org/events/my?ref=he_ev_me",
                                SIGNUPURL = "https://www.internations.org/users/signin";
    private final String mUser;
    private final String mPass;
    httpClient mClient;
    boolean mSigned = false;
    ArrayList<InEvent> mEvents = new ArrayList<InEvent>();
    public boolean passIsSet(){
        return mPass!=null&&mPass.length()>0;
    }
    public InternationsBot(SharedPreferences sharedPref) {
        mClient = new httpClient();
        mUser = sharedPref.getString("pr_email", "");
        mPass = sharedPref.getString("pr_password", "");
    }
    //TODO unsubscribe
    public boolean rsvp(InEvent event,boolean going){
        try {
            String url = event.getRsvpUrl(going);
            if(going)
               url = mClient.geturl_string(url);
            else{
//                _method	DELETE
//                common_base_form[_token]	693022c0153c1f23757f834d5b8ad89dd99a7257
//                redirectRoute	_activity_group_activity_get
//                redirectRouteParameters[a...	470
//                redirectRouteParameters[a...	62901
                ArrayList<NameValuePair> parms = new ArrayList<NameValuePair>();
                parms.add(new BasicNameValuePair("_method","DELETE"));
                url = mClient.posturl_string(url, parms);
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
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
        } catch (IOException e) {
            e.printStackTrace();
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
        Bundle b=new Bundle();
        List<Cookie> cookies;
        if(( cookies = mClient==null?null:mClient.getCookies())!=null){
          for(Cookie cookie:cookies)
              b.putString(cookie.getName(), cookie.getValue() + "" + "; domain=" + cookie.getDomain());
        }
        return b;
    }
}
