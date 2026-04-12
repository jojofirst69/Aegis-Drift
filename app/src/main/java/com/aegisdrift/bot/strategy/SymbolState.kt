package com.aegisdrift.bot.strategy

// Holds live trading state for ONE symbol (BTC or ETH)
// Bot maintains two separate instances of this — one per symbol

data class SymbolState(
    val symbol: String,              // e.g. "BTCUSDT" or "ETHUSDT"

    // Position tracking
    var position: Int       = 0,     // 0 = flat, 1 = long, -1 = short
    var entryPrice: Double  = 0.0,
    var stopPrice: Double   = 0.0,
    var qty: Double         = 0.0,
    var marginReserved: Double = 0.0, // reserved margin for short trades
    var entryFee: Double    = 0.0,
    var entryTime: String   = "",
    var entryBarIndex: Int  = 0,
    var dbTradeId: Long     = -1L,   // Room DB row id for open trade

    // Live metrics
    var currentPrice: Double = 0.0,
    var currentAvwap: Double = 0.0,
    var anchorSide: String   = "",
    var anchorStop: Double   = 0.0,

    // Session stats
    var sessionTrades: Int   = 0,
    var sessionWins: Int     = 0,
    var sessionPnl: Double   = 0.0,

    // Last signal for UI display
    var lastSignal: String   = "NONE",
    var lastUpdate: Long     = 0L
) {
    val winRate: Double
        get() = if (sessionTrades > 0)
            sessionWins.toDouble() / sessionTrades * 100.0
        else 0.0

    val unrealizedPnl: Double
        get() = when (position) {
            1    -> (currentPrice - entryPrice) * qty
            -1   -> (entryPrice - currentPrice) * qty
            else -> 0.0
        }
}
