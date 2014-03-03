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

import android.widget.Toast;

import java.util.ArrayList;

public class InError {
    private static InError _instance = null;
    private ArrayList<ErrorItem> errors = new ArrayList<ErrorItem>();
    private ErrorItem maxErr=null;

    public static boolean isOk() {
        return get().getSeverity().ordinal()<ErrSeverity.ERRROR.ordinal();
    }

    public boolean hasType(ErrType type) {
        for(ErrorItem i:errors)if(i.type==type)return true;
        return false;
    }

    public enum ErrType{NETWORK,PARSE,FORMPROC,DATABASE,LOGIN,UNKNOWN};
    public enum ErrSeverity{NONE,INFO,WARNING,ERRROR};

    public class ErrorItem{
        String text;
        ErrType type;
        ErrSeverity severity;
        ErrorItem(ErrSeverity severity, ErrType type,String text){
            this.severity = severity;
            this.type = type;
            this.text = text;
        }
    }
    static InError get(){
        if (_instance == null) _instance = new InError();
        return _instance;
    }
    void add(ErrType type,String text){
        add(ErrSeverity.ERRROR,type,text);
    }
    void add(ErrSeverity severity,ErrType type,String text){
        ErrorItem err = new ErrorItem(severity, type, text);
        errors.add(err);
        if(maxErr==null|| severity.ordinal()>maxErr.severity.ordinal()) maxErr = err;
    }
    void clear(){
        maxErr = null;
        errors.clear();
    }
    ErrSeverity getSeverity(){
        return maxErr==null?ErrSeverity.NONE:maxErr.severity;
    }
    void showmax(ErrSeverity minseverity){
        if (maxErr!=null&&maxErr.severity.ordinal()>=minseverity.ordinal())
            Toast.makeText(InApp.get().getBaseContext(), maxErr.text, Toast.LENGTH_LONG).show();
    }
    void showmax(){
        showmax(ErrSeverity.WARNING);
    }
}
