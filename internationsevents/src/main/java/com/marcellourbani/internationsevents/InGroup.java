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
import android.os.PatternMatcher;
import android.support.v4.util.ArrayMap;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class InGroup {
    String mId,mDesc;
    int mMembers,mActivities;
    private boolean mSaved;
    InGroup(Cursor c){
        mId = c.getString(c.getColumnIndex("id"));
        mDesc = c.getString(c.getColumnIndex("description"));
        mMembers =c.getInt(c.getColumnIndex("members"));
        mActivities =c.getInt(c.getColumnIndex("activities"));
        mSaved = true;
    }
    InGroup(Element e){
        mDesc        = e.text();
        String   url = e.attr("href");
        String[] x;
        ///activity-group/329?ref=pr_gr
        final Pattern p = Pattern.compile("/([0-9]+)");
        Matcher m = p.matcher(url);
        if(m.find()) mId = m.group(1);
//        //mId          = tmp.get(0).attr("href").split("/([0-9]+)")[1];
//        mMembers     = Integer.parseInt( e.select("TD.results-members").get(0).text());
//        mActivities  = Integer.parseInt( e.select("TD.results-activities").get(0).text());
    }
    boolean isSaved(){
        if (!mSaved){
           SQLiteDatabase db = InApp.get().getDB().getRodb();
           Cursor c = db.rawQuery("select * from groups where id = ?;",new String[]{mId});
           mSaved   = c!=null&&c.getCount() > 0;
        }
        return mSaved;
    }
    void save(){
        SQLiteDatabase db = InApp.get().getDB().getWrdb();
        ContentValues values = new ContentValues();
        values.put("description",mDesc);
        values.put("members",mMembers);
        values.put("activities",mActivities);
        if(isSaved()){
            db.update("groups", values,"id=?", new String[]{mId});
        }else{
            values.put("id",mId);
            if(db.insert("groups", null, values)>0) mSaved = true;
        }
    }
    static ArrayMap<String, InGroup> loadGroups(){
        SQLiteDatabase db = InApp.get().getDB().getWrdb();
        ArrayMap<String, InGroup> groups = new ArrayMap<>();
        Cursor c = db.rawQuery("select * from groups;",null);
        while(c!=null&&c.moveToNext()){
            InGroup g = new InGroup(c);
            groups.put(g.mId,g);
        }
        return  groups;
    }

    public void delete() {
        SQLiteDatabase db = InApp.get().getDB().getWrdb();
        String[]key = new String[]{mId};
        db.delete("groups","id = ?",key);
    }
}
