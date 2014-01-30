package com.marcellourbani.internationsevents;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

public class InEvent {
    String mIconUrl, mTitle, mLocation, mEventUrl;
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

    InEvent(Element e) {
        try {
            mIconUrl = getAttr(e.select("p.guide-photo img"), 0, "src");
            Elements tmp = e.select("h3.guide-name a");
            mTitle = getAttr(tmp, 0, "title");
            mEventUrl = getAttr(tmp, 0, "href");
            mSubscribed = !(getAttr(e.select("span.already-guest img"), 0, "src").equals(""));
            tmp = e.select("td.col_city");
            mLocation = tmp.text();
            tmp = e.select("td.col_datetime p.date");
            String startd = tmp.get(0).text();
            String endd = tmp.size() > 1 ? tmp.get(1).text() : startd;
            endd = endd.equals("") ? startd : null;
            tmp = e.select("td.col_datetime p.time");
            String startt = tmp.get(0).text();
            String endt = tmp.size() > 1 ? tmp.get(1).text() : null;
            if (endt!=null&&endd == null) endd = startd;
            DateFormat df = new SimpleDateFormat("MMM dd,yyyy kk:mm");
            mStart = new GregorianCalendar();
            mStart.setTime(df.parse(startd + " " + startt));
            if(endd!=null){
                mStop = new GregorianCalendar();
                mStop.setTime(df.parse(endd + " " + endt));
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
