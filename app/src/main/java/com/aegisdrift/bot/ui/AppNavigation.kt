package com.aegisdrift.bot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aegisdrift.bot.ui.theme.Cyan400
import com.aegisdrift.bot.ui.theme.SurfaceDark

sealed class Screen(val route: String, val label: String, val icon: String) {
    object Dashboard : Screen("dashboard", "Dashboard", "📊")
    object Settings  : Screen("settings",  "Settings",  "⚙️")
}

@Composable
fun AppNavigation(vm: BotViewModel = viewModel()) {
    val navController = rememberNavController()
    val screens       = listOf(Screen.Dashboard, Screen.Settings)
    val backStack     by navController.currentBackStackEntryAsState()
    val currentRoute  = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                tonalElevation = 0.dp
            ) {
                screens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick  = {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Dashboard.route) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        icon  = { Text(screen.icon, fontSize = 20.sp) },
                        label = {
                            Text(screen.label,
                                color = if (currentRoute == screen.route) Cyan400
                                        else Color.Gray,
                                fontSize = 11.sp)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = Cyan400,
                            unselectedIconColor = Color.Gray,
                            indicatorColor      = Color.Transparent
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Dashboard.route,
            modifier         = Modifier.padding(padding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen(vm) }
            composable(Screen.Settings.route)  { SettingsScreen(vm) }
        }
    }
}
