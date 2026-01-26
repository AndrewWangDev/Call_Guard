package com.example.phoneblocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.phoneblocker.data.local.AppDatabase
import com.example.phoneblocker.data.repository.UserPreferencesRepository
import com.example.phoneblocker.ui.MainViewModel
import com.example.phoneblocker.ui.MainViewModelFactory
import com.example.phoneblocker.ui.navigation.Screen
import com.example.phoneblocker.ui.screens.ConfigScreen
import com.example.phoneblocker.ui.screens.HistoryScreen
import com.example.phoneblocker.ui.theme.PhoneBlockAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(this)
        val userPreferencesRepository = UserPreferencesRepository(this)
        val viewModelFactory = MainViewModelFactory(
            database.blockLogDao(),
            database.regexRuleDao(),
            userPreferencesRepository
        )

        setContent {
            PhoneBlockAppTheme {
                MainApp(viewModelFactory)
            }
        }
    }
}

@Composable
fun MainApp(viewModelFactory: MainViewModelFactory) {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel(factory = viewModelFactory)
    val screens = listOf(Screen.History, Screen.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.route == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.History.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.History.route) {
                val logs by viewModel.blockLogs.collectAsState()
                HistoryScreen(
                    logs = logs,
                    onClearLogs = { viewModel.clearLogs() }
                )
            }
            composable(Screen.Settings.route) {
                val isBlockingEnabled by viewModel.isBlockingEnabled.collectAsState()
                val regexRules by viewModel.regexRules.collectAsState()
                
                ConfigScreen(
                    isBlockingEnabled = isBlockingEnabled,
                    onBlockingEnabledChange = { viewModel.setBlockingEnabled(it) },
                    regexRules = regexRules,
                    onAddRule = { pattern, type -> viewModel.addRule(pattern, type) },
                    onDeleteRule = { rule -> viewModel.deleteRule(rule) }
                )
            }
        }
    }
}
