package com.aegisdrift.bot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aegisdrift.bot.ui.theme.AegisDriftTheme
import com.aegisdrift.bot.ui.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AegisDriftTheme {
                AppNavigation()
            }
        }
    }
}
