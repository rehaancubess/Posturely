package com.mobil80.posturely

import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNotificationName

actual fun createPostureTrackingInterface(): PostureTrackingInterface {
    return IOSPostureTrackingInterface()
}

class IOSPostureTrackingInterface : PostureTrackingInterface {
    private var isCurrentlyTracking = false
    private var currentPoseData = "No pose data available (iOS)"
    private var currentLandmarks = mutableListOf<Pair<Float, Float>>()
    private var landmarkCount = 0
    
    init {
        // Listen for pose landmark updates from Swift
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = "PoseLandmarksUpdated",
            `object` = null,
            queue = null
        ) { notification ->
            notification?.let { notif ->
                val userInfo = notif.userInfo
                if (userInfo != null) {
                    val landmarks = userInfo["landmarks"]
                    
                    if (landmarks is List<*>) {
                        val newLandmarks = mutableListOf<Pair<Float, Float>>()
                        
                        for (i in landmarks.indices) {
                            val landmark = landmarks[i]
                            if (landmark is Map<*, *>) {
                                val xValue = landmark["x"]
                                val yValue = landmark["y"]
                                
                                val x = when (xValue) {
                                    is Double -> xValue.toFloat()
                                    is Float -> xValue
                                    is Int -> xValue.toFloat()
                                    is Number -> xValue.toFloat()
                                    else -> 0.5f
                                }
                                
                                val y = when (yValue) {
                                    is Double -> yValue.toFloat()
                                    is Float -> yValue
                                    is Int -> yValue.toFloat()
                                    is Number -> yValue.toFloat()
                                    else -> 0.5f
                                }
                                
                                newLandmarks.add(Pair(x, y))
                            }
                        }
                        
                        if (newLandmarks.isNotEmpty()) {
                            // Apply 180-degree rotation (flip both x and y)
                            val rotatedLandmarks = newLandmarks.map { (x, y) ->
                                Pair(1f - x, 1f - y)
                            }
                            updateLandmarks(rotatedLandmarks)
                        }
                    }
                }
            }
        }
    }
    
    override fun startTracking() {
        if (!isCurrentlyTracking) {
            isCurrentlyTracking = true
            currentPoseData = "Starting pose detection on iOS..."
            currentLandmarks.clear()
            landmarkCount = 0
            
            // Post a notification to trigger the native camera
            NSNotificationCenter.defaultCenter.postNotificationName(
                aName = "StartPostureTracking",
                `object` = null
            )
        }
    }
    
    override fun stopTracking() {
        if (isCurrentlyTracking) {
            isCurrentlyTracking = false
            currentPoseData = "Stopping pose detection on iOS..."
            currentLandmarks.clear()
            landmarkCount = 0
            
            // Post a notification to stop the native camera
            NSNotificationCenter.defaultCenter.postNotificationName(
                aName = "StopPostureTracking",
                `object` = null
            )
        }
    }
    
    override fun isTracking(): Boolean {
        return isCurrentlyTracking
    }
    
    override fun getPoseData(): String {
        return if (isCurrentlyTracking) {
            if (landmarkCount > 0) {

                // Return actual pose data with landmark information
                val noseX = if (currentLandmarks.isNotEmpty()) currentLandmarks[0].first else 0.5f
                val noseY = if (currentLandmarks.isNotEmpty()) currentLandmarks[0].second else 0.5f
                val leftShoulderX = if (currentLandmarks.size > 11) currentLandmarks[11].first else 0.5f
                val leftShoulderY = if (currentLandmarks.size > 11) currentLandmarks[11].second else 0.5f
                val rightShoulderX = if (currentLandmarks.size > 12) currentLandmarks[12].first else 0.5f
                val rightShoulderY = if (currentLandmarks.size > 12) currentLandmarks[12].second else 0.5f
                
                // Calculate distances and ratios (in normalized coordinates for ratio)
                val noseToShoulderCenter = kotlin.math.sqrt(
                    (noseX - (leftShoulderX + rightShoulderX) / 2) * (noseX - (leftShoulderX + rightShoulderX) / 2) + 
                    (noseY - (leftShoulderY + rightShoulderY) / 2) * (noseY - (leftShoulderY + rightShoulderY) / 2)
                )
                
                // Calculate shoulder width for reference
                val shoulderWidth = kotlin.math.sqrt(
                    (leftShoulderX - rightShoulderX) * (leftShoulderX - rightShoulderX) + 
                    (leftShoulderY - rightShoulderY) * (leftShoulderY - rightShoulderY)
                )
                
                // Calculate ratio (normalized by shoulder width)
                val ratioCenter = if (shoulderWidth > 0) noseToShoulderCenter / shoulderWidth else 0f
                
                """
                Pose Detected!
                Landmarks: $landmarkCount
                Head: x: ${(noseX * 1000).toInt() / 1000.0}, y: ${(noseY * 1000).toInt() / 1000.0}
                Shoulders: L(${(leftShoulderX * 1000).toInt() / 1000.0}), R(${(rightShoulderX * 1000).toInt() / 1000.0})
                
                Distance Ratio (normalized):
                Nose to Shoulder Center: ${(ratioCenter * 100).toInt() / 100.0}
                """.trimIndent()
            } else {

                "Pose detection active - waiting for landmarks..."
            }
        } else {

            "No pose data available (iOS)"
        }
    }
    
    override fun getLandmarkCount(): Int {
        return landmarkCount
    }
    
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
    
    // Method to update pose data from native iOS code
    fun updatePoseData(data: String) {
        currentPoseData = data
    }
    
    // Method to update landmark data from native iOS code
    fun updateLandmarks(landmarks: List<Pair<Float, Float>>) {
        currentLandmarks.clear()
        currentLandmarks.addAll(landmarks)
        landmarkCount = landmarks.size
    }
    
    fun cleanup() {
        // Cleanup not needed for simplified approach
    }
} 