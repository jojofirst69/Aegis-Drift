package com.aegisdrift.bot

import android.app.Application
import android.util.Log
import com.aegisdrift.bot.data.AppDatabase
import com.aegisdrift.bot.data.PrefManager

class BotApplication : Application() {

    lateinit var prefs: PrefManager
    lateinit var db: AppDatabase

    override fun onCreate() {
        super.onCreate()
        prefs = PrefManager(this)
        db    = AppDatabase.getInstance(this)
        Log.i("BotApplication", "App started — demo=${prefs.isDemoMode}")
    }
}
