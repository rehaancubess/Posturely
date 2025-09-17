package com.example.posturelynew

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.*
import java.awt.image.BufferedImage
import javax.swing.SwingUtilities
import kotlin.math.*
import com.example.posturelynew.camera.DesktopCamera
import com.example.posturelynew.DesktopMediaPipeService


class DesktopCameraService {
    private var isRunning = false
    private var isTracking = false
    private var currentFrame: ImageBitmap? = null
    private var onFrameUpdate: ((ImageBitmap) -> Unit)? = null
    
    // Posture tracking variables (same as mobile)
    private var poseData = "No pose data available"
    private var landmarks = List(33) { Pair(0.5f, 0.5f) }
    private var score = 0
    private var postureStatus = "NO SIGNAL"
    
    // Real macOS camera using JavaCV
    private var desktopCamera: DesktopCamera? = null
    private var isCameraInitialized = false
    
    // MediaPipe pose detection (same as mobile)
    private var mediaPipeService: DesktopMediaPipeWsService? = null
    private var isMediaPipeInitialized = false
    

    
    init {
        initializeCamera()
        // MediaPipe will be initialized lazily when startTracking() is called
    }
    
    private fun initializeCamera() {
        try {
            desktopCamera = DesktopCamera()
            
            // Try to find a working camera index
            var workingIndex = -1
            for (index in 0..2) { // Try indices 0, 1, 2
                
                desktopCamera?.selectCamera(index)
                if (desktopCamera?.testCameraAccess() == true) {
                    workingIndex = index
                    
                    break
                } else {
                    
                }
            }
            
            isCameraInitialized = workingIndex >= 0
            
            if (isCameraInitialized) {
                
                val cameras = desktopCamera?.getAvailableCameras() ?: emptyList()
                
                cameras.forEachIndexed { index, name ->
                    
                }
                
            } else {
                
            }
        } catch (e: Exception) {
            
            isCameraInitialized = false
        }
    }
    
    private fun initializeMediaPipe() {
        try {
            
            mediaPipeService = DesktopMediaPipeWsService()
            mediaPipeService?.setListener(object : DesktopMediaPipeWsService.PoseLandmarkerListener {
                override fun onPoseLandmarkerResult(landmarks: List<Pair<Float, Float>>, score: Int, status: String) {
                    // Update our local state with MediaPipe results
                    this@DesktopCameraService.landmarks = landmarks
                    this@DesktopCameraService.score = score
                    this@DesktopCameraService.postureStatus = status
                    println("DesktopCameraService: Received MediaPipe results: $score points, $status")
                }
                
                override fun onPoseLandmarkerError(error: String) {
                    
                }
            })
            mediaPipeService?.setupPoseLandmarker()

            // Wait up to 10s for async WS init
            var attempts = 0
            val maxAttempts = 100
            while (!(mediaPipeService?.isInitialized() ?: false) && attempts < maxAttempts) {
                Thread.sleep(100)
                attempts++
            }
            isMediaPipeInitialized = mediaPipeService?.isInitialized() ?: false
            
            if (isMediaPipeInitialized) {
            } else {
            }
        } catch (e: Exception) {
            
            e.printStackTrace()
            isMediaPipeInitialized = false
        }
    }
    

    
    fun showCameraSelectionDialog(onSelected: (Int) -> Unit) {
        val cameras = desktopCamera?.getAvailableCameras() ?: emptyList()
        
        if (cameras.size <= 1) {
            onSelected(0)
            return
        }
        
        // Show camera selection dialog
        SwingUtilities.invokeLater {
            showCameraSelectionSwing(cameras, onSelected)
        }
    }
    
    private fun showCameraSelectionSwing(cameras: List<String>, onSelected: (Int) -> Unit) {
        val dialog = Frame("Select Camera")
        dialog.layout = FlowLayout()
        dialog.isResizable = false
        
        val label = Label("Select a camera:")
        dialog.add(label)
        
        val comboBox = Choice()
        cameras.forEachIndexed { index, name ->
            comboBox.add("$index: $name")
        }
        
        val currentIndex = desktopCamera?.getSelectedCameraIndex() ?: 0
        comboBox.select(currentIndex)
        dialog.add(comboBox)
        
        val okButton = Button("OK")
        okButton.addActionListener {
            val selectedIndex = comboBox.selectedIndex
            desktopCamera?.selectCamera(selectedIndex)
            onSelected(selectedIndex)
            dialog.dispose()
        }
        dialog.add(okButton)
        
        val cancelButton = Button("Cancel")
        cancelButton.addActionListener {
            dialog.dispose()
        }
        dialog.add(cancelButton)
        
        dialog.pack()
        dialog.setLocationRelativeTo(null)
        dialog.isVisible = true
    }
    
