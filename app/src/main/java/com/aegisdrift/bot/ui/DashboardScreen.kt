package com.aegisdrift.bot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aegisdrift.bot.data.TradeEntity
import com.aegisdrift.bot.strategy.SymbolState
import com.aegisdrift.bot.ui.theme.*

@Composable
fun DashboardScreen(vm: BotViewModel = viewModel()) {
    val isRunning by vm.isRunning.collectAsState()
    val equity    by vm.equity.collectAsState()
    val btc       by vm.btcState.collectAsState()
    val eth       by vm.ethState.collectAsState()
    val trades    by vm.trades.collectAsState()
    val isDemo    by vm.isDemoMode.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("⚡ Aegis Drift Bot",
                        color = Cyan400, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(if (isDemo) "● DEMO MODE" else "● LIVE MODE",
                        color = if (isDemo) Yellow400 else Green400, fontSize = 12.sp)
                }
                Button(
                    onClick = { if (isRunning) vm.stopBot() else vm.startBot() },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) Red400 else Green400
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        if (isRunning) "⏹ STOP" else "▶ START",
                        color = Color.Black, fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ── Equity card ───────────────────────────────────────────────
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape  = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Equity", color = Color.Gray, fontSize = 13.sp)
                    Text("$${"%.2f".format(equity)}",
                        color = Cyan400, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (isRunning) "🟢 Bot Active" else "🔴 Bot Stopped",
                        color = if (isRunning) Green400 else Red400,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // ── BTC Card ──────────────────────────────────────────────────
        item { SymbolCard(state = btc) }

        // ── ETH Card ──────────────────────────────────────────────────
        item { SymbolCard(state = eth) }

        // ── Trade history ─────────────────────────────────────────────
        item {
            Text("Trade History", color = Color.Gray,
                fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        }

        if (trades.isEmpty()) {
            item {
                Text("No trades yet", color = Color.DarkGray,
                    fontSize = 13.sp, modifier = Modifier.padding(8.dp))
            }
        } else {
            items(trades) { trade -> TradeRow(trade) }
        }
    }
}

// ── Symbol card (BTC or ETH) ──────────────────────────────────────────
@Composable
fun SymbolCard(state: SymbolState) {
    val posColor = when (state.position) {
        1    -> Green400
        -1   -> Red400
        else -> Color.Gray
    }
    val posLabel = when (state.position) {
        1    -> "LONG"
        -1   -> "SHORT"
        else -> "FLAT"
    }
    Card(
        colors   = CardDefaults.cardColors(containerColor = CardDark),
        shape    = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {

            Row(horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                Text(state.symbol, color = Cyan400,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(posLabel, color = posColor,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Row(horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                StatItem("Price",  "${"%.2f".format(state.currentPrice)}")
                StatItem("AVWAP",  "${"%.2f".format(state.currentAvwap)}")
                StatItem("Signal", state.lastSignal)
            }

            Row(horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                StatItem("Unrealized PnL",
                    "${"%.4f".format(state.unrealizedPnl)}",
                    if (state.unrealizedPnl >= 0) Green400 else Red400)
                StatItem("Session PnL",
                    "${"%.4f".format(state.sessionPnl)}",
                    if (state.sessionPnl >= 0) Green400 else Red400)
                StatItem("Win Rate", "${"%.1f".format(state.winRate)}%")
            }

            if (state.position != 0) {
                Row(horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()) {
                    StatItem("Entry",  "${"%.2f".format(state.entryPrice)}")
                    StatItem("Stop",   "${"%.2f".format(state.stopPrice)}", Red400)
                    StatItem("Qty",    "${"%.5f".format(state.qty)}")
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, valueColor: Color = Color.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 11.sp)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ── Trade history row ─────────────────────────────────────────────────
@Composable
fun TradeRow(trade: TradeEntity) {
    val pnl      = trade.netPnlUsd ?: 0.0
    val pnlColor = if (pnl >= 0) Green400 else Red400
    val sideColor= if (trade.side == "long") Green400 else Red400

    Card(
        colors   = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape    = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(trade.side.uppercase(), color = sideColor,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(trade.entryTime.take(16), color = Color.Gray, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${"%.2f".format(trade.entryPrice)}", color = Color.White, fontSize = 12.sp)
                Text("→ ${"%.2f".format(trade.exitPrice ?: 0.0)}", color = Color.Gray, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (trade.status == "OPEN") "OPEN" else "${"%.4f".format(pnl)}",
                    color = if (trade.status == "OPEN") Yellow400 else pnlColor,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(trade.exitReason ?: "running", color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}
