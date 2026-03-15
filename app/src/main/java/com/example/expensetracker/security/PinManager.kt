package com.example.expensetracker.security

import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

object PinManager {
    private const val KEY_PIN_HASH = "app_lock_pin_hash"
    private const val KEY_PIN_SALT = "app_lock_pin_salt"

    fun setPin(prefs: SharedPreferences, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hash = hashPin(salt, pin)
        prefs.edit()
            .putString(KEY_PIN_SALT, saltBase64)
            .putString(KEY_PIN_HASH, hash)
            .apply()
    }

    fun verifyPin(prefs: SharedPreferences, pin: String): Boolean {
        val saltBase64 = prefs.getString(KEY_PIN_SALT, "") ?: return false
        val storedHash = prefs.getString(KEY_PIN_HASH, "") ?: return false
        if (saltBase64.isEmpty() || storedHash.isEmpty()) return false
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        return hashPin(salt, pin) == storedHash
    }

    fun hasPin(prefs: SharedPreferences): Boolean {
        val hash = prefs.getString(KEY_PIN_HASH, "") ?: ""
        return hash.isNotEmpty()
    }

    fun clearPin(prefs: SharedPreferences) {
        prefs.edit()
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_HASH)
            .apply()
    }

    private fun hashPin(salt: ByteArray, pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        md.update(pin.toByteArray(Charsets.UTF_8))
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
