package com.mobil80.posturely

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.mobil80.posturely.native.PostureTrackingManager
import com.mobil80.posturely.native.PoseDataBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Global instance that will be shared
private var globalPostureInterface: AndroidPostureTrackingInterface? = null

actual fun createPostureTrackingInterface(): PostureTrackingInterface {
            if (globalPostureInterface == null) {
            globalPostureInterface = AndroidPostureTrackingInterface()
        }
    return globalPostureInterface!!
}

// Android-specific function to set up context and lifecycle
fun setupAndroidPostureTracking(context: android.content.Context, lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
    // Create the global instance if it doesn't exist
    if (globalPostureInterface == null) {
        globalPostureInterface = AndroidPostureTrackingInterface()
    }
    
    // Configure the global instance
    globalPostureInterface?.setContext(context)
    globalPostureInterface?.setLifecycleOwner(lifecycleOwner)
}

class AndroidPostureTrackingInterface : PostureTrackingInterface {
    private var isCurrentlyTracking = false
    private var currentPoseData = "No pose data available (Android)"
    private var currentLandmarks = mutableListOf<Pair<Float, Float>>()
    private var landmarkCount = 0
    
    // Performance optimization: throttle updates
    private var lastUpdateTime = 0L
    private val updateInterval = 20L // Reduced to 20ms for very responsive updates
    
    // StateFlow for real-time landmark updates
    private val _landmarksFlow = MutableStateFlow<List<Pair<Float, Float>>>(emptyList())
    val landmarksFlow: StateFlow<List<Pair<Float, Float>>> = _landmarksFlow.asStateFlow()
    
    // Native Android services
    private var postureTrackingManager: PostureTrackingManager? = null
    private var context: Context? = null
    private var lifecycleOwner: LifecycleOwner? = null
    
    override fun startTracking() {
        if (!isCurrentlyTracking) {
            isCurrentlyTracking = true
            currentPoseData = "Starting pose detection on Android..."
            currentLandmarks.clear()
            landmarkCount = 0
            
            Log.d("AndroidPostureTracking", "Starting tracking - context: ${context != null}, lifecycle: ${lifecycleOwner != null}")
            
            // Initialize native Android services if context is available
            if (context != null && lifecycleOwner != null) {
                try {
                    Log.d("AndroidPostureTracking", "Initializing PostureTrackingManager")
                    postureTrackingManager = PostureTrackingManager(context!!)
                    postureTrackingManager?.startPostureTracking(lifecycleOwner!!)
                    Log.d("AndroidPostureTracking", "PostureTrackingManager started successfully")
                } catch (e: Exception) {
                    Log.e("AndroidPostureTracking", "Error starting PostureTrackingManager: ${e.message}")
                    // Fallback to simulation
                    simulatePoseDetection()
                }
            } else {
                Log.w("AndroidPostureTracking", "Context or lifecycle not available, using simulation")
                // Fallback to simulation if context is not available
                simulatePoseDetection()
            }
        }
    }
    
    override fun stopTracking() {
        if (isCurrentlyTracking) {
            isCurrentlyTracking = false
            currentPoseData = "Stopping pose detection on Android..."
            currentLandmarks.clear()
            landmarkCount = 0
            
            // Stop native Android services
            postureTrackingManager?.stopPostureTracking()
            postureTrackingManager = null
        }
    }
    
    override fun isTracking(): Boolean {
        return isCurrentlyTracking
    }
    
    override fun getPoseData(): String {
        return if (isCurrentlyTracking) {
            val landmarkCount = getLandmarkCount()
            if (landmarkCount >= 33) {
                // Calculate distances and ratios
                val noseX = getLandmarkX(0)
                val noseY = getLandmarkY(0)
                val leftShoulderX = getLandmarkX(11)
                val leftShoulderY = getLandmarkY(11)
                val rightShoulderX = getLandmarkX(12)
                val rightShoulderY = getLandmarkY(12)
                
                // Calculate distances and ratios (in normalized coordinates for ratio)
                val noseToShoulderCenter = kotlin.math.sqrt(
                    (noseX - (leftShoulderX + rightShoulderX) / 2) * (noseX - (leftShoulderX + rightShoulderX) / 2) + 
                    (noseY - (leftShoulderY + rightShoulderY) / 2) * (noseY - (leftShoulderY + rightShoulderY) / 2)
                )
                
                // Calculate shoulder width for reference
                val shoulderWidth = kotlin.math.sqrt(
                    (leftShoulderX - rightShoulderX) * (leftShoulderX - rightShoulderX) + (leftShoulderY - rightShoulderY) * (leftShoulderY - rightShoulderY)
                )
                
                // Calculate ratio (normalized by shoulder width)
                val ratioCenter = if (shoulderWidth > 0) noseToShoulderCenter / shoulderWidth else 0f
                
                """
                Pose Detected!
                Landmarks: $landmarkCount
                Head: x: ${String.format("%.3f", noseX)}, y: ${String.format("%.3f", noseY)}
                Shoulders: L(${String.format("%.3f", leftShoulderX)}), R(${String.format("%.3f", rightShoulderX)})
                
                Distance Ratio (normalized):
                Nose to Shoulder Center: ${String.format("%.2f", ratioCenter)}
                """.trimIndent()
            } else {
                "Pose detection active - $landmarkCount landmarks detected"
            }
        } else {
            "No pose data available (Android)"
        }
    }
    
