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


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.util.List;

import okhttp3.FormBody;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpClient {
    private final static String UA = "user-agent:Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.130 Safari/537.36";
    private final OkHttpClient client;
    private final CookieManager manager;

    public HttpClient() {
        manager=new CookieManager();
        manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        client = new OkHttpClient.Builder()
                .followRedirects(false)
                .cookieJar(new JavaNetCookieJar(manager))
                .build();
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
    public JSONObject geturl_in_json(String url) throws IOException, JSONException {
        Request request = new Request.Builder()
                .header("User-Agent", UA)
                .header("accept", "application/vnd.org.internations.frontend+json")
                .url(url)
                .build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) throw new HttpClientException( response);
        return new JSONObject( response.body().string());
    }
    public String login(String url, List<NameValuePair> params) throws Throwable {
        client.newCall(new Request.Builder().url(url).build()).execute();
        FormBody.Builder builder = new FormBody.Builder();
        for (NameValuePair param : params) builder.add(param.getName(), param.getValue());
        RequestBody formBody = builder.build();
        Request request = new Request.Builder()
                .url(url)
                .header("content-type","application/x-www-form-urlencoded")
                .header("accept","application/json")
                .method("POST",formBody)
                .build();
        Response response = client.newCall(request).execute();
        String redirectUrl = response.header("location");
        if(response.code() == 302 && redirectUrl.equals("https://www.internations.org/start/")){
            Response resp2 = client.newCall(new Request.Builder().url(redirectUrl).build()).execute();
            if (!resp2.isSuccessful()) throw new HttpClientException(resp2);
            return resp2.body().string();
        }
        throw new HttpClientException(response);
    }

    public String posturl_string(String url, List<NameValuePair> params) throws Throwable {
        FormBody.Builder builder = new FormBody.Builder();
        for (NameValuePair param : params) builder.add(param.getName(), param.getValue());
        RequestBody formBody = builder.build();
        Request request = new Request.Builder()
                .url(url)
                .header("content-type","application/x-www-form-urlencoded")
                .header("accept","application/json")
                .method("POST",formBody)
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
