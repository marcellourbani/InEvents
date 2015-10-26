package com.marcellourbani.internationsevents;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.util.ArrayMap;

/**
 * Created by Marcello on 25/10/2015.
 */
class InAsyncTask extends AsyncTask<String, Integer, Boolean> {


    private Intent mIntent;
    private Listener mListener;

    public InAsyncTask(Intent i) {
        mIntent = i;
    }
    public InAsyncTask setListener(Listener listener){
        mListener=listener;
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
            if (bot.passIsSet()&&mListener!=null) mListener.onFailed(mIntent);
            return false;
        }
        InError.get().clear();
        switch (mIntent.getAction()) {
            case InService.ACTION_REFRESH_ALL:
                bot.readMyEvents(true, "");
                if (InError.isOk()) InCalendar.syncEvents(bot.mEvents);
                if (InError.isOk() && bot.isExpired(InternationsBot.Refreshkeys.GROUPS)) {
                    bot.readMyGroups();
                    if (InError.isOk()) bot.saveGroups();
                }
                if (InError.isOk()) bot.readGroupsEvents();
                break;
            case InService.ACTION_REFRESH:
                bot.readMyEvents(true, "");
                break;
            case InService.ACTION_SUBSCRIBE:
            case InService.ACTION_UNSUBSCRIBE:
                boolean newRSVP = mIntent.getAction() == InService.ACTION_SUBSCRIBE;
                InEvent event = readEvent();
                bot.rsvp(event, newRSVP);
                if (event == null || event.imGoing() != newRSVP)
                    InError.get().add(InError.ErrSeverity.INFO,
                            InError.ErrType.UNKNOWN,
                            "Error " + (newRSVP ? "subscribing (list full?)" : "unsubscribing") + ", please try from website");
                else InError.get().add(InError.ErrSeverity.INFO,
                        InError.ErrType.UNKNOWN,
                        "Event " + (newRSVP ? "" : "un") + "subscribed successfully");
                break;
            case InService.ACTION_LOAD:
                bot.loadEvents();
                break;
        }

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
        if(mListener!=null) mListener.onCompleted(mIntent);
    }
    public interface Listener{
       void onCompleted(Intent intent);
       void onFailed(Intent intent);
    }
}
