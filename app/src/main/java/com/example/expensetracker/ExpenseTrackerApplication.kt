package com.example.expensetracker

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.expensetracker.security.AppLockManager

class ExpenseTrackerApplication : Application() {

    lateinit var appLockManager: AppLockManager
        private set

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("ExpenseAppPrefs", MODE_PRIVATE)
        appLockManager = AppLockManager(prefs)

        // Lock on cold start if enabled
        appLockManager.checkAndLock()

        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    appLockManager.onAppBackground()
                }

                override fun onStart(owner: LifecycleOwner) {
                    appLockManager.onAppForeground()
                }
            }
        )
    }
}
