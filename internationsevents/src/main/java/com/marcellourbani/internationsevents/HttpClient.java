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

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;


import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.util.List;

public class HttpClient {
    private final static String UA = "user-agent:Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.130 Safari/537.36";
    private final OkHttpClient client;
    private final CookieManager manager;

    public HttpClient() {
        client = new OkHttpClient();
        manager=new CookieManager();
        manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        client.setCookieHandler(manager);
    }

    public String geturl_string(String url) throws IOException {
        Request request = new Request.Builder()
                .header("User-Agent", UA)
                .url(url)
                .build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) throw new HttpClientException( response);
        return response.body().string();
    }


    public String posturl_string(String url, List<NameValuePair> params) throws Throwable {
        FormEncodingBuilder builder = new FormEncodingBuilder();
        for (NameValuePair param : params) builder.add(param.getName(), param.getValue());
        RequestBody formBody = builder.build();
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", UA)
                .post(formBody)
                .build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) throw new HttpClientException(response);

        return response.body().string();
    }

    public List<HttpCookie> getCookies() {
        return manager.getCookieStore().getCookies();
    }

    public static class NameValuePair {
        private String name, value;

        public NameValuePair(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }


        public String getValue() {
            return value;
        }
    }
    public static class HttpClientException extends IOException{
        public final Response response;

        HttpClientException(Response r){
            super("Unexpected code " + r);
            response = r;
        }
    }
}
