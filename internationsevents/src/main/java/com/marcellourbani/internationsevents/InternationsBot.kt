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
package com.marcellourbani.internationsevents

import android.content.ContentValues
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import androidx.collection.ArrayMap
import com.marcellourbani.internationsevents.HttpClient.HttpClientException
import com.marcellourbani.internationsevents.data.*
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.json.JSONException
import org.jsoup.Jsoup
import java.io.IOException
import java.net.HttpCookie
import java.net.MalformedURLException
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

fun nowAsIso(): String {
    val tz = TimeZone.getTimeZone("UTC")
    val df: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    df.timeZone = tz
    return df.format(Date())
}

class InternationsBot(sharedPref: SharedPreferences) {
    private var mUser: String?
    private var mPass: String?
    private val mPref: SharedPreferences
    var mClient: HttpClient?
    @JvmField
    var mSigned = false
    @JvmField
    var mEvents: ArrayMap<Long, Event> = ArrayMap()
    var mGroups = ArrayMap<String, InGroup?>()
    private var mMyUser: User? = null

    enum class Refreshkeys {
        GROUPS, EVENTS, MYEVENTS;

        val limit: Long
            get() {
                val pref = PreferenceManager.getDefaultSharedPreferences(InApp.get())
                return when (this) {
                    GROUPS -> (Integer.valueOf(
                        pref.getString(
                            "pr_group_timeout",
                            "24"
                        )
                    ) * 3600000).toLong()
                    EVENTS -> (Integer.valueOf(
                        pref.getString(
                            "pr_grev_timeout",
                            "24"
                        )
                    ) * 3600000).toLong()
                    MYEVENTS -> (Integer.valueOf(
                        pref.getString(
                            "pr_myev_timeout",
                            "60"
                        )
                    ) * 60000).toLong()
                    else -> 24 * 3600000
                }
            }
        val key: Int
            get() {
                return when (this) {
                    GROUPS -> 1
                    EVENTS -> 2
                    MYEVENTS -> 3
                }
                return 0
            }
    }

    fun passIsSet(): Boolean {
        mUser = mPref.getString("pr_email", "")
        mPass = mPref.getString("pr_password", "")
        return mPass!!.length > 0
    }

    fun rsvp(event: InEvent, going: Boolean): Boolean {
        val RESULTP = Pattern.compile("\"success\":([a-z]*),\"message\":\"([^\"]*)\"")
        try {
            if (!sign()) return false
            val url = event.getRsvpUrl(going)
            val result: String
            val params = ArrayList<HttpClient.NameValuePair>()
            params.add(HttpClient.NameValuePair("_method", if (going) "PUT" else "DELETE"))
            result = mClient!!.post_formencoded(url, params)
            val m = RESULTP.matcher(result)
            return if (m.matches()) {
                InError.get().add(
                    InError.ErrSeverity.INFO,
                    InError.ErrType.NETWORK,
                    m.group(2)
                )
                if (m.group(1) == "true") {
                    event.set_attendance(going)
                    true
                } else {
                    false
                }
            } else {
                //InError.get().add(InError.ErrType.NETWORK, "Error changing RSVP, please try from browser.\n" );
                false
            }
        } catch (exception: HttpClientException) {
            InError.get().add(
                InError.ErrType.NETWORK,
                "Error changing RSVP (event closed?).\nPlease try over the website\n\n"
            )
        } catch (e: Throwable) {
            InError.get().add(
                InError.ErrType.NETWORK, """
     Error changing RSVP.
     network connection error
     
     ${e.message}
     """.trimIndent()
            )
            Log.d(INTAG, e.message!!)
        }
        return false
    }

