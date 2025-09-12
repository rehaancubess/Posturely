package com.example.posturelynew.native

import android.content.Context

import androidx.lifecycle.LifecycleOwner
import com.google.mediapipe.framework.image.MPImage

class PostureTrackingManager(private val context: Context) {
    private var cameraFragment: CameraFragment? = null
    private var poseLandmarkerHelper: PoseLandmarkerHelper? = null
    private var isTracking = false
    
    fun startPostureTracking(lifecycleOwner: LifecycleOwner) {
        if (isTracking) return
        
        try {
            // Initialize PoseLandmarkerHelper with listener
            poseLandmarkerHelper = PoseLandmarkerHelper(context, object : PoseLandmarkerHelper.PoseLandmarkerListener {
                override fun onPoseLandmarkerResult(result: com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult, image: MPImage) {
                    // Result is already handled in PoseLandmarkerHelper
                }
                
                override fun onPoseLandmarkerError(error: RuntimeException) {
                    // Handle error silently
                }
            })
            
            // Set up pose landmarker
            poseLandmarkerHelper?.setupPoseLandmarker()
            
            // Initialize camera fragment
            cameraFragment = CameraFragment(context, poseLandmarkerHelper!!)
            
            // Start camera
            cameraFragment?.startCamera(lifecycleOwner)
            
            isTracking = true
        } catch (e: Exception) {
            // Handle error silently
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