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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Objects;

public class InWeb extends Activity {

    public static final String EVENT_URL = "EVENTURL";
    public static final String CURRENT_COOKIES = "CUR_COOKIES";
    private WebView web = null;
    private MenuItem loading=null;
    protected void setLoading(boolean isloading){
        if(loading!=null){
            if(isloading)
              loading.setActionView(R.layout.actionbar_indeterminate_progress);
            else
              loading.setActionView(null);
        }
    };
    /* Class that prevents opening the Browser */
    private class InsideWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            setLoading(true);
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            setLoading(false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_web);
        web = (WebView) this.findViewById(R.id.inwebview);
        Intent i=getIntent();
        String eventurl = i.getStringExtra(EVENT_URL);
        Bundle cookies = i.getBundleExtra(CURRENT_COOKIES);
        Uri uri = i.getData();
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        web.setInitialScale(getScale());
        setcookies(cookies);
        web.setWebViewClient(new InsideWebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        if(savedInstanceState == null){
            if (eventurl != null)
                web.loadUrl(eventurl);
            else if (uri!=null) new SignWorker().execute(uri.toString());
        }
    }
    void setcookies(Bundle cookies){
        if (cookies != null) {
            CookieSyncManager.createInstance(this);
            CookieManager cookieManager = CookieManager.getInstance();
            for (String key : cookies.keySet()) {
                cookieManager.setCookie(InternationsBot.BASEURL, key + '=' + cookies.getString(key));
            }
            CookieSyncManager.getInstance().sync();
        }
    }
    private int getScale() {
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point p = new Point();
        display.getSize(p);
        Double val = new Double(p.x) / new Double(1280);
        val = val * 100d;
        return val.intValue();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (web != null) web.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (web != null) web.restoreState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.in_web, menu);
        loading = menu.findItem(R.id.action_loading);
        return true;
    }
    private class SignWorker extends AsyncTask<String, Integer, Boolean> {
        String url;
        @Override
        protected void onPostExecute(Boolean o) {
            if(o){
              setcookies(InApp.getbot().getCookies());
              web.loadUrl(url);
            }
        }

        @Override
        protected Boolean doInBackground(String...strings) {
            url=strings[0];
            setLoading(true);
            return InApp.getbot().sign();
        }
    }
}
