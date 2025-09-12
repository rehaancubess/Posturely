package com.example.posturelynew.camera

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.bytedeco.javacv.*
import java.awt.image.BufferedImage
import java.util.concurrent.*
import kotlin.math.*

class DesktopCamera {
    private var grabber: FFmpegFrameGrabber? = null
    private var converter: Java2DFrameConverter? = null
    private var isRunning = false
    private var executor: ScheduledExecutorService? = null
    private var onFrameUpdate: ((ImageBitmap) -> Unit)? = null
    private var lastBufferedImage: BufferedImage? = null
    
    // Camera state
    private var selectedCameraIndex = 0
    private var availableCameras = listOf<String>()
    
    init {
        discoverCameras()
    }
    
    private fun discoverCameras() {
        try {
            // For macOS, we'll try common camera indices
            availableCameras = listOf(
                "Built-in Camera (Index 0)",
                "External Camera (Index 1)",
                "Additional Camera (Index 2)"
            )
            println("DesktopCamera: Available cameras: $availableCameras")
        } catch (e: Exception) {
            println("DesktopCamera: Error discovering cameras: ${e.message}")
            availableCameras = listOf("Default Camera")
        }
    }
    
    fun getAvailableCameras(): List<String> = availableCameras
    fun getSelectedCameraIndex(): Int = selectedCameraIndex
    fun getSelectedCameraName(): String = availableCameras.getOrNull(selectedCameraIndex) ?: "Unknown Camera"
    
    fun selectCamera(index: Int) {
        if (index >= 0 && index < availableCameras.size) {
            selectedCameraIndex = index
            println("DesktopCamera: Selected camera $index: ${availableCameras[index]}")
            
            // Only restart camera if it's already running and we have a callback
            if (isRunning && onFrameUpdate != null) {
                println("DesktopCamera: Restarting camera with new selection")
                stopCamera()
                startCamera(onFrameUpdate!!)
            }
        }
    }
    
    fun startCamera(onFrameUpdate: (ImageBitmap) -> Unit) {
        this.onFrameUpdate = onFrameUpdate
        println("DesktopCamera: startCamera called with device $selectedCameraIndex")
        
        if (isRunning) {
            println("DesktopCamera: Camera already running")
            return
        }
        
        try {
            println("DesktopCamera: Initializing JavaCV components...")
            converter = Java2DFrameConverter()
            println("DesktopCamera: Java2DFrameConverter created successfully")
            
            // Create frame grabber for macOS camera
            println("DesktopCamera: Creating FFmpegFrameGrabber for device $selectedCameraIndex")
            grabber = FFmpegFrameGrabber(selectedCameraIndex.toString()).apply {
                println("DesktopCamera: Setting format to avfoundation")
                format = "avfoundation"   // macOS camera driver
                println("DesktopCamera: Setting frameRate to 30.0")
                frameRate = 30.0
                println("DesktopCamera: Setting imageWidth to 640")
                imageWidth = 640
                println("DesktopCamera: Setting imageHeight to 480")
                imageHeight = 480
                println("DesktopCamera: Starting grabber...")
                start()
                println("DesktopCamera: Grabber started successfully!")
            }
            
            isRunning = true
            println("DesktopCamera: Camera started successfully with device $selectedCameraIndex")
            
            // Start frame capture loop
            println("DesktopCamera: Starting frame capture loop...")
            startFrameCapture()
            
        } catch (e: Exception) {
            println("DesktopCamera: Failed to start camera: ${e.message}")
            e.printStackTrace()
            throw RuntimeException("Failed to start camera: ${e.message}")
        }
    }
    
    private fun startFrameCapture() {
        println("DesktopCamera: Creating frame capture executor...")
        executor = Executors.newSingleThreadScheduledExecutor()
        println("DesktopCamera: Starting frame capture loop at 30 FPS...")
        
        executor?.scheduleAtFixedRate({
            try {
                if (isRunning && grabber != null) {
                    val frame = grabber?.grab()
                    if (frame != null) {
                        println("DesktopCamera: Frame grabbed successfully, image: ${frame.image != null}")
                        if (frame.image != null && converter != null) {
                            println("DesktopCamera: Converting frame to BufferedImage...")
                            val bufferedImage = converter?.convert(frame)
                            if (bufferedImage != null) {
                                lastBufferedImage = bufferedImage
                                println("DesktopCamera: Converting BufferedImage to Compose ImageBitmap...")
                                val composeImage = bufferedImage.toComposeImageBitmap()
                                println("DesktopCamera: Invoking onFrameUpdate callback...")
                                onFrameUpdate?.invoke(composeImage)
                                println("DesktopCamera: Frame update completed successfully")
                            } else {
                                println("DesktopCamera: Failed to convert frame to BufferedImage")
                            }
                        } else {
                            println("DesktopCamera: Frame image is null or converter is null")
                        }
                    } else {
                        println("DesktopCamera: Failed to grab frame")
                    }
                } else {
                    println("DesktopCamera: Camera not running or grabber is null. isRunning: $isRunning, grabber: ${grabber != null}")
                }
            } catch (e: Exception) {
                println("DesktopCamera: Frame capture error: ${e.message}")
                e.printStackTrace()
            }
        }, 0, 33, TimeUnit.MILLISECONDS) // ~30 FPS
    }
    

    
    fun stopCamera() {
        isRunning = false
        
        try {
            executor?.shutdown()
            executor = null
            
            grabber?.stop()
            grabber?.release()
            grabber = null
            
            converter = null
            
            println("DesktopCamera: Camera stopped")
        } catch (e: Exception) {
            println("DesktopCamera: Error stopping camera: ${e.message}")
        }
    }
    
    fun isRunning(): Boolean = isRunning
    
    fun isInitialized(): Boolean = isRunning
    
    fun testCameraAccess(): Boolean {
        return try {
            // Try to create a temporary grabber to test access
            val testGrabber = FFmpegFrameGrabber(selectedCameraIndex.toString()).apply {
                format = "avfoundation"
                frameRate = 30.0
                imageWidth = 640
                imageHeight = 480
            }
            testGrabber.start()
            testGrabber.stop()
            testGrabber.release()
            true
        } catch (e: Exception) {
            println("DesktopCamera: Camera access test failed: ${e.message}")
            false
        }
    }

    fun getLastBufferedImage(): BufferedImage? = lastBufferedImage
}
