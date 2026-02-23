package com.example.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.data.ExpenseRepository
import com.example.expensetracker.data.local.ExpenseDatabase
import com.example.expensetracker.theme.ExpenseTrackerTheme
import com.example.expensetracker.ui.screen.ExpenseListScreen
import com.example.expensetracker.ui.screen.SettingsScreen
import com.example.expensetracker.ui.viewmodel.ExpenseViewModel
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.Crossfade
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import com.example.expensetracker.widget.ExpenseAppWidget
import androidx.glance.appwidget.updateAll

enum class Screen {
    Home, Settings, Statistics
}

class MainActivity : ComponentActivity() {

    private val database by lazy { ExpenseDatabase.getInstance(applicationContext) }
    private val repository by lazy { ExpenseRepository(database.expenseDao()) }
    
    private val addExpenseTrigger = MutableStateFlow(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
                val expenseViewModel: ExpenseViewModel = viewModel(
                    factory = ExpenseViewModel.factory(repository)
                )
                val uiState by expenseViewModel.uiState.collectAsStateWithLifecycle()

                val addExpenseTriggerValue by addExpenseTrigger.collectAsState()
                var currentScreen by remember { mutableStateOf(Screen.Home) }

                BackHandler(enabled = currentScreen == Screen.Settings || currentScreen == Screen.Statistics) {
                    currentScreen = Screen.Home
                }

                Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
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
                                onAddExpense = { amountInput, type, category, note, dateMillis ->
                                    expenseViewModel.addExpense(amountInput, type, category, note, dateMillis)
                                    updateWidget()
                                },
                                onUpdateExpense = { id, amountInput, type, category, note ->
                                    expenseViewModel.updateExpense(id, amountInput, type, category, note)
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
                                viewModel = expenseViewModel
                            )
                        }
                        Screen.Settings -> {
                            SettingsScreen(
                                onBackClick = { currentScreen = Screen.Home },
                                onExportUri = { uri ->
                                    expenseViewModel.exportDataToUri(applicationContext, uri)
                                },
                                onImportUri = { uri ->
                                    expenseViewModel.importDataFromUri(applicationContext, uri)
                                },
                                onGenerateTestData = {
                                    expenseViewModel.generateTestData()
                                }
                            )
                        }
                        Screen.Statistics -> {
                            com.example.expensetracker.ui.screen.StatisticsScreen(
                                uiState = uiState,
                                onBackClick = { currentScreen = Screen.Home }
                            )
                        }
                    }
                }
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
