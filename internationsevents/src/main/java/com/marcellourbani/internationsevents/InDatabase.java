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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class InDatabase {
    private static InDatabase _inst;
    private final Helper helper;


    public InDatabase(Context cont) {
        helper = new Helper(cont);
        _inst = this;
    }
    static public InDatabase get(Context context){
        if(_inst==null) new InDatabase(context);
        return _inst;
    }
    public SQLiteDatabase getWrdb() {
        return _inst.helper.getWritableDatabase();
    }

    public SQLiteDatabase getRodb() {
        return _inst.helper.getReadableDatabase();
    }

    private class Helper extends SQLiteOpenHelper{
        static final String dbname = "internations_db";
        static final int version = 1;

        public Helper(Context context) {
            super(context, dbname, null, version);
        }

        @Override
        public void onCreate(SQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS groups ("
                    + "id           text,   " + "description text,"
                    + "members      integer," + "activities   integer,"
                    + "primary key (id));");
            database.execSQL("CREATE TABLE IF NOT EXISTS events ("
                    + "id           text   ," + "groupid      text,"
                    + "timelimit    integer," + "title        text   ,"
                    + "iconurl      text   ," + "location     text   ,"
                    + "subscribed   integer," + "eventurl     text   ,"
                    + "starttime    integer," + "endtime      integer,"
                    + "myevent      integer," + "groupdesc    text   ,"
                    + "primary key (id));");
            database.execSQL("CREATE TABLE IF NOT EXISTS refreshes ("
                    + "id           integer," + "lastrun      integer,"
                    + "primary key (id));");
            database.execSQL("CREATE INDEX IF NOT EXISTS eventtimel ON events "
                    + "(timelimit)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase database, int i, int i2) {

        }
    }
}
