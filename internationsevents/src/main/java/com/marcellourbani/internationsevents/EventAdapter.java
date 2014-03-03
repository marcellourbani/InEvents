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

import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

public class EventAdapter extends ArrayAdapter<InEvent> {
    final DateFormat df = new SimpleDateFormat("dd.MM.yy"),
            tf = new SimpleDateFormat("kk:mm");
    private final List<InEvent> events;
    EventList eventList;

    public EventAdapter(Context context, int resource, List<InEvent> objects) {
        super(context, resource, objects);
        this.eventList = (EventList) context;
        events = objects;
    }
    private class Controls{
        Button rsvp;
        ImageView icon,locicon;
        InEvent event;
        TextView title,location,startdt,starttm,group;
        Controls(View view,InEvent event){
            title = (TextView) view.findViewById(R.id.eititle);
            location = (TextView) view.findViewById(R.id.eilocation);
            locicon = (ImageView) view.findViewById(R.id.eilocationic);
            rsvp = (Button) view.findViewById(R.id.eirsvp);
            startdt = (TextView) view.findViewById(R.id.eidate);
            starttm = (TextView) view.findViewById(R.id.eitime);
            group = (TextView) view.findViewById(R.id.eigroup);
            icon = (ImageView) view.findViewById(R.id.eiicon);
            setEvent(event);
        }
        void setEvent(InEvent ev){
            event = ev;
            if (event == null) {
                rsvp.setOnClickListener(null);
                title.setOnClickListener(null);
                icon.setOnClickListener(null);
                locicon.setOnClickListener(null);
                location.setOnClickListener(null);
            }else{
                startdt.setText(event.mStart != null ? df.format(event.mStart.getTime()) : "");
                starttm.setText(event.mStart != null && event.mMine ? tf.format(event.mStart.getTime()) : "");
                group.setText(event.mGroup);
                Picasso.with(eventList).load(event.mIconUrl).into(icon);
                title.setText(event.mTitle);
                location.setText(event.mLocation);
                if (event.imGoing()) {
                    rsvp.setText("Going");
                }
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
                if (event.mLocation != null && event.mLocation.length() > 0) {
                    locicon.setOnClickListener(startmap);
                    location.setOnClickListener(startmap);
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
            view.setTag(new Controls(view,event));
        }else
            ((Controls) view.getTag()).setEvent(event);
        return view;
    }
}
