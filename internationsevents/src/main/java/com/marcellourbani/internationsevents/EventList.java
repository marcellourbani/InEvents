package com.marcellourbani.internationsevents;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.provider.Browser;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
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

    protected enum Operations {LOAD, RSVPYES, RSVPNO}

    ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //TODO? no refresh when network not connected
        //TODO service
        //TODO? intent filter
        //TODO? store events in database for offline access
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.event_list, menu);
        refresh = menu.findItem(R.id.action_refresh);
        mFrag.loadevents();
        return true;
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
                mFrag.loadevents();
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
            View rootView = inflater.inflate(R.layout.fragment_event_list, container, false);
            return rootView;
        }

        public void loadevents() {
            EventList el = ((EventList) getActivity());
            if (el != null && el.refresh != null)
                el.refresh.setActionView(R.layout.actionbar_indeterminate_progress);
            mNw = new NetWorker();
            mNw.execute(Operations.LOAD);
        }

        public void rsvp(InEvent event, boolean going) {
            if (going) {
                if(event.mSubscribed)return;
                EventList el = ((EventList) getActivity());
                if (el != null && el.refresh != null)
                    el.refresh.setActionView(R.layout.actionbar_indeterminate_progress);
                mNw = new NetWorker();
                mNw.mEvent = event;
                mNw.execute(going ? Operations.RSVPYES : Operations.RSVPNO);
            } else {
                if(getActivity()!=null&&getActivity().getApplication()!=null)
                Toast.makeText(getActivity().getApplication().getBaseContext(), "Unsubscribe not implemented yet",Toast.LENGTH_SHORT).show();
            }
        }

        private class NetWorker extends AsyncTask<Operations, Void, Boolean> {
            InEvent mEvent = null;

            @Override
            protected void onPostExecute(Boolean o) {
                super.onPostExecute(o);
                EventList el = ((EventList) getActivity());
                if (el != null && el.refresh != null)
                    el.refresh.setActionView(null);
                if (o) {
                    for (InEvent event : mIbot.getEvents()) {
                        if (event.mSubscribed)
                            InCalendar.modifyEvent(getActivity(), event);
                    }
                    ArrayAdapter<InEvent> aa = new ArrayAdapter<InEvent>(getActivity(), R.layout.fragment_event_list, mIbot.getEvents()) {
                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            View view = convertView;
                            if (view == null) {
                                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                view = inflater.inflate(R.layout.event_item, null);
                            }
                            final InEvent event = getItem(position);
                            if (event != null) {
                                DateFormat df = new SimpleDateFormat("dd.MM.yy kk:mm");
                                TextView title = (TextView) view.findViewById(R.id.eititle);
                                TextView location = (TextView) view.findViewById(R.id.eilocation);
                                CheckBox rsvp = (CheckBox) view.findViewById(R.id.eigoing);
                                TextView start = (TextView) view.findViewById(R.id.eistart);
                                TextView group = (TextView) view.findViewById(R.id.eigroup);
                                ImageView icon = (ImageView) view.findViewById(R.id.eiicon);
                                rsvp.setText(event.mSubscribed ? "Going" : "Not going");
                                rsvp.setChecked(event.mSubscribed);
                                start.setText(event.mStart != null ? df.format(event.mStart.getTime()) : "");
                                group.setText(event.mGroup);
                                Picasso.with(getActivity()).load(event.mIconUrl).into(icon);
                                title.setText(event.mTitle);
                                location.setText(event.mLocation);
                                rsvp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                    @Override
                                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                        rsvp(event, b);
                                    }
                                });
                                if(event.mSubscribed) rsvp.setClickable(false);
                                title.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(event.mEventUrl));
                                        Bundle bundle = new Bundle();
                                        String cookies = mIbot.getCookies();
                                        bundle.putString("Cookie", cookies);
                                        i.putExtra(Browser.EXTRA_HEADERS, bundle);
                                        startActivity(i);
                                    }
                                });
                            }
                            return view;
                        }
                    };
                    EventsFragment.this.setListAdapter(aa);
                    aa.notifyDataSetChanged();
                } else if (!mIbot.passIsSet()) {
                    startActivity(new Intent(getActivity(), InPreferences.class));
                }
            }

            @Override
            protected Boolean doInBackground(Operations... ops) {
                if (mIbot.sign()) {
                    if (mIbot.mSigned)
                        switch (ops[0]) {
                            case LOAD:
                                mIbot.readMyEvents();
                                break;
                            case RSVPNO:
                                if (mIbot.rsvp(mEvent, false))
                                    mIbot.readMyEvents();
                                break;
                            case RSVPYES:
                                if (mIbot.rsvp(mEvent, true))
                                    mIbot.readMyEvents();
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
