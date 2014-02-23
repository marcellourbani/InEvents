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
import android.content.Context;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class EventList extends Activity {
    protected static InternationsBot mIbot;
    private static EventsFragment mFrag;
    protected MenuItem refresh;
    private static final int SETPASSWORD = 2001;

    protected enum Operations {LOAD, RSVPYES, RSVPNO,REFRESH}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //TODO? no refresh when network not connected
        //TODO service
        //TODO? intent filter
        mIbot = new InternationsBot(PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_event_list);
        mFrag = new EventsFragment(mIbot);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, mFrag)
                    .commit();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==SETPASSWORD && mIbot.passIsSet()&&mFrag!=null) mFrag.loadevents(true);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.event_list, menu);
        refresh = menu.findItem(R.id.action_refresh);
        mFrag.loadevents(false);
        return true;
    }
    void setProgressIndicator(boolean on){
        if(refresh!=null)
            if(on)
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
                mFrag.loadevents(true);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class EventsFragment extends ListFragment {
        protected static InternationsBot mIbot;
        private NetWorker mNw;

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

        public void loadevents(boolean refresh) {
            EventList el = ((EventList) getActivity());
            if(el!=null)el.setProgressIndicator(true);
            mNw = new NetWorker();
            if (refresh)
                mNw.execute(Operations.REFRESH);
            else  mNw.execute(Operations.LOAD);
        }
        private class LinkWorker extends AsyncTask<InEvent, Integer, Boolean> {
            InEvent mEvent;
            @Override
            protected void onPostExecute(Boolean o) {
                EventList el = ((EventList)getActivity());
                if(el!=null)el.setProgressIndicator(false);
                if(o){
                    Intent web = new Intent(getActivity(), InWeb.class);
                    web.putExtra(InWeb.EVENT_URL, mEvent.mEventUrl);
                    web.putExtra(InWeb.CURRENT_COOKIES, mIbot.getCookies());
                    startActivity(web);
                }else
                Toast.makeText(getActivity().getApplication().getBaseContext(), "Login failed", Toast.LENGTH_SHORT).show();
            }
            @Override
            protected Boolean doInBackground(InEvent...inEvents) {
                mEvent = inEvents[0];
                return mIbot.sign();
            }
        }

        private class NetWorker extends AsyncTask<Operations, Integer, Boolean> {
            InEvent mEvent = null;
            boolean needRefresh = false;
            private Operations mOperation;

            @Override
            protected void onPostExecute(Boolean o) {
                super.onPostExecute(o);
                EventList el = ((EventList) getActivity());
                if(el!=null)el.setProgressIndicator(false);
                if (o) {
                    for (InEvent event : mIbot.getEvents()) {
                        if (event.mSubscribed)
                            InCalendar.modifyEvent(getActivity(), event);
                    }
                    ArrayAdapter<InEvent> aa = new ArrayAdapter<InEvent>(getActivity(), R.layout.fragment_event_list, mIbot.getEvents()) {
                        final DateFormat df = new SimpleDateFormat("dd.MM.yy"),
                                tf = new SimpleDateFormat("kk:mm");

                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            View view = convertView;
                            if (view == null) {
                                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                view = inflater.inflate(R.layout.event_item, null);
                            }
                            final InEvent event = getItem(position);
                            if (event != null) {
                                TextView title = (TextView) view.findViewById(R.id.eititle);
                                TextView location = (TextView) view.findViewById(R.id.eilocation);
                                ImageView locicon = (ImageView) view.findViewById(R.id.eilocationic);
                                Button rsvp = (Button) view.findViewById(R.id.eirsvp);
                                TextView startdt = (TextView) view.findViewById(R.id.eidate);
                                TextView starttm = (TextView) view.findViewById(R.id.eitime);
                                TextView group = (TextView) view.findViewById(R.id.eigroup);
                                ImageView icon = (ImageView) view.findViewById(R.id.eiicon);
                                startdt.setText(event.mStart != null ? df.format(event.mStart.getTime()) : "");
                                starttm.setText(event.mStart != null && event.mMine? tf.format(event.mStart.getTime()) : "");
                                group.setText(event.mGroup);
                                Picasso.with(getActivity()).load(event.mIconUrl).into(icon);
                                title.setText(event.mTitle);
                                location.setText(event.mLocation);
                                if (event.mSubscribed) {
                                    rsvp.setClickable(false);
                                    rsvp.setEnabled(false);
                                    rsvp.setText("Going");
                                } else
                                    rsvp.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            rsvp(event, true);
                                        }
                                    });
                                if (event.mSubscribed) rsvp.setClickable(false);
                                View.OnClickListener startweb = new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        showevent(event);
                                    }
                                };
                                title.setOnClickListener(startweb);
                                icon.setOnClickListener(startweb);
                                View.OnClickListener startmap = new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        showmap(event);
                                    }
                                };
                                if(event.mLocation!=null && event.mLocation.length()>0){
                                  locicon.setOnClickListener(startmap);
                                  location.setOnClickListener(startmap);
                                }
                            }
                            return view;
                        }
                    };
                    EventsFragment.this.setListAdapter(aa);
                    aa.notifyDataSetChanged();
                    if (needRefresh)
                        loadevents(true);
                } else if (!mIbot.passIsSet()) {
                    getActivity().startActivityForResult(new Intent(getActivity(), InPreferences.class), SETPASSWORD);
                }
            }
            private void showmap(InEvent event){
                Uri uri = Uri.parse("geo:0,0?q="+event.mLocation);
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }

            private void showevent(InEvent event) {
                LinkWorker worker = new LinkWorker();
                ((EventList)getActivity()).setProgressIndicator(true);
                worker.execute(event);
            }

            public void rsvp(InEvent event, boolean going) {
                if (going) {
                    if (event.mSubscribed) return;
                    EventList el = ((EventList) getActivity());
                    if (el != null && el.refresh != null)
                        el.refresh.setActionView(R.layout.actionbar_indeterminate_progress);
                    mNw = new NetWorker();
                    mNw.mEvent = event;
                    mNw.execute(going ? Operations.RSVPYES : Operations.RSVPNO);
                } else {
                    if (getActivity() != null && getActivity().getApplication() != null)
                        Toast.makeText(getActivity().getApplication().getBaseContext(), "Unsubscribe not implemented yet", Toast.LENGTH_SHORT).show();
                }
            }
            protected void onProgressUpdate() {
               // onPostExecute(true);
            }
            void refresh(){
                mIbot.readMyEvents(false);//will save everything later
                // publishProgress();
                mIbot.readMyGroups();
                mIbot.readGroupsEvents();
                mIbot.saveEvents(true);
                mIbot.saveGroups();
            }
            @Override
            protected Boolean doInBackground(Operations... ops) {
                if(ops[0]==Operations.LOAD){
                    mIbot.loadEvents();
                    mIbot.loadMyGroups();
                    needRefresh = mIbot.mEvents.isEmpty()||mIbot.isExpired(InternationsBot.Refreshkeys.EVENTS);
                    return  true;
                }
                needRefresh = false;
                if (mIbot.sign()) {
                    if (mIbot.mSigned)
                        switch (ops[0]) {
                            case LOAD:
                                mIbot.loadEvents();
                                mIbot.loadMyGroups();
                                break;
                            case REFRESH:
                                refresh();
                                break;
                            case RSVPNO:
                                if (mIbot.rsvp(mEvent, false))
                                    mIbot.readMyEvents(true);
                                break;
                            case RSVPYES:
                                if (mIbot.rsvp(mEvent, true))
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
