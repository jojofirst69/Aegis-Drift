package com.aegisdrift.bot.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aegisdrift.bot.MainActivity
import com.aegisdrift.bot.api.BitgetClient
import com.aegisdrift.bot.data.AppDatabase
import com.aegisdrift.bot.data.PrefManager
import com.aegisdrift.bot.data.TradingMode
import com.aegisdrift.bot.strategy.*
import kotlinx.coroutines.*

class TradingService : Service() {

    private val TAG        = "TradingService"
    private val CHANNEL_ID = "aegis_bot_channel"
    private val NOTIF_ID   = 1

    private val scope       = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var prefs:    PrefManager
    private lateinit var db:       AppDatabase
    private lateinit var client:   BitgetClient
    private lateinit var executor: TradeExecutor

    private val btcState = SymbolState("BTCUSDT")
    private val ethState = SymbolState("ETHUSDT")

    companion object {
        var isRunning = false
        val symbolStates = mutableMapOf<String, SymbolState>()
    }

    override fun onCreate() {
        super.onCreate()
        prefs    = PrefManager(this)
        db       = AppDatabase.getInstance(this)
        client   = BitgetClient(prefs.apiKey, prefs.apiSecret, prefs.apiPassphrase, prefs.tradingMode == TradingMode.DEMO)
        executor = TradeExecutor(client, db, prefs)
        symbolStates["BTCUSDT"] = btcState
        symbolStates["ETHUSDT"] = ethState
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("Bot running — BTC & ETH"))
        isRunning          = true
        prefs.isBotRunning = true
        startBotLoop()
        Log.i(TAG, "TradingService started")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        isRunning          = false
        prefs.isBotRunning = false
        Log.i(TAG, "TradingService stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startBotLoop() {
        scope.launch {
            while (isActive) {
                try {
                    val btcJob = async { procesSymbol(btcState, "1H") }
                    val ethJob = async { procesSymbol(ethState, "1H") }
                    btcJob.await()
                    ethJob.await()
                    updateNotification()
                } catch (e: Exception) {
                    Log.e(TAG, "Bot loop error: ${e.message}")
                }
                delay(60_000L)
            }
        }
    }

    private suspend fun procesSymbol(state: SymbolState, granularity: String) {
        val rawCandles = client.fetchCandles(state.symbol, granularity, 200)
        if (rawCandles.size < PIVOT_LEN * 2 + 2) {
            Log.w(TAG, "${state.symbol} not enough candles"); return
        }

        val bars = rawCandles.map {
            Bar(it.openTime, it.open, it.high, it.low, it.close, it.volume)
        }

        state.currentPrice = bars.last().close
        state.lastUpdate   = System.currentTimeMillis()

        val result         = StrategyEngine.getSignal(bars, state.position)
        state.currentAvwap = result.avwap
        state.anchorSide   = result.anchorSide
        state.anchorStop   = result.anchorStop

        if (state.position == 1 && state.currentPrice <= state.stopPrice) {
            executor.closePosition(state, state.currentPrice, "stop_loss"); return
        }
        if (state.position == -1 && state.currentPrice >= state.stopPrice) {
            executor.closePosition(state, state.currentPrice, "stop_loss"); return
        }

        when (result.signal) {
            Signal.LONG_ENTRY  -> executor.openLong(state, state.currentPrice, result.anchorStop)
            Signal.SHORT_ENTRY -> executor.openShort(state, state.currentPrice, result.anchorStop)
            Signal.LONG_EXIT   -> executor.closePosition(state, state.currentPrice, "signal_exit")
            Signal.SHORT_EXIT  -> executor.closePosition(state, state.currentPrice, "signal_exit")
            Signal.NONE        -> Log.d(TAG, "${state.symbol} no signal")
        }
    }

    private fun createNotificationChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "Aegis Bot", NotificationManager.IMPORTANCE_LOW)
        ch.description = "Auto trading bot status"
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⚡ Aegis Drift Bot")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val btcPnl = "BTC: ${"%.2f".format(btcState.sessionPnl)}$"
        val ethPnl = "ETH: ${"%.2f".format(ethState.sessionPnl)}$"
        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, buildNotification("$btcPnl | $ethPnl"))
    }
}
