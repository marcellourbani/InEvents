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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

public class InWeb extends ActionBarActivity implements InWebFragment.OnFragmentInteractionListener {

    public static final String EVENT_URL = "EVENTURL";
    public static final String CURRENT_COOKIES = "CUR_COOKIES";
    private static final String WEBFRAG = "WEBFRAG";
    private MenuItem loading=null;
    private boolean mIsloading;

    protected void setLoading(boolean isloading){
        mIsloading = isloading;
        if(loading!=null){
            if(isloading)
              loading.setActionView(R.layout.actionbar_indeterminate_progress);
            else
              loading.setActionView(null);
        }
    }

    @Override
    public void onFragmentLoadingStatus(boolean loading) {
        setLoading(loading);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_web);
        InWebFragment fragment = (InWebFragment) getSupportFragmentManager().findFragmentByTag(WEBFRAG);
        if(savedInstanceState==null|| fragment ==null){
            Intent i=getIntent();
            String eventurl = i.getStringExtra(EVENT_URL);
            Bundle cookies = i.getBundleExtra(CURRENT_COOKIES);
            fragment = InWebFragment.newInstance(eventurl,cookies);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.infragparent, fragment, WEBFRAG)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.in_web, menu);
        loading = menu.findItem(R.id.action_loading);
        setLoading(mIsloading);
        return true;
    }
}
