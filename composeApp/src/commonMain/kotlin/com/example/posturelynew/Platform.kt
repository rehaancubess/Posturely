package com.mobil80.posturely

import androidx.compose.runtime.Composable

expect fun getPlatformName(): String

expect fun getPlatformImage(): String

// AirPods tracking interface for cross-platform posture monitoring
expect class AirPodsTracker() {
    fun isConnected(): Boolean
    fun startTracking()
    fun stopTracking()
    fun getCurrentTiltAngle(): Float
    fun isTracking(): Boolean
    fun getConnectedDeviceName(): String?
}

// Storage interface for cross-platform persistent storage
expect class PlatformStorage() {
    fun saveBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun saveString(key: String, value: String)
    fun getString(key: String, defaultValue: String): String
    fun clear()
}

// Cross-platform email intent
expect fun openEmailClient(to: String, subject: String = "", body: String = "")

// Cross-platform URL opening
expect fun openUrl(url: String)

// Tracking permission helpers
// Phone: camera permission for pose tracking
expect fun requestPhoneTrackingPermissions()

// AirPods: motion permission on iOS (no-op on others)
expect fun requestAirPodsPermissions()