package com.example.expensetracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.data.ExpenseRepository
import com.example.expensetracker.data.local.ExpenseDatabase
import com.example.expensetracker.security.BiometricHelper
import com.example.expensetracker.theme.ExpenseTrackerTheme
import com.example.expensetracker.ui.screen.ExpenseListScreen
import com.example.expensetracker.ui.screen.LockScreen
import com.example.expensetracker.ui.screen.SettingsScreen
import com.example.expensetracker.ui.viewmodel.ExpenseViewModel
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.Crossfade
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.expensetracker.ui.screen.AiAnalysisActivity
import kotlinx.coroutines.flow.update
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import com.example.expensetracker.widget.ExpenseAppWidget
import androidx.glance.appwidget.updateAll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import com.example.expensetracker.ui.screen.AssetScreen

enum class Screen {
    Home, Asset, Statistics, Settings, Backup
}

class MainActivity : AppCompatActivity() {

    private val database by lazy { ExpenseDatabase.getInstance(applicationContext) }
    private val repository by lazy { ExpenseRepository(database.expenseDao(), database.assetDao(), database) }
    
    private val addExpenseTrigger = MutableStateFlow(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            val sharedPreferences = applicationContext.getSharedPreferences("ExpenseAppPrefs", android.content.Context.MODE_PRIVATE)
            val expenseViewModel: ExpenseViewModel = viewModel(
                factory = ExpenseViewModel.factory(repository, sharedPreferences)
            )
            val uiState by expenseViewModel.uiState.collectAsStateWithLifecycle()
            val aiConfigState by expenseViewModel.aiConfigState.collectAsStateWithLifecycle()
            val aiAccountingUiState by expenseViewModel.aiAccountingUiState.collectAsStateWithLifecycle()

            ExpenseTrackerTheme(
                dynamicColor = uiState.dynamicColorEnabled,
                themeColor = uiState.themeColor,
                amoledDarkModeEnabled = uiState.amoledDarkModeEnabled,
                themeMode = uiState.themeMode
            ) {
                val appLockManager = (application as ExpenseTrackerApplication).appLockManager
                val isLocked by appLockManager.isLocked.collectAsStateWithLifecycle()

                val addExpenseTriggerValue by addExpenseTrigger.collectAsState()
                var currentScreen by remember { mutableStateOf(Screen.Home) }

                BackHandler(enabled = currentScreen != Screen.Home) {
                    if (currentScreen == Screen.Backup) {
                        currentScreen = Screen.Settings
                    } else {
                        currentScreen = Screen.Home
                    }
                }

                val mainScreens = if (uiState.assetPageEnabled) {
                    listOf(Screen.Home, Screen.Asset, Screen.Statistics)
                } else {
                    listOf(Screen.Home, Screen.Statistics)
                }

                // If asset page was disabled while viewing it, go home
                if (!uiState.assetPageEnabled && currentScreen == Screen.Asset) {
                    currentScreen = Screen.Home
                }

                Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    bottomBar = {
                        if (currentScreen in mainScreens) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentScreen == Screen.Home,
                                    onClick = { currentScreen = Screen.Home },
                                    icon = { Icon(Icons.Default.Edit, contentDescription = "记账") },
                                    label = { Text("记账") }
                                )
                                if (uiState.assetPageEnabled) {
                                    NavigationBarItem(
                                        selected = currentScreen == Screen.Asset,
                                        onClick = { currentScreen = Screen.Asset },
                                        icon = { Icon(Icons.Default.AccountBalance, contentDescription = "资产") },
                                        label = { Text("资产") }
                                    )
                                }
                                NavigationBarItem(
                                    selected = currentScreen == Screen.Statistics,
                                    onClick = { currentScreen = Screen.Statistics },
                                    icon = { Icon(Icons.Default.BarChart, contentDescription = "统计") },
                                    label = { Text("统计") }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Crossfade(
                        targetState = currentScreen,
                        label = "ScreenTransition",
                        modifier = Modifier.padding(innerPadding)
                    ) { screen ->
                        when (screen) {
                            Screen.Home -> {
                                ExpenseListScreen(
                                    uiState = uiState,
                                    isAmountValid = expenseViewModel::isAmountValid,
                                    searchQuery = uiState.searchQuery,
                                    addExpenseTrigger = addExpenseTriggerValue,
                                    onSearchQueryChange = { query ->
                                        expenseViewModel.updateSearchQuery(query)
                                    },
                                    onAddExpense = { amountInput, type, category, note, assetId, dateMillis ->
                                        expenseViewModel.addExpense(amountInput, type, category, note, assetId, dateMillis)
                                        updateWidget()
                                    },
                                    onUpdateExpense = { id, amountInput, type, category, note, assetId ->
                                        expenseViewModel.updateExpense(id, amountInput, type, category, note, assetId)
                                        updateWidget()
                                    },
                                    onDeleteExpense = { id ->
                                        expenseViewModel.deleteExpense(id)
                                        updateWidget()
                                    },
                                    onDeleteMultipleExpenses = { ids ->
                                        expenseViewModel.deleteMultipleExpenses(ids)
                                        updateWidget()
                                    },
                                    onBatchUpdateCategory = { ids, category ->
                                        expenseViewModel.updateCategoriesForIds(ids, category)
                                    },
                                    onApplyDateRange = { startDateInput, endDateInput ->
                                        expenseViewModel.applyDateRange(startDateInput, endDateInput)
                                    },
                                    onClearDateRange = {
                                        expenseViewModel.clearDateRange()
                                    },
                                    onSetMonthlyBudget = { amountInput ->
                                        expenseViewModel.setMonthlyBudget(amountInput)
                                    },
                                    onClearMonthlyBudget = {
                                        expenseViewModel.clearMonthlyBudget()
                                    },
                                    onPreviousMonth = {
                                        expenseViewModel.goToPreviousMonth()
                                    },
                                    onNextMonth = {
                                        expenseViewModel.goToNextMonth()
                                    },
                                    onCurrentMonth = {
                                        expenseViewModel.goToCurrentMonth()
                                    },
                                    onSettingsClick = {
                                        currentScreen = Screen.Settings
                                    },
                                    onStatisticsClick = {
                                        currentScreen = Screen.Statistics
                                    },
                                    aiAccountingUiState = aiAccountingUiState,
                                    onAiAccountingSaved = {
                                        updateWidget()
                                    },
                                    viewModel = expenseViewModel
                                )
                            }
                            Screen.Asset -> {
                                AssetScreen(viewModel = expenseViewModel)
                            }
                            Screen.Settings -> {
                                SettingsScreen(
                                    uiState = uiState,
                                    aiConfigState = aiConfigState,
                                    sharedPreferences = sharedPreferences,
                                    onDynamicColorChange = { expenseViewModel.updateDynamicColor(it) },
                                    onThemeColorChange = { expenseViewModel.updateThemeColor(it) },
                                    onAmoledDarkModeChange = { expenseViewModel.updateAmoledDarkMode(it) },
                                    onThemeModeChange = { expenseViewModel.updateThemeMode(it) },
                                    onAppLockChange = { expenseViewModel.updateAppLockEnabled(it) },
                                    onBiometricUnlockChange = { expenseViewModel.updateBiometricUnlockEnabled(it) },
                                    onAssetPageChange = { expenseViewModel.updateAssetPageEnabled(it) },
                                    onAutoWebDavBackupOnEntryChange = { expenseViewModel.updateAutoWebDavBackupOnEntryEnabled(it) },
                                    onBackClick = { currentScreen = Screen.Home },
                                    onBackupClick = { currentScreen = Screen.Backup },
                                    onAiAnalysisClick = {
                                        startActivity(Intent(this@MainActivity, AiAnalysisActivity::class.java))
                                    },
                                    onAiBaseUrlChange = expenseViewModel::updateAiBaseUrl,
                                    onAiApiKeyChange = expenseViewModel::updateAiApiKey,
                                    onAiModelChange = expenseViewModel::updateAiModel,
                                    onFetchAiModels = expenseViewModel::fetchAiModels,
                                    onTestAiConnection = expenseViewModel::testAiConnection,
                                    onClearAiTestMessage = expenseViewModel::clearAiTestMessage,
                                    onGenerateTestData = {
                                        expenseViewModel.generateTestData()
                                    }
                                )
                            }
                            Screen.Backup -> {
                                com.example.expensetracker.ui.screen.BackupScreen(
                                    viewModel = expenseViewModel,
                                    onBackClick = { currentScreen = Screen.Settings },
                                    onExportUri = { uri ->
                                        expenseViewModel.exportDataToUri(applicationContext, uri)
                                    },
                                    onImportUri = { uri ->
                                        expenseViewModel.importDataFromUri(applicationContext, uri)
                                    }
                                )
                            }
                            Screen.Statistics -> {
                                com.example.expensetracker.ui.screen.StatisticsScreen(
                                    uiState = uiState,
                                    onBackClick = { currentScreen = Screen.Home },
                                    onDateRangeChanged = { start, end ->
                                        expenseViewModel.applyDateRange(start, end)
                                    }
                                )
                            }
                        }
                    }
                }

                // Lock screen overlay
                if (isLocked) {
                    LockScreen(
                        sharedPreferences = sharedPreferences,
                        onUnlocked = { appLockManager.unlock() },
                        onBiometricClick = {
                            BiometricHelper.authenticate(
                                activity = this@MainActivity,
                                onSuccess = { appLockManager.unlock() },
                                onFailure = { }
                            )
                        },
                        biometricEnabled = uiState.biometricUnlockEnabled
                            && BiometricHelper.canAuthenticate(this@MainActivity)
                    )
                }
                } // Box
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == "com.example.expensetracker.ACTION_ADD_EXPENSE") {
            addExpenseTrigger.value = System.currentTimeMillis()
        }
    }

    private fun updateWidget() {
        lifecycleScope.launch {
            ExpenseAppWidget().updateAll(applicationContext)
        }
    }
}
