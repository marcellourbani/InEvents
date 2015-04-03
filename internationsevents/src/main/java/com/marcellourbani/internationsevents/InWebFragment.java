package com.marcellourbani.internationsevents;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class InWebFragment extends Fragment {
    private static final String ARG_URL = "URL";
    private static final String ARG_COOKIES = "COOKIES";

    private String mUrl;
    private Bundle mCookies;
    private WebView web = null;
    private OnFragmentInteractionListener mListener;
    private boolean mIsLoading;

    public static InWebFragment newInstance(String url, Bundle cookies) {
        InWebFragment fragment = new InWebFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        args.putBundle(ARG_COOKIES, cookies);
        fragment.setArguments(args);
        return fragment;
    }

    public InWebFragment() {
        // Required empty public constructor
    }

    public void loadUrl(String url, Bundle cookies) {
        mUrl = url;
        mCookies = cookies;
        setcookies(mCookies);
        setLoading(true);
        web.loadUrl(mUrl);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        if (getArguments() != null) {
            mUrl = getArguments().getString(ARG_URL);
            mCookies = getArguments().getBundle(ARG_COOKIES);
        }
    }

    private int getScale() {
        Display display = ((WindowManager) InApp.get().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point p = new Point();
        display.getSize(p);
        Double val = p.x / 1280d;
        val = val * 100d;
        return val.intValue();
    }

    void setcookies(Bundle cookies) {
        if (cookies != null) {
            CookieSyncManager.createInstance(InApp.get());
            CookieManager cookieManager = CookieManager.getInstance();
            for (String key : cookies.keySet()) {
                cookieManager.setCookie(InternationsBot.BASEURL, key + '=' + cookies.getString(key));
            }
            CookieSyncManager.getInstance().sync();
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_in_web, container, false);
        web = (WebView) view.findViewById(R.id.inwebview);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setLoadsImagesAutomatically(true);
        web.setInitialScale(getScale());
        web.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        web.setScrollbarFadingEnabled(true);
        setcookies(mCookies);
        web.setWebViewClient(new InsideWebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        if (savedInstanceState == null) {
            if (mUrl != null) {
                loadUrl(mUrl, mCookies);
            }
        } else web.restoreState(savedInstanceState);

        return view;
    }

    //    private class SignWorker extends AsyncTask<String, Integer, Boolean> {
//        String url;
//        @Override
//        protected void onPostExecute(Boolean o) {
//            if(o){
//                setcookies(InApp.getbot().getCookies());
//                web.loadUrl(url);
//            }
//        }
//
//        @Override
//        protected Boolean doInBackground(String...strings) {
//            url=strings[0];
//            return InApp.getbot().sign();
//        }
//    }
//    @Override
//    public void onConfigurationChanged(Configuration newConfig) {
//        if (web != null) {
//            // Remove the WebView from the old placeholder
//            mWebParent.removeView(web);
//        }
//        super.onConfigurationChanged(newConfig);
//        // Reinitialize the UI
//        initUI();
//    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
            mListener.onFragmentLoadingStatus(mIsLoading);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the state of the WebView
        web.saveState(outState);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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
            } else if (url.startsWith("http:") || url.startsWith("https:")) {
                setLoading(true);
                view.loadUrl(url);
                return true;
            } else if (url.startsWith("mailto:")) {
                try {
                    Intent emailIntent = new Intent(Intent.ACTION_SEND, Uri.parse(url));
                    emailIntent.setType("message/rfc822");
                    String recipient = url.substring(url.indexOf(":") + 1);
                    emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{recipient});
                    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "");
                    emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "");

                    getActivity().startActivity(Intent.createChooser(emailIntent, "Send mail..."));
                } catch (Exception ignored) {
                }
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

    private void setLoading(boolean loading) {
        mIsLoading = loading;
        if (mListener != null) mListener.onFragmentLoadingStatus(loading);
    }

    public interface OnFragmentInteractionListener {
        void onFragmentLoadingStatus(boolean loading);
    }

}
