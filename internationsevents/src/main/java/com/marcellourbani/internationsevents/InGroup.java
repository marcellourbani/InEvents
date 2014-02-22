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

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Date;

/**
 * Created by Marcello on 20/02/14.
 */
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
        ArrayMap<String, InGroup> groups = new ArrayMap<String, InGroup>();
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
