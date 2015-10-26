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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class InPreferences extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new PrefsFragment()).commit();
    }
    private static class PSL implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String s) {
            if(s!=null&&s.equals("pr_refresh_interval"))
                InService.schedule(true,false);
        }
    }
    public static class PrefsFragment extends PreferenceFragment {
        private PSL psl;
        SharedPreferences prefs;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if(getActivity()!=null){
                prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                if(psl==null)psl = new PSL();
                prefs.registerOnSharedPreferenceChangeListener(psl);
            }
            addPreferencesFromResource(R.xml.preferences);
            ListPreference calendarPref = (ListPreference) findPreference("pr_calendar");
            if(calendarPref!=null&&!InCalendar.addCalendarsPreferences(getActivity(),calendarPref))
                calendarPref.setEnabled(false);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if(prefs!=null)prefs.unregisterOnSharedPreferenceChangeListener(psl);
        }
    }
    @Override
    public void finish() {
        Intent data = new Intent();
        setResult(RESULT_OK, data);

        super.finish();
    }
}
