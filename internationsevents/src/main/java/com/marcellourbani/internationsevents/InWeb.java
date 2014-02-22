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
import android.graphics.Point;
import android.os.Bundle;
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
    private WebView web=null;

    /* Class that prevents opening the Browser */
    private class InsideWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_web);
        web = (WebView) this.findViewById(R.id.inwebview);
        if (savedInstanceState == null) {

            String eventurl = getIntent().getStringExtra(EVENT_URL);
            Bundle cookies = getIntent().getBundleExtra(CURRENT_COOKIES);
            WebSettings settings = web.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setBuiltInZoomControls(true);
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);
            settings.setSupportZoom(true);
            web.setInitialScale(getScale());
            if (cookies != null) {
                CookieSyncManager.createInstance(this);
                CookieManager cookieManager = CookieManager.getInstance();
                for (String key : cookies.keySet()) {
                    cookieManager.setCookie(InternationsBot.BASEURL, key + '=' + cookies.getString(key));
                }
                CookieSyncManager.getInstance().sync();
            }
            web.setWebViewClient(new InsideWebViewClient());
            web.setWebChromeClient(new WebChromeClient());
            if (eventurl != null) {
                web.loadUrl(eventurl);
            }
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
    protected void onSaveInstanceState(Bundle outState )
    {
        super.onSaveInstanceState(outState);
        if(web!=null)web.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        if(web!=null)web.restoreState(savedInstanceState);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.in_web, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_back) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
