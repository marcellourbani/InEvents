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

import static com.marcellourbani.internationsevents.R.id.*;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.ListFragment;

import java.util.ArrayList;

public class EventList extends AppCompatActivity {
    private static final int PERMISSION_CALENDAR=1;
    private final String EVFRAG = "EVFRAG";
    private EventsFragment mFrag;
    protected MenuItem refresh;
    private static final int SETPASSWORD = 2001;
    private DataUpdateReceiver dataUpdateReceiver;
    String mNotifiedEvent;
    Intent mLastIntent = null;
    private ArrayList<AsyncTask> tasks = new ArrayList<>();

    protected enum Operations {LOAD, RSVPYES, RSVPNO, REFRESH, REFRESHALL}

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mLastIntent = intent;
        mNotifiedEvent = intent.getStringExtra(InApp.NOTIFIEDEVENT);
    }
    private boolean needPermissions(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false;
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED;
    }
    private void checkPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && needPermissions()) {
            requestPermissions(new String[] { Manifest.permission.WRITE_CALENDAR,Manifest.permission.READ_CALENDAR }, PERMISSION_CALENDAR);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_CALENDAR:
                initializeFragment();
                return;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();
        setContentView(R.layout.activity_event_list);
        mFrag = (EventsFragment) getSupportFragmentManager().findFragmentByTag(EVFRAG);
        if (savedInstanceState == null || mFrag == null) {
            initializeFragment();
        }else if (mFrag.isNew()) mFrag.loadevents(false, false);
    }

    private void initializeFragment() {
        if(needPermissions()) return;
        mNotifiedEvent = getIntent().getStringExtra(InApp.NOTIFIEDEVENT);
        mFrag = new EventsFragment();
        mFrag.setRetainInstance(true);
        mFrag.loadevents(false, false);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.container, mFrag, EVFRAG)
                .commit();
        InService.schedule(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dataUpdateReceiver == null) dataUpdateReceiver = new DataUpdateReceiver();
        IntentFilter intentFilter = new IntentFilter(InService.RELOAD_EVENTS);
        if (mLastIntent != null) {
            mNotifiedEvent = mLastIntent.getStringExtra(InApp.NOTIFIEDEVENT);
            mFrag.scrollToNotified();
        }
        registerReceiver(dataUpdateReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (dataUpdateReceiver != null) unregisterReceiver(dataUpdateReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETPASSWORD && InApp.getbot().passIsSet() && mFrag != null)
            mFrag.loadevents(true, true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.event_list, menu);
        refresh = menu.findItem(action_refresh);
        return true;
    }

    void setProgressIndicator(boolean on, AsyncTask t) {
        if (on) tasks.add(t);
        else tasks.remove(t);
        if (refresh != null)
            if (!tasks.isEmpty())
                refresh.setActionView(R.layout.actionbar_indeterminate_progress);
            else
                refresh.setActionView(null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent web;
        switch (id) {
            case action_settings:
                startActivity(new Intent(this, InPreferences.class));
                return true;
            case action_refresh:
                mFrag.loadevents(true, false);
                return true;
            case action_refresh_all:
                //Intent i = new Intent(InApp.get(),InService.class);
                //InApp.get().startService(i);
                mFrag.loadevents(true, true);
                return true;
            case action_home:
                web = new Intent(EventList.this, InWeb.class);
                web.putExtra(InWeb.EVENT_URL, InternationsBot.BASEURL);
                web.putExtra(InWeb.CURRENT_COOKIES, InApp.getbot().getCookies());
                startActivity(web);
                return true;
            case action_messages:
                web = new Intent(EventList.this, InWeb.class);
                web.putExtra(InWeb.EVENT_URL, InternationsBot.MESSAGEURL);
                web.putExtra(InWeb.CURRENT_COOKIES, InApp.getbot().getCookies());
                startActivity(web);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void showmap(InEvent event) {
        Uri uri = Uri.parse("geo:0,0?q=" + event.mLocation);
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    private void showEventActivity(InEvent event) {
        Intent web = new Intent(EventList.this, InWeb.class);
        web.putExtra(InWeb.EVENT_URL, event.mEventUrl);
        web.putExtra(InWeb.CURRENT_COOKIES, InApp.getbot().getCookies());
        startActivity(web);
    }

    public void showevent(InEvent event) {
        if (InApp.getbot().mSigned) {
            showEventActivity(event);
        } else {
            LinkWorker worker = new LinkWorker();
            EventList.this.setProgressIndicator(true,worker);
            worker.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, event);
        }
    }

    public void rsvp(InEvent event, boolean going) {
        mFrag.rsvp(event, going);
    }

    private class LinkWorker extends AsyncTask<InEvent, Integer, Boolean> {
        InEvent mEvent;

        @Override
        protected void onPostExecute(Boolean o) {
            EventList.this.setProgressIndicator(false,this);
            if (o)
                showEventActivity(mEvent);
            else
                InError.get().showmax();
            InError.get().clear();
        }

        @Override
        protected Boolean doInBackground(InEvent... inEvents) {
            InError.get().clear();
            mEvent = inEvents[0];
            return InApp.getbot().sign();
        }
    }


    public static class EventsFragment extends ListFragment {
        protected static InternationsBot mIbot;
        private NetWorker mNw;
        private EventAdapter mEventAdapter = null;

        public EventsFragment() {
            mIbot = InApp.getbot();
        }
        public boolean isNew(){
            return mEventAdapter==null;
        }
        @Override
        public void onAttach(@NonNull Context c) {
            super.onAttach(c);
            scrollToNotified();
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView;
            rootView = inflater.inflate(R.layout.fragment_event_list, container, false);
            return rootView;
        }

        public void loadevents(boolean refresh, boolean all) {
            EventList el = ((EventList) getActivity());
            mNw = new NetWorker();
            if (el != null) el.setProgressIndicator(true, mNw);
            if (refresh) {
                if (all)
                    mNw.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, Operations.REFRESHALL);
                else
                    mNw.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, Operations.REFRESH);
            } else mNw.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, Operations.LOAD);
        }

        void scrollToNotified() {
            EventList el = ((EventList) getActivity());
            if (el != null && el.mNotifiedEvent != null && mEventAdapter != null && mIbot != null) {
                InEvent ev = mIbot.mEvents.get(el.mNotifiedEvent);
                if (ev != null) {
                    int pos = mEventAdapter.getPosition(ev);
                    if (pos >= 0) {
                        EventsFragment.this.getListView().smoothScrollToPosition(pos);
                        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                        EventsFragment.this.getListView().setSelection(pos);
                    }
                }
                el.mNotifiedEvent = null;
            }
        }

        public void rsvp(InEvent event, boolean going) {
            EventList el = (EventList) getActivity();
            NetWorker nw = new NetWorker();
            if (el != null) el.setProgressIndicator(true, nw);
            nw.mEvent = event;
            nw.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    going ? Operations.RSVPYES : Operations.RSVPNO);
        }

        private class NetWorker extends AsyncTask<Operations, Integer, Boolean> {
            InEvent mEvent = null;
            boolean fromDB = false;
            private Operations mOperation;

            @Override
            protected void onPostExecute(Boolean o) {
                super.onPostExecute(o);
                EventList el = ((EventList) getActivity());
                if (el != null) el.setProgressIndicator(false, this);
                if (o) {
                    InCalendar.syncEvents(mIbot.mEvents);
                    if (mEventAdapter == null) {
                        mEventAdapter = EventAdapter.create(getActivity(), R.layout.fragment_event_list, mIbot.mEvents);
                        EventsFragment.this.setListAdapter(mEventAdapter);
                    } else mEventAdapter.updateEvents(mIbot.mEvents);
                    EventsFragment.this.getListView().invalidateViews();
                    if (fromDB) {
                        if (mIbot.mEvents.isEmpty() || mIbot.isExpired(InternationsBot.Refreshkeys.EVENTS) && InApp.get().isConnected(true))
                            loadevents(true, true);
                        else if (mIbot.isExpired(InternationsBot.Refreshkeys.MYEVENTS))
                            loadevents(true, false);
                    }
                    if (mOperation == Operations.RSVPNO || mOperation == Operations.RSVPYES)
                        InError.get().showmax(InError.ErrSeverity.INFO);
                    else scrollToNotified();
                } else {
                    InError.get().showmax();
                    if (!mIbot.passIsSet() || InError.get().hasType(InError.ErrType.LOGIN)) {
                        getActivity().startActivityForResult(new Intent(getActivity(), InPreferences.class), SETPASSWORD);
                    }
                }
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                try {
                    if (mEventAdapter != null && EventsFragment.this.getListView() != null) {
                        mEventAdapter.updateEvents(mIbot.mEvents);
                        EventsFragment.this.getListView().invalidateViews();
                    }
                } catch (Exception e) {
                    Log.d(InternationsBot.INTAG,e.toString());
                }
            }

            void refresh(boolean all) {
                mIbot.readMyEvents(true, all ? InternationsBot.ALLEVENTS : "");//will save everything later
                publishProgress();
                if (all) {
                    if (InError.isOk()) mIbot.readMyGroups();
                    if (InError.isOk()) mIbot.saveGroups();
                    if (InError.isOk()) mIbot.readGroupsEvents();
                    if (InError.isOk()) mIbot.saveEvents(true);
                } else {
                    if (InError.isOk() && mIbot.isExpired(InternationsBot.Refreshkeys.GROUPS)) {
                        mIbot.readMyGroups();
                        if (InError.isOk()) mIbot.saveGroups();
                    }
                    if (InError.isOk() && mIbot.isExpired(InternationsBot.Refreshkeys.EVENTS)) {
                        mIbot.readGroupsEvents();
                        if (InError.isOk()) mIbot.saveEvents(true);
                    }
                }
            }

            @Override
            protected Boolean doInBackground(Operations... ops) {
                mOperation = ops[0];
                InError.get().clear();
                if (ops[0] == Operations.LOAD) {
                    mIbot.loadEvents();
                    if (InError.isOk()) mIbot.loadMyGroups();
                    fromDB = true;
                    return InError.isOk();
                }
                fromDB = false;
                if (mIbot.sign()) {
                    if (mIbot.mSigned)
                        switch (ops[0]) {
                            case REFRESH:
                                refresh(false);
                                break;
                            case REFRESHALL:
                                refresh(true);
                                break;
                            case RSVPNO:
                            case RSVPYES:
                                boolean newRSVP = ops[0] == Operations.RSVPYES;
                                mIbot.rsvp(mEvent, newRSVP);
                                if (InError.isOk()) {
                                    mIbot.readMyEvents(true, mEvent.mEventId);
                                    InEvent subev = mIbot.mEvents.get(mEvent.mEventId);
                                    if (subev == null || subev.imGoing() != newRSVP)
                                        InError.get().add(InError.ErrSeverity.INFO,
                                                InError.ErrType.UNKNOWN,
                                                "Error " + (newRSVP ? "subscribing (list full?)" : "unsubscribing") + ", please try from website");
                                    else InError.get().add(InError.ErrSeverity.INFO,
                                            InError.ErrType.UNKNOWN,
                                            "Event " + (newRSVP ? "" : "un") + "subscribed successfully");
                                }
                                break;
                        }
                    else
                        return false;
                }
                return true;
            }
        }
    }

    private class DataUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(InService.RELOAD_EVENTS)) {
                mFrag.loadevents(false, false);
            }
        }
    }
}
