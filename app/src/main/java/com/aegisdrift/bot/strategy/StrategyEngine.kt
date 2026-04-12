package com.aegisdrift.bot.strategy

// ── Exact 1-to-1 translation of your Python strategy ──────────────────
// Pivot detection → Anchor VWAP → Signal crossover → Risk management
// Nothing changed except language (Python → Kotlin)

const val PIVOT_LEN     = 5
const val RISK_PER_TRADE = 1.0   // USD risk per trade
const val FEE           = 0.001  // 0.1% per side

data class Bar(
    val openTime: Long,
    val open:     Double,
    val high:     Double,
    val low:      Double,
    val close:    Double,
    val volume:   Double
)

data class StrategyResult(
    val signal:      Signal,
    val anchorStop:  Double,   // stop loss price
    val avwap:       Double,   // current anchor VWAP value
    val anchorSide:  String    // "low" or "high"
)

enum class Signal { LONG_ENTRY, SHORT_ENTRY, LONG_EXIT, SHORT_EXIT, NONE }

object StrategyEngine {

    // ── Step 1: Detect pivot highs and lows (same as Python) ──────────
    fun detectPivots(bars: List<Bar>): Pair<BooleanArray, BooleanArray> {
        val n   = bars.size
        val sh  = BooleanArray(n)
        val sl  = BooleanArray(n)
        val hi  = DoubleArray(n) { bars[it].high }
        val lo  = DoubleArray(n) { bars[it].low }

        for (i in PIVOT_LEN until n - PIVOT_LEN) {
            val hiSlice = hi.slice(i - PIVOT_LEN..i + PIVOT_LEN)
            val loSlice = lo.slice(i - PIVOT_LEN..i + PIVOT_LEN)
            if (hi[i] == hiSlice.max()) sh[i] = true
            if (lo[i] == loSlice.min()) sl[i] = true
        }
        return Pair(sh, sl)
    }

    // ── Step 2: Compute Anchor VWAP from last pivot (same as Python) ──
    fun computeAnchorVwap(bars: List<Bar>): Triple<DoubleArray, Array<String?>, DoubleArray> {
        val n         = bars.size
        val avwap     = DoubleArray(n) { Double.NaN }
        val ancSide   = arrayOfNulls<String>(n)
        val ancStop   = DoubleArray(n) { Double.NaN }

        val tp        = DoubleArray(n) { (bars[it].high + bars[it].low + bars[it].close) / 3.0 }
        val tpv       = DoubleArray(n) { tp[it] * bars[it].volume }
        val cumVol    = DoubleArray(n)
        val cumTpv    = DoubleArray(n)

        cumVol[0] = bars[0].volume
        cumTpv[0] = tpv[0]
        for (i in 1 until n) {
            cumVol[i] = cumVol[i - 1] + bars[i].volume
            cumTpv[i] = cumTpv[i - 1] + tpv[i]
        }

        val (sh, sl) = detectPivots(bars)

        var ancIdx  = -1
        var ancType = ""
        var ancPrice = Double.NaN

        for (i in 0 until n) {
            if (sl[i]) { ancIdx = i; ancType = "low";  ancPrice = bars[i].low }
            else if (sh[i]) { ancIdx = i; ancType = "high"; ancPrice = bars[i].high }

            if (ancIdx >= 0) {
                val prevVol = if (ancIdx > 0) cumVol[ancIdx - 1] else 0.0
                val prevTpv = if (ancIdx > 0) cumTpv[ancIdx - 1] else 0.0
                val vol     = cumVol[i] - prevVol
                val vtp     = cumTpv[i] - prevTpv
                avwap[i]    = if (vol > 0) vtp / vol else Double.NaN
                ancSide[i]  = ancType
                ancStop[i]  = ancPrice
            }
        }
        return Triple(avwap, ancSide, ancStop)
    }

    // ── Step 3: Generate signal for latest bar (same logic as Python) ─
    fun getSignal(bars: List<Bar>, currentPosition: Int): StrategyResult {
        if (bars.size < PIVOT_LEN * 2 + 2)
            return StrategyResult(Signal.NONE, Double.NaN, Double.NaN, "")

        val (avwap, ancSide, ancStop) = computeAnchorVwap(bars)
        val last = bars.size - 1
        val prev = last - 1

        val currClose  = bars[last].close
        val prevClose  = bars[prev].close
        val currAvwap  = avwap[last]
        val prevAvwap  = avwap[prev]
        val side       = ancSide[last] ?: ""
        val stop       = ancStop[last]

        if (currAvwap.isNaN() || prevAvwap.isNaN())
            return StrategyResult(Signal.NONE, stop, currAvwap, side)

        val slopeUp = currAvwap > prevAvwap
        val slopeDn = currAvwap < prevAvwap

        // Cross above VWAP
        val crossAbove = currClose > currAvwap && prevClose <= prevAvwap
        // Cross below VWAP
        val crossBelow = currClose < currAvwap && prevClose >= prevAvwap

        val (sh, sl) = detectPivots(bars)

        val signal = when {
            // ── Long entry (same 4 conditions as Python) ──
            currentPosition == 0 &&
            side == "low" &&
            crossAbove &&
            slopeUp &&
            currClose > stop
            -> Signal.LONG_ENTRY

            // ── Short entry (same 4 conditions as Python) ──
            currentPosition == 0 &&
            side == "high" &&
            crossBelow &&
            slopeDn &&
            currClose < stop
            -> Signal.SHORT_ENTRY

            // ── Long exit ──
            currentPosition == 1 &&
            (crossBelow || sh[last])
            -> Signal.LONG_EXIT

            // ── Short exit ──
            currentPosition == -1 &&
            (crossAbove || sl[last])
            -> Signal.SHORT_EXIT

            else -> Signal.NONE
        }

        return StrategyResult(signal, stop, currAvwap, side)
    }

    // ── Step 4: Calculate position size (same as Python) ─────────────
    fun calcQty(capital: Double, entryPrice: Double, stopPrice: Double): Double {
        val riskPerUnit = Math.abs(entryPrice - stopPrice)
        if (riskPerUnit <= 0) return 0.0
        val qtyByRisk   = RISK_PER_TRADE / riskPerUnit
        val qtyByCap    = capital / (entryPrice * (1 + FEE))
        return minOf(qtyByRisk, qtyByCap).coerceAtLeast(0.0)
    }
}
