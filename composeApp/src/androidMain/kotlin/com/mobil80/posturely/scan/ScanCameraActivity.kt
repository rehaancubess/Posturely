package com.mobil80.posturely.scan

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.YuvImage
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.mobil80.posturely.native.PoseLandmark
import com.mobil80.posturely.native.PoseLandmarkerHelper
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.*

class ScanCameraActivity : ComponentActivity(), PoseLandmarkerHelper.PoseLandmarkerListener {
    
    companion object {
        private const val TAG = "ScanCameraActivity"
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
        const val EXTRA_SCAN_ID = "scan_id"
        const val EXTRA_FRONT_IMAGE = "front_image"
        const val EXTRA_SIDE_IMAGE = "side_image"
        
        fun createIntent(context: Context): Intent {
            return Intent(context, ScanCameraActivity::class.java)
        }
    }
    
    // Camera components
    private lateinit var previewView: PreviewView
    private lateinit var overlayView: ScanOverlayView
    private var camera: androidx.camera.core.Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    // MediaPipe
    private var poseLandmarkerHelper: PoseLandmarkerHelper? = null
    private var currentLandmarks: List<PoseLandmark> = emptyList()
    
    // UI Elements
    private lateinit var statusLabel: TextView
    private lateinit var backButton: Button
    private lateinit var topBar: View
    
    // Scan flow state
    private var flowStage = 0 // 0=find front, 1=find side after first countdown, 2=done
    private var frontStableSince: Long? = null
    private var sideStableSince: Long? = null
    private val requiredStableMillis = 700L
    private var countdownPlayer: MediaPlayer? = null
    private var instructionPlayer: MediaPlayer? = null
    private val scanId = UUID.randomUUID().toString()
    
    // Image capture
    private var frontImageB64: String? = null
    private var sideImageB64: String? = null
    private var latestBitmap: Bitmap? = null
    
    // Performance optimization
    private var lastProcessTime = 0L
    private val minFrameInterval = 100L // 10 FPS
    
    // Smoothing
    private var smoothedLandmarks: Array<FloatArray> = Array(33) { FloatArray(3) }
    private val smoothingAlpha = 0.4f
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Log.e(TAG, "Camera permission denied")
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create UI programmatically to match iOS
        setupUI()
        
