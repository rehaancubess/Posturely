package com.example.posturelynew.native

import android.util.Log

object PoseDataManager {
    private var currentLandmarks: List<PoseLandmark> = emptyList()
    private var currentWorldLandmarks: List<PoseLandmark> = emptyList()
    
    // Performance monitoring
    private var lastUpdateTime = 0L
    private var frameCount = 0
    
    fun updatePoseData(landmarks: List<PoseLandmark>, worldLandmarks: List<PoseLandmark>) {
        currentLandmarks = landmarks
        currentWorldLandmarks = worldLandmarks
        
        // Performance monitoring
        frameCount++
        val currentTime = System.currentTimeMillis()
        if (frameCount % 30 == 0) { // Log every 30 frames
            val fps = 1000.0 / (currentTime - lastUpdateTime) * 30
            Log.d("PoseDataManager", "Performance: ~${String.format("%.1f", fps)} FPS, ${landmarks.size} landmarks")
        }
        lastUpdateTime = currentTime
        
        Log.d("PoseDataManager", "Updated pose data with ${landmarks.size} landmarks")
    }
    
    fun getLandmarks(): List<PoseLandmark> = currentLandmarks
    
    fun getWorldLandmarks(): List<PoseLandmark> = currentWorldLandmarks
    
    fun getLandmarkCount(): Int = currentLandmarks.size
    
    fun getLandmarkX(index: Int): Float {
        return if (index < currentLandmarks.size) currentLandmarks[index].x else 0.5f
    }
    
    fun getLandmarkY(index: Int): Float {
        return if (index < currentLandmarks.size) currentLandmarks[index].y else 0.5f
    }
    
    fun clearData() {
        currentLandmarks = emptyList()
        currentWorldLandmarks = emptyList()
    }
} 