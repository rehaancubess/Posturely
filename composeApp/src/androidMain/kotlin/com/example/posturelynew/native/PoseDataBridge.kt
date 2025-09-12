package com.example.posturelynew.native

object PoseDataBridge {
    // Static methods to expose pose data to Compose
    @JvmStatic
    fun getLandmarkCount(): Int = PoseDataManager.getLandmarkCount()
    
    @JvmStatic
    fun getLandmarkX(index: Int): Float = PoseDataManager.getLandmarkX(index)
    
    @JvmStatic
    fun getLandmarkY(index: Int): Float = PoseDataManager.getLandmarkY(index)
    
    @JvmStatic
    fun getLandmarks(): List<PoseLandmark> = PoseDataManager.getLandmarks()
    
    // Method to update global landmarks (called from native code)
    fun updateGlobalLandmarks(landmarks: List<PoseLandmark>, worldLandmarks: List<PoseLandmark>) {
        PoseDataManager.updatePoseData(landmarks, worldLandmarks)
    }
} 