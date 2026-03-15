package com.example.expensetracker.security

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppLockManager(private val sharedPreferences: SharedPreferences) {

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val isEnabled: Boolean
        get() = sharedPreferences.getBoolean("app_lock_enabled", false)

    fun checkAndLock() {
        if (isEnabled && PinManager.hasPin(sharedPreferences)) {
            _isLocked.value = true
        }
    }

    fun onAppBackground() {
        // No timestamp needed — immediate lock on any background transition
    }

    fun onAppForeground() {
        if (isEnabled && PinManager.hasPin(sharedPreferences)) {
            _isLocked.value = true
        }
    }

    fun unlock() {
        _isLocked.value = false
    }
}
