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
        client = BitgetClient(prefs.apiKey, prefs.apiSecret, prefs.apiPassphrase, prefs.tradingMode)

        executor = TradeExecutor(client, db, prefs)
        symbolStates["BTCUSDT"] = btcState
        symbolStates["ETHUSDT"] = ethState
        
        // FIX 1: Reset equity on service start
        resetEquityOnStart()
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

    // FIX 2: Reset equity properly
    private fun resetEquityOnStart() {
        val startBalance = prefs.startBalance

        btcState.equity = startBalance
        ethState.equity = startBalance
        btcState.sessionPnl = 0.0
        ethState.sessionPnl = 0.0
        prefs.saveEquity(startBalance)
        Log.i(TAG, "Equity reset to $startBalance")
    }

    // FIX 3: Separate price update loop (10s) + strategy loop (60s)
    private fun startBotLoop() {
        // Price update every 10 seconds
        scope.launch {
            while (isActive) {
                try {
                    updatePricesOnly()
                } catch (e: Exception) {
                    Log.e(TAG, "Price update failed", e)
                }
                delay(10_000L) // 10 seconds
            }
        }
        
        // Strategy decisions every 60 seconds
        scope.launch {
            while (isActive) {
                try {
                    procesSymbol(btcState, "1H")
                    procesSymbol(ethState, "1H")
                    updateNotification()
                } catch (e: Exception) {
                    Log.e(TAG, "Strategy loop error", e)
                }
                delay(60_000L)
            }
        }
    }

    // FIX 4: Price-only update (no strategy)
    private suspend fun updatePricesOnly() {
        try {
            val btcPrice = client.fetchTicker("BTCUSDT")
            val ethPrice = client.fetchTicker("ETHUSDT")
            
            btcState.currentPrice = btcPrice
            ethState.currentPrice = ethPrice
            btcState.lastUpdate = System.currentTimeMillis()
            ethState.lastUpdate = System.currentTimeMillis()
            
            Log.d(TAG, "Prices updated: BTC=${btcState.currentPrice}, ETH=${ethState.currentPrice}")
        } catch (e: Exception) {
            Log.e(TAG, "Price fetch failed", e)
        }
    }

    private suspend fun procesSymbol(state: SymbolState, granularity: String) {
        try {
            // FIX 5: Skip historical backfill on first run
            if (state.lastUpdate == 0L) {
                Log.i(TAG, "${state.symbol} skipping backfill - live mode only")
                return
            }

            val rawCandles = client.fetchCandles(state.symbol, granularity, 50) // Reduced from 200
            if (rawCandles.size < PIVOT_LEN * 2 + 2) {
                Log.w(TAG, "${state.symbol} not enough candles"); return
            }

            val bars = rawCandles.map {
                Bar(it.openTime, it.open, it.high, it.low, it.close, it.volume)
            }

            state.currentPrice = bars.last().close
            state.lastUpdate   = System.currentTimeMillis()

            val result = StrategyEngine.getSignal(bars, state.position)
            state.currentAvwap = result.avwap
            state.anchorSide   = result.anchorSide
            state.anchorStop   = result.anchorStop

            // Stop loss checks
            if (state.position == 1 && state.currentPrice <= state.stopPrice) {
                executor.closePosition(state, state.currentPrice, "stop_loss")
                return
            }
            if (state.position == -1 && state.currentPrice >= state.stopPrice) {
                executor.closePosition(state, state.currentPrice, "stop_loss")
                return
            }

            // Entry/exit signals (only live data)
            when (result.signal) {
                Signal.LONG_ENTRY  -> executor.openLong(state, state.currentPrice, result.anchorStop)
                Signal.SHORT_ENTRY -> executor.openShort(state, state.currentPrice, result.anchorStop)
                Signal.LONG_EXIT   -> executor.closePosition(state, state.currentPrice, "signal_exit")
                Signal.SHORT_EXIT  -> executor.closePosition(state, state.currentPrice, "signal_exit")
                Signal.NONE        -> Log.d(TAG, "${state.symbol} no signal")
            }
        } catch (e: Exception) {
            Log.e(TAG, "${state.symbol} processing failed", e)
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
        val prices = "BTC:${"%.0f".format(btcState.currentPrice)} ETH:${"%.0f".format(ethState.currentPrice)}"
        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, buildNotification("$btcPnl | $prices"))
    }
}
