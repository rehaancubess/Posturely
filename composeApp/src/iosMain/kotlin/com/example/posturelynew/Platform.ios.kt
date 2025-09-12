package com.example.posturelynew

import platform.Foundation.NSUserDefaults
import platform.CoreMotion.*
import kotlinx.cinterop.ExperimentalForeignApi
import com.example.posturelynew.NativeAirPodsBridge
import kotlinx.coroutines.*
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun getPlatformName(): String = "iOS"

actual fun getPlatformImage(): String = "ios"

@OptIn(ExperimentalForeignApi::class)
actual class AirPodsTracker {
    private var isCurrentlyTracking = false
    private var currentTiltAngle = 0f
    private var connectedDevice: String? = null
    private var hasMotionData = false
    
    actual fun isConnected(): Boolean {
        // Use native iOS method to check for connected AirPods/Beats
        return checkForConnectedAudioDevices()
    }
    
    private fun checkForConnectedAudioDevices(): Boolean {
        try {
            // Single check; avoid spamming status notifications
            println("🔍 [DEBUG] AirPodsTracker - Checking connection once")
            NativeAirPodsBridge.checkInitialStatus()
            runBlocking { delay(150L) }
            val isConnected = NativeAirPodsBridge.isAirPodsConnected()
            if (isConnected) {
                connectedDevice = NativeAirPodsBridge.getConnectedDeviceName()
                // Don't immediately trust hasMotionData here; it will be set when motion notifications come
                println("🔍 [DEBUG] AirPodsTracker - Connected to $connectedDevice (motion pending)")
            }
            return isConnected
        } catch (e: Exception) {
            println("🔍 [DEBUG] AirPodsTracker - Exception: ${e.message}")
            connectedDevice = null
            return false
        }
    }
    
    actual fun startTracking() {
        if (isCurrentlyTracking) return
        if (!isConnected()) return
        
        isCurrentlyTracking = true
        
        // Always request motion tracking from Swift; if permission not granted, Swift handles UI and fallback
        NativeAirPodsBridge.startMonitoring()
        
        println("🔍 [DEBUG] Started AirPods tracking - Device: $connectedDevice")
    }
    
    actual fun stopTracking() {
        isCurrentlyTracking = false
        currentTiltAngle = 0f
        NativeAirPodsBridge.stopMonitoring()
        NativeAirPodsBridge.resetState()
        connectedDevice = null
        hasMotionData = false
        println("🔍 [DEBUG] Stopped AirPods tracking")
    }
    
    actual fun getCurrentTiltAngle(): Float {
        if (!isCurrentlyTracking) return 0f
        if (!NativeAirPodsBridge.isAirPodsConnected()) return 0f
        
        // Read motion state without re-triggering status checks; rely on notifications updating the bridge
        val bridgeHasMotion = NativeAirPodsBridge.hasMotionData()
        if (bridgeHasMotion) {
            hasMotionData = true // persist once seen
            val pitchAngle = NativeAirPodsBridge.getCurrentPitchAngle()
            currentTiltAngle = pitchAngle.toFloat()
            println("🔍 [DEBUG] AirPodsTracker.getCurrentTiltAngle() - Motion data: true, Pitch: $pitchAngle°")
            return currentTiltAngle
        }
        
        // No motion yet; keep last known (0f initially)
        println("🔍 [DEBUG] AirPodsTracker.getCurrentTiltAngle() - Waiting for motion data")
        return currentTiltAngle
    }
    
    actual fun isTracking(): Boolean = isCurrentlyTracking
    actual fun getConnectedDeviceName(): String? = connectedDevice
    fun hasMotionData(): Boolean = hasMotionData
}

actual class PlatformStorage {
    private val userDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults
    
    actual fun saveBoolean(key: String, value: Boolean) {
        userDefaults.setBool(value, key)
        userDefaults.synchronize()
    }
    
    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return userDefaults.boolForKey(key) ?: defaultValue
    }
    
    actual fun saveString(key: String, value: String) {
        userDefaults.setObject(value, key)
        userDefaults.synchronize()
    }
    
    actual fun getString(key: String, defaultValue: String): String {
        return userDefaults.stringForKey(key) ?: defaultValue
    }
    
    actual fun clear() {
        userDefaults.removeObjectForKey("isLoggedIn")
        userDefaults.removeObjectForKey("userEmail")
        userDefaults.synchronize()
    }
}

actual fun openEmailClient(to: String, subject: String, body: String) {
    fun enc(s: String): String = s.replace(" ", "%20")
    val mailto = "mailto:$to?subject=" + enc(subject) +
            "&body=" + enc(body)
    val url = NSURL(string = mailto)
    if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
        UIApplication.sharedApplication.openURL(
            url = url,
            options = emptyMap<Any?, Any?>(),
            completionHandler = null
        )
    }
}