        // Initialize MediaPipe
        setupPoseLandmarker()
        
        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // Play initial instruction when page opens
        playInstruction("incamera")
    }
    
    private fun setupUI() {
        // Create main container
        val container = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        
        val rootLayout = android.widget.FrameLayout(this).apply {
            layoutParams = container
            setBackgroundColor(Color.BLACK)
        }
        
        // Camera preview
        previewView = PreviewView(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            // Mirror preview horizontally for front camera (selfie view), like iOS
            scaleX = -1f
        }
        rootLayout.addView(previewView)
        
        // Overlay for skeleton
        overlayView = ScanOverlayView(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleX = -1f
            // mirrored overlay
        }
        rootLayout.addView(overlayView) 
        
        // Top bar (matches iOS yellow theme)
        topBar = View(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                200 // Fixed height for top bar
            ).apply {
                topMargin = 0
            }
            setBackgroundColor(Color.parseColor("#FFFED867")) // App yellow
        }
        rootLayout.addView(topBar)
        
        // Back button
        backButton = Button(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = 32
                topMargin = getStatusBarHeight() + 16
            }
            text = "←"
            textSize = 24f
            setTextColor(Color.parseColor("#0F1931")) // Text primary
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(16, 16, 16, 16)
            setOnClickListener { finish() }
        }
        rootLayout.addView(backButton)
        
        // Title
        val titleLabel = TextView(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                val screenWidth = resources.displayMetrics.widthPixels
                leftMargin = (screenWidth / 2) - 150 // Approximate center
                topMargin = getStatusBarHeight() + 24
            }
            text = "Full Body Scan"
            textSize = 20f
            setTextColor(Color.parseColor("#0F1931"))
        }
        rootLayout.addView(titleLabel)
        
        // Status label
        statusLabel = TextView(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 100
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
            }
            text = "Position yourself for front view"
            textSize = 18f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#80000000")) // Semi-transparent background
            setPadding(32, 16, 32, 16)
        }
        rootLayout.addView(statusLabel)
        
        setContentView(rootLayout)
        
        // Make status bar same color as app
        window.statusBarColor = Color.parseColor("#FFFED867")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }
    
    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }
    
    private fun setupPoseLandmarker() {
        try {
            // Use the same setup as the working CameraFragment
            poseLandmarkerHelper = PoseLandmarkerHelper(this, this)
            poseLandmarkerHelper?.setupPoseLandmarker()
            Log.d(TAG, "PoseLandmarkerHelper setup completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up PoseLandmarkerHelper", e)
            // Fallback to high confidence setup
            try {
                poseLandmarkerHelper = PoseLandmarkerHelper(this, this)
                poseLandmarkerHelper?.setupPoseLandmarkerWithHighConfidence()
                Log.d(TAG, "Fallback high confidence PoseLandmarkerHelper setup completed")
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Fallback setup also failed", fallbackError)
            }
        }
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return
        
        // Preview (use defaults like working implementation)
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
        
        // Image analysis for pose detection (same config as working CameraFragment)
        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build() // Use defaults like working implementation - no output format specified
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImage(imageProxy)
                }
            }
        
        // Front camera selector (same as working implementation)
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()
        
        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()
            
            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                this as LifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }
    
    private fun processImage(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessTime < minFrameInterval) {
            imageProxy.close()
            return
        }
        lastProcessTime = currentTime
        
        try {
            Log.d(TAG, "Processing frame - format: ${imageProxy.format}, size: ${imageProxy.width}x${imageProxy.height}")
            
            // Use the same approach as working CameraFragment - single bitmap conversion
            val bitmap = imageProxyToBitmap(imageProxy)
            if (bitmap != null) {
                // Store bitmap for capture
                latestBitmap?.recycle() // Recycle previous bitmap
                latestBitmap = bitmap
                Log.d(TAG, "Converted bitmap: ${bitmap.width}x${bitmap.height}")
                
                try {
                    val mpImage = BitmapImageBuilder(bitmap).build()
                    Log.d(TAG, "Calling poseLandmarkerHelper.detectAsync")
                    poseLandmarkerHelper?.detectAsync(mpImage, imageProxy.imageInfo.timestamp)
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating MPImage or calling detectAsync", e)
                }
            } else {
                Log.w(TAG, "Failed to convert imageProxy to bitmap, format: ${imageProxy.format}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image", e)
        } finally {
            imageProxy.close()
        }
    }
    
    private fun imageProxyToBitmapForMediaPipe(imageProxy: ImageProxy): Bitmap? {
        return try {
            val mediaImage = imageProxy.image
            Log.d(TAG, "ImageProxy format: ${imageProxy.format}, expected: ${android.graphics.ImageFormat.YUV_420_888}")
            if (mediaImage != null && imageProxy.format == android.graphics.ImageFormat.YUV_420_888) {
                // For MediaPipe, we want the raw bitmap without rotation/mirroring
                // so that landmark coordinates are in the original image coordinate system
                val bitmap = yuvToRgb(mediaImage, imageProxy)
                if (bitmap != null) {
                    Log.d(TAG, "Created MediaPipe bitmap: ${bitmap.width}x${bitmap.height}")
                } else {
                    Log.w(TAG, "Failed to create MediaPipe bitmap from YUV conversion")
                }
                bitmap
            } else {
                Log.w(TAG, "Unsupported image format for MediaPipe: ${imageProxy.format}, mediaImage: ${mediaImage != null}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting image to bitmap for MediaPipe", e)
            null
        }
    }
    
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val mediaImage = imageProxy.image
            if (mediaImage != null && imageProxy.format == android.graphics.ImageFormat.YUV_420_888) {
                // Use the same YUV to RGB conversion as working CameraFragment
                val bitmap = yuvToRgbSimple(mediaImage, imageProxy)
                
                // Handle front camera rotation and mirroring like the working implementation
                val matrix = Matrix().apply {
                    postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                    // Mirror around the center to match iOS selfie view
                    postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
                }
                val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                
                // Recycle original bitmap to free memory
                if (bitmap != rotatedBitmap) {
                    bitmap.recycle()
                }
                
                rotatedBitmap
            } else {
                Log.w(TAG, "Unsupported image format: ${imageProxy.format}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting image to bitmap", e)
            null
        }
    }
    
    // Simple YUV to RGB conversion matching working CameraFragment
    private fun yuvToRgbSimple(image: android.media.Image, imageProxy: ImageProxy): Bitmap {
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
    
    private fun yuvToRgb(image: android.media.Image, imageProxy: ImageProxy): Bitmap? {
        return try {
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer
            
            // Create a copy of the buffers to avoid concurrent modification
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            
            // Validate buffer sizes
            if (ySize <= 0 || uSize <= 0 || vSize <= 0) {
                Log.w(TAG, "Invalid buffer sizes: y=$ySize, u=$uSize, v=$vSize")
                return null
            }
            
            val nv21 = ByteArray(ySize + uSize + vSize)
            
            // Copy data safely
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)
            
            // Create YuvImage and convert to JPEG
            val yuvImage = YuvImage(nv21, android.graphics.ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            val success = yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 100, out)
            
            if (!success) {
                Log.w(TAG, "Failed to compress YUV to JPEG")
                return null
            }
            
            val imageBytes = out.toByteArray()
            if (imageBytes.isEmpty()) {
                Log.w(TAG, "Compressed image bytes are empty")
                return null
            }
            
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            if (bitmap == null) {
                Log.w(TAG, "Failed to decode JPEG bytes to bitmap")
                return null
            }
            
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error in yuvToRgb conversion", e)
            null
        }
    }
    
    // MediaPipe callbacks
    override fun onPoseLandmarkerResult(result: PoseLandmarkerResult, image: MPImage) {
        try {
            Log.d(TAG, "MediaPipe result: ${result.landmarks().size} poses detected")
            if (result.landmarks().isNotEmpty()) {
                Log.d(TAG, "Processing ${result.landmarks()[0].size} landmarks")
                val poseLandmarks = result.landmarks()[0]
                
                // Convert to our PoseLandmark format using the same approach as PoseLandmarkerHelper
                val landmarks = poseLandmarks.mapIndexed { index, landmark ->
                    val x = landmark.x()
                    val y = landmark.y()
                    val z = landmark.z()
                    
                    // Apply EMA smoothing like iOS
                    if (index < smoothedLandmarks.size) {
                        smoothedLandmarks[index][0] = smoothingAlpha * x + (1 - smoothingAlpha) * smoothedLandmarks[index][0]
                        smoothedLandmarks[index][1] = smoothingAlpha * y + (1 - smoothingAlpha) * smoothedLandmarks[index][1]
                        smoothedLandmarks[index][2] = smoothingAlpha * z + (1 - smoothingAlpha) * smoothedLandmarks[index][2]
                    }
                    
                    // Use the same visibility/presence extraction as PoseLandmarkerHelper
                    val visibility = when (val vis = landmark.visibility()) {
                        is Float -> vis
                        is Double -> vis.toFloat()
                        is Number -> vis.toFloat()
                        else -> 0.0f
                    }
                    val presence = when (val pres = landmark.presence()) {
                        is Float -> pres
                        is Double -> pres.toFloat()
                        is Number -> pres.toFloat()
                        else -> 0.0f
                    }
                    
                    Log.d(TAG, "Landmark $index: vis=$visibility, pres=$presence")
                    
                    PoseLandmark(
                        x = if (index < smoothedLandmarks.size) smoothedLandmarks[index][0] else x,
                        y = if (index < smoothedLandmarks.size) smoothedLandmarks[index][1] else y,
                        z = if (index < smoothedLandmarks.size) smoothedLandmarks[index][2] else z,
                        visibility = visibility,
                        presence = presence
                    )
                }
                
                currentLandmarks = landmarks
                
                // Update overlay on UI thread
                runOnUiThread {
                    try {
                        Log.d(TAG, "Updating overlay with ${landmarks.size} landmarks")
                        overlayView.updateLandmarks(landmarks)
                        evaluateAutoFlow()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating UI with landmarks", e)
                    }
                }
            } else {
                Log.d(TAG, "No poses detected")
                // Update overlay with empty landmarks
                runOnUiThread {
                    try {
                        overlayView.updateLandmarks(emptyList())
                    } catch (e: Exception) {
                        Log.e(TAG, "Error clearing overlay", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing MediaPipe result", e)
        }
    }
    
    override fun onPoseLandmarkerError(error: RuntimeException) {
        Log.e(TAG, "PoseLandmarker error: ${error.message}", error)
    }
    
    private fun evaluateAutoFlow() {
        val currentTime = System.currentTimeMillis()
        
        when (flowStage) {
            0 -> { // Looking for front pose
                if (isFrontPoseVisible()) {
                    if (frontStableSince == null) {
                        frontStableSince = currentTime
                        statusLabel.text = "Hold front position..."
                    } else if (currentTime - frontStableSince!! >= requiredStableMillis) {
                        // Front pose confirmed
                        statusLabel.text = "Front pose detected! Get ready..."
                        flowStage = 1
                        playCountdownThenCapture(true) // Capture front image
                    }
                } else {
                    frontStableSince = null
                    statusLabel.text = "Position yourself for front view"
                }
            }
            1 -> { // Looking for side pose
                if (isSidePoseVisible()) {
                    if (sideStableSince == null) {
                        sideStableSince = currentTime
                        statusLabel.text = "Hold side position..."
                    } else if (currentTime - sideStableSince!! >= requiredStableMillis) {
                        // Side pose confirmed
                        statusLabel.text = "Side pose detected! Get ready..."
                        flowStage = 2
                        playCountdownThenCapture(false) // Capture side image
                    }
                } else {
                    sideStableSince = null
                    statusLabel.text = "Turn to your side"
                }
            }
        }
    }
    
    private fun isFrontPoseVisible(): Boolean {
        // Require full body visibility - both shoulders AND knees must be in frame
        // Check if knee landmarks are actually visible (not just calculated)
        if (currentLandmarks.size < 33) return false
        
        val leftShoulder = currentLandmarks[11]
        val rightShoulder = currentLandmarks[12]
        val leftKnee = currentLandmarks[25]
        val rightKnee = currentLandmarks[26]
        
        // Android visibility is always ~0.0. Use plausibility checks instead of visibility:
        // Consider knee "in frame" if its normalized coordinates are within bounds and
        // distances to hip/ankle are reasonable (> small epsilon)
        fun kneePlausible(kneeIdx: Int, hipIdx: Int, ankleIdx: Int): Boolean {
            val k = currentLandmarks[kneeIdx]
            val h = currentLandmarks[hipIdx]
            val a = currentLandmarks[ankleIdx]
            val inBounds = k.x in 0.0f..1.0f && k.y in 0.0f..1.0f
            val dh = kotlin.math.hypot((h.x - k.x).toDouble(), (h.y - k.y).toDouble()).toFloat()
            val da = kotlin.math.hypot((a.x - k.x).toDouble(), (a.y - k.y).toDouble()).toFloat()
            return inBounds && dh > 0.05f && da > 0.05f
        }
        val leftKneePlausible = kneePlausible(25, 23, 27)
        val rightKneePlausible = kneePlausible(26, 24, 28)
        val kneesInFrame = leftKneePlausible || rightKneePlausible
        
        // MediaPipe coordinates are normalized (0-1), match iOS logic exactly:
        // - shoulderDx = horizontal distance between shoulders (width)
        // - shoulderDy = vertical distance between shoulders (level)
        val shoulderDx = abs(leftShoulder.x - rightShoulder.x)
        val shoulderDy = abs(leftShoulder.y - rightShoulder.y)
        
        // Calculate knee angles (match iOS exactly)
        val leftKneeAngle = calculateKneeAngle(25) // Left knee
        val rightKneeAngle = calculateKneeAngle(26) // Right knee
        
        // Front pose conditions (match iOS exactly):
        // - ShoulderWidth > 0.20 (person visible, front-facing)
        // - ShoulderLevel < 0.06 (shoulders roughly horizontal)
        // - At least one knee angle in 150°-185° range (straight legs)
        val shouldersOK = (shoulderDx > 0.20f) && (shoulderDy < 0.06f)
        val kneeOK = (leftKneeAngle >= 150.0 && leftKneeAngle <= 185.0) || 
                    (rightKneeAngle >= 150.0 && rightKneeAngle <= 185.0)
        
        Log.d(TAG, "Front pose check: landmarks=${currentLandmarks.size}, shouldersOK=$shouldersOK, kneesInFrame=$kneesInFrame (L=$leftKneePlausible R=$rightKneePlausible), kneeOK=$kneeOK")
        
        return shouldersOK && kneesInFrame && kneeOK
    }
    
    private fun isSidePoseVisible(): Boolean {
        // Use landmark count as primary indicator (like existing system)
        if (currentLandmarks.size < 33) return false
        
        val leftShoulder = currentLandmarks[11]
        val rightShoulder = currentLandmarks[12]
        val leftHip = currentLandmarks[23]
        val rightHip = currentLandmarks[24]
        
        // MediaPipe coordinates are normalized (0-1), match iOS logic exactly:
        // - shoulderDx = horizontal distance between shoulders (width)
        // - torsoLen = vertical distance from shoulders to hips (torso length)
        val shoulderDx = abs(leftShoulder.x - rightShoulder.x)
        val torsoLen = abs(((leftHip.y + rightHip.y) * 0.5f) - ((leftShoulder.y + rightShoulder.y) * 0.5f))
        
        // Calculate knee angles (match iOS exactly)
        val leftKneeAngle = calculateKneeAngle(25) // Left knee
        val rightKneeAngle = calculateKneeAngle(26) // Right knee
        
        // Side pose conditions (match iOS exactly):
        // - Shoulder width very small (profile) < 0.08
        // - Torso length reasonable (> 0.18) to ensure person presence
        // - Knee angles plausible (150-185)
        val shoulderNarrow = shoulderDx < 0.08f
        val torsoOK = torsoLen > 0.18f
        val kneesOK = (leftKneeAngle >= 150.0 && leftKneeAngle <= 185.0) || 
                     (rightKneeAngle >= 150.0 && rightKneeAngle <= 185.0)
        
        Log.d(TAG, "Side pose check: landmarks=${currentLandmarks.size}, shoulderDx=$shoulderDx, torsoLen=$torsoLen, leftKnee=$leftKneeAngle, rightKnee=$rightKneeAngle")
        
        return shoulderNarrow && torsoOK && kneesOK
    }
    
    private fun calculateKneeAngle(kneeIndex: Int): Double {
        if (currentLandmarks.size < 33) return 0.0
        
        val knee = currentLandmarks[kneeIndex]
        val hip = if (kneeIndex == 25) currentLandmarks[23] else currentLandmarks[24] // Left or right hip
        val ankle = if (kneeIndex == 25) currentLandmarks[27] else currentLandmarks[28] // Left or right ankle
        
        // Calculate vectors
        val v1x = hip.x - knee.x
        val v1y = hip.y - knee.y
        val v2x = ankle.x - knee.x
        val v2y = ankle.y - knee.y
        
        // Calculate angle using dot product
        val dot = v1x * v2x + v1y * v2y
        val mag1 = sqrt(v1x * v1x + v1y * v1y)
        val mag2 = sqrt(v2x * v2x + v2y * v2y)
        
        if (mag1 == 0.0f || mag2 == 0.0f) return 0.0
        
        val cosAngle = dot / (mag1 * mag2)
        val angleRad = acos(cosAngle.coerceIn(-1.0f, 1.0f).toDouble())
        return Math.toDegrees(angleRad)
    }
    
    private fun playCountdownThenCapture(isFrontCapture: Boolean) {
        try {
            countdownPlayer?.release()
            // Use raw resource identifier
            val countdownResId = resources.getIdentifier("countdown", "raw", packageName)
            countdownPlayer = MediaPlayer.create(this, countdownResId)
            
            countdownPlayer?.setOnCompletionListener {
                // Wait 3 seconds after audio completes (like iOS)
                Handler(Looper.getMainLooper()).postDelayed({
                    captureAndStoreImage(isFrontCapture)
                }, 3000)
            }
            
            countdownPlayer?.start()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error playing countdown", e)
            // Fallback: capture after 3 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                captureAndStoreImage(isFrontCapture)
            }, 3000)
        }
    }
    
    private fun captureAndStoreImage(isFrontCapture: Boolean) {
        val bitmap = latestBitmap ?: return
        
        // Create composite image with overlay
        val compositeBitmap = createCompositeImage(bitmap)
        val base64 = bitmapToBase64(compositeBitmap)
        
        if (isFrontCapture) {
            frontImageB64 = base64
            Log.d(TAG, "Saved FRONT image (base64 length=${base64?.length ?: 0})")
            statusLabel.text = "Front captured! Turn to your side"
            // Play instruction to turn to side after front capture
            playInstruction("turntoside")
        } else {
            sideImageB64 = base64
            Log.d(TAG, "Saved SIDE image (base64 length=${base64?.length ?: 0})")
            
            // Send results back to Compose
            sendResultsToCompose()
        }
    }
    
    private fun createCompositeImage(baseBitmap: Bitmap): Bitmap {
        val composite = Bitmap.createBitmap(baseBitmap.width, baseBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(composite)
        
        // Draw base image mirrored horizontally to match selfie preview
        val baseMatrix = Matrix().apply {
            postScale(-1f, 1f, baseBitmap.width / 2f, baseBitmap.height / 2f)
        }
        canvas.drawBitmap(baseBitmap, baseMatrix, null)
        
        // Draw skeleton overlay in the same mirrored coordinate system
        canvas.save()
        canvas.scale(-1f, 1f, baseBitmap.width / 2f, baseBitmap.height / 2f)
        drawSkeletonOnCanvas(canvas, baseBitmap.width, baseBitmap.height)
        canvas.restore()

        // Rotate the composed image anticlockwise by 90° for Android dialog preview
        val rotateMatrix = Matrix().apply { postRotate(-90f) }
        val rotated = Bitmap.createBitmap(composite, 0, 0, composite.width, composite.height, rotateMatrix, true)
        if (rotated != composite) {
            composite.recycle()
        }
        return rotated
    }
    
    private fun drawSkeletonOnCanvas(canvas: Canvas, width: Int, height: Int) {
        if (currentLandmarks.isEmpty()) return
        
        val paint = Paint().apply {
            color = Color.GREEN
            strokeWidth = 12f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        
        // Define connections (same as iOS)
        val connections = listOf(
            // Face
            Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 7),
            Pair(0, 4), Pair(4, 5), Pair(5, 6), Pair(6, 8),
            // Shoulders
            Pair(9, 10), Pair(11, 12), Pair(11, 13), Pair(13, 15),
            Pair(12, 14), Pair(14, 16), Pair(11, 23), Pair(12, 24),
            // Body
            Pair(23, 24), Pair(23, 25), Pair(25, 27), Pair(27, 29), Pair(27, 31),
            Pair(24, 26), Pair(26, 28), Pair(28, 30), Pair(28, 32),
            // Arms
            Pair(15, 17), Pair(15, 19), Pair(15, 21), Pair(17, 19),
            Pair(16, 18), Pair(16, 20), Pair(16, 22), Pair(18, 20)
        )
        
        for ((start, end) in connections) {
            if (start < currentLandmarks.size && end < currentLandmarks.size) {
                val startLandmark = currentLandmarks[start]
                val endLandmark = currentLandmarks[end]
                
                // Convert normalized coordinates to screen coordinates (no mirroring here; canvas is mirrored)
                val startX = startLandmark.x * width
                val startY = startLandmark.y * height
                val endX = endLandmark.x * width
                val endY = endLandmark.y * height
                
                canvas.drawLine(startX, startY, endX, endY, paint)
            }
        }
    }
    
    private fun bitmapToBase64(bitmap: Bitmap): String? {
        return try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting bitmap to base64", e)
            null
        }
    }
    
    private fun sendResultsToCompose() {
        // Send results back to ScanImagesBridge
        val scanImagesBridge = com.mobil80.posturely.scan.ScanImagesBridge
        scanImagesBridge.onImagesReady?.invoke(scanId, frontImageB64 ?: "", sideImageB64 ?: "")
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up bitmaps first
        latestBitmap?.recycle()
        latestBitmap = null
        
        // Close MediaPipe first to prevent crashes
        poseLandmarkerHelper?.close()
        poseLandmarkerHelper = null
        
        // Clean up camera resources
        camera?.cameraControl?.cancelFocusAndMetering()
        cameraProvider?.unbindAll()
        camera = null
        cameraProvider = null
        
        countdownPlayer?.release()
        countdownPlayer = null
        instructionPlayer?.release()
        instructionPlayer = null
        
        cameraExecutor.shutdown()
        Log.d(TAG, "Activity destroyed and resources cleaned up")
    }

    private fun playInstruction(basename: String) {
        try {
            // Prefer Android res/raw to guarantee packaging
            val resId = resources.getIdentifier(basename, "raw", packageName)
            if (resId != 0) {
                instructionPlayer?.release()
                instructionPlayer = MediaPlayer.create(this, resId)
                instructionPlayer?.start()
                return
            }

            // Fallback to KMP assets if needed
            val assetPath = "files/${basename}.mp3"
            assets.openFd(assetPath).use { afd ->
                instructionPlayer?.release()
                instructionPlayer = MediaPlayer()
                instructionPlayer?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                instructionPlayer?.prepare()
                instructionPlayer?.start()
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing instruction $basename", e)
        }
    }

    override fun onResume() {
        super.onResume()
        // Keep screen on while scanning is active
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        // Allow screen to sleep when not actively scanning
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onPause()
    }
}
