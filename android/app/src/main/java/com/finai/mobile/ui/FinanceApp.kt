package com.finai.mobile.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.finai.mobile.ui.components.LoadingBox
import com.finai.mobile.ui.screens.AiChatScreen
import com.finai.mobile.ui.screens.BudgetsScreen
import com.finai.mobile.ui.screens.DashboardScreen
import com.finai.mobile.ui.screens.LoginScreen
import com.finai.mobile.ui.screens.ProfileScreen
import com.finai.mobile.ui.screens.TransactionsScreen
import com.finai.mobile.ui.viewmodel.AuthViewModel
import com.finai.mobile.ui.viewmodel.FinanceViewModel

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("dashboard", "Home", Icons.Default.AutoGraph),
    Tab("transactions", "Records", Icons.Default.AddCircle),
    Tab("budgets", "Budgets", Icons.Default.BarChart),
    Tab("assistant", "Assistant", Icons.Default.Forum),
    Tab("profile", "Profile", Icons.Default.AccountCircle),
)

@Composable
fun FinanceApp(authViewModel: AuthViewModel, financeViewModel: FinanceViewModel) {
    val auth by authViewModel.state.collectAsState()

    if (auth.checkingSession) {
        LoadingBox()
        return
    }

    if (auth.user == null) {
        LoginScreen(authViewModel)
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(Unit) { financeViewModel.loadInitial() }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                // Keep a single copy of each tab on the back stack so
                                // switching tabs never grows the history.
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(padding),
        ) {
            composable("dashboard") { DashboardScreen(financeViewModel) }
            composable("transactions") { TransactionsScreen(financeViewModel) }
            composable("budgets") { BudgetsScreen(financeViewModel) }
            composable("assistant") { AiChatScreen(financeViewModel) }
            composable("profile") { ProfileScreen(authViewModel, financeViewModel) }
        }
    }
}