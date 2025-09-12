package com.example.posturelynew

import androidx.compose.runtime.Composable

actual fun createPostureTrackingInterface(): PostureTrackingInterface {
    return DesktopPostureTrackingInterface()
}

class DesktopPostureTrackingInterface : PostureTrackingInterface {
    private var isCurrentlyTracking = false
    private var currentPoseData = "No pose data available (Desktop)"
    private var currentLandmarks = mutableListOf<Pair<Float, Float>>()
    private var landmarkCount = 0
    
    override fun startTracking() {
        if (!isCurrentlyTracking) {
            isCurrentlyTracking = true
            currentPoseData = "Starting pose detection on Desktop..."
            currentLandmarks.clear()
            landmarkCount = 0
        }
    }
    
    override fun stopTracking() {
        if (isCurrentlyTracking) {
            isCurrentlyTracking = false
            currentPoseData = "Stopping pose detection on Desktop..."
            currentLandmarks.clear()
            landmarkCount = 0
        }
    }
    
    override fun isTracking(): Boolean = isCurrentlyTracking
    
    override fun getPoseData(): String = currentPoseData
    
    override fun getLandmarkCount(): Int = landmarkCount
    
    override fun getLandmarkX(index: Int): Float {
        return if (index >= 0 && index < currentLandmarks.size) {
            currentLandmarks[index].first
        } else {
            0.5f
        }
    }
    
    override fun getLandmarkY(index: Int): Float {
        return if (index >= 0 && index < currentLandmarks.size) {
            currentLandmarks[index].second
        } else {
            0.5f
        }
    }
}
