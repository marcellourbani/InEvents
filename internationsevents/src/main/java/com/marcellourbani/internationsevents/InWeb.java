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
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
    }
    /* Class that prevents opening the Browser */
    private class InsideWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("tel:")) {
                Intent intent = new Intent(Intent.ACTION_DIAL,
                        Uri.parse(url));
                startActivity(intent);
                return true;
            }else if(url.startsWith("http:") || url.startsWith("https:")) {
                setLoading(true);
                view.loadUrl(url);
                return true;
            }else if (url.startsWith("mailto:")) {
                try {
                    Intent emailIntent = new Intent(Intent.ACTION_SEND, Uri.parse(url));
                    emailIntent.setType("message/rfc822");
                    String recipient = url.substring( url.indexOf(":")+1 );
                    emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{recipient});
                    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "");
                    emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "");

                    InWeb.this.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
                }
                catch (Exception ignored) {}
                return true;
            }
            return false;

        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            setLoading(false);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
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
            if (eventurl != null){
                setLoading(true);
                web.loadUrl(eventurl);
            }
            else if (uri!=null) {
                setLoading(true);
                new SignWorker().execute(uri.toString());
            }
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
        Double val = p.x /  1280d;
        val = val * 100d;
        return val.intValue();
    }
    @Override
    protected void onSaveInstanceState( @NonNull Bundle  outState) {
        super.onSaveInstanceState(outState);
        if (web != null) web.saveState(outState);
    }
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
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
            return InApp.getbot().sign();
        }
    }
}
