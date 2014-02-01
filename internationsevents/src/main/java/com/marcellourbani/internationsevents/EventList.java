package com.marcellourbani.internationsevents;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.ListFragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class EventList extends ActionBarActivity {
    protected static InternationsBot mIbot;
    private static EventsFragment mFrag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIbot = new InternationsBot(PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_event_list);
        mFrag = new EventsFragment(mIbot);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, mFrag)
                    .commit();
        }
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    @Override
                    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                        if (s.equals("pr_password"))
                            mFrag.loadevents();
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.event_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, InPreferences.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class EventsFragment extends ListFragment {
        protected static InternationsBot mIbot;
        private NetWorker mNw;

        public EventsFragment(InternationsBot bot) {
            mIbot = bot;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_event_list, container, false);
            loadevents();
            return rootView;
        }

        public void loadevents() {
            if (mNw == null) mNw = new NetWorker();
            mNw.execute("");
        }

        private class NetWorker extends AsyncTask<String, Void, Boolean> {
            @Override
            protected void onPostExecute(Boolean o) {
                super.onPostExecute(o);
                if (o) {
                    ArrayAdapter<InEvent> aa = new ArrayAdapter<InEvent>(getActivity(), R.layout.fragment_event_list, mIbot.getEvents()) {
                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            View view = convertView;
                            if (view == null) {
                                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                view = inflater.inflate(R.layout.event_item, null);
                            }
                            InEvent event = getItem(position);
                            if (event != null) {
                                DateFormat df = new SimpleDateFormat("dd.MM.yy kk:mm");
                                TextView title = (TextView) view.findViewById(R.id.eititle);
                                TextView location = (TextView) view.findViewById(R.id.eilocation);
                                TextView rsvp = (TextView) view.findViewById(R.id.eirsvp);
                                TextView start = (TextView) view.findViewById(R.id.eistart);
                                TextView stop = (TextView) view.findViewById(R.id.eistop);
                                TextView group = (TextView) view.findViewById(R.id.eigroup);
                                ImageView icon = (ImageView) view.findViewById(R.id.eiicon);
                                rsvp.setText(event.mSubscribed ? "Going" : "Not going");
                                start.setText(event.mStart != null ? df.format(event.mStart.getTime()) : "");
                                stop.setText(event.mStop != null ? df.format(event.mStop.getTime()) : "");
                                group.setText(event.mGroup);
                                Picasso.with(getActivity()).load(event.mIconUrl).into(icon);
                                title.setText(event.mTitle);
                                location.setText(event.mLocation);
                            }
                            return view;
                        }
                    };
                    EventsFragment.this.setListAdapter(aa);
                    aa.notifyDataSetChanged();
                } else
                    startActivity(new Intent(getActivity(), InPreferences.class));
            }

            @Override
            protected Boolean doInBackground(String... strings) {
                if (mIbot.sign())
                    mIbot.readMyEvents();
                else
                    return false;
                return true;
            }
        }
    }
}
