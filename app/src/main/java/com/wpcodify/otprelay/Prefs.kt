package com.wpcodify.otprelay

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    fun get(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences("otp_relay", Context.MODE_PRIVATE)
}
