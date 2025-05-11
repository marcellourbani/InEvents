package com.marcellourbani.internationsevents.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object Serializer {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val userAdapter = moshi.adapter(User::class.java)
    val eventResponseAdapter = moshi.adapter(EventResponse::class.java)
    val groupResponseAdapter = moshi.adapter(GroupResponse::class.java)
}