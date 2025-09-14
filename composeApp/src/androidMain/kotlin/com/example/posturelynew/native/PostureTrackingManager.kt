package com.example.posturelynew.native

import android.content.Context

import androidx.lifecycle.LifecycleOwner
import com.google.mediapipe.framework.image.MPImage

class PostureTrackingManager(private val context: Context) {
    private var cameraFragment: CameraFragment? = null
    private var poseLandmarkerHelper: PoseLandmarkerHelper? = null
    private var isTracking = false
    
    fun startPostureTracking(lifecycleOwner: LifecycleOwner) {
        if (isTracking) {
            android.util.Log.d("PostureTrackingManager", "Already tracking, skipping start")
            return
        }
        
        try {
            android.util.Log.d("PostureTrackingManager", "Starting posture tracking")
            
            // Initialize PoseLandmarkerHelper with listener
            poseLandmarkerHelper = PoseLandmarkerHelper(context, object : PoseLandmarkerHelper.PoseLandmarkerListener {
                override fun onPoseLandmarkerResult(result: com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult, image: MPImage) {
                    // Result is already handled in PoseLandmarkerHelper
                }
                
                override fun onPoseLandmarkerError(error: RuntimeException) {
                    android.util.Log.e("PostureTrackingManager", "PoseLandmarker error: ${error.message}")
                }
            })
            
            android.util.Log.d("PostureTrackingManager", "Setting up pose landmarker")
            // Set up pose landmarker
            poseLandmarkerHelper?.setupPoseLandmarker()
            
            android.util.Log.d("PostureTrackingManager", "Initializing camera fragment")
            // Initialize camera fragment
            cameraFragment = CameraFragment(context, poseLandmarkerHelper!!)
            
            android.util.Log.d("PostureTrackingManager", "Starting camera")
            // Start camera
            cameraFragment?.startCamera(lifecycleOwner)
            
            isTracking = true
            android.util.Log.d("PostureTrackingManager", "Posture tracking started successfully")
        } catch (e: Exception) {
            android.util.Log.e("PostureTrackingManager", "Error starting posture tracking: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun stopPostureTracking() {
        if (!isTracking) return
        
        try {
            // Stop camera first
            cameraFragment?.stopCamera()
            
            // Clear pose data
            PoseDataManager.clearData()
            
            // Close pose landmarker helper
            poseLandmarkerHelper?.close()
            
            // Reset references
            cameraFragment = null
            poseLandmarkerHelper = null
            
            isTracking = false
        } catch (e: Exception) {
            // Handle error silently
        }
    }
    
    fun isCurrentlyTracking(): Boolean = isTracking
} 