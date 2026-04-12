package com.aegisdrift.bot.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aegisdrift.bot.data.AppDatabase
import com.aegisdrift.bot.data.PrefManager
import com.aegisdrift.bot.data.TradingMode
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

    private val _isRunning = MutableStateFlow(TradingService.isRunning)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _equity = MutableStateFlow(prefs.currentEquity)
    val equity: StateFlow<Double> = _equity

    private val _btcState = MutableStateFlow(SymbolState("BTCUSDT"))
    val btcState: StateFlow<SymbolState> = _btcState

    private val _ethState = MutableStateFlow(SymbolState("ETHUSDT"))
    val ethState: StateFlow<SymbolState> = _ethState

    private val _trades = MutableStateFlow<List<TradeEntity>>(emptyList())
    val trades: StateFlow<List<TradeEntity>> = _trades

    private val _apiKey     = MutableStateFlow(prefs.apiKey)
    val apiKey: StateFlow<String> = _apiKey

    private val _apiSecret  = MutableStateFlow(prefs.apiSecret)
    val apiSecret: StateFlow<String> = _apiSecret

    private val _apiPass    = MutableStateFlow(prefs.apiPassphrase)
    val apiPass: StateFlow<String> = _apiPass

    private val _tradingMode = MutableStateFlow(prefs.tradingMode)
    val tradingMode: StateFlow<String> = _tradingMode

    init {
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

    fun startBot() {
        val ctx = getApplication<Application>()
        ctx.startForegroundService(Intent(ctx, TradingService::class.java))
        _isRunning.value = true
    }

    fun stopBot() {
        val ctx = getApplication<Application>()
        ctx.stopService(Intent(ctx, TradingService::class.java))
        _isRunning.value = false
    }

    fun saveSettings(
        key: String,
        secret: String,
        pass: String,
        mode: String,
        balance: Double
    ) {
        prefs.apiKey        = key
        prefs.apiSecret     = secret
        prefs.apiPassphrase = pass
        prefs.tradingMode   = mode
        prefs.startBalance  = balance
        prefs.currentEquity = balance
        _apiKey.value       = key
        _apiSecret.value    = secret
        _apiPass.value      = pass
        _tradingMode.value  = mode
        _equity.value       = balance
    }

    fun resetStats() {
        viewModelScope.launch {
            db.tradeDao().deleteAll()
            prefs.totalTrades   = 0
            prefs.winCount      = 0
            prefs.currentEquity = prefs.startBalance
            _trades.value       = emptyList()
            _equity.value       = prefs.startBalance
        }
    }
}
