package com.example.sequencelab.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sequencelab.R
import com.example.sequencelab.data.AppSettings
import com.example.sequencelab.ui.screens.AboutScreen
import com.example.sequencelab.ui.screens.CalculatorScreen
import com.example.sequencelab.ui.screens.HistoryScreen
import com.example.sequencelab.ui.screens.SettingsScreen

private enum class Destination(val route: String, val label: String, @param:DrawableRes val icon: Int) {
    CALCULATOR("calculator", "Calculator", R.drawable.ic_functions),
    HISTORY("history", "History", R.drawable.ic_history),
    SETTINGS("settings", "Settings", R.drawable.ic_settings),
    ABOUT("about", "About", R.drawable.ic_info),
}

@Composable
fun SequenceLabApp(viewModel: SequenceLabViewModel, settings: AppSettings) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(viewModel) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    fun navigate(route: String) = navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    val selected = backStackEntry?.destination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navigate(destination.route) },
                        icon = { Icon(painterResource(destination.icon), contentDescription = null) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.CALCULATOR.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
        ) {
            composable(Destination.CALCULATOR.route) { CalculatorScreen(viewModel, settings) }
            composable(Destination.HISTORY.route) {
                HistoryScreen(viewModel, settings, onOpen = { navigate(Destination.CALCULATOR.route) })
            }
            composable(Destination.SETTINGS.route) { SettingsScreen(viewModel, settings) }
            composable(Destination.ABOUT.route) { AboutScreen() }
        }
    }
}
