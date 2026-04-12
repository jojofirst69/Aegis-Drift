package com.aegisdrift.bot.data

import android.content.Context
import android.content.SharedPreferences

class PrefManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("aegis_prefs", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString("api_key", "") ?: ""
        set(v) = prefs.edit().putString("api_key", v).apply()

    var apiSecret: String
        get() = prefs.getString("api_secret", "") ?: ""
        set(v) = prefs.edit().putString("api_secret", v).apply()

    var apiPassphrase: String
        get() = prefs.getString("api_passphrase", "") ?: ""
        set(v) = prefs.edit().putString("api_passphrase", v).apply()

    var isDemoMode: Boolean
        get() = prefs.getBoolean("demo_mode", true)
        set(v) = prefs.edit().putBoolean("demo_mode", v).apply()

    var startBalance: Double
        get() = prefs.getFloat("start_balance", 100f).toDouble()
        set(v) = prefs.edit().putFloat("start_balance", v.toFloat()).apply()

    var currentEquity: Double
        get() = prefs.getFloat("current_equity", 100f).toDouble()
        set(v) = prefs.edit().putFloat("current_equity", v.toFloat()).apply()

    var isBotRunning: Boolean
        get() = prefs.getBoolean("bot_running", false)
        set(v) = prefs.edit().putBoolean("bot_running", v).apply()

    var totalTrades: Int
        get() = prefs.getInt("total_trades", 0)
        set(v) = prefs.edit().putInt("total_trades", v).apply()

    var winCount: Int
        get() = prefs.getInt("win_count", 0)
        set(v) = prefs.edit().putInt("win_count", v).apply()
}
