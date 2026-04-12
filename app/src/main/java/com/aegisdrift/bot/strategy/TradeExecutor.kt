package com.aegisdrift.bot.strategy

import android.util.Log
import com.aegisdrift.bot.api.BitgetClient
import com.aegisdrift.bot.data.AppDatabase
import com.aegisdrift.bot.data.PrefManager
import com.aegisdrift.bot.data.TradeEntity
import java.text.SimpleDateFormat
import java.util.*

class TradeExecutor(
    private val client: BitgetClient,
    private val db: AppDatabase,
    private val prefs: PrefManager
) {
    private val TAG = "TradeExecutor"
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun now() = sdf.format(Date())

    // ── Open Long ─────────────────────────────────────────────────────
    suspend fun openLong(state: SymbolState, price: Double, stop: Double) {
        val qty = StrategyEngine.calcQty(prefs.currentEquity, price, stop)
        if (qty <= 0) { Log.w(TAG, "${state.symbol} qty=0, skip long"); return }

        val cost = qty * price
        val fee  = cost * StrategyEngine.FEE

        val success = client.placeOrder(state.symbol, "buy", formatQty(state.symbol, qty))
        if (!success) { Log.e(TAG, "${state.symbol} long order failed"); return }

        prefs.currentEquity -= (cost + fee)
        state.position       = 1
        state.entryPrice     = price
        state.stopPrice      = stop
        state.qty            = qty
        state.entryFee       = fee
        state.entryTime      = now()
        state.lastSignal     = "LONG ENTRY"

        val id = db.tradeDao().insertTrade(
            TradeEntity(
                side        = "long",
                entryTime   = state.entryTime,
                entryPrice  = price,
                stopPrice   = stop,
                exitTime    = null,
                exitPrice   = null,
                exitReason  = null,
                qty         = qty,
                netPnlUsd   = null,
                barsHeld    = null,
                status      = "OPEN"
            )
        )
        state.dbTradeId = id
        Log.i(TAG, "${state.symbol} LONG opened @ $price qty=$qty stop=$stop")
    }

    // ── Open Short ────────────────────────────────────────────────────
    suspend fun openShort(state: SymbolState, price: Double, stop: Double) {
        val qty = StrategyEngine.calcQty(prefs.currentEquity, price, stop)
        if (qty <= 0) { Log.w(TAG, "${state.symbol} qty=0, skip short"); return }

        val cost = qty * price
        val fee  = cost * StrategyEngine.FEE

        val success = client.placeOrder(state.symbol, "sell", formatQty(state.symbol, qty))
        if (!success) { Log.e(TAG, "${state.symbol} short order failed"); return }

        prefs.currentEquity  -= (cost + fee)
        state.position        = -1
        state.entryPrice      = price
        state.stopPrice       = stop
        state.qty             = qty
        state.entryFee        = fee
        state.marginReserved  = cost
        state.entryTime       = now()
        state.lastSignal      = "SHORT ENTRY"

        val id = db.tradeDao().insertTrade(
            TradeEntity(
                side        = "short",
                entryTime   = state.entryTime,
                entryPrice  = price,
                stopPrice   = stop,
                exitTime    = null,
                exitPrice   = null,
                exitReason  = null,
                qty         = qty,
                netPnlUsd   = null,
                barsHeld    = null,
                status      = "OPEN"
            )
        )
        state.dbTradeId = id
        Log.i(TAG, "${state.symbol} SHORT opened @ $price qty=$qty stop=$stop")
    }

    // ── Close Position ────────────────────────────────────────────────
    suspend fun closePosition(state: SymbolState, price: Double, reason: String) {
        if (state.position == 0) return

        val exitSide = if (state.position == 1) "sell" else "buy"
        val success  = client.placeOrder(
            state.symbol, exitSide,
            formatQty(state.symbol, state.qty),
            reduceOnly = true
        )
        if (!success) { Log.e(TAG, "${state.symbol} close order failed"); return }

        val exitFee = state.qty * price * StrategyEngine.FEE
        val pnl     = if (state.position == 1)
            (price - state.entryPrice) * state.qty - state.entryFee - exitFee
        else
            (state.entryPrice - price) * state.qty - state.entryFee - exitFee

        val returnedCash = if (state.position == 1)
            state.qty * price - exitFee
        else
            state.marginReserved + (state.entryPrice - price) * state.qty - exitFee

        prefs.currentEquity += returnedCash
        state.sessionTrades++
        if (pnl > 0) state.sessionWins++
        state.sessionPnl += pnl
        prefs.totalTrades++
        if (pnl > 0) prefs.winCount++

        // Update DB
        if (state.dbTradeId >= 0) {
            val open = db.tradeDao().getOpenTrade()
            if (open != null) {
                db.tradeDao().updateTrade(
                    open.copy(
                        exitTime   = now(),
                        exitPrice  = price,
                        exitReason = reason,
                        netPnlUsd  = pnl,
                        status     = "CLOSED"
                    )
                )
            }
        }

        Log.i(TAG, "${state.symbol} CLOSED @ $price reason=$reason pnl=$pnl")

        state.position       = 0
        state.qty            = 0.0
        state.entryPrice     = 0.0
        state.stopPrice      = 0.0
        state.marginReserved = 0.0
        state.entryFee       = 0.0
        state.dbTradeId      = -1L
        state.lastSignal     = "EXIT ($reason)"
    }

    // ── Format qty based on symbol precision ─────────────────────────
    private fun formatQty(symbol: String, qty: Double): String =
        when {
            symbol.startsWith("BTC") -> "%.4f".format(qty)
            symbol.startsWith("ETH") -> "%.3f".format(qty)
            else                     -> "%.3f".format(qty)
        }
}
