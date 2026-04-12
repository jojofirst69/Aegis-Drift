package com.aegisdrift.bot.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aegisdrift.bot.data.AppDatabase
import com.aegisdrift.bot.data.PrefManager
import com.aegisdrift.bot.data.TradeEntity
import com.aegisdrift.bot.service.TradingService
import com.aegisdrift.bot.strategy.SymbolState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BotViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = PrefManager(app)
    private val db    = AppDatabase.getInstance(app)

    // ── Bot running state ─────────────────────────────────────────────
    private val _isRunning = MutableStateFlow(TradingService.isRunning)
    val isRunning: StateFlow<Boolean> = _isRunning

    // ── Equity ────────────────────────────────────────────────────────
    private val _equity = MutableStateFlow(prefs.currentEquity)
    val equity: StateFlow<Double> = _equity

    // ── Symbol states (BTC + ETH) ─────────────────────────────────────
    private val _btcState = MutableStateFlow(SymbolState("BTCUSDT"))
    val btcState: StateFlow<SymbolState> = _btcState

    private val _ethState = MutableStateFlow(SymbolState("ETHUSDT"))
    val ethState: StateFlow<SymbolState> = _ethState

    // ── Trade history ─────────────────────────────────────────────────
    private val _trades = MutableStateFlow<List<TradeEntity>>(emptyList())
    val trades: StateFlow<List<TradeEntity>> = _trades

    // ── Settings ──────────────────────────────────────────────────────
    private val _apiKey       = MutableStateFlow(prefs.apiKey)
    val apiKey: StateFlow<String> = _apiKey

    private val _apiSecret    = MutableStateFlow(prefs.apiSecret)
    val apiSecret: StateFlow<String> = _apiSecret

    private val _apiPass      = MutableStateFlow(prefs.apiPassphrase)
    val apiPass: StateFlow<String> = _apiPass

    private val _isDemoMode   = MutableStateFlow(prefs.isDemoMode)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode

    init {
        // Poll service state every 5 seconds to refresh UI
        viewModelScope.launch {
            while (true) {
                _isRunning.value = TradingService.isRunning
                _equity.value    = prefs.currentEquity

                TradingService.symbolStates["BTCUSDT"]?.let {
                    _btcState.value = it.copy()
                }
                TradingService.symbolStates["ETHUSDT"]?.let {
                    _ethState.value = it.copy()
                }

                _trades.value = db.tradeDao().getAllTrades()
                delay(5_000L)
            }
        }
    }

    // ── Start bot ─────────────────────────────────────────────────────
    fun startBot() {
        val ctx = getApplication<Application>()
        ctx.startForegroundService(Intent(ctx, TradingService::class.java))
        _isRunning.value = true
    }

    // ── Stop bot ──────────────────────────────────────────────────────
    fun stopBot() {
        val ctx = getApplication<Application>()
        ctx.stopService(Intent(ctx, TradingService::class.java))
        _isRunning.value = false
    }

    // ── Save settings ─────────────────────────────────────────────────
    fun saveSettings(key: String, secret: String, pass: String, demo: Boolean, balance: Double) {
        prefs.apiKey         = key
        prefs.apiSecret      = secret
        prefs.apiPassphrase  = pass
        prefs.isDemoMode     = demo
        prefs.startBalance   = balance
        prefs.currentEquity  = balance
        _apiKey.value        = key
        _apiSecret.value     = secret
        _apiPass.value       = pass
        _isDemoMode.value    = demo
        _equity.value        = balance
    }

    // ── Reset stats ───────────────────────────────────────────────────
    fun resetStats() {
        viewModelScope.launch {
            db.tradeDao().deleteAll()
            prefs.totalTrades  = 0
            prefs.winCount     = 0
            prefs.currentEquity = prefs.startBalance
            _trades.value      = emptyList()
            _equity.value      = prefs.startBalance
        }
    }
}
