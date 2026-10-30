package com.example.phoneblocker

import android.app.role.RoleManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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

    private val viewModel: MainViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val userPreferencesRepository = UserPreferencesRepository(applicationContext)
        MainViewModelFactory(
            database.blockLogDao(),
            database.regexRuleDao(),
            userPreferencesRepository
        )
    }

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkRoleStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request Call Screening Role on Android 10+ (API 29+)
        checkAndRequestCallScreeningRole()

        setContent {
            PhoneBlockAppTheme {
                MainApp(
                    viewModel = viewModel,
                    onRequestRole = { checkAndRequestCallScreeningRole() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkRoleStatus()
    }

    private fun checkRoleStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            val isHeld = roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
            viewModel.updateRoleState(isHeld)
        } else {
            viewModel.updateRoleState(true)
        }
    }

    private fun checkAndRequestCallScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                roleRequestLauncher.launch(intent)
            }
        }
    }
}

@Composable
fun MainApp(
    viewModel: MainViewModel,
    onRequestRole: () -> Unit
) {
    val navController = rememberNavController()
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
                val isBlockUnknownEnabled by viewModel.isBlockUnknownEnabled.collectAsState()
                val isRoleHeld by viewModel.isRoleHeld.collectAsState()
                val regexRules by viewModel.regexRules.collectAsState()

                ConfigScreen(
                    isBlockingEnabled = isBlockingEnabled,
                    onBlockingEnabledChange = { viewModel.setBlockingEnabled(it) },
                    isBlockUnknownEnabled = isBlockUnknownEnabled,
                    onBlockUnknownEnabledChange = { viewModel.setBlockUnknownEnabled(it) },
                    isRoleHeld = isRoleHeld,
                    onRequestRole = onRequestRole,
                    regexRules = regexRules,
                    onAddRule = { pattern, type -> viewModel.addRule(pattern, type) },
                    onDeleteRule = { rule -> viewModel.deleteRule(rule) }
                )
            }
        }
    }
}
