package com.aegisdrift.bot.data

import android.content.Context
import android.content.SharedPreferences

// Trading modes
object TradingMode {
    const val PAPER  = "PAPER"   // fully local, no API needed
    const val DEMO   = "DEMO"    // Bitget paper trading via API
    const val LIVE   = "LIVE"    // real money via API
}

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

    // Single trading mode replaces isDemoMode + isPaperMode
    var tradingMode: String
        get() = prefs.getString("trading_mode", TradingMode.PAPER) ?: TradingMode.PAPER
        set(v) = prefs.edit().putString("trading_mode", v).apply()

    var startBalance: Double
        get() = prefs.getFloat("start_balance", 100f).toDouble()
        set(v) = prefs.edit().putFloat("start_balance", v.toFloat()).apply()

    var currentEquity: Double
        get() = prefs.getFloat("current_equity", 100f).toDouble()
        set(v) = prefs.edit().putFloat("current_equity", v.toFloat()).apply()

    // 🔥 FIXED: These 2 methods for TradingService equity reset
    fun getStartBalance(): Double = prefs.getFloat("start_balance", 100.0f).toDouble()
    
    fun saveEquity(amount: Double) {
        edit { putFloat("current_equity", amount.toFloat()) }
    }

    var isBotRunning: Boolean
        get() = prefs.getBoolean("bot_running", false)
        set(v) = prefs.edit().putBoolean("bot_running", v).apply()

    var totalTrades: Int
        get() = prefs.getInt("total_trades", 0)
        set(v) = prefs.edit().putInt("total_trades", v).apply()

    var winCount: Int
        get() = prefs.getInt("win_count", 0)
        set(v) = prefs.edit().putInt("win_count", v).apply()

    // Helper inline function for cleaner edits
    private inline fun edit(block: SharedPreferences.Editor.() -> Unit) {
        val editor = prefs.edit()
        editor.block()
        editor.apply()
    }
}
