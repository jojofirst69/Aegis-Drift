package com.aegisdrift.bot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aegisdrift.bot.ui.theme.*

@Composable
fun SettingsScreen(vm: BotViewModel = viewModel()) {
    val savedKey    by vm.apiKey.collectAsState()
    val savedSecret by vm.apiSecret.collectAsState()
    val savedPass   by vm.apiPass.collectAsState()
    val savedDemo   by vm.isDemoMode.collectAsState()
    val equity      by vm.equity.collectAsState()

    var apiKey      by remember { mutableStateOf(savedKey) }
    var apiSecret   by remember { mutableStateOf(savedSecret) }
    var apiPass     by remember { mutableStateOf(savedPass) }
    var isDemo      by remember { mutableStateOf(savedDemo) }
    var balance     by remember { mutableStateOf(equity.toString()) }
    var showSecret  by remember { mutableStateOf(false) }
    var saved       by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("⚙️ Settings", color = Cyan400,
            fontSize = 22.sp, fontWeight = FontWeight.Bold)

        // ── API Credentials ───────────────────────────────────────────
        Text("Bitget API Credentials", color = Color.Gray, fontSize = 13.sp)

        BotTextField(
            label = "API Key",
            value = apiKey,
            onValueChange = { apiKey = it; saved = false }
        )

        BotTextField(
            label       = "API Secret",
            value       = apiSecret,
            onValueChange = { apiSecret = it; saved = false },
            isPassword  = true,
            showPassword = showSecret,
            onToggleShow = { showSecret = !showSecret }
        )

        BotTextField(
            label = "API Passphrase",
            value = apiPass,
            onValueChange = { apiPass = it; saved = false },
            isPassword  = true,
            showPassword = showSecret,
            onToggleShow = { showSecret = !showSecret }
        )

        // ── Demo / Live toggle ────────────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape  = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("Demo Mode", color = Color.White, fontSize = 15.sp)
                    Text(
                        if (isDemo) "Paper trading — no real money"
                        else        "⚠️ LIVE — real money at risk",
                        color    = if (isDemo) Yellow400 else Red400,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked         = isDemo,
                    onCheckedChange = { isDemo = it; saved = false },
                    colors          = SwitchDefaults.colors(
                        checkedThumbColor   = Yellow400,
                        uncheckedThumbColor = Red400
                    )
                )
            }
        }

        // ── Starting balance ──────────────────────────────────────────
        BotTextField(
            label         = "Starting Balance (USDT)",
            value         = balance,
            onValueChange = { balance = it; saved = false },
            keyboardType  = KeyboardType.Decimal
        )

        // ── Save button ───────────────────────────────────────────────
        Button(
            onClick = {
                vm.saveSettings(
                    apiKey, apiSecret, apiPass,
                    isDemo,
                    balance.toDoubleOrNull() ?: 100.0
                )
                saved = true
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Cyan400),
            shape    = RoundedCornerShape(24.dp)
        ) {
            Text(
                if (saved) "✅ Saved!" else "Save Settings",
                color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp
            )
        }

        // ── Reset stats ───────────────────────────────────────────────
        OutlinedButton(
            onClick  = { vm.resetStats() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = Red400),
            shape    = RoundedCornerShape(24.dp)
        ) {
            Text("🗑 Reset All Stats & Trades", fontWeight = FontWeight.Bold)
        }

        // ── Info box ──────────────────────────────────────────────────
        Card(
            colors   = CardDefaults.cardColors(containerColor = CardDark),
            shape    = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("ℹ️ How to get Bitget API keys", color = Cyan400, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold)
                Text("1. Go to bitget.com → Profile → API Management",
                    color = Color.Gray, fontSize = 12.sp)
                Text("2. Create API → enable Futures trading permission",
                    color = Color.Gray, fontSize = 12.sp)
                Text("3. For demo: use Bitget demo account API keys",
                    color = Color.Gray, fontSize = 12.sp)
                Text("4. Paste keys above and tap Save", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun BotTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean    = false,
    showPassword: Boolean  = false,
    onToggleShow: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label, color = Color.Gray) },
        modifier      = Modifier.fillMaxWidth(),
        singleLine    = true,
        visualTransformation = if (isPassword && !showPassword)
            PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon  = if (isPassword && onToggleShow != null) {{
            TextButton(onClick = onToggleShow) {
                Text(if (showPassword) "Hide" else "Show",
                    color = Cyan400, fontSize = 12.sp)
            }
        }} else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Cyan400,
            unfocusedBorderColor = Color.DarkGray,
            focusedTextColor     = Color.White,
            unfocusedTextColor   = Color.White,
            cursorColor          = Cyan400
        ),
        shape = RoundedCornerShape(12.dp)
    )
}
