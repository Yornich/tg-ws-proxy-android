package com.tgwsproxy.ui

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tgwsproxy.R
import com.tgwsproxy.ui.about.AboutScreen
import com.tgwsproxy.ui.home.HomeScreen
import com.tgwsproxy.ui.log.LogScreen
import com.tgwsproxy.ui.onboarding.OnboardingScreen
import com.tgwsproxy.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home       : Screen("home")
    object Settings   : Screen("settings")
    object Log        : Screen("log")
    object About      : Screen("about")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val seenOnboarding = remember { prefs.getBoolean("onboarding_done", false) }
    val startDest = if (seenOnboarding) Screen.Home.route else Screen.Onboarding.route

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isOnboarding = currentDestination?.route == Screen.Onboarding.route

    val bottomItems = listOf(
        Triple(Screen.Home,     Icons.Rounded.Shield,   stringResource(R.string.nav_home)),
        Triple(Screen.Settings, Icons.Rounded.Settings, stringResource(R.string.nav_settings)),
        Triple(Screen.Log,      Icons.Rounded.Article,  stringResource(R.string.nav_log)),
    )

    Scaffold(
        topBar = {
            if (!isOnboarding) {
                TopAppBar(
                    title = { Text("TG WS Proxy") },
                    actions = {
                        IconButton(onClick = {
                            navController.navigate(Screen.About.route) {
                                launchSingleTop = true
                            }
                        }) {
                            Icon(Icons.Rounded.Info, contentDescription = stringResource(R.string.nav_about))
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (!isOnboarding) {
                NavigationBar {
                    bottomItems.forEach { (screen, icon, label) ->
                        NavigationBarItem(
                            icon     = { Icon(icon, contentDescription = label) },
                            label    = { Text(label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick  = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = startDest,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(onDone = {
                    prefs.edit().putBoolean("onboarding_done", true).apply()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.Home.route) {
                HomeScreen(onOpenLog = {
                    navController.navigate(Screen.Log.route) {
                        launchSingleTop = true
                    }
                })
            }
            composable(Screen.Settings.route) {
                val activity = (LocalContext.current as? android.app.Activity)
                SettingsScreen(
                    onLanguageChanged = {
                        activity?.let {
                            val intent = it.intent
                            it.finish()
                            it.startActivity(intent)
                            it.overridePendingTransition(0, 0)
                        }
                    }
                )
            }
            composable(Screen.Log.route) { LogScreen() }
            composable(Screen.About.route) {
                AboutScreen(onBack = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                })
            }
        }
    }
}
