package com.example.posturelynew

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import posturelynew.composeapp.generated.resources.Res
import posturelynew.composeapp.generated.resources.giraffeenew
import com.example.posturelynew.home.calculateRealMetrics
import kotlin.math.sin
import kotlin.math.cos
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.posturelynew.audio.AudioPlayer

@OptIn(ExperimentalResourceApi::class)
@Composable
fun DesktopLiveTrackingScreen(
    onBackPressed: () -> Unit,
    userEmail: String = "",
    autoStart: Boolean = false
) {
    var isTracking by remember { mutableStateOf(false) }
    var currentScore by remember { mutableStateOf(0) }
    var postureStatus by remember { mutableStateOf("NO SIGNAL") }
    var showCamera by remember { mutableStateOf(false) }
    var cameraFrame by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var showCalibration by remember { mutableStateOf(false) }
    var isCalibrated by remember { mutableStateOf(false) }
    
    // Posture metrics from real tracking data
    var torsoTilt by remember { mutableStateOf(0.0) }
    var shoulderTilt by remember { mutableStateOf(0.0) }
    var neckFlex by remember { mutableStateOf(0.0) }
    var headZDelta by remember { mutableStateOf(0.0) }
    var shoulderAsymY by remember { mutableStateOf(0.0) }
    
    // Python-style posture ratio calculation (exact implementation)
    var postureRatio by remember { mutableStateOf(0.0) }
    var faceToShoulderDistance by remember { mutableStateOf(0.0) }
    var shoulderWidth by remember { mutableStateOf(0.0) }
    var faceCenter by remember { mutableStateOf(Pair(0.0, 0.0)) }
    var shoulderCenter by remember { mutableStateOf(Pair(0.0, 0.0)) }
    var noseCoordinates by remember { mutableStateOf(Pair(0, 0)) }
    var leftShoulderCoordinates by remember { mutableStateOf(Pair(0, 0)) }
    var rightShoulderCoordinates by remember { mutableStateOf(Pair(0, 0)) }
    var poseConfidence by remember { mutableStateOf(0) }
    var frameCount by remember { mutableStateOf(0) }
    
    // Calibration thresholds
    var calibratedThresholds by remember { mutableStateOf<Map<String, Double>?>(null) }
    
    // Posture ratio calibration
    var calibratedRatio by remember { mutableStateOf(0.0) }
    var isRatioCalibrated by remember { mutableStateOf(false) }
    
    // Audio sequence and countdown
    var isAudioSequencePlaying by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableStateOf(0) }
    var showCountdown by remember { mutableStateOf(false) }
    
    // Stuck ratio detection
    var lastPostureRatio by remember { mutableStateOf(0.0) }
    var ratioStuckStartTime by remember { mutableStateOf(0L) }
    var isRatioStuck by remember { mutableStateOf(false) }
    var wasRatioStuck by remember { mutableStateOf(false) }
    var personReturnedMessage by remember { mutableStateOf("") }
    var messageStartTime by remember { mutableStateOf(0L) }
    var ratioChangeStreak by remember { mutableStateOf(0) }
    
    // Initialize audio player
    val audioPlayer = remember { AudioPlayer() }
    
    // Score smoothing mechanism (same as mobile)
    var scoreHistory by remember { mutableStateOf(listOf<Int>()) }
    var smoothedScore by remember { mutableStateOf(0) }
    var updateCounter by remember { mutableStateOf(0) }
    
    // Visibility detection for desktop tracking
    var isUserVisible by remember { mutableStateOf(true) }
    
    // Posture data recording service (optional for desktop)
    val postureDataService = remember { PostureDataService() }
    val isRecording by postureDataService.isRecordingFlow.collectAsState()
    val lastRecordedScore by postureDataService.lastRecordedScore.collectAsState()
    val lastRecordedTime by postureDataService.lastRecordedTime.collectAsState()
    
    val cameraService = remember { DesktopCameraService() }
    val coroutineScope = rememberCoroutineScope()
    
    // Function to smooth score changes (same as mobile)
    fun smoothScore(newScore: Int): Int {
        // Only update score every 3rd update (every 300ms since updates happen every 100ms)
        if (updateCounter % 3 != 0) {
            return smoothedScore
        }
        
        // Add new score to history (keep last 8 scores)
        val newHistory = (scoreHistory + newScore).takeLast(8)
        scoreHistory = newHistory
        
        // Calculate trend (average of last 3 scores vs previous 3 scores)
        if (newHistory.size >= 6) {
            val recentAvg = newHistory.takeLast(3).average()
            val previousAvg = newHistory.dropLast(3).takeLast(3).average()
            val trend = recentAvg - previousAvg
            
            // Apply gradual change based on trend with exponential smoothing
            val maxChange = 4 // Maximum score change per update
            val change = when {
                trend > 8 -> maxChange // Strong upward trend
                trend > 4 -> 3 // Moderate upward trend
                trend > 2 -> 2 // Slight upward trend
                trend < -8 -> -maxChange // Strong downward trend
                trend < -4 -> -3 // Moderate downward trend
                trend < -2 -> -2 // Slight downward trend
                else -> {
                    // For small trends, use weighted average
                    val weight = 0.3 // 30% new data, 70% previous
                    ((newScore * weight) + (smoothedScore * (1 - weight))).toInt() - smoothedScore
                }
            }
            
            val newSmoothedScore = (smoothedScore + change).coerceIn(0, 100)
            return newSmoothedScore
        } else {
            // Not enough history yet, use weighted average
            if (scoreHistory.isNotEmpty()) {
                val weight = 0.4 // 40% new data, 60% previous
                val newSmoothedScore = ((newScore * weight) + (smoothedScore * (1 - weight))).toInt()
                return newSmoothedScore.coerceIn(0, 100)
            } else {
                // First score, use directly
                return newScore
            }
        }
    }
    
    // Start camera when tracking starts
    LaunchedEffect(isTracking) {
        if (isTracking && showCamera) {
            cameraService.startCamera { frame ->
                cameraFrame = frame
            }
        } else {
            cameraService.stopCamera()
        }
    }
    
    // Update posture data and calculate score in real-time (mirror mobile logic)
    LaunchedEffect(isTracking) {
        if (isTracking) {
            while (true) {
                val landmarkCount = cameraService.getLandmarkCount()
                
                // Check visibility for desktop tracking
                val wasVisible = isUserVisible
                isUserVisible = landmarkCount >= 4 // User is visible if we have at least 4 landmarks
                
                // Log visibility changes
                if (wasVisible != isUserVisible) {
                    if (isUserVisible) {
                        println("✅ DesktopLiveTracking: Person is back in frame (landmarks: $landmarkCount)")
                    } else {
                        println("❌ DesktopLiveTracking: Person is not in frame (landmarks: $landmarkCount)")
                    }
                }
                
                // Always try to collect landmarks and compute ratio
                frameCount++
                    val newLandmarks = mutableListOf<Pair<Float, Float>>()
                    for (i in 0 until kotlin.math.min(landmarkCount, 33)) {
                        val x = cameraService.getLandmarkX(i)
                        val y = cameraService.getLandmarkY(i)
                        newLandmarks.add(Pair(x, y))
                    }

                var newRatioComputed = false
                if (landmarkCount >= 33 && newLandmarks.size >= 33) {
                    val postureData = calculatePostureRatio(newLandmarks)
                    if (postureData != null) {
                        postureRatio = postureData.ratio
                        faceToShoulderDistance = postureData.faceToShoulderDistance
                        shoulderWidth = postureData.shoulderWidth
                        faceCenter = postureData.faceCenter
                        shoulderCenter = postureData.shoulderCenter
                        newRatioComputed = true
                    }
                }

                // Ratio-based presence detection only
                val currentTime = System.currentTimeMillis()
                val ratioDifference = kotlin.math.abs(postureRatio - lastPostureRatio)

                if (newRatioComputed) {
                    if (ratioDifference < 0.002) { // tighter: require bigger change to count as movement
                        if (ratioStuckStartTime == 0L) {
                            ratioStuckStartTime = currentTime
                            println("🔍 Frame $frameCount: Starting to track stuck ratio at $postureRatio")
                        } else if (currentTime - ratioStuckStartTime > 2000 && !isRatioStuck) {
                            isRatioStuck = true
                            println("🚨 Frame $frameCount: Posture ratio stuck at $postureRatio for more than 2 seconds")
                        }
                        ratioChangeStreak = 0
                    } else {
                        // Only resume after sustained meaningful changes to avoid noise
                        ratioChangeStreak = (ratioChangeStreak + 1).coerceAtMost(10)
                        if (ratioChangeStreak >= 3) { // require 3 consecutive frames of change
                            if (isRatioStuck) {
                                personReturnedMessage = "Welcome back! Person detected again"
                                messageStartTime = currentTime
                                println("🎉 Frame $frameCount: Person returned! Sustained ratio change from $lastPostureRatio to $postureRatio")
                            }
                            ratioStuckStartTime = 0L
                            isRatioStuck = false
                            lastPostureRatio = postureRatio
                            ratioChangeStreak = 0
                        }
                    }
                } else {
                    // No new ratio computed this frame -> treat as no change
                    if (ratioStuckStartTime == 0L) {
                        ratioStuckStartTime = currentTime
                    } else if (currentTime - ratioStuckStartTime > 2000 && !isRatioStuck) {
                        isRatioStuck = true
                        println("🚨 Frame $frameCount: No ratio updates for more than 2 seconds")
                    }
                    ratioChangeStreak = 0
                }

                // Clear welcome back message after 3 seconds
                if (personReturnedMessage.isNotEmpty() && System.currentTimeMillis() - messageStartTime > 3000) {
                    personReturnedMessage = ""
                }

                // isUserVisible: ratio must be active AND we must have minimal landmarks
                isUserVisible = (!isRatioStuck && postureRatio > 0) && landmarkCount >= 4

                if (!isRatioStuck && newRatioComputed && postureRatio > 0) {
                    // Calculate score based on calibrated ratio
                    currentScore = if (isRatioCalibrated && calibratedRatio > 0) {
                        calculateRatioScore(postureRatio, calibratedRatio)
                    } else {
                        if (postureRatio >= 0.98) 85 else 30
                    }

                    // Determine posture status based on calibrated ratio
                    postureStatus = if (isRatioCalibrated) {
                        when {
                            currentScore >= 80 -> "Good Posture"
                            currentScore >= 60 -> "Fair Posture"
                            else -> "Poor Posture"
                        }
                    } else {
                        if (postureRatio >= 0.98) "Good Posture" else "Bad Posture - Slouching!"
                    }

                    // Compute metrics and smoothing (optional; does not affect visibility)
                    val metrics = com.example.posturelynew.home.calculateRealMetrics(newLandmarks)
                    torsoTilt = metrics.torsoTilt
                    shoulderTilt = metrics.shoulderTilt
                    neckFlex = metrics.neckFlex
                    headZDelta = metrics.headZDelta
                    shoulderAsymY = metrics.shoulderAsymY

                    smoothedScore = smoothScore(currentScore)
                    
                    if (userEmail.isNotEmpty()) {
                        postureDataService.addPostureScore(smoothedScore)
                    }
                    
                    println("🎯 Desktop live tracking update: ratio=${postureRatio.toString().take(5)}, score=$currentScore, smoothed=$smoothedScore, stuck=$isRatioStuck")
                } else {
                    postureStatus = "Person not in frame"
                    currentScore = 0
                    // Do not zero postureRatio; keep last value to allow change detection
                }
                
                // Update FPS and counter for smoothing (same as mobile)
                updateCounter++ // Increment counter for smoothing
                kotlinx.coroutines.delay(100) // Update every 100ms like mobile
            }
        }
    }
    
    // Cleanup when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            cameraService.stopCamera()
            
            // Stop posture data recording
            if (userEmail.isNotEmpty()) {
                postureDataService.stopRecording()
            }
        }
    }
    
    // Theming colors to match mobile/brand look
    val pageBg = Color(0xFFFED867)
    val cardBg = Color(0xFFFFF0C0)
    val textPrimary = Color(0xFF0F1931)
    val accentBrown = Color(0xFFD69C2F)

    var showAdvanced by remember { mutableStateOf(false) }

    // Reusable toggle tracking action to keep behavior identical across UI buttons
    val onToggleTracking: () -> Unit = {
        isTracking = !isTracking
        if (isTracking) {
            if (!showCamera) {
                showCamera = true
                coroutineScope.launch {
                    cameraService.stopCamera()
                    delay(200)
                    cameraService.selectCameraIndex(0)
                    delay(100)
                    cameraService.startCamera { frame ->
                        cameraFrame = frame
                    }
                    delay(500)
                    cameraService.startTracking()

                    isAudioSequencePlaying = true
                    audioPlayer.playAudioSequence()

                    delay(11000)
                    showCountdown = true
                    for (i in 3 downTo 1) {
                        countdownValue = i
                        delay(1000)
                    }
                    showCountdown = false
                    isAudioSequencePlaying = false

                    delay(2000)
                    if (postureRatio > 0 && !postureRatio.isNaN()) {
                        calibratedRatio = postureRatio
                        isRatioCalibrated = true
                        println("🎯 Auto-calibrated ratio: $calibratedRatio (Current = GOOD)")
                    }
                }
            } else {
                cameraService.startTracking()
                isAudioSequencePlaying = true
                audioPlayer.playAudioSequence()
                coroutineScope.launch {
                    delay(11000)
                    showCountdown = true
                    for (i in 3 downTo 1) {
                        countdownValue = i
                        delay(1000)
                    }
                    showCountdown = false
                    isAudioSequencePlaying = false
                    delay(2000)
                    if (postureRatio > 0 && !postureRatio.isNaN()) {
                        calibratedRatio = postureRatio
                        isRatioCalibrated = true
                        println("🎯 Auto-calibrated ratio: $calibratedRatio (Current = GOOD)")
                    }
                }
            }
        } else {
            cameraService.stopTracking()
            isAudioSequencePlaying = false
            showCountdown = false
            countdownValue = 0
        }
    }

    // Auto-start flow when requested (from homepage button)
    LaunchedEffect(autoStart) {
        if (autoStart && !isTracking) {
            onToggleTracking()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .padding(40.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onBackPressed,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("← Back", fontSize = 16.sp)
            }
            
            // Score and Status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            when {
                                smoothedScore >= 80 -> Color(0xFF28A745) // Green
                                smoothedScore >= 60 -> Color(0xFFFFA000) // Orange
                                else -> Color(0xFFEF4444) // Red
                            },
                            androidx.compose.foundation.shape.CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Score: show labeled, large, color-coded value
                Text(
                    text = "Score: $smoothedScore",
                    color = textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.width(12.dp))
                // Short status label next to the score
                val headerStatus = when {
                    !isUserVisible -> "NO PERSON"
                    smoothedScore >= 80 -> "GOOD"
                    smoothedScore >= 60 -> "OK"
                    else -> "BAD"
                }
                
                // More detailed status for debugging
                val detailedStatus = when {
                    !isUserVisible -> "Person not detected"
                    smoothedScore >= 80 -> "Good posture"
                    smoothedScore >= 60 -> "Fair posture"
                    else -> "Poor posture"
                }
                Text(
                    text = headerStatus,
                    color = textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Top cards layout matching the mock
        Row(
                modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left: Giraffe card
            Card(
                modifier = Modifier.weight(1f).height(260.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(painter = painterResource(Res.drawable.giraffeenew), contentDescription = "Giraffe")
                }
            }

            // Right: Status ring + Start/Stop button
            Card(
                modifier = Modifier.weight(1f).height(260.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    val statusLabel = when {
                        !isUserVisible -> "NO PERSON"
                        smoothedScore >= 80 -> "GOOD"
                        smoothedScore >= 60 -> "OK"
                        else -> "BAD"
                    }
                    val ringColor = when {
                    smoothedScore >= 80 -> Color(0xFF34C759)
                    smoothedScore >= 60 -> Color(0xFFFFA000)
                    else -> Color(0xFFEF4444)
                }
                    StatusRingLarge(
                        percent = (smoothedScore / 100f).coerceIn(0f, 1f),
                        label = statusLabel,
                        ringColor = ringColor,
                        textColor = textPrimary
                    )

                    Button(
                        onClick = onToggleTracking,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentBrown)
                    ) {
        Text(
                            if (isTracking) "Stop tracking" else "Start tracking",
            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Toggle for advanced controls (camera feed etc.)
        TextButton(onClick = { showAdvanced = !showAdvanced }) {
            Text(if (showAdvanced) "Hide advanced controls" else "Show advanced controls", color = textPrimary)
        }

        // Camera View and advanced controls (hidden by default)
        if (showAdvanced) {
        
        // Camera View
        if (showCamera && cameraFrame != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(Color.Black, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = cameraFrame!!,
                    contentDescription = "Camera Feed",
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // Camera placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(Color(0xFF2C2C2E), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📷",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Not Active",
                        color = Color.Gray,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Use the buttons below to control camera and tracking",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Ratio and Score Display - Side by Side
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isUserVisible && postureRatio > 0) {
                // Ratio on the left
            Text(
                    text = String.format("%.3f", postureRatio),
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(end = 32.dp)
                )
                
                // Score on the right
            Text(
                    text = "$currentScore",
                    color = when {
                        currentScore >= 80 -> Color(0xFF28A745) // Green
                        currentScore >= 60 -> Color(0xFFFFA000) // Orange
                        else -> Color(0xFFEF4444) // Red
                    },
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold
            )
            } else {
                Text(
                    text = "—",
                    color = Color.Gray,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        
        // Status display below
        Text(
            text = postureStatus,
            color = when {
                !isUserVisible -> Color(0xFF6C757D) // Gray for no person
                currentScore >= 80 -> Color(0xFF28A745) // Green
                currentScore >= 60 -> Color(0xFFFFA000) // Orange
                else -> Color(0xFFEF4444) // Red
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            textAlign = TextAlign.Center
        )
            
            // Welcome back message
            if (personReturnedMessage.isNotEmpty()) {
                Text(
                    text = personReturnedMessage,
                color = Color(0xFF28A745), // Green
                fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
            
        // Calibrate Button - Ratio-based calibration
        if (isTracking && postureRatio > 0) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                        // Set current ratio as the "good" baseline
                        if (postureRatio > 0 && !postureRatio.isNaN()) {
                            calibratedRatio = postureRatio
                            isRatioCalibrated = true
                            println("🎯 Calibrated ratio: $calibratedRatio (Current = GOOD)")
                        } else {
                            println("❌ Cannot calibrate: Invalid ratio value")
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                Text(
                    "🎯 Calibrate (Current Ratio = GOOD)",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
                if (isRatioCalibrated) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "✅ Calibrated ratio: ${String.format("%.3f", calibratedRatio)} (Current = GOOD)",
                    color = Color(0xFF28A745),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        
        // Countdown Display (overlay)
        if (showCountdown && countdownValue > 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = countdownValue.toString(),
                    color = Color.White,
                    fontSize = 120.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(32.dp)
                )
            }
        }
        
        // Close advanced controls block
        }
        
        // Audio Sequence Status
        if (isAudioSequencePlaying) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = if (showCountdown) "Get Ready..." else "Sit Straight!",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            Color(0xFF28A745).copy(alpha = 0.8f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                        .padding(top = 32.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Action Buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Camera Status
            Text(
                text = "🖥️ Desktop Camera Active",
                color = Color(0xFF28A745),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            
            // Section Title
            Text(
                text = "Camera Controls",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Camera Selection Button
            Button(
                onClick = {
                    cameraService.showCameraSelectionDialog { selectedIndex ->
                        println("Selected camera: $selectedIndex")
                        // Restart camera with new selection if it's running
                        if (showCamera) {
                            cameraService.stopCamera()
                            cameraService.startCamera { frame ->
                                cameraFrame = frame
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
            ) {
                Text(
                    "📹 Select Camera: ${cameraService.getSelectedCameraName()}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Camera Toggle Button
            Button(
                onClick = { 
                    showCamera = !showCamera
                    if (showCamera) {
                        // Always restart camera when starting, regardless of tracking state
                        // Simulate exactly what happens when manually selecting camera index 0
                        coroutineScope.launch {
                            // First stop any existing camera completely
                            cameraService.stopCamera()
                            // Wait a moment for clean shutdown
                            delay(200)
                            // Then manually select camera index 0 (same as the dialog would do)
                            cameraService.selectCameraIndex(0)
                            // Wait a moment for the camera selection to take effect
                            delay(100)
                            // Finally start the camera with the selected index
                            cameraService.startCamera { frame ->
                                cameraFrame = frame
                            }
                        }
                    } else {
                        cameraService.stopCamera()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showCamera) Color(0xFFEF4444) else Color(0xFF007AFF)
                )
            ) {
                Text(
                    if (showCamera) "📷 Stop Camera" else "📷 Start Camera (Auto-selects Camera 0)",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Start/Stop Tracking Button
            Button(
                onClick = onToggleTracking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTracking) accentBrown else accentBrown
                )
            ) {
                Text(
                    if (isTracking) "⏹ Stop Tracking" else "▶ Start Tracking (Auto-starts camera)",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Footer Hint
            Text(
                "Real camera with MediaPipe WebSocket pose detection - no mock data",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            
            // MediaPipe Status
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = cameraService.getMediaPipeStatus(),
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Test Pose Detection Button
            if (isTracking && showCamera) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        // Manually trigger a pose detection to test the system
                        val testImage = cameraService.getCurrentFrame()
                        if (testImage != null) {
                            println("DesktopLiveTrackingScreen: Manually testing pose detection...")
                            // This will help debug if the issue is with frame processing
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B7280))
                ) {
                    Text(
                        "🧪 Test Pose Detection",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}


@Composable
fun StatusRingLarge(
    percent: Float,
    label: String,
    ringColor: Color,
    textColor: Color
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            drawArc(color = Color(0xFFE8EEF5), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))
            drawArc(color = ringColor, startAngle = -90f, sweepAngle = 360f * percent, useCenter = false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// --- Ratio helpers used by desktop tracking ---

data class PostureRatioData(
    val ratio: Double,
    val faceToShoulderDistance: Double,
    val shoulderWidth: Double,
    val faceCenter: Pair<Double, Double>,
    val shoulderCenter: Pair<Double, Double>
)

private fun calculatePostureRatio(landmarks: List<Pair<Float, Float>>): PostureRatioData? {
    return try {
        if (landmarks.size < 33) return null
        val nose = landmarks[0]
        val leftEye = landmarks[2]
        val rightEye = landmarks[5]
        val leftShoulder = landmarks[11]
        val rightShoulder = landmarks[12]

        val shoulderWidth = calculateDistance(leftShoulder, rightShoulder)
        val faceCenterX = (leftEye.first + rightEye.first + nose.first) / 3.0
        val faceCenterY = (leftEye.second + rightEye.second + nose.second) / 3.0
        val shoulderCenterX = (leftShoulder.first + rightShoulder.first) / 2.0
        val shoulderCenterY = (leftShoulder.second + rightShoulder.second) / 2.0
        val faceToShoulderDistance = kotlin.math.abs(faceCenterY - shoulderCenterY)
        val ratio = faceToShoulderDistance / shoulderWidth
        
        PostureRatioData(
            ratio = ratio,
            faceToShoulderDistance = faceToShoulderDistance,
            shoulderWidth = shoulderWidth,
            faceCenter = Pair(faceCenterX, faceCenterY),
            shoulderCenter = Pair(shoulderCenterX, shoulderCenterY)
        )
    } catch (_: Exception) { null }
}

private fun calculateDistance(p1: Pair<Float, Float>, p2: Pair<Float, Float>): Double {
    val dx = p1.first - p2.first
    val dy = p1.second - p2.second
    return kotlin.math.sqrt((dx * dx + dy * dy).toDouble())
}

private fun calculateRatioScore(currentRatio: Double, calibratedRatio: Double): Int {
    if (calibratedRatio <= 0 || currentRatio <= 0 || calibratedRatio.isNaN() || currentRatio.isNaN()) return 50
    val drop = calibratedRatio - currentRatio
    if (drop <= 0) return 100
    val score = 100.0 - drop * 200.0
    return score.toInt().coerceIn(0, 100)
}
