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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.collection.ArrayMap;

import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EventAdapter extends ArrayAdapter<InEvent> {
    final static DateFormat DF = new SimpleDateFormat("dd.MM.yy"),
            TF = new SimpleDateFormat("kk:mm");
    private final List<InEvent> events;
    EventList eventList;

    public EventAdapter(Context context, int resource, List<InEvent> objects) {
        super(context, resource, objects);
        this.eventList = (EventList) context;
        events = objects;
    }

    public static EventAdapter create(Context context, int resource, ArrayMap<String, InEvent> evmap) {
        ArrayList<InEvent> eventlist = new ArrayList<>();
        return new EventAdapter(context,resource,eventlist).updateEvents(evmap);
    }

    public EventAdapter updateEvents(ArrayMap<String, InEvent> eventmap) {
        events.clear();
        events.addAll(eventmap.values());
        Collections.sort(events, new Comparator<InEvent>() {
            @Override
            public int compare(InEvent e1, InEvent e2) {
                return e1.mStart.compareTo(e2.mStart);
            }
        });
        this.notifyDataSetChanged();
        return this;
    }

    private class Controls{
        Button rsvp;
        ImageView icon,newicon;
        InEvent event;
        TextView title,location,startdt,starttm,group;
        Controls(View view,InEvent event){
            title = view.findViewById(R.id.eititle);
            location = view.findViewById(R.id.eilocation);
            rsvp = view.findViewById(R.id.eirsvp);
            startdt = view.findViewById(R.id.eidate);
            starttm = view.findViewById(R.id.eitime);
            group = view.findViewById(R.id.eigroup);
            icon = view.findViewById(R.id.eiicon);
            newicon = view.findViewById(R.id.einew);
            setEvent(event);
        }
        void setEvent(InEvent ev){
            event = ev;
            if (event == null) {
                rsvp.setOnClickListener(null);
                title.setOnClickListener(null);
                icon.setOnClickListener(null);
                location.setOnClickListener(null);
            }else{
                startdt.setText(event.mStart != null ? DF.format(event.mStart.getTime()) : "");
                starttm.setText(event.mStart != null && event.mMine ? TF.format(event.mStart.getTime()) : "");
                if(event.isEvent())group.setVisibility(View.GONE);
                else {
                    group.setText(event.mGroup);
                    group.setVisibility(View.VISIBLE);
                }
                Picasso.get().load(event.mIconUrl).into(icon);
                title.setText(event.mTitle);
                location.setText(event.mLocation);
                rsvp.setText(event.getRsvpText());
                rsvp.setEnabled(event.rsvpChangeable());
                rsvp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        eventList.rsvp(event, !event.imGoing());
                    }
                });
                View.OnClickListener startweb = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        eventList.showevent(event);
                    }
                };
                title.setOnClickListener(startweb);
                icon.setOnClickListener(startweb);
                View.OnClickListener startmap = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        eventList.showmap(event);
                    }
                };
                newicon.setVisibility(event.addedrecently()?View.VISIBLE:View.INVISIBLE);
                if (event.mLocation != null && event.mLocation.length() > 0) {
                    location.setOnClickListener(startmap);
                    location.setVisibility(View.VISIBLE);
                }else{
                    location.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        final InEvent event = getItem(position);
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.event_item, null);
            if (view != null) {
                view.setTag(new Controls(view,event));
            }
        }else
            ((Controls) view.getTag()).setEvent(event);
        return view;
    }
}
