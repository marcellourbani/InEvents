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
import android.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class EventList extends Activity {
    private static EventsFragment mFrag;
    protected MenuItem refresh;
    private static final int SETPASSWORD = 2001;
    private DataUpdateReceiver dataUpdateReceiver;

    protected enum Operations {LOAD, RSVPYES, RSVPNO, REFRESH, REFRESHALL}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //TODO new event notification
        setContentView(R.layout.activity_event_list);
        final String EVFRAG = "EVFRAG";
        mFrag = (EventsFragment) getFragmentManager().findFragmentByTag(EVFRAG);
        if (savedInstanceState == null || mFrag == null) {
            mFrag = new EventsFragment(InApp.getbot());
            mFrag.setRetainInstance(true);
            getFragmentManager().beginTransaction()
                    .add(R.id.container, mFrag, EVFRAG)
                    .commit();
            InService.schedule(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dataUpdateReceiver == null) dataUpdateReceiver = new DataUpdateReceiver();
        IntentFilter intentFilter = new IntentFilter(InService.RELOAD_EVENTS);
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

    void setProgressIndicator(boolean on) {
        if (refresh != null)
            if (on)
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
        }
        return super.onOptionsItemSelected(item);
    }

    public void showmap(InEvent event) {
        Uri uri = Uri.parse("geo:0,0?q=" + event.mLocation);
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    public void showevent(InEvent event) {
        LinkWorker worker = new LinkWorker();
        EventList.this.setProgressIndicator(true);
        worker.execute(event);
    }

    public void rsvp(InEvent event, boolean going) {
        mFrag.rsvp(event, going);
    }

    private class LinkWorker extends AsyncTask<InEvent, Integer, Boolean> {
        InEvent mEvent;

        @Override
        protected void onPostExecute(Boolean o) {
            EventList.this.setProgressIndicator(false);
            if (o) {
                Intent web = new Intent(EventList.this, InWeb.class);
                web.putExtra(InWeb.EVENT_URL, mEvent.mEventUrl);
                web.putExtra(InWeb.CURRENT_COOKIES, InApp.getbot().getCookies());
                startActivity(web);
            } else
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

        public EventsFragment(InternationsBot bot) {
            mIbot = bot;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
        }

        @Override
        public View onCreateView( LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView;
            rootView = inflater.inflate(R.layout.fragment_event_list, container, false);
            return rootView;
        }

        public void loadevents(boolean refresh, boolean all) {
            EventList el = ((EventList) getActivity());
            if (el != null) el.setProgressIndicator(true);
            mNw = new NetWorker();
            if (refresh) {
                if (all)
                    mNw.execute(Operations.REFRESHALL);
                else
                    mNw.execute(Operations.REFRESH);
            } else mNw.execute(Operations.LOAD);
        }

        public void rsvp(InEvent event, boolean going) {
            EventList el = (EventList) getActivity();
            if(el!=null)el.setProgressIndicator(true);
            mNw = new NetWorker();
            mNw.mEvent = event;
            mNw.execute(going ? Operations.RSVPYES : Operations.RSVPNO);
        }

        private class NetWorker extends AsyncTask<Operations, Integer, Boolean> {
            InEvent mEvent = null;
            boolean fromDB = false;
            private Operations mOperation;

            @Override
            protected void onPostExecute(Boolean o) {
                super.onPostExecute(o);
                EventList el = ((EventList) getActivity());
                if (el != null) el.setProgressIndicator(false);
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
                } else {
                    InError.get().showmax();
                    if (!mIbot.passIsSet() || InError.get().hasType(InError.ErrType.LOGIN)) {
                        getActivity().startActivityForResult(new Intent(getActivity(), InPreferences.class), SETPASSWORD);
                    }
                }
            }

            protected void onProgressUpdate() {
                // onPostExecute(true);
            }

            void refresh(boolean all) {
                mIbot.readMyEvents(true);//will save everything later
                // publishProgress();
                if (all) {
                    if (InError.isOk()) mIbot.readMyGroups();
                    if (InError.isOk()) mIbot.saveGroups();
                    if (InError.isOk()) mIbot.readGroupsEvents();
                    if (InError.isOk()) mIbot.saveEvents(all);
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
                                    mIbot.readMyEvents(true);
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
                mFrag.loadevents(false,false);
            }
        }
    }
}
