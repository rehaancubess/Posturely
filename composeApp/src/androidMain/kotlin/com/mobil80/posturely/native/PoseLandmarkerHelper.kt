package com.mobil80.posturely.native

import android.content.Context

import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PoseLandmarkerHelper(
    private val context: Context,
    private val poseLandmarkerListener: PoseLandmarkerListener
) {
    private var poseLandmarker: PoseLandmarker? = null
    private var isInitialized = false
    
    // Frame skipping for better performance
    private var frameCount = 0
    private val processEveryNFrames = 1 // Process every frame like the blog post
    
    // Synchronization to prevent crashes during shutdown
    private val landmarkerLock = Object()
    private var isClosing = false
    
    interface PoseLandmarkerListener {
        fun onPoseLandmarkerResult(result: PoseLandmarkerResult, image: MPImage)
        fun onPoseLandmarkerError(error: RuntimeException)
    }
    
    // StateFlow for real-time landmark updates
    private val _landmarksFlow = MutableStateFlow<List<PoseLandmark>>(emptyList())
    val landmarksFlow: StateFlow<List<PoseLandmark>> = _landmarksFlow
    
        fun setupPoseLandmarker() {
        try {
            val modelName = "pose_landmarker_full.task" // Using full model like the blog post
            val baseOptions = BaseOptions.builder().setModelAssetPath(modelName).build()

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener(this::returnLivestreamResult)
                .setErrorListener(this::returnLivestreamError)
                .build() // Removed custom confidence thresholds to use defaults

            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            isInitialized = true
        } catch (e: Exception) {
            android.util.Log.e("PoseLandmarkerHelper", "Error setting up pose landmarker", e)
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
        } catch (e: Exception) {
            android.util.Log.e("PoseLandmarkerHelper", "Error setting up high confidence pose landmarker", e)
        }
    }
    
    fun detectAsync(mpImage: MPImage, timestamp: Long) {
        synchronized(landmarkerLock) {
            if (!isInitialized || poseLandmarker == null || isClosing) {
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
            android.util.Log.e("PoseLandmarkerHelper", "Error in detectAsync", e)
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
                val worldLandmarks = if (result.worldLandmarks().isNotEmpty()) {
                    result.worldLandmarks()[0]
                } else {
                    emptyList()
                }
                
                // Debug logging for detection performance
                android.util.Log.d("PoseLandmarkerHelper", "Detected pose with ${poseLandmarks.size} landmarks")
                
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
                    android.util.Log.d("PoseLandmarkerHelper", "Avg visibility: $avgVisibility, Avg presence: $avgPresence")
                }
                
                // Optimized landmark conversion
                val landmarks = poseLandmarks.map { landmark ->
                    PoseLandmark(
                        x = landmark.x(),
                        y = landmark.y(),
                        z = landmark.z(),
                        visibility = when (val vis = landmark.visibility()) {
                            is Float -> vis
                            is Double -> vis.toFloat()
                            is Number -> vis.toFloat()
                            else -> 0.0f
                        },
                        presence = when (val pres = landmark.presence()) {
                            is Float -> pres
                            is Double -> pres.toFloat()
                            is Number -> pres.toFloat()
                            else -> 0.0f
                        }
                    )
                }
                
                val worldLandmarksList = worldLandmarks.map { landmark ->
                    PoseLandmark(
                        x = landmark.x(),
                        y = landmark.y(),
                        z = landmark.z(),
                        visibility = when (val vis = landmark.visibility()) {
                            is Float -> vis
                            is Double -> vis.toFloat()
                            is Number -> vis.toFloat()
                            else -> 0.0f
                        },
                        presence = when (val pres = landmark.presence()) {
                            is Float -> pres
                            is Double -> pres.toFloat()
                            is Number -> pres.toFloat()
                            else -> 0.0f
                        }
                    )
                }
                
                synchronized(landmarkerLock) {
                    if (!isClosing) {
                        // Update StateFlow
                        _landmarksFlow.value = landmarks
                        
                        // Update PoseDataManager
                        PoseDataManager.updatePoseData(landmarks, worldLandmarksList)
                        
                        // Notify listener
                        poseLandmarkerListener.onPoseLandmarkerResult(result, image)
                    }
                }
            } else {
                android.util.Log.d("PoseLandmarkerHelper", "No pose detected in frame")
            }
        } catch (e: Exception) {
            android.util.Log.e("PoseLandmarkerHelper", "Error processing pose result", e)
        }
    }
    
    private fun returnLivestreamError(error: RuntimeException) {
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
        } catch (e: Exception) {
            android.util.Log.e("PoseLandmarkerHelper", "Error closing pose landmarker", e)
        }
    }
} 