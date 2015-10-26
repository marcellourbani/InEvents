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

import android.app.ActivityManager;
import android.support.v4.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;

public class EventList extends AppCompatActivity {
    private static EventsFragment mFrag;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);
        final String EVFRAG = "EVFRAG";
        mFrag = (EventsFragment) getSupportFragmentManager().findFragmentByTag(EVFRAG);
        if (savedInstanceState == null || mFrag == null) {
            mNotifiedEvent = getIntent().getStringExtra(InApp.NOTIFIEDEVENT);
            mFrag = new EventsFragment();
            mFrag.setRetainInstance(true);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, mFrag, EVFRAG)
                    .commit();
            InService.schedule(false, true);
        }
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
        if (requestCode == SETPASSWORD && InApp.getbot().passIsSet() && mFrag != null)
            mFrag.loadevents(true, true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.event_list, menu);
        refresh = menu.findItem(R.id.action_refresh);
        mFrag.loadevents(false, false);
        return true;
    }

    void updateProgressIndicator(boolean on, AsyncTask t) {
        boolean isRunning = false;
        if (t != null) {
            if (on) tasks.add(t);
            else tasks.remove(t);
        }
        if (!tasks.isEmpty())
            isRunning = true;
        else {
            ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (InService.class.getName().equals(service.service.getClassName())) {
                    isRunning = true;
                }
            }
        }
        if (isRunning) refresh.setActionView(R.layout.actionbar_indeterminate_progress);
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
            case R.id.action_settings:
                startActivity(new Intent(this, InPreferences.class));
                return true;
            case R.id.action_refresh:
                mFrag.loadevents(true, false);
                return true;
            case R.id.action_refresh_all:
                mFrag.loadevents(true, true);
                return true;
            case R.id.action_home:
                web = new Intent(EventList.this, InWeb.class);
                web.putExtra(InWeb.EVENT_URL, InternationsBot.BASEURL);
                web.putExtra(InWeb.CURRENT_COOKIES, InApp.getbot().getCookies());
                startActivity(web);
                return true;
            case R.id.action_messages:
                web = new Intent(EventList.this, InWeb.class);
                web.putExtra(InWeb.EVENT_URL, InternationsBot.MESSAGEURL);
                web.putExtra(InWeb.CURRENT_COOKIES, InApp.getbot().getCookies());
                startActivity(web);
                return true;
        }
        return super.onOptionsItemSelected(item);
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
            EventList.this.updateProgressIndicator(true, worker);
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
            EventList.this.updateProgressIndicator(false, this);
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
        private EventAdapter mEventAdapter = null;
        private ArrayMap<String, InEvent> mEvents = new ArrayMap<>();
        private boolean loaded = false;

        public EventsFragment() {
        }

        @Override
        public void onAttach(Context c) {
            super.onAttach(c);
            if (loaded)
                scrollToNotified();
            else {
                loaded = true;
                loadevents(false, false);
            }
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
            Intent i = new Intent(el, InService.class);

            if (refresh) {
                if (all) i.setAction(InService.ACTION_REFRESH_ALL);
                else i.setAction(InService.ACTION_REFRESH);
            } else i.setAction(InService.ACTION_LOAD);
            el.startService(i);
        }

        void scrollToNotified() {
            EventList el = ((EventList) getActivity());
            if (el != null && el.mNotifiedEvent != null && mEventAdapter != null && mEvents != null) {
                InEvent ev = mEvents.get(el.mNotifiedEvent);
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
            Intent i = new Intent(el, InService.class);
            i.setAction(going ? InService.ACTION_SUBSCRIBE : InService.ACTION_UNSUBSCRIBE);
            i.putExtra(InService.EVENTID, event.mEventId);
            el.startService(i);
        }

        public void setEvents(ArrayList<InEvent> events, InError error) {
            EventList el = ((EventList) getActivity());
            error.showmax(InError.ErrSeverity.INFO);

            if (el != null) el.updateProgressIndicator(false, null);

            if (error.getSeverity().ordinal() < InError.ErrSeverity.ERRROR.ordinal()) {
                ArrayMap<String, InEvent> newEvents = new ArrayMap<>();
                if (events != null) {
                    for (InEvent event : events) {
                        newEvents.put(event.mEventId, event);
                        mEvents.put(event.mEventId, event);
                    }
                    InCalendar.syncEvents(newEvents);

                    if (mEventAdapter == null) {
                        mEventAdapter = EventAdapter.create(getActivity(), R.layout.fragment_event_list, mEvents);
                        EventsFragment.this.setListAdapter(mEventAdapter);
                    } else mEventAdapter.updateEvents(mEvents);
                }
            }
        }

    }

    private class DataUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (InService.RELOAD_EVENTS.equals(intent.getAction())) {
                InError.get().readFromIntent(intent);
                Bundle b = intent.getBundleExtra(InService.EVENTLIST);
                ArrayList<InEvent> events = b.getParcelableArrayList(InService.EVENTLIST);
                mFrag.setEvents(events, InError.get());
            } else if (InService.SERVICE_STATUS_CHANGE.equals(intent.getAction())) {
                updateProgressIndicator(intent.getBooleanExtra(InService.ISRUNNING, false), null);
            }
            ;
        }
    }
}