    fun startCamera(onFrameUpdate: (ImageBitmap) -> Unit) {
        
        this.onFrameUpdate = onFrameUpdate
        
        isRunning = true
        
        
        if (isCameraInitialized) {
            // Initialize MediaPipe if not already done (needed for pose detection)
            if (!isMediaPipeInitialized) {
                
                try {
                    initializeMediaPipe()
                } catch (e: Exception) {
                    
                    // Don't fail camera start, just log warning
                }
            }
            
            
            startNativeCamera()
        } else {
            
            throw RuntimeException("Camera not initialized - cannot start camera service")
        }
    }
    
    private fun startNativeCamera() {
        try {
            
            desktopCamera?.startCamera { frame ->
                
                currentFrame = frame
                
                onFrameUpdate?.invoke(frame)
                
                
                // Process frame with MediaPipe for real pose detection
                if (isMediaPipeInitialized) {
                    try {
                        // Use the actual camera BufferedImage when available
                        val bufferedImage = desktopCamera?.getLastBufferedImage() ?: frame.toBufferedImage()
                        mediaPipeService?.detectAsync(bufferedImage, System.currentTimeMillis())
                    } catch (e: Exception) {
                        
                    }
                } else {
                    
                }
            }
            
        } catch (e: Exception) {
            
            e.printStackTrace()
            throw RuntimeException("Failed to start camera: ${e.message}")
        }
    }
    
    private fun createMockFrame() {
        
        // Create a simple mock frame when camera is not available
        val mockFrame = createMockImageBitmap()
        currentFrame = mockFrame
        onFrameUpdate?.invoke(mockFrame)
        
    }
    
    private fun createMockImageBitmap(): ImageBitmap {
        // Create a simple black frame for testing purposes
        val width = 640
        val height = 480
        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        
        val graphics = bufferedImage.createGraphics()
        graphics.color = Color.BLACK
        graphics.fillRect(0, 0, width, height)
        
        // Add some text to indicate it's a mock frame
        graphics.color = Color.WHITE
        graphics.font = Font("Arial", Font.BOLD, 24)
        val text = "Mock Camera Frame"
        val fontMetrics = graphics.fontMetrics
        val textWidth = fontMetrics.stringWidth(text)
        val textHeight = fontMetrics.height
        val x = (width - textWidth) / 2
        val y = (height + textHeight) / 2
        graphics.drawString(text, x, y)
        
        graphics.dispose()
        
        return bufferedImage.toComposeImageBitmap()
    }


    
    fun startTracking() {
        isTracking = true
        
        // Initialize MediaPipe if not already initialized
        if (!isMediaPipeInitialized) {
            
            try {
                initializeMediaPipe()
            } catch (e: Exception) {
                
                e.printStackTrace()
                isTracking = false
                throw RuntimeException("Failed to initialize MediaPipe: ${e.message}")
            }
        }
        
        if (isMediaPipeInitialized) {
            
            mediaPipeService?.startTracking()
            // Real tracking will happen in the camera frame callback
        } else {
            
            isTracking = false
            throw RuntimeException("MediaPipe not initialized - cannot start tracking without pose detection")
        }
    }
    
    fun stopTracking() {
        isTracking = false
        mediaPipeService?.stopTracking()
    }
    

    
    fun stopCamera() {
        isRunning = false
        isTracking = false
        
        desktopCamera?.stopCamera()
        mediaPipeService?.close()
        
        currentFrame = null
        onFrameUpdate = null
    }
    
    fun isRunning(): Boolean = isRunning
    fun isTracking(): Boolean = isTracking
    fun getCurrentFrame(): ImageBitmap? = currentFrame
    
    fun isMediaPipeAvailable(): Boolean = isMediaPipeInitialized
    
    fun getMediaPipeStatus(): String = mediaPipeService?.getStatus() ?: "MediaPipe service not created"
    
    // Camera management
    fun getAvailableCameras(): List<String> = desktopCamera?.getAvailableCameras() ?: emptyList()
    fun getSelectedCameraIndex(): Int = desktopCamera?.getSelectedCameraIndex() ?: 0
    fun getSelectedCameraName(): String = desktopCamera?.getSelectedCameraName() ?: "Unknown Camera"
    
    fun selectCameraIndex(index: Int) {
        
        desktopCamera?.selectCamera(index)
    }
    
    // Posture tracking interface methods (same as mobile)
    fun getPoseData(): String = poseData
    fun getLandmarkCount(): Int = landmarks.size
    fun getLandmarkX(index: Int): Float = landmarks.getOrNull(index)?.first ?: 0.5f
    fun getLandmarkY(index: Int): Float = landmarks.getOrNull(index)?.second ?: 0.5f
    fun getCurrentScore(): Int = score
    fun getPostureStatus(): String = postureStatus
    
    // MediaPipe integration is now handled by DesktopMediaPipeService
}

// Extension function to convert Compose ImageBitmap to BufferedImage
private fun ImageBitmap.toBufferedImage(): BufferedImage {
    val width = this.width
    val height = this.height
    val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    
    // For now, create a basic conversion - in a real implementation you'd need proper pixel copying
    val graphics = bufferedImage.createGraphics()
    graphics.color = Color.BLACK
    graphics.fillRect(0, 0, width, height)
    graphics.dispose()
    
    return bufferedImage
}


