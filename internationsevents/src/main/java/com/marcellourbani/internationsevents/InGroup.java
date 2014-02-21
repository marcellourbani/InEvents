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

/**
 * Created by Marcello on 20/02/14.
 */
public class InGroup {
    String mId,mDesc;
    int mMembers,mActivities;
    InGroup(Element e){
        Elements tmp = e.select("TD.results-group.cf DIV.group-info a");
        mDesc        = tmp.get(0).text();
        String   url = tmp.get(0).attr("href");
        String[] x   = url.split(".*/([0-9]+)");
        x   = url.split("/");
        mId = x[x.length-1];
        //mId          = tmp.get(0).attr("href").split("/([0-9]+)")[1];
        mMembers     = Integer.parseInt( e.select("TD.results-members").get(0).text());
        mActivities  = Integer.parseInt( e.select("TD.results-activities").get(0).text());
    }
}
