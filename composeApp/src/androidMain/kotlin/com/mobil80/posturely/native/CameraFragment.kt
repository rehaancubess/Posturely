package com.mobil80.posturely.native

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.YuvImage

import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * CameraFragment handles camera operations for posture tracking.
 * 
 * Features:
 * - Automatically selects wide angle or ultra-wide camera when available for better pose detection
 * - Falls back to default front camera if wide angle not available
 * - Real-time pose detection with MediaPipe integration
 * - Frame rate limiting for optimal performance
 */
class CameraFragment(
    private val context: Context,
    private val poseLandmarkerHelper: PoseLandmarkerHelper
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var isRunning = false
    
    // Frame rate limiting for better performance
    private val lastProcessTime = AtomicLong(0)
    private val minFrameInterval = 100L // Back to 10 FPS for better stability
    
    // Synchronization to prevent crashes during shutdown
    private val processingLock = Object()
    private var isShuttingDown = false
    
    fun startCamera(lifecycleOwner: LifecycleOwner) {
        if (isRunning) return
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build() // Removed target resolution to use defaults
                
                imageAnalyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImage(imageProxy)
                }
                
                // Use front camera for self-monitoring posture tracking
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()
                
                android.util.Log.d("CameraFragment", "Using front camera for posture tracking")
                
                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageAnalyzer
                )
                
                this.imageAnalyzer = imageAnalyzer
                isRunning = true
            } catch (e: Exception) {
                // Handle error silently
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    private fun processImage(imageProxy: ImageProxy) {
        synchronized(processingLock) {
            if (isShuttingDown || !isRunning) {
                imageProxy.close()
                return
            }
        }
        
        try {
            // Frame rate limiting
            val currentTime = System.currentTimeMillis()
            val lastTime = lastProcessTime.get()
            if (currentTime - lastTime < minFrameInterval) {
                imageProxy.close()
                return
            }
            lastProcessTime.set(currentTime)
            
            // Debug logging for frame processing
            android.util.Log.d("CameraFragment", "Processing frame at ${currentTime}ms")
            
            val bitmap = imageProxyToBitmap(imageProxy)
            if (bitmap != null) {
                try {
                    synchronized(processingLock) {
                        if (!isShuttingDown && isRunning) {
                            val mpImage = BitmapImageBuilder(bitmap).build()
                            val timestamp = imageProxy.imageInfo.timestamp // Use proper timestamp like blog post
                            poseLandmarkerHelper.detectAsync(mpImage, timestamp)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CameraFragment", "Error in pose detection", e)
                }
            } else {
                android.util.Log.w("CameraFragment", "Failed to convert image to bitmap")
            }
        } catch (e: Exception) {
            android.util.Log.e("CameraFragment", "Error processing image", e)
        } finally {
            imageProxy.close()
        }
    }
    
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val mediaImage = imageProxy.image
            if (mediaImage != null && imageProxy.format == android.graphics.ImageFormat.YUV_420_888) {
                val bitmap = yuvToRgb(mediaImage, imageProxy)
                
                // Handle front camera rotation and mirroring like the blog post
                val matrix = Matrix().apply {
                    postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                    postScale(-1f, 1f, bitmap.width.toFloat(), bitmap.height.toFloat()) // Mirror for front camera
                }
                val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                
                // Recycle original bitmap to free memory
                if (bitmap != rotatedBitmap) {
                    bitmap.recycle()
                }
                
                rotatedBitmap
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("CameraFragment", "Error converting image to bitmap", e)
            null
        }
    }
    
    private fun yuvToRgb(image: android.media.Image, imageProxy: ImageProxy): Bitmap {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer
        
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        
        val nv21 = ByteArray(ySize + uSize + vSize)
        
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        
        val yuvImage = YuvImage(nv21, android.graphics.ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
    
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    fun stopCamera() {
        if (!isRunning) return
        
        try {
            // Mark as shutting down to prevent new processing
            synchronized(processingLock) {
                isShuttingDown = true
            }
            
            // Wait a bit for any ongoing processing to complete
            Thread.sleep(100)
            
            // Unbind all use cases
            cameraProvider?.unbindAll()
            
            // Shutdown executor
            cameraExecutor.shutdown()
            
            // Clear references
            cameraProvider = null
            camera = null
            imageAnalyzer = null
            
            isRunning = false
            isShuttingDown = false
        } catch (e: Exception) {
            android.util.Log.e("CameraFragment", "Error stopping camera", e)
        }
    }
} 