    fun readMyGroups(): ArrayMap<String, InGroup?> {
        try {
            val gso =
                mClient!!.geturl_in_json("https://www.internations.org/api/activity-groups/my?limit=300&offset=0")
            var num = gso.getInt("total")
            val rawgr = gso.getJSONObject("_embedded").getJSONArray("self")
            if (num < rawgr.length()) num = rawgr.length()
            for (i in 0 until num) {
                val group = InGroup(rawgr.getJSONObject(i))
                if (group.mId != null) mGroups[group.mId] = group
            }
        } catch (e: Exception) {
            InError.get().add(
                InError.ErrType.NETWORK, """
     Error downloading my groups.
     ${e.message}
     """.trimIndent()
            )
            Log.d(INTAG, e.message!!)
        }
        return mGroups
    }

    fun loadMyGroups(): ArrayMap<String, InGroup?> {
        mGroups = InGroup.loadGroups()
        return mGroups
    }

    fun saveEvents(all: Boolean) {
//        for (e in mEvents!!.values) if (all || e.mMine) e.save()
        for (e in mEvents!!.values) saveEvent(e)
        writeRefresh(Refreshkeys.MYEVENTS)
        if (all) writeRefresh(Refreshkeys.EVENTS)
    }

    fun saveGroups() {
        val oldgroups = InGroup.loadGroups()
        for (g in oldgroups.values) if (mGroups[g.mId] == null) g.delete()
        for (g in mGroups.values) g!!.save()
        writeRefresh(Refreshkeys.GROUPS)
    }

    private fun writeRefresh(refk: Refreshkeys) {
        val db = InApp.get().db.wrdb
        val key = arrayOf(refk.key.toString())
        val values = ContentValues()
        values.put("id", refk.key)
        values.put("lastrun", Date().time)
        db.delete("refreshes", "id = ?", key)
        db.insert("refreshes", null, values)
    }

    fun isExpired(refk: Refreshkeys): Boolean {
        val db = InApp.get().db.rodb
        val key = arrayOf(refk.key.toString())
        val c = db.query(
            false,
            "refreshes",
            arrayOf("id", "lastrun"),
            "id=?",
            key,
            null,
            null,
            null,
            null
        )
        return if (c != null && c.moveToNext()) {
            val last = c.getLong(1)
            val now = Date().time
            val limit = refk.limit
            c.close()
            now - last > limit
        } else {
            c?.close()
            true
        }
    }

    fun loadEvents(): ArrayMap<Long, Event> {
        try {
            val events = loadEvents()
            if (mEvents == null) mEvents = events else for (event in events.values) {
                addOrUpdateEvent(event, true)
            }
        } catch (e: Exception) {
            InError.get().add(
                InError.ErrType.DATABASE, """
     Error loading events from database
     ${e.message}
     """.trimIndent()
            )
        }
        return mEvents
    }

    fun clearold() {
        InEvent.clearold()
    }

    private fun extractTable(source: String?, id: String): String? {
        val m = Pattern.compile("(<table[^>]*$id.*/table>)", Pattern.DOTALL).matcher(source)
        return if (m.find()) {
            val s = m.group(1)
            s.substring(0, s.indexOf("/table>") + 7)
        } else null
    }

    private fun extractDiv(source: String?, clas: String, nextclass: String): String? {
        val start: Int
        var end = 0
        val startm = Pattern.compile("(?s)<div[^>]*class=[^>=]*$clas").matcher(source)
        val endm = Pattern.compile("(?s)<div[^>]*class=[^>=]*$nextclass").matcher(source)
        start = if (startm.find()) {
            startm.start()
        } else return null
        if (endm.find()) {
            end = endm.start() - 1
        }
        return if (end > start) source!!.substring(start, end) else source!!.substring(start)
    }

    @Throws(JSONException::class, IOException::class)
    private fun readMyUser(): User? {
        val me = mClient!!.geturl_string(BASEURL + "/api/users/current")
        return Serializer.userAdapter.fromJson(me)
    }

    @Throws(JSONException::class, IOException::class)
    private fun readMyInvitations(): List<Event> {
        val me =
            mClient!!.geturl_string(BASEURL + "/api/calendar-entries/invitations?offset=0&limit=100&pending=true")
        val response = Serializer.eventResponseAdapter.fromJson(me) ?: return listOf()
        return response._embedded.self
    }

