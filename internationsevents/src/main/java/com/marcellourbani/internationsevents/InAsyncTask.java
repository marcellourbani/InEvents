package com.marcellourbani.internationsevents;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.util.ArrayMap;

import java.util.ArrayList;

class InAsyncTask extends AsyncTask<String, Integer, Boolean> {


    private Intent mIntent;
    private Listener mListener;
    private ArrayList<InEvent> events;

    public InAsyncTask(Intent i) {
        mIntent = i;
    }

    public InAsyncTask setListener(Listener listener) {
        mListener = listener;
        return this;
    }

    @Override
    protected Boolean doInBackground(String... strings) {
        SharedPreferences prefs;
        prefs = PreferenceManager.getDefaultSharedPreferences(InApp.get());
        InternationsBot bot = new InternationsBot(prefs);
        bot.clearold();
        bot.loadEvents();
        bot.loadMyGroups();
        if (!bot.sign()) {
            if (bot.passIsSet() && mListener != null) mListener.onFailed(mIntent, InError.get());
            return false;
        }
        InError.get().clear();

        if (InService.ACTION_REFRESH_ALL.equals(mIntent.getAction())) {
            bot.readMyEvents(true, "");
            if (InError.isOk()) InCalendar.syncEvents(bot.mEvents);
            if (InError.isOk() && bot.isExpired(InternationsBot.Refreshkeys.GROUPS)) {
                bot.readMyGroups();
                if (InError.isOk()) bot.saveGroups();
            }
            if (InError.isOk()) bot.readGroupsEvents();
        } else if (InService.ACTION_REFRESH.equals(mIntent.getAction())) {
            bot.readMyEvents(true, "");
        } else if (InService.ACTION_SUBSCRIBE.equals(mIntent.getAction())
                || InService.ACTION_UNSUBSCRIBE.equals(mIntent.getAction())) {
            boolean newRSVP = InService.ACTION_SUBSCRIBE.equals(mIntent.getAction());
            InEvent event = readEvent();
            bot.rsvp(event, newRSVP);
            if (event == null || event.imGoing() != newRSVP)
                InError.get().add(InError.ErrSeverity.INFO,
                        InError.ErrType.UNKNOWN,
                        "Error " + (newRSVP ? "subscribing (list full?)" : "unsubscribing") + ", please try from website");
            else InError.get().add(InError.ErrSeverity.INFO,
                    InError.ErrType.UNKNOWN,
                    "Event " + (newRSVP ? "" : "un") + "subscribed successfully");
        } else if (InService.ACTION_LOAD.equals(mIntent.getAction())) {
            bot.loadEvents();
        }
        events = bot.getEvents();
        if (InError.isOk()) bot.saveEvents(true);
        return InError.isOk();
    }

    private InEvent readEvent() {
        ArrayMap<String, InEvent> events = InEvent.loadEvents();
        return events.get(mIntent.getStringExtra(InService.EVENTID));
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);
        if (mListener != null) mListener.onCompleted(mIntent, events, InError.get());
    }

    public interface Listener {
        void onCompleted(Intent intent, ArrayList<InEvent> events, InError error);

        void onFailed(Intent intent, InError error);
    }
}
