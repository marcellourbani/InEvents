package com.marcellourbani.internationsevents;

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
    List<InEvent> mEvents = new ArrayList<InEvent>();

    public InternationsBot(String user, String pass) {
        mClient = new httpClient();
        mUser = user;
        mPass = pass;
    }

    public void readMyEvents() {
        try {
            sign();
            String ev = mClient.geturl_string("http://www.internations.org/events/my?ref=he_ev_me");
            Document doc = Jsoup.parse(ev);
            Elements elements = doc.select("#my_upcoming_events_table tbody tr");
            for (Element e : elements) {
                mEvents.add(new InEvent(e));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sign() {
        if (!mSigned)
            try {
                List<NameValuePair> parms = new ArrayList<NameValuePair>();
                parms.add(new BasicNameValuePair("user_email",mUser));
                parms.add(new BasicNameValuePair("user_password",mPass));
                mClient.posturl_string("https://www.internations.org/users/signin", parms);
                mSigned = true;
            } catch (Throwable e) {
                e.printStackTrace();
            }
    }
}
