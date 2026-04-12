package com.aegisdrift.bot.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aegisdrift.bot.data.PrefManager

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = PrefManager(context)

        // Only auto-restart if bot was running before phone rebooted
        if (prefs.isBotRunning) {
            Log.i("BootReceiver", "Phone rebooted — restarting trading bot")
            val serviceIntent = Intent(context, TradingService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
