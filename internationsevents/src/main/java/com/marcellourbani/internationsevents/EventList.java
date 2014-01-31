package com.marcellourbani.internationsevents;

import android.content.Context;
import android.net.Uri;
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
import android.widget.ListAdapter;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class EventList extends ActionBarActivity implements ActionBar.OnNavigationListener {

    /**
     * The serialization (saved instance state) Bundle key representing the
     * current dropdown position.
     */
    private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);

        // Set up the action bar to show a dropdown list.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        // Set up the dropdown list navigation in the action bar.
        actionBar.setListNavigationCallbacks(
                // Specify a SpinnerAdapter to populate the dropdown list.
                new ArrayAdapter<String>(
                        actionBar.getThemedContext(),
                        android.R.layout.simple_list_item_1,
                        android.R.id.text1,
                        new String[]{
                                getString(R.string.title_future),
                                getString(R.string.title_subscribed),
                        }),
                this);
    }

    @Override
    public void onRestoreInstanceState( Bundle savedInstanceState) {
        // Restore the previously serialized current dropdown position.
        if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
            getSupportActionBar().setSelectedNavigationItem(
                    savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Serialize the current dropdown position.
        outState.putInt(STATE_SELECTED_NAVIGATION_ITEM,
                getSupportActionBar().getSelectedNavigationIndex());
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
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        // When the given dropdown item is selected, show its contents in the
        // container view.
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, EventsFragment.newInstance(position + 1))
                .commit();
        return true;
    }

    public static class EventsFragment extends ListFragment {
        protected static InternationsBot mIbot;
        private NetWorker mNw;
        private static final String ARG_SECTION_NUMBER = "section_number";
        public static EventsFragment newInstance(int sectionNumber) {
            EventsFragment fragment = new EventsFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }
        public EventsFragment() {
        }
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            mIbot = new InternationsBot(PreferenceManager.getDefaultSharedPreferences(getActivity()));
            mNw = new NetWorker();
            View rootView = inflater.inflate(R.layout.fragment_event_list, container, false);
//            TextView textView = (TextView) rootView.findViewById(R.id.);
//            textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
            mNw.execute("1");
            return rootView;
        }
    private class NetWorker extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPostExecute(Boolean o) {
            super.onPostExecute(o);
            if(o){
                ArrayAdapter<InEvent> aa = new ArrayAdapter<InEvent>(getActivity(), R.layout.fragment_event_list, mIbot.getEvents()) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = convertView;
                        if(view==null){
                            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            view = inflater.inflate(R.layout.event_item,null);
                        }
                        InEvent event = getItem(position);
                        if (event!=null){
                            DateFormat df = new SimpleDateFormat("dd.MM.yy kk:mm");
                            TextView title = (TextView) view.findViewById(R.id.eititle);
                            TextView location = (TextView) view.findViewById(R.id.eilocation);
                            TextView rsvp = (TextView) view.findViewById(R.id.eirsvp);
                            TextView start = (TextView) view.findViewById(R.id.eistart);
                            TextView stop = (TextView) view.findViewById(R.id.eistop);
                            TextView group = (TextView) view.findViewById(R.id.eigroup);
                            ImageView icon = (ImageView) view.findViewById(R.id.eiicon);
                            rsvp.setText(event.mSubscribed?"Going":"Not going");
                            start.setText(event.mStart != null ? df.format(event.mStart.getTime()) : "");
                            stop.setText(event.mStop!=null?df.format(event.mStop.getTime()):"");
                            group.setText("");
                              //  icon.setImageURI(new URI(event.mIconUrl));
                            title.setText(event.mTitle);
                            location.setText(event.mLocation);
                        }
                        return view;
                    }
                };
                EventsFragment.this.setListAdapter(aa);
                aa.notifyDataSetChanged();
            }
            else
                startActivity( new Intent(getActivity(),InPreferences.class));
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
