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

import org.json.JSONException;
import org.json.JSONObject;


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

    public InGroup(JSONObject go) throws JSONException {
        mId = Integer.toString(go.getInt("activityGroupId"));
        mDesc = go.getString("name");
        mMembers = go.getInt("memberCount");
        mSaved = true;
    }

    boolean isSaved(){
        if (!mSaved){
            SQLiteDatabase db = InApp.get().getDB().getRodb();
            Cursor c = db.rawQuery("select * from groups where id = ?;",new String[]{mId});
            mSaved   = c!=null&&c.getCount() > 0;
            if(c!=null)c.close();
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
        if(c!=null)c.close();
        return  groups;
    }

    public void delete() {
        SQLiteDatabase db = InApp.get().getDB().getWrdb();
        String[]key = new String[]{mId};
        db.delete("groups","id = ?",key);
    }
}
