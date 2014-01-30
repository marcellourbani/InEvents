package com.marcellourbani.internationsevents;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class httpClient {
    DefaultHttpClient httpClient;
    public httpClient() {
        DefaultHttpClient client = new DefaultHttpClient();
        ClientConnectionManager mgr = client.getConnectionManager();
        HttpParams params = client.getParams();
        httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(params,
                mgr.getSchemeRegistry()), params);
    }

    public InputStream geturl_stream(String url) throws IOException {
        HttpGet httpget = new HttpGet(url);
        HttpResponse response = httpClient.execute(httpget);
        HttpEntity entity = response.getEntity();
        return entity.getContent();
    }
    public String geturl_string(String url) throws IOException {
        return streamToString(geturl_stream(url));
    }
    public InputStream posturl_stream(String url, List<NameValuePair> params) throws IOException{
        HttpPost httppost = new HttpPost(url);
        httppost.setEntity(new UrlEncodedFormEntity(params));
        HttpResponse response = httpClient.execute(httppost);
        if (response.getStatusLine().getStatusCode() == 401) {
            return null;
        }
        HttpEntity entity = response.getEntity();
        return entity.getContent();
    }
    public String posturl_string(String url, List<NameValuePair> params)throws Throwable {
        return streamToString(posturl_stream(url, params));
    }
    public String download(String url, List<NameValuePair> params, File file) throws IOException{
        HttpPost httppost = new HttpPost(url);
        httppost.setEntity(new UrlEncodedFormEntity(params));
        HttpResponse response = httpClient.execute(httppost);
        HttpEntity en = response.getEntity();

        InputStream is;
        String mt = null;
        BufferedInputStream bis;
        BufferedOutputStream bos = null;
        if ((is = en.getContent()) != null) {
            bis = new BufferedInputStream(is);
            bos = new BufferedOutputStream(new FileOutputStream(file));
            int i;
            while ((i = bis.read()) != -1) {
                bos.write(i);
            }
            mt = en.getContentType().getValue();
        }
        if(bos!=null)bos.close();
        return mt;
    }
    public byte[] streamToByte(InputStream is) throws IOException{
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
        BufferedInputStream bis = new BufferedInputStream(is);
        int i;
        while ((i = bis.read()) != -1) {
            bos.write(i);
        }
        return bos.toByteArray();
    }
    private String streamToString(InputStream is) throws IOException{
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

}
