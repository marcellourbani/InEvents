package com.marcellourbani.internationsevents;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;


public class InApp extends Application {
    private static InApp _inst;
    private InDatabase db;
    private String inToken;

    public static InApp get() {
        return _inst;
    }

    public InDatabase getDB() {
        return db == null ? db = new InDatabase(this) : db;
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

    public boolean isConnected(boolean any) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNI = connectivityManager.getActiveNetworkInfo();
        return activeNI != null && activeNI.isConnected() && (any || activeNI.getType() == ConnectivityManager.TYPE_WIFI);
    }

    public boolean isConnected() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(InApp.get());
        Boolean usemobile = prefs.getBoolean("pr_refresh_mobile", false);
        return isConnected(usemobile);
    }
}
