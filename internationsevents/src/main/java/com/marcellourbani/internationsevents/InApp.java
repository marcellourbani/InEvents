package com.marcellourbani.internationsevents;

import android.app.Application;


public class InApp extends Application{
    private static InApp _inst;
    private InDatabase db;
    private String inToken;

    public static InApp get(){
        return _inst;
    }
    public InDatabase getDB(){
        return db==null?db=new InDatabase(this):db;
    }
    public String getInToken() {
        return inToken;
    }
    public void setInToken(String inToken) {
        this.inToken = inToken;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        _inst = this;
        inToken = null;
    }
}