    override fun getLandmarkCount(): Int {
        // Try to get data from native services first, fallback to local state
        return try {
            val count = PoseDataBridge.getLandmarkCount()
            Log.d("AndroidPostureTracking", "Got landmark count from bridge: $count")
            count
        } catch (e: Exception) {
            Log.w("AndroidPostureTracking", "Failed to get landmark count from bridge, using local: $landmarkCount", e)
            landmarkCount
        }
    }
    
    override fun getLandmarkX(index: Int): Float {
        // Try to get data from native services first, fallback to local state
        return try {
            val x = PoseDataBridge.getLandmarkX(index)
            if (index == 0) Log.d("AndroidPostureTracking", "Got landmark X from bridge: index=$index, x=$x")
            x
        } catch (e: Exception) {
            Log.w("AndroidPostureTracking", "Failed to get landmark X from bridge, using local", e)
            if (index >= 0 && index < currentLandmarks.size) {
                currentLandmarks[index].first
            } else {
                0.5f
            }
        }
    }
    
    override fun getLandmarkY(index: Int): Float {
        // Try to get data from native services first, fallback to local state
        return try {
            val y = PoseDataBridge.getLandmarkY(index)
            if (index == 0) Log.d("AndroidPostureTracking", "Got landmark Y from bridge: index=$index, y=$y")
            y
        } catch (e: Exception) {
            Log.w("AndroidPostureTracking", "Failed to get landmark Y from bridge, using local", e)
            if (index >= 0 && index < currentLandmarks.size) {
                currentLandmarks[index].second
            } else {
                0.5f
            }
        }
    }
    
    private fun updateLandmarks(landmarks: List<Pair<Float, Float>>) {
        // Throttle updates for better performance
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime < updateInterval) {
            return
        }
        lastUpdateTime = currentTime
        
        currentLandmarks.clear()
        currentLandmarks.addAll(landmarks)
        landmarkCount = landmarks.size
        _landmarksFlow.value = landmarks
        
        Log.d("AndroidPostureTracking", "Updated landmarks - count: $landmarkCount")
        if (landmarks.isNotEmpty()) {
            Log.d("AndroidPostureTracking", "First landmark: (${landmarks[0].first}, ${landmarks[0].second})")
        }
    }
    
    private fun simulatePoseDetection() {
        // Simulate realistic pose landmarks for testing
        val simulatedLandmarks = listOf(
            Pair(0.5f, 0.1f),   // Nose
            Pair(0.48f, 0.08f), // Left eye
            Pair(0.52f, 0.08f), // Right eye
            Pair(0.45f, 0.08f), // Left ear
            Pair(0.55f, 0.08f), // Right ear
            Pair(0.49f, 0.12f), // Left mouth
            Pair(0.51f, 0.12f), // Right mouth
            Pair(0.47f, 0.15f), // Left shoulder
            Pair(0.53f, 0.15f), // Right shoulder
            Pair(0.46f, 0.18f), // Left elbow
            Pair(0.54f, 0.18f), // Right elbow
            Pair(0.45f, 0.21f), // Left wrist
            Pair(0.55f, 0.21f), // Right wrist
            Pair(0.44f, 0.24f), // Left pinky
            Pair(0.56f, 0.24f), // Right pinky
            Pair(0.43f, 0.25f), // Left index
            Pair(0.57f, 0.25f), // Right index
            Pair(0.42f, 0.26f), // Left thumb
            Pair(0.58f, 0.26f), // Right thumb
            Pair(0.48f, 0.3f),  // Left hip
            Pair(0.52f, 0.3f),  // Right hip
            Pair(0.47f, 0.4f),  // Left knee
            Pair(0.53f, 0.4f),  // Right knee
            Pair(0.46f, 0.5f),  // Left ankle
            Pair(0.54f, 0.5f),  // Right ankle
            Pair(0.45f, 0.55f), // Left heel
            Pair(0.55f, 0.55f), // Right heel
            Pair(0.44f, 0.56f), // Left foot index
            Pair(0.56f, 0.56f), // Right foot index
            Pair(0.5f, 0.05f)   // Additional landmark
        )
        
        updateLandmarks(simulatedLandmarks)
        Log.d("AndroidPostureTracking", "Simulated pose detection with ${simulatedLandmarks.size} landmarks")
    }
    
    // Methods to set context and lifecycle owner for native services
    fun setContext(context: Context) {
        this.context = context
        Log.d("AndroidPostureTracking", "Context set successfully")
    }
    
    fun setLifecycleOwner(lifecycleOwner: LifecycleOwner) {
        this.lifecycleOwner = lifecycleOwner
        Log.d("AndroidPostureTracking", "LifecycleOwner set successfully")
    }
    
    fun cleanup() {
        stopTracking()
    }
} 