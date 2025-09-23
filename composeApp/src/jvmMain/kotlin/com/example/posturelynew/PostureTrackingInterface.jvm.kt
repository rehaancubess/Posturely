package com.mobil80.posturely

actual fun createPostureTrackingInterface(): PostureTrackingInterface {
    return JvmPostureTrackingInterface()
}

class JvmPostureTrackingInterface : PostureTrackingInterface {
    private var isCurrentlyTracking = false
    private var currentPoseData = "No pose data available (JVM)"
    
    override fun startTracking() {
        isCurrentlyTracking = true
        currentPoseData = "Starting pose detection on JVM..."
        println("JVM: Starting posture tracking")
    }
    
    override fun stopTracking() {
        isCurrentlyTracking = false
        currentPoseData = "Stopping pose detection on JVM..."
        println("JVM: Stopping posture tracking")
    }
    
    override fun isTracking(): Boolean {
        return isCurrentlyTracking
    }
    
    override fun getPoseData(): String {
        return currentPoseData
    }
    
    override fun getLandmarkCount(): Int {
        return 0 // Placeholder for JVM
    }
    
    override fun getLandmarkX(index: Int): Float {
        return 0.0f // Placeholder for JVM
    }
    
    override fun getLandmarkY(index: Int): Float {
        return 0.0f // Placeholder for JVM
    }
} 