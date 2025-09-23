package com.mobil80.posturely.scan

import android.content.Context
import android.util.Log
import com.mobil80.posturely.native.PoseLandmark
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class ScanPoseLandmarkerHelper(
    private val context: Context,
    private val poseLandmarkerListener: PoseLandmarkerListener
) {
    private var poseLandmarker: PoseLandmarker? = null
    private var isInitialized = false
    
    // Frame skipping for better performance
    private var frameCount = 0
    private val processEveryNFrames = 1 // Process every frame
    
    // Synchronization to prevent crashes during shutdown
    private val landmarkerLock = Object()
    private var isClosing = false
    
    interface PoseLandmarkerListener {
        fun onPoseLandmarkerResult(result: PoseLandmarkerResult, image: MPImage)
        fun onPoseLandmarkerError(error: RuntimeException)
    }
    
    fun setupPoseLandmarker() {
        try {
            val modelName = "pose_landmarker_full.task"
            val baseOptions = BaseOptions.builder().setModelAssetPath(modelName).build()

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener(this::returnLivestreamResult)
                .setErrorListener(this::returnLivestreamError)
                .build()

            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            isInitialized = true
            Log.d("ScanPoseLandmarkerHelper", "Standard pose landmarker initialized")
        } catch (e: Exception) {
            Log.e("ScanPoseLandmarkerHelper", "Error setting up pose landmarker", e)
        }
    }
    
    fun setupPoseLandmarkerWithHighConfidence() {
        try {
            val modelName = "pose_landmarker_full.task"
            val baseOptions = BaseOptions.builder().setModelAssetPath(modelName).build()

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setMinPoseDetectionConfidence(0.75f) // High confidence like iOS
                .setMinPosePresenceConfidence(0.75f)
                .setMinTrackingConfidence(0.75f)
                .setResultListener(this::returnLivestreamResult)
                .setErrorListener(this::returnLivestreamError)
                .build()

            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            isInitialized = true
            Log.d("ScanPoseLandmarkerHelper", "High confidence pose landmarker initialized")
        } catch (e: Exception) {
            Log.e("ScanPoseLandmarkerHelper", "Error setting up high confidence pose landmarker", e)
        }
    }
    
    fun detectAsync(mpImage: MPImage, timestamp: Long) {
        synchronized(landmarkerLock) {
            if (!isInitialized || poseLandmarker == null || isClosing) {
                Log.w("ScanPoseLandmarkerHelper", "Skipping detection - not ready")
                return
            }
        }
        
        // Frame skipping for better performance
        frameCount++
        if (frameCount % processEveryNFrames != 0) {
            return
        }
        
        try {
            synchronized(landmarkerLock) {
                if (!isClosing && poseLandmarker != null) {
                    poseLandmarker?.detectAsync(mpImage, timestamp)
                }
            }
        } catch (e: Exception) {
            Log.e("ScanPoseLandmarkerHelper", "Error in detectAsync", e)
        }
    }
    
    private fun returnLivestreamResult(result: PoseLandmarkerResult, image: MPImage) {
        synchronized(landmarkerLock) {
            if (isClosing) {
                return
            }
        }
        
        try {
            if (result.landmarks().isNotEmpty()) {
                val poseLandmarks = result.landmarks()[0] // Get first pose
                
                Log.d("ScanPoseLandmarkerHelper", "Detected pose with ${poseLandmarks.size} landmarks")
                
                // Log confidence levels for debugging
                if (poseLandmarks.isNotEmpty()) {
                    val avgVisibility = poseLandmarks.map { landmark ->
                        when (val vis = landmark.visibility()) {
                            is Float -> vis
                            is Double -> vis.toFloat()
                            is Number -> vis.toFloat()
                            else -> 0.0f
                        }
                    }.average()
                    val avgPresence = poseLandmarks.map { landmark ->
                        when (val pres = landmark.presence()) {
                            is Float -> pres
                            is Double -> pres.toFloat()
                            is Number -> pres.toFloat()
                            else -> 0.0f
                        }
                    }.average()
                    Log.d("ScanPoseLandmarkerHelper", "Avg visibility: $avgVisibility, Avg presence: $avgPresence")
                }
                
                synchronized(landmarkerLock) {
                    if (!isClosing) {
                        // Notify listener directly - NO PoseDataManager usage
                        poseLandmarkerListener.onPoseLandmarkerResult(result, image)
                    }
                }
            } else {
                Log.d("ScanPoseLandmarkerHelper", "No pose detected in frame")
            }
        } catch (e: Exception) {
            Log.e("ScanPoseLandmarkerHelper", "Error processing pose result", e)
            poseLandmarkerListener.onPoseLandmarkerError(RuntimeException("Processing error", e))
        }
    }
    
    private fun returnLivestreamError(error: RuntimeException) {
        Log.e("ScanPoseLandmarkerHelper", "MediaPipe error: ${error.message}")
        poseLandmarkerListener.onPoseLandmarkerError(error)
    }
    
    fun close() {
        try {
            synchronized(landmarkerLock) {
                isClosing = true
            }
            
            // Wait a bit for any ongoing processing to complete
            Thread.sleep(50)
            
            synchronized(landmarkerLock) {
                poseLandmarker?.close()
                poseLandmarker = null
                isInitialized = false
                isClosing = false
            }
            Log.d("ScanPoseLandmarkerHelper", "Pose landmarker closed successfully")
        } catch (e: Exception) {
            Log.e("ScanPoseLandmarkerHelper", "Error closing pose landmarker", e)
        }
    }
}
