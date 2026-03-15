package com.example.expensetracker.ui.screen

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.ExpenseTrackerApplication
import com.example.expensetracker.data.ExpenseRepository
import com.example.expensetracker.data.local.ExpenseDatabase
import com.example.expensetracker.security.BiometricHelper
import com.example.expensetracker.theme.ExpenseTrackerTheme
import com.example.expensetracker.ui.viewmodel.ExpenseViewModel

class TrendDetailActivity : AppCompatActivity() {

    private val database by lazy { ExpenseDatabase.getInstance(applicationContext) }
    private val repository by lazy { ExpenseRepository(database.expenseDao(), database.assetDao(), database) }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val sharedPreferences = applicationContext.getSharedPreferences("ExpenseAppPrefs", MODE_PRIVATE)
            val expenseViewModel: ExpenseViewModel = viewModel(
                factory = ExpenseViewModel.factory(repository, sharedPreferences)
            )
            val uiState by expenseViewModel.uiState.collectAsStateWithLifecycle()

            val appLockManager = (application as ExpenseTrackerApplication).appLockManager
            val isLocked by appLockManager.isLocked.collectAsStateWithLifecycle()

            ExpenseTrackerTheme(
                dynamicColor = uiState.dynamicColorEnabled,
                themeColor = uiState.themeColor,
                amoledDarkModeEnabled = uiState.amoledDarkModeEnabled,
                themeMode = uiState.themeMode
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("收支趋势") },
                                navigationIcon = {
                                    IconButton(onClick = { finish() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background,
                                    titleContentColor = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        }
                    ) { padding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            contentAlignment = Alignment.Center
                        ) {
                            TrendLineChart(
                                dailySummaries = uiState.dailySummaries.reversed(),
                                modifier = Modifier.fillMaxSize().padding(16.dp)
                            )
                        }
                    }

                    if (isLocked) {
                        LockScreen(
                            sharedPreferences = sharedPreferences,
                            onUnlocked = { appLockManager.unlock() },
                            onBiometricClick = {
                                BiometricHelper.authenticate(
                                    activity = this@TrendDetailActivity,
                                    onSuccess = { appLockManager.unlock() },
                                    onFailure = { }
                                )
                            },
                            biometricEnabled = uiState.biometricUnlockEnabled
                                && BiometricHelper.canAuthenticate(this@TrendDetailActivity)
                        )
                    }
                }
            }
        }
    }
}
