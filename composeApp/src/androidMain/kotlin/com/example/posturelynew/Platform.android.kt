package com.example.posturelynew

import android.content.Context
import android.content.SharedPreferences
import android.app.Application
import android.content.Intent
import android.net.Uri

actual fun getPlatformName(): String = "Android"

actual fun getPlatformImage(): String = "android"

actual class AirPodsTracker {
    actual fun isConnected(): Boolean = false
    actual fun startTracking() { /* Not supported on Android */ }
    actual fun stopTracking() { /* Not supported on Android */ }
    actual fun getCurrentTiltAngle(): Float = 0f
    actual fun isTracking(): Boolean = false
    actual fun getConnectedDeviceName(): String? = null
}

private fun obtainApplicationContext(): Context {
    // Try to get the Application context via ActivityThread.currentApplication()
    return try {
        val activityThreadClass = Class.forName("android.app.ActivityThread")
        val currentApplicationMethod = activityThreadClass.getMethod("currentApplication")
        val app = currentApplicationMethod.invoke(null) as Application
        app.applicationContext
    } catch (_: Throwable) {
        throw IllegalStateException("Application context not available. Ensure app is initialized before using PlatformStorage.")
    }
}

actual class PlatformStorage {
    private val context: Context by lazy { obtainApplicationContext() }
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("PosturelyPrefs", Context.MODE_PRIVATE)
    }
    
    actual fun saveBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }
    
    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }
    
    actual fun saveString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }
    
    actual fun getString(key: String, defaultValue: String): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }
    
    actual fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}

actual fun openEmailClient(to: String, subject: String, body: String) {
    // Build a mailto URI
    val uri = Uri.parse("mailto:$to").buildUpon()
        .appendQueryParameter("subject", subject)
        .appendQueryParameter("body", body)
        .build()
    val intent = Intent(Intent.ACTION_SENDTO, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        val ctx = obtainApplicationContext()
        ctx.startActivity(intent)
    } catch (_: Throwable) {
        // ignore if no email client
    }
}