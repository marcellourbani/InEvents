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
import android.content.Intent;
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
    protected static InternationsBot mIbot;
    private static EventsFragment mFrag;
    protected MenuItem refresh;
    private static final int SETPASSWORD = 2001;

    protected enum Operations {LOAD, RSVPYES, RSVPNO, REFRESH, REFRESHALL}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //TODO? no refresh when network not connected
        //TODO service
        //TODO? intent filter
        mIbot = new InternationsBot(PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_event_list);
        final String EVFRAG = "EVFRAG";
        mFrag = (EventsFragment) getFragmentManager().findFragmentByTag(EVFRAG);
        if (savedInstanceState == null||mFrag==null) {
            mFrag = new EventsFragment(mIbot);
            mFrag.setRetainInstance(true);
            getFragmentManager().beginTransaction()
                    .add(R.id.container, mFrag,EVFRAG)
                    .commit();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SETPASSWORD && mIbot.passIsSet() && mFrag != null)
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
                web.putExtra(InWeb.CURRENT_COOKIES, mIbot.getCookies());
                startActivity(web);
            } else
                InError.get().showmax();
            InError.get().clear();
        }

        @Override
        protected Boolean doInBackground(InEvent... inEvents) {
            InError.get().clear();
            mEvent = inEvents[0];
            return mIbot.sign();
        }
    }

    public static class EventsFragment extends ListFragment {
        protected static InternationsBot mIbot;
        private NetWorker mNw;
        private EventAdapter eventAdapter;

        public EventsFragment(InternationsBot bot) {
            mIbot = bot;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
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
            el.setProgressIndicator(true);
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
                    for (InEvent event : mIbot.getEvents()) {
                        if (event.imGoing())
                            InCalendar.modifyEvent(getActivity(), event);
                    }
                    eventAdapter = new EventAdapter(getActivity(), R.layout.fragment_event_list, mIbot.getEvents());
                    EventsFragment.this.setListAdapter(eventAdapter);
                    eventAdapter.notifyDataSetChanged();
                    if (fromDB) {
                        if (mIbot.mEvents.isEmpty() || mIbot.isExpired(InternationsBot.Refreshkeys.EVENTS))
                            loadevents(true, true);
                        else if (mIbot.isExpired(InternationsBot.Refreshkeys.MYEVENTS))
                            loadevents(true, false);
                    }
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
                }
                if (InError.isOk()) mIbot.saveEvents(all);

            }

            @Override
            protected Boolean doInBackground(Operations... ops) {
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
                                mIbot.rsvp(mEvent, false);
                                if (InError.isOk())
                                    mIbot.readMyEvents(true);
                                break;
                            case RSVPYES:
                                mIbot.rsvp(mEvent, true);
                                if ( InError.isOk())
                                    mIbot.readMyEvents(true);
                                break;
                        }
                    else
                        return false;
                }
                return true;
            }
        }
    }
}
