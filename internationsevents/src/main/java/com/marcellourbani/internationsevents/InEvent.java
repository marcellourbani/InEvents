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

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InEvent {
    static String mToken="";
    private Pattern mActPattern;
    private Pattern mEventPattern;
    private String mGroupId=null;
    String mEventId;
    String mIconUrl, mTitle, mLocation, mEventUrl,mGroup;
    boolean mSubscribed;
    GregorianCalendar mStart, mStop;

    private String getAttr(Elements els, int idx, String name) {
        try {
            String s = els.get(idx).attr(name);
            return s != null ? s : "";
        } catch (Throwable t) {
            return "";
        }
    }
    public String getRsvpUrl(boolean attend){
        if(mGroupId==null){
          return "http://www.internations.org/events/"+
                  ( attend?"signin/":"signout/")+mEventId;
        }else {
            return "http://www.internations.org/activity-group/"+mGroupId+"/activity/"+mEventId+
                    "/attendance/"+(attend?"accept":"decline");
        }
    }
    public boolean isEvent(){
        return mGroupId==null;
    }
    InEvent(Element e) {
        try {
            final URL INURL = new URL(InternationsBot.BASEURL);
            mIconUrl = getAttr(e.select("p.guide-photo img"), 0, "src");
            Elements tmp = e.select("div.guide-entry p");
            if(tmp!=null&&tmp.size()>=2&&!tmp.get(1).hasClass("guide-name"))
                mGroup = tmp.get(1).text();
            tmp = e.select("h3.guide-name a");
            mTitle = tmp!=null&&tmp.size()>0? tmp.get(0).text():"";
            mEventUrl =( new URL( INURL , getAttr(tmp, 0, "href"))).toString();
            mActPattern = Pattern.compile("activity-group/([0-9]+)/activity/([0-9]+)");
            mEventPattern = Pattern.compile("/events/.*[^0-9]([0-9]+)$");
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
            if(mLocation.length()>4 && mLocation.substring(0,3).equals("At ")) mLocation = mLocation.substring(3);
            tmp = e.select("td.col_attend input#common_base_form__token");
            String token=null;
            if(tmp!=null&&tmp.size()>0) token = tmp.get(0).attr("value");
            if(token!=null&& token.length()>0)mToken = token;
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
            mStart.setTime(df.parse(startd + " " + startt));
            if (endd != null) {
                mStop = new GregorianCalendar();
                mStop.setTime(df.parse(endd + " " + endt));
            }
        } catch (Throwable t) {
            String mError = t.toString();
        }
    }
}
