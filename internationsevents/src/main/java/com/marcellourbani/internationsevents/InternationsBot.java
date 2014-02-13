package com.marcellourbani.internationsevents;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InternationsBot {
    private final String mUser;
    private final String mPass;
    httpClient mClient;
    boolean mSigned = false;
    ArrayList<InEvent> mEvents = new ArrayList<InEvent>();

    public InternationsBot(SharedPreferences sharedPref) {
        mClient = new httpClient();
        mUser = sharedPref.getString("pr_email", "");
        mPass = sharedPref.getString("pr_password","");
    }

    public void readMyEvents() {
        try {
            String ev = mClient.geturl_string("http://www.internations.org/events/my?ref=he_ev_me");
            Document doc = Jsoup.parse(ev);
            Elements elements = doc.select("#my_upcoming_events_table tbody tr");
            for (Element e : elements) {
                InEvent event = new InEvent(e);
                mEvents.add(event);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean sign() {
        if (!mSigned && mPass!=null&&mPass.length()>0)
            try {
                List<NameValuePair> parms = new ArrayList<NameValuePair>();
                parms.add(new BasicNameValuePair("user_email",mUser));
                parms.add(new BasicNameValuePair("user_password",mPass));
                mSigned= mClient.posturl_string("https://www.internations.org/users/signin", parms)
                        .indexOf("Incorrect email or password")<=0;
            } catch (Throwable e) {
                mSigned = false;
            }
        else
            mSigned = false;
        return mSigned;
    }

    public ArrayList<InEvent> getEvents() {
        return mEvents;
    }
}
