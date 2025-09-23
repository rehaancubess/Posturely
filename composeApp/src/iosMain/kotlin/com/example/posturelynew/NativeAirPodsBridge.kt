package com.mobil80.posturely

import kotlinx.cinterop.*
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class)
object NativeAirPodsBridge {
    
    private var isConnected = false
    private var deviceName: String? = null
    private var hasMotionData = false
    private var pitchAngle = 0.0
    
    init {
        // Listen for AirPods connection status changes from Swift
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = "AirPodsConnectionStatusChanged",
            `object` = null,
            queue = null
        ) { notification ->
            notification?.let { notif ->
                println("🔍 [DEBUG] Received AirPodsConnectionStatusChanged notification")
                val userInfo = notif.userInfo
                if (userInfo != null) {
                    val connected = userInfo["isConnected"] as? Boolean ?: false
                    val name = userInfo["deviceName"] as? String ?: "Unknown"
                    val motionDataField = userInfo["hasMotionData"] as? Boolean
                    val pitch = userInfo["pitchAngle"] as? Double
                    
                    println("🔍 [DEBUG] Updating bridge state - Connected: $connected, Device: $name, Motion field: ${motionDataField ?: "(unchanged)"}")
                    
                    // Always update connection and device name
                    isConnected = connected
                    deviceName = if (connected) name else null
                    
                    // Only update motion flags if motion data is explicitly present and true
                    // Do NOT clear existing motion state on connection-only updates
                    if (motionDataField == true || pitch != null) {
                        hasMotionData = true
                        if (pitch != null) {
                            pitchAngle = pitch
                        }
                    }
                    
                    println("🔍 [DEBUG] Bridge state updated - isConnected: $isConnected, deviceName: $deviceName, hasMotionData: $hasMotionData, pitch: $pitchAngle")
                }
            }
        }
        
        // Check initial status
        checkInitialStatus()
    }
    
    fun checkInitialStatus() {
        // Post a notification to trigger the Swift service to check status
        NSNotificationCenter.defaultCenter.postNotificationName(
            aName = "CheckAirPodsStatus",
            `object` = null
        )
        println("🔍 [DEBUG] Posted CheckAirPodsStatus notification")
    }
    
    fun isAirPodsConnected(): Boolean {
        println("🔍 [DEBUG] isAirPodsConnected() called - returning: $isConnected")
        return isConnected
    }
    
    fun getConnectedDeviceName(): String? {
        println("🔍 [DEBUG] getConnectedDeviceName() called - returning: $deviceName")
        return deviceName
    }
    
    fun hasMotionData(): Boolean {
        println("🔍 [DEBUG] hasMotionData() called - returning: $hasMotionData")
        return hasMotionData
    }
    
    fun getCurrentPitchAngle(): Double {
        println("🔍 [DEBUG] getCurrentPitchAngle() called - returning: $pitchAngle°")
        return pitchAngle
    }
    
    fun startMonitoring() {
        println("🔍 [DEBUG] startMonitoring() called - starting motion tracking")
        // Post a notification to start motion tracking in Swift service
        NSNotificationCenter.defaultCenter.postNotificationName(
            aName = "StartAirPodsMotionTracking",
            `object` = null
        )
    }
    
    fun stopMonitoring() {
        println("🔍 [DEBUG] stopMonitoring() called - stopping Swift service monitoring and motion tracking")
        // Post a notification to stop the Swift service monitoring
        NSNotificationCenter.defaultCenter.postNotificationName(
            aName = "StopAirPodsMonitoring",
            `object` = null
        )
    }
    
    fun resetState() {
        // Clear bridge-side cached state so next session starts clean
        isConnected = false
        deviceName = null
        hasMotionData = false
        pitchAngle = 0.0
        println("🔍 [DEBUG] Bridge state reset")
    }
}