    @Throws(JSONException::class, IOException::class)
    private fun readMyGroups2(): List<Group> {
        val me =
            mClient!!.geturl_string(BASEURL + "/api/activity-groups/my?limit=100&offset=0")
        val response = Serializer.groupResponseAdapter.fromJson(me) ?: return listOf()
        return response._embedded.self
    }


    @Throws(JSONException::class, IOException::class)
    private fun readMyEvents(): List<Event> {
        val url = BASEURL + "/api/calendar-entries?offset=0&limit=100&types[]=event&types[]=activity&orderBy=startsOn&endsAfter="+
                nowAsIso()+"&attendeeId="+mMyUser?.id.toString()+"&orderDirection=asc&locality=global"
        val me = mClient!!.geturl_string(url)
        val response = Serializer.eventResponseAdapter.fromJson(me) ?: return listOf()
        return response._embedded.self
    }
    fun readMyEvents(save: Boolean, torefresh: String) {

//        final String DIVCLASS = "js-calendar-my-events", NEXTDIVCLAS = "t-recommended-events";
        val DIVCLASS = "js-calendar-your-invitations"
        val DIVCLASS2 = "js-calendar-my-events"
        val NEXTDIVCLAS = "t-recommended-events"
        var divclass = DIVCLASS
        try {
            mMyUser = readMyUser()
            val invitations = readMyInvitations()
            val myevents = readMyEvents()
            var groups = readMyGroups2()
            val events = ArrayMap<String, InEvent?>()
            var ev = mClient!!.geturl_string(MYEVENTSURL)
            var evtab = extractTable(ev, "my_upcoming_events_table")
            if (evtab != null && evtab.length > 0) ev = evtab else evtab = null
            if (evtab == null) {
                var e = extractDiv(ev, DIVCLASS, NEXTDIVCLAS)
                if (e == null) {
                    e = extractDiv(ev, DIVCLASS2, NEXTDIVCLAS)
                    divclass = DIVCLASS2
                }
                ev = e
            }
            if (ev != null) {
                val doc = Jsoup.parse(ev)
                val elements =
                    if (evtab == null) doc.select("div.$divclass div.t-calendar-entry") else doc.select(
                        "#my_upcoming_events_table tbody tr"
                    )
                for (evel in elements) {
                    try {
                        val event = InEvent(evel, evtab == null)
                        if (!event.isExpired) {
                            if (needRefine(event, torefresh)) refineEvent(event)
//                            addOrUpdateEvent(event)
                            events[event.mEventId] = event
                        }
                    } catch (e: MalformedURLException) {
                        InError.get()
                            .add(InError.ErrType.PARSE, "Error parsing my events URL" + e.message)
                        Log.d(INTAG, e.message!!)
                    } catch (e: ParseException) {
                        InError.get()
                            .add(InError.ErrType.PARSE, "Error parsing my events" + e.message)
                        Log.d(INTAG, e.message!!)
                    }
                }
            }
            if (!InError.isOk()) return
            //reset attendance if required
//            for (e in mEvents!!.values) {
//                if (e.imGoing() && events[e.mEventId] == null) {
//                    e.set_attendance(false)
//                    events[e.mEventId] = e
//                }
//            }
            if (save) {
                for (e in events.values) e!!.save()
                writeRefresh(Refreshkeys.MYEVENTS)
            }
        } catch (e: IOException) {
            InError.get().add(InError.ErrType.NETWORK, """
     Error downloading my events.
     ${e.message}
     """.trimIndent())
            Log.d(INTAG, e.message!!)
        } catch (e: Exception) {
            InError.get().add( InError.ErrType.UNKNOWN, """
     Error downloading my events.
     ${e.message}
     """.trimIndent()
            )
            Log.d(INTAG, e.message!!)
        }
    }

    @Throws(Exception::class)
    private fun refineEvent(event: InEvent) {
        var ev = event.refineUrl
        ev = mClient!!.geturl_string(ev)
        event.refine(ev)
    }

    private fun addOrUpdateEvent(event: Event, fromdb: Boolean = false) {
        val old = mEvents!![event.id]
        if (old == null) {
            mEvents!![event.id] = event
        } else  //the 'my events' view gives more details, do not overwrite if comes from there and this doesn't
            if (event.mLocation != null || old.mLocation == null) old.merge(event)
    }

    fun readGroupsEvents() {
        var group: InGroup? = null
        try {
            for (grp in mGroups.values) {
                group = grp
                val url =
                    BASEURL + "/api/activity-groups/" + group!!.mId + "/activities/upcoming?limit=100&offset=0"
                val jsevents =
                    mClient!!.geturl_in_json(url).getJSONObject("_embedded").getJSONArray("self")
                for (i in 0 until jsevents.length()) {
                    val event = InEvent(jsevents.getJSONObject(i))
                    val old = mEvents!![event.mEventId]
                    if (old == null) {
                        refineEvent(event)
                        addOrUpdateEvent(event)
                    }
                }
            }
        } catch (e: IOException) {
            InError.get().add(
                InError.ErrType.NETWORK, """
     Error downloading events for group ${group!!.mDesc}.
     ${e.message}
     """.trimIndent()
            )
            Log.d(INTAG, e.message!!)
        } catch (e: Exception) {
            val txt: String
            txt = if (group == null) "Unknown" else group.mDesc
            InError.get().add(
                InError.ErrType.NETWORK, """
     Error parsing events for group $txt.
     ${e.message}
     """.trimIndent()
            )
            Log.d(INTAG, e.message!!)
        }
    }

    //returns true if the web transaction was successful, sets mSigned based on the outcome
    fun sign(): Boolean {
        if (!mSigned) {
            if (passIsSet()) try {
                val parms: MutableList<HttpClient.NameValuePair> = ArrayList()
                parms.add(HttpClient.NameValuePair("user_email", mUser))
                parms.add(HttpClient.NameValuePair("user_password", mPass))
                parms.add(HttpClient.NameValuePair("remember_me", "1"))
                val signoutcome = mClient!!.login(SIGNUPURL, parms)
                mSigned =
                    signoutcome.indexOf("Redirecting to https://www.internations.org/login/") <= 0
                if (!mSigned) InError.get()
                    .add(InError.ErrType.LOGIN, "Error signing in, check your user and password")
            } catch (e: Throwable) {
                mSigned = false
                InError.get().add(
                    InError.ErrType.NETWORK, """
     Error signing in, check your network connection.
     ${e.message}
     """.trimIndent()
                )
                return false
            }
        }
        return true
    }

    val events: ArrayList<InEvent>
        get() {
            val events = ArrayList<InEvent>()
            events.addAll(mEvents!!.values)
            Collections.sort(events) { e1, e2 -> e1.mStart.compareTo(e2.mStart) }
            return events
        }
    val cookies: Bundle
        get() {
            val b = Bundle()
            var cookies: List<HttpCookie> = mClient?.cookies?:listOf()
            for (cookie in cookies) b.putString(
                cookie.name,
                cookie.value + "" + "; domain=" + cookie.domain
            )
            return b
        }

    companion object {
        const val BASEURL = "https://www.internations.org"
        const val MESSAGEURL = "https://www.internations.org/message/?ref=he_msg"
        private const val MYEVENTSURL = "http://www.internations.org/events/my?ref=he_ev_me"
        private const val SIGNUPURL = "https://www.internations.org/security/do-login/?v=2"
        const val INTAG = "IN_EVENTS"
        const val ALLEVENTS = "ALLEVENTS"
    }

    init {
        mClient = HttpClient()
        mPref = sharedPref
        mUser = sharedPref.getString("pr_email", "")
        mPass = sharedPref.getString("pr_password", "")
    }
}