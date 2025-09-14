package com.example.posturelynew.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import posturelynew.composeapp.generated.resources.Res
import posturelynew.composeapp.generated.resources.giraffeenew
import posturelynew.composeapp.generated.resources.calibrating
import posturelynew.composeapp.generated.resources.phone
import posturelynew.composeapp.generated.resources.laptop
import posturelynew.composeapp.generated.resources.airpods
import com.example.posturelynew.AirPodsTracker
import com.example.posturelynew.audio.rememberAudioPlayer
import com.example.posturelynew.audio.soundResList
import com.example.posturelynew.createPostureTrackingInterface
import com.example.posturelynew.PoseMetrics
import kotlinx.coroutines.delay
import com.example.posturelynew.home.calculateRealMetrics
import com.example.posturelynew.home.calculatePostureScore
import com.example.posturelynew.home.smoothScore
import com.example.posturelynew.PlatformStorage
import com.example.posturelynew.PostureDataService
import com.example.posturelynew.ProgressService
import com.example.posturelynew.ProgressData
import com.example.posturelynew.getPlatformName
import kotlinx.coroutines.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.style.TextAlign


// Top-level helpers moved above to avoid local scope/resolution issues
@Composable
fun ScoreRing(percent: Float, color: Color) {
    Canvas(Modifier.size(96.dp)) {
        val stroke = 12.dp.toPx()
        drawArc(color = Color(0xFFE8EEF5), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(width = stroke))
        drawArc(color = color, startAngle = -90f, sweepAngle = 360f * percent, useCenter = false, style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}


@Composable
fun ScoreRingCenter(
    percent: Float,
    color: Color,
    primary: Color,
    secondary: Color,
    score: Int = 92,
    isLive: Boolean = false,
    isWaiting: Boolean = false
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(112.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            drawArc(color = Color(0xFFE8EEF5), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(width = stroke))
            drawArc(color = color, startAngle = -90f, sweepAngle = 360f * percent, useCenter = false, style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isWaiting) {
                Text("--", color = primary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("Starting...", color = secondary, fontSize = 10.sp)
            } else {
                Text(score.toString(), color = Color.Black, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Score", color = secondary, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, title: String, value: String, valueColor: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0C0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                color = valueColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                title,
                color = Color(0xFF6B7280),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

data class SessionSummary(
    val sessionTime: String,
    val totalProgress: String,
    val averageScore: Int
)

@OptIn(ExperimentalResourceApi::class)
@Composable
fun DeviceOptionCard(
    icon: DrawableResource,
    label: String,
    selected: Boolean = false,
    cardWidth: Dp = 130.dp,
    cardHeight: Dp = 150.dp,
    onClick: () -> Unit
) {
    val accentBrown = Color(0xFF7A4B00)
    val textPrimary = Color(0xFF0F1931)
    
    Card(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) accentBrown else Color(0xFFFFF0C0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 4.dp else 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Use proper asset icon
            Image(
                painter = painterResource(icon),
                contentDescription = label,
                modifier = Modifier
                    .size(48.dp)
                    .padding(bottom = 12.dp),
                contentScale = ContentScale.Fit,
                colorFilter = if (selected) androidx.compose.ui.graphics.ColorFilter.tint(Color.White) else androidx.compose.ui.graphics.ColorFilter.tint(accentBrown)
            )
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) Color.White else textPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun HomeDashboardPage(
    isDark: Boolean = true,
    onToggleTheme: (() -> Unit)? = null,
    onNavigateToLiveTracking: (() -> Unit)? = null,
    onNavigateToAirPodsTracking: (() -> Unit)? = null,
    onNavigateToLaptopTracking: (() -> Unit)? = null,
    isMonitoring: Boolean = false,
    isPostureGood: Boolean = true,
    sessionMinutes: Int = 34,
    sessionGoalMinutes: Int = 60,
    onTrackingStateChange: (Boolean) -> Unit = {}
) {
    val pageBgDefault = Color(0xFFFED866)
    val pageBgTracking = Color(0xFFFED866)
    val textPrimary = Color(0xFF0F1931)
    val subText = Color(0xFF6B7280)
    val accentBrown = Color(0xFF7A4B00)

    var monitoring by remember { mutableStateOf(isMonitoring) }
    var monitoringSource by remember { mutableStateOf<String?>(null) }
    var selectedSource by remember { mutableStateOf<String?>(null) }
    var showDeviceSelectionError by remember { mutableStateOf(false) }
    var shouldShake by remember { mutableStateOf(false) }
    
    // Shake animation
    val shakeOffset by animateFloatAsState(
        targetValue = if (shouldShake) 10f else 0f,
        animationSpec = spring(dampingRatio = 0.3f, stiffness = 300f),
        finishedListener = { shouldShake = false }
    )
    
    // Live tracking state
    var isLiveTracking by remember { mutableStateOf(false) }
    var currentScore by remember { mutableStateOf(0) }
    var smoothedScore by remember { mutableStateOf(0) }
    var postureStatus by remember { mutableStateOf("GOOD") }
    var isCalibrated by remember { mutableStateOf(false) }
    var calibratedThresholds by remember { mutableStateOf<Map<String, Double>?>(null) }
    
    // Session timer state
    var sessionStartTime by remember { mutableStateOf(0L) }
    var sessionDurationSeconds by remember { mutableStateOf(0) }
    
    // Posture alert variables
    var lowScoreTicks by remember { mutableStateOf(0) }
    var isBeepPlaying by remember { mutableStateOf(false) }
    var lowScoreThreshold by remember { mutableStateOf(80) }
    var beepTick by remember { mutableStateOf(0) } // ticks of 200ms; 10 ticks = 2s

    // Visibility (phone camera) state
    var isUserVisible by remember { mutableStateOf(true) }
    
    // Posture tracking interface
    val postureInterface = remember { createPostureTrackingInterface() }
    
    // AirPods tracking
    val airPodsTracker = remember { AirPodsTracker() }
    var airPodsConnected by remember { mutableStateOf(false) }
    var showAirPodsAlert by remember { mutableStateOf(false) }
    var showCongratulationsDialog by remember { mutableStateOf(false) }
    var sessionSummary by remember { mutableStateOf<SessionSummary?>(null) }
    
    // PostureDataService for recording posture data
    val storage = remember { PlatformStorage() }
    val userEmail = storage.getString("userEmail", "")
    val postureDataService = remember { PostureDataService() }
    val progressService = remember { ProgressService() }
    
    // Collect progress data
    val progressData by progressService.progressData.collectAsState()
    val isProgressLoading by progressService.isLoading.collectAsState()
    
    // Debug progress data
    LaunchedEffect(progressData) {
        println("🔍 HomeDashboard: Progress data updated - totalMinutes: ${progressData.totalMinutes}, averageScore: ${progressData.averageScore}, samples: ${progressData.totalSamples}")
    }
    val audioPlayer = rememberAudioPlayer()
    var isAudioSequencePlaying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Initialize progress tracking when user email is available
    LaunchedEffect(userEmail) {
        if (userEmail.isNotEmpty()) {
            println("📊 HomeDashboard: User email available: $userEmail")
            
            // Load initial progress data when screen loads
            println("📊 HomeDashboard: Loading initial progress data")
            progressService.startProgressTracking(userEmail)
            
            // Immediately refresh to ensure data is loaded
            println("📊 HomeDashboard: Refreshing progress data immediately")
            progressService.refreshProgress()
            
            // Set up progress update callback
            postureDataService.setProgressUpdateCallback {
                println("🔄 HomeDashboard: Triggering progress refresh after new data")
                CoroutineScope(Dispatchers.IO).launch {
                    progressService.refreshProgress()
                }
            }
        }
    }
    LaunchedEffect(monitoring, monitoringSource) {
        if (monitoring && (monitoringSource == "Phone" || monitoringSource == "AirPods")) {
            // Progress tracking is already running from initial load, just refresh data
            if (userEmail.isNotEmpty()) {
                println("📊 HomeDashboard: Refreshing progress data for tracking session")
                progressService.refreshProgress()
            }
            
            // Wait for second audio (countdown) to start playing
            // sitstraight plays immediately, then 11s wait, then countdown starts
            delay(11000) // 11 seconds - start live tracking when countdown begins
            
            // Start live tracking when second audio (countdown) starts
            isLiveTracking = true
            sessionStartTime = 0L // Will be set by timer loop
            
            if (monitoringSource == "Phone") {
                // Phone tracking with camera
                postureInterface.startTracking()
                
                // Wait a moment for first pose data, then auto-calibrate
                delay(2000) // Wait 2 seconds for stable pose detection
                
                // Auto-calibrate: set current posture as "good" baseline
                val landmarkCount = postureInterface.getLandmarkCount()
                if (landmarkCount > 0) {
                    val newLandmarks = mutableListOf<Pair<Float, Float>>()
                    for (i in 0 until minOf(landmarkCount, 33)) {
                        val x = postureInterface.getLandmarkX(i)
                        val y = postureInterface.getLandmarkY(i)
                        newLandmarks.add(Pair(x, y))
                    }
                    
                    if (newLandmarks.isNotEmpty()) {
                        val metrics = calculateRealMetrics(newLandmarks)
                        // Set current posture as baseline with some margin
                        calibratedThresholds = mapOf(
                            "torsoTilt" to (metrics.torsoTilt + 3.0), // Add margin like LiveTrackingScreen
                            "shoulderTilt" to (metrics.shoulderTilt + 3.0),
                            "neckFlex" to (metrics.neckFlex + 3.0),
                            "headZDelta" to (metrics.headZDelta - 0.02), // Stricter for forward head
                            "shoulderAsymY" to (metrics.shoulderAsymY + 0.02)
                        )
                        isCalibrated = true
                    }
                }
            } else if (monitoringSource == "AirPods") {
                // AirPods tracking
                airPodsTracker.startTracking()
                
                // Wait a moment for first tilt data, then auto-calibrate
                delay(2000) // Wait 2 seconds for stable tilt detection
                
                // Auto-calibrate: set current AirPods angle as "good" baseline
                val currentTilt = airPodsTracker.getCurrentTiltAngle()
                println("📊 HomeDashboard: AirPods auto-calibration - Current tilt: $currentTilt°")
                if (kotlin.math.abs(currentTilt) < 30) { // Only calibrate if angle is reasonable
                    // Set current tilt as baseline with some margin
                    val baselineTilt = kotlin.math.abs(currentTilt.toDouble())
                    calibratedThresholds = mapOf(
                        "airPodsTilt" to (baselineTilt + 8.0) // Add 8 degree margin for better tolerance
                    )
                    isCalibrated = true
                    println("📊 HomeDashboard: AirPods calibrated - Baseline: $baselineTilt°, Threshold: ${baselineTilt + 8.0}°")
                } else {
                    println("📊 HomeDashboard: AirPods angle too extreme for calibration: $currentTilt°")
                }
            }
        }
    }
    
    // Live tracking update loop
    LaunchedEffect(isLiveTracking) {
        if (isLiveTracking) {
            while (true) {
                try {
                    if (monitoringSource == "Phone") {
                        // Phone tracking with camera
                        val landmarkCount = postureInterface.getLandmarkCount()
                        // Consider user not visible if we can't see enough landmarks (less than 4 landmarks = out of frame)
                        val wasVisible = isUserVisible
                        isUserVisible = landmarkCount >= 4 // User is visible if we have at least 4 landmarks
                        
                        // Log visibility changes with clear messages
                        if (wasVisible != isUserVisible) {
                            if (isUserVisible) {
                                println("✅ HomeDashboard: Person is back in frame (landmarks: $landmarkCount)")
                            } else {
                                println("❌ HomeDashboard: Person is not in frame (landmarks: $landmarkCount)")
                            }
                        }
                        
                        if (!isUserVisible) {
                            // Pause alerts and reset counters when not visible
                            if (isBeepPlaying) {
                                audioPlayer.stopBeepSound()
                                isBeepPlaying = false
                            }
                            if (lowScoreTicks != 0 || beepTick != 0) {
                                lowScoreTicks = 0
                                beepTick = 0
                            }
                        }
                        if (landmarkCount > 0) {
                            val newLandmarks = mutableListOf<Pair<Float, Float>>()
                            for (i in 0 until minOf(landmarkCount, 33)) {
                                val x = postureInterface.getLandmarkX(i)
                                val y = postureInterface.getLandmarkY(i)
                                newLandmarks.add(Pair(x, y))
                            }
                            
                            // Calculate real metrics from landmarks
                            val metrics = calculateRealMetrics(newLandmarks)
                            
                            // Calculate real-time score with calibrated thresholds
                            val scoreResult = calculatePostureScore(metrics, calibratedThresholds)
                            currentScore = scoreResult.score
                            
                            // Apply smoothing to prevent wild fluctuations
                            smoothedScore = smoothScore(currentScore, smoothedScore)
                            
                            // Update status based on smoothed score
                            postureStatus = when {
                                !isCalibrated -> "CALIBRATING"
                                smoothedScore >= 80 -> "GOOD"
                                smoothedScore >= 60 -> "OK"
                                else -> "BAD"
                            }
                            
                            // Posture alert logic (beep every 2s after 5s below threshold)
                            if (isUserVisible && smoothedScore < lowScoreThreshold) {
                                if (lowScoreTicks < 25) {
                                    // Accumulate until 5 seconds (25 * 200ms)
                                    lowScoreTicks++

                                } else {
                                    // Sustained bad posture: play a short beep every 2 seconds
                                    beepTick++ // 10 ticks * 200ms = 2s
                                    if (beepTick >= 10) {
                                        audioPlayer.playSound(2) // single beep
                                        beepTick = 0
                                    }
                                }
                            } else {
                                // Score is back above threshold -> reset alert state
                                if (lowScoreTicks != 0 || beepTick != 0) {
                                    lowScoreTicks = 0
                                    beepTick = 0
                                }
                                // Ensure any continuous beep is stopped (safety)
                                if (isBeepPlaying) {
                                    audioPlayer.stopBeepSound()
                                    isBeepPlaying = false
                                }
                            }
                            

                        }
                    } else if (monitoringSource == "AirPods") {
                        // AirPods tracking with tilt angle
                        if (airPodsTracker.isTracking()) {
                            val currentTilt = airPodsTracker.getCurrentTiltAngle()
                            println("🔍 HomeDashboard: AirPods loop running - isCalibrated: $isCalibrated, currentTilt: $currentTilt°")
                            
                            // Calculate score based on tilt angle and calibrated threshold
                            val tiltThreshold = calibratedThresholds?.get("airPodsTilt") ?: 12.0
                            val tiltDeviation = kotlin.math.abs(currentTilt.toDouble())
                            
                            println("📊 HomeDashboard: AirPods tracking - Current tilt: $currentTilt°, Deviation: $tiltDeviation°, Threshold: $tiltThreshold°")
                            
                            // Improved score calculation with smoother transitions
                            currentScore = when {
                                !isCalibrated -> 50 // Show neutral score during calibration
                                tiltDeviation <= tiltThreshold -> 100
                                tiltDeviation <= tiltThreshold + 5.0 -> {
                                    val excess = tiltDeviation - tiltThreshold
                                    (100 - (excess * 3.0)).toInt().coerceAtLeast(80)
                                }
                                tiltDeviation <= tiltThreshold + 10.0 -> {
                                    val excess = tiltDeviation - tiltThreshold - 5.0
                                    (80 - (excess * 4.0)).toInt().coerceAtLeast(60)
                                }
                                tiltDeviation <= tiltThreshold + 20.0 -> {
                                    val excess = tiltDeviation - tiltThreshold - 10.0
                                    (60 - (excess * 2.0)).toInt().coerceAtLeast(30)
                                }
                                else -> {
                                    val excess = tiltDeviation - tiltThreshold - 20.0
                                    (30 - (excess * 1.0)).toInt().coerceAtLeast(0)
                                }
                            }
                            
                            println("📊 HomeDashboard: AirPods score calculated: $currentScore")
                            
                            // Apply smoothing to prevent wild fluctuations
                            smoothedScore = smoothScore(currentScore, smoothedScore)
                            
                            // Update status based on smoothed score
                            postureStatus = when {
                                !isCalibrated -> "CALIBRATING"
                                smoothedScore >= 80 -> "GOOD"
                                smoothedScore >= 60 -> "OK"
                                else -> "BAD"
                            }
                            
                            // Posture alert logic for AirPods (beep every 2s after 5s below threshold)
                            if (smoothedScore < lowScoreThreshold) {
                                if (lowScoreTicks < 25) {
                                    // Accumulate until 5 seconds (25 * 200ms)
                                    lowScoreTicks++

                                } else {
                                    // Sustained bad posture: play a short beep every 2 seconds
                                    beepTick++ // 10 ticks * 200ms = 2s
                                    if (beepTick >= 10) {
                                        audioPlayer.playSound(2) // single beep
                                        beepTick = 0

                                    }
                                }
                            } else {
                                // Score is back above threshold -> reset alert state
                                if (lowScoreTicks != 0 || beepTick != 0) {
                                    lowScoreTicks = 0
                                    beepTick = 0

                                }
                                // Ensure any continuous beep is stopped (safety)
                                if (isBeepPlaying) {
                                    audioPlayer.stopBeepSound()
                                    isBeepPlaying = false
                                }
                            }
                            

                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    // Allow coroutine to cancel when isLiveTracking becomes false
                    throw e
                } catch (e: Exception) {
                    println("❌ Home tracking error: ${e.message}")
                }
                
                delay(200) // Update every 200ms for home dashboard
            }
        }
    }
    
    // Session timer update loop
    LaunchedEffect(isLiveTracking) {
        if (isLiveTracking) {
            // Set start time when tracking begins
            if (sessionStartTime == 0L) {
                sessionStartTime = 1L // Mark as started
            }
            
            // Start PostureDataService if user email is available
            if (userEmail.isNotEmpty()) {
                val source = if (monitoringSource == "AirPods") "airpods" else "phone"
                println("📊 HomeDashboard: Starting PostureDataService with email='$userEmail', source='$source'")
                postureDataService.startRecording(userEmail, source)
            }
            
            while (isLiveTracking) {
                // Pause timer when user not visible
                if (isUserVisible) {
                    sessionDurationSeconds++
                    
                    // Check if we've completed a full minute (60 seconds)
                    if (sessionDurationSeconds % 60 == 0) {
                        val minuteNumber = sessionDurationSeconds / 60
                        println("📊 HomeDashboard: Completed minute $minuteNumber (${sessionDurationSeconds} seconds)")
                        
                        // Get current posture score and record it
                        if (userEmail.isNotEmpty()) {
                            val currentPostureScore = smoothedScore
                            println("📊 HomeDashboard: Recording posture score $currentPostureScore for minute $minuteNumber")
                            postureDataService.addPostureScore(currentPostureScore)
                        }
                    }
                } else {
                    // User is out of frame - timer is paused, don't record data
                    println("⏸️ HomeDashboard: Timer paused - person is not in frame")
                }
                delay(1000) // Update every second
            }
            
            // Stop PostureDataService when tracking ends
            if (userEmail.isNotEmpty()) {
                postureDataService.stopRecording()
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isLiveTracking) pageBgTracking else pageBgDefault)
    ) {
        val hScale = (maxWidth / 390.dp).coerceIn(0.85f, 1.2f)
        val vScale = (maxHeight / 932.dp).coerceIn(0.85f, 1.2f)
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp * hScale, vertical = 16.dp * vScale)
                .widthIn(max = 420.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { selectedSource = null },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
         // Top section with header and tracking status
         Column {
             // Header with improved spacing
             if (!monitoring) {
                 // Add spacing to bring header lower when not tracking
                 Spacer(modifier = Modifier.height(60.dp * vScale))
                 
                 Column(
                     horizontalAlignment = Alignment.CenterHorizontally,
                     modifier = Modifier.padding(horizontal = 24.dp * hScale)
                 ) {
                     Text(
                         "Hello", 
                         color = textPrimary, 
                         fontSize = 48.sp, 
                         fontWeight = FontWeight.ExtraBold
                     )
                     Spacer(modifier = Modifier.height(12.dp * vScale))
                     Text(
                         "Improve your posture by tracking it with", 
                         color = textPrimary, 
                         fontSize = 18.sp, 
                         fontWeight = FontWeight.Normal, 
                         textAlign = TextAlign.Center,
                         lineHeight = 24.sp
                     )
                 }
             } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val sourceLabel = when (monitoringSource) {
                        "AirPods" -> "🎧 AirPods"
                        "Phone" -> "📱 Phone"
                        "Laptop" -> "💻 Laptop"
                        else -> (monitoringSource ?: "Device")
                    }
                    Text("Tracking via", color = textPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(6.dp))
                    Text(sourceLabel, color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    // Status pill
                    val statusColor = when {
                        smoothedScore >= 80 -> Color(0xFF34C759)
                        smoothedScore >= 60 -> Color(0xFFFFA000)
                        else -> Color(0xFFEF4444)
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFE69A), RoundedCornerShape(24.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        val statusLabel = when {
                            !isCalibrated -> "Calibrating"
                            smoothedScore >= 80 -> "Good"
                            smoothedScore >= 60 -> "Okay"
                            else -> "Bad"
                        }
                        Text(statusLabel, color = statusColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp * vScale))


        }

        // Not tracking UI
        if (!monitoring) {
             Spacer(modifier = Modifier.height(32.dp * vScale))

             // Device tracking options with improved responsive spacing
             BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                 val screenWidth = maxWidth
                 val availableWidth = screenWidth - (32.dp * hScale) // Account for horizontal padding
                 val spacing = 16.dp * hScale
                 val cardCount = if (getPlatformName() == "iOS") 3 else 2
                 val cardWidth = ((availableWidth - spacing * (cardCount - 1)) / cardCount).coerceIn(100.dp, 140.dp)
                 val cardHeight = (cardWidth * 1.2f)

                 Row(
                     modifier = Modifier.fillMaxWidth(),
                     horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally)
                 ) {
                     // Phone option
                     DeviceOptionCard(
                         icon = Res.drawable.phone,
                         label = "Phone",
                         selected = selectedSource == "Phone",
                         cardWidth = cardWidth,
                         cardHeight = cardHeight,
                         onClick = {
                             selectedSource = "Phone"
                         }
                     )
                     
                     // Laptop option
                     DeviceOptionCard(
                         icon = Res.drawable.laptop,
                         label = "Laptop",
                         selected = selectedSource == "Laptop",
                         cardWidth = cardWidth,
                         cardHeight = cardHeight,
                         onClick = { 
                             selectedSource = "Laptop"
                         }
                     )
                     
                     // AirPods option - iOS only
                     if (getPlatformName() == "iOS") {
                         DeviceOptionCard(
                             icon = Res.drawable.airpods,
                             label = "AirPods",
                             selected = selectedSource == "AirPods",
                             cardWidth = cardWidth,
                             cardHeight = cardHeight,
                             onClick = { 
                                 selectedSource = "AirPods"
                             }
                         )
                     }
                 }
             }

            Spacer(modifier = Modifier.height(48.dp * vScale))

            // Start tracking button with improved spacing
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Error message above button
                if (showDeviceSelectionError) {
                    Text(
                        text = "Pick the device you want to track with",
                        color = Color(0xFFEF4444),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp * hScale)
                            .padding(bottom = 16.dp * vScale)
                    )
                }
                
                Button(
                    onClick = {
                        if (selectedSource == null) {
                            showDeviceSelectionError = true
                            shouldShake = true
                            scope.launch {
                                delay(3000) // Hide error after 3 seconds
                                showDeviceSelectionError = false
                            }
                        } else {
                            showDeviceSelectionError = false
                            when (selectedSource) {
                                "Phone" -> {
                                    monitoringSource = "Phone"
                                    monitoring = true
                                    isAudioSequencePlaying = true
                                    audioPlayer.playAudioSequence()
                                    scope.launch {
                                        delay(14000)
                                        isAudioSequencePlaying = false
                                    }
                                }
                                "Laptop" -> {
                                    onNavigateToLaptopTracking?.invoke()
                                }
                                "AirPods" -> {
                                    airPodsConnected = airPodsTracker.isConnected()
                                    if (airPodsConnected) {
                                        // Immediately start tracking; iOS will present motion alert if needed
                                        monitoringSource = "AirPods"
                                        monitoring = true
                                        isAudioSequencePlaying = true
                                        audioPlayer.playAudioSequence()
                                        scope.launch {
                                            delay(14000)
                                            isAudioSequencePlaying = false
                                        }
                                    } else {
                                        showAirPodsAlert = true
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp * vScale)
                        .offset(x = shakeOffset.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentBrown),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (selectedSource == "Laptop") "How it Works" else "Start Tracking",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp * vScale))

            // Live Tracking button (temporary - will be removed later) - COMMENTED OUT
            /*
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF007AFF)), // Blue
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                onClick = { onNavigateToLiveTracking?.invoke() }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Live Tracking (Temporary)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
            */
        } else {
            Spacer(modifier = Modifier.height(32.dp * vScale))
            
            // Monitoring state - Hero: Centered image (calibrating or giraffe)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(
                            if (
                                isAudioSequencePlaying ||
                                (monitoring && (monitoringSource == "Phone" || monitoringSource == "AirPods") && !isLiveTracking)
                            ) {
                                Res.drawable.calibrating
                            } else {
                                Res.drawable.giraffeenew
                            }
                        ),
                        contentDescription = if (
                            isAudioSequencePlaying ||
                            (monitoring && (monitoringSource == "Phone" || monitoringSource == "AirPods") && !isLiveTracking)
                        ) "Calibrating" else "Giraffe",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp * vScale))

            // Session time and Score (match layout of mock) - Hidden for laptop tracking and during audio sequence
            if (monitoring && monitoringSource == "Laptop") {
                // Laptop tracking - no session/score cards
            } else if (monitoring && !isAudioSequencePlaying) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    modifier = Modifier.weight(1f).height(140.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0C0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(16.dp), 
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Current Session", color = subText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        val sessionTimeText = if (isLiveTracking && sessionDurationSeconds > 0) {
                            val minutes = sessionDurationSeconds / 60
                            val seconds = sessionDurationSeconds % 60
                            "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
                        } else {
                            "00:00"
                        }
                        Text(sessionTimeText, color = textPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        
                        // Show pause indicator when person is not visible
                        if (isLiveTracking && !isUserVisible) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("⏸️ PAUSED", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                // Score display - live tracking integration
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0C0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when {
                            isLiveTracking && !isUserVisible -> {
                                // User not visible overlay - clear message
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Person is not in frame", color = Color(0xFFEF4444), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(6.dp))
                                    Text("Timer paused - please come back into view", color = subText, fontSize = 12.sp)
                                }
                            }
                            isLiveTracking && smoothedScore > 0 -> {
                                // Show live score
                                val scorePercent = smoothedScore / 100f
                                val scoreColor = when {
                                    smoothedScore >= 80 -> Color(0xFF34C759)
                                    smoothedScore >= 60 -> Color(0xFFFFA000)
                                    else -> Color(0xFFEF4444)
                                }
                                ScoreRingCenter(
                                    percent = scorePercent, 
                                    color = scoreColor, 
                                    primary = textPrimary, 
                                    secondary = subText,
                                    score = smoothedScore,
                                    isLive = true
                                )
                            }
                            monitoring && monitoringSource == "Phone" && !isLiveTracking -> {
                                // Show waiting state during audio sequence
                                ScoreRingCenter(
                                    percent = 0.0f, 
                                    color = Color(0xFFFFA000), 
                                    primary = textPrimary, 
                                    secondary = subText,
                                    score = 0,
                                    isLive = false,
                                    isWaiting = true
                                )
                            }
                            else -> {
                                // Show current score card like mock
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Current Score", color = subText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(6.dp))
                                    Text(smoothedScore.coerceAtLeast(0).toString(), color = textPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp * vScale))
        }
        }
        
        // Spacer before Today's Progress (moved inside conditional to avoid extra gap when hidden)

        // Today's Progress Section - show only when not tracking via Phone/AirPods and not calibrating
        if (!monitoring || (monitoringSource != "Phone" && monitoringSource != "AirPods" && !isAudioSequencePlaying)) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Today's Progress", 
                    color = textPrimary, 
                    fontSize = 22.sp, 
                    fontWeight = FontWeight.ExtraBold
                )
                
                if (isProgressLoading) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF3B82F6),
                        strokeWidth = 2.dp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp * vScale))
            
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f), 
                    title = "Total Time", 
                    value = if (progressData.totalMinutes > 0) {
                        val hours = progressData.totalMinutes / 60
                        val minutes = progressData.totalMinutes % 60
                        if (hours > 0) {
                            "${hours}h${minutes}m"
                        } else {
                            "${minutes}m"
                        }
                    } else "0m", 
                    valueColor = textPrimary
                )
                StatCard(
                    modifier = Modifier.weight(1f), 
                    title = "Average Score", 
                    value = if (progressData.averageScore > 0) "${progressData.averageScore}" else "0", 
                    valueColor = textPrimary
                )
            }
        }
        }
        
        // Stop button below Today's Progress cards
        if (monitoring) {
            Spacer(modifier = Modifier.height(if (isAudioSequencePlaying) 8.dp else 16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF7A4B00), RoundedCornerShape(16.dp))
                    .clickable {
                        // Store session summary before stopping
                        val sessionTimeMinutes = sessionDurationSeconds / 60
                        val sessionTimeSeconds = sessionDurationSeconds % 60
                        val sessionTimeText = if (sessionTimeMinutes > 0) {
                            "${sessionTimeMinutes}m ${sessionTimeSeconds}s"
                        } else {
                            "${sessionTimeSeconds}s"
                        }
                        
                        sessionSummary = SessionSummary(
                            sessionTime = sessionTimeText,
                            totalProgress = if (progressData.totalMinutes > 0) {
                                val hours = progressData.totalMinutes / 60
                                val minutes = progressData.totalMinutes % 60
                                "${hours}h ${minutes}m"
                            } else "0h 0m",
                            averageScore = progressData.averageScore
                        )
                        
                        // Stop monitoring and tracking
                        monitoring = false
                        isLiveTracking = false
                        // Immediately stop any audio flows (sequence, countdown, beep)
                        audioPlayer.stopAllSounds()
                        if (monitoringSource == "Phone") {
                            postureInterface.stopTracking()
                        } else if (monitoringSource == "AirPods") {
                            airPodsTracker.stopTracking()
                        }
                        // Reset session data
                        sessionStartTime = 0L
                        sessionDurationSeconds = 0
                        currentScore = 0
                        smoothedScore = 0
                        isCalibrated = false
                        calibratedThresholds = null
                        monitoringSource = null
                        selectedSource = null
                        
                        // Stop beep sound if playing
                        if (isBeepPlaying) {
                            audioPlayer.stopBeepSound()
                            isBeepPlaying = false
                        }
                        lowScoreTicks = 0
                        beepTick = 0
                        
                        // Stop PostureDataService
                        if (userEmail.isNotEmpty()) {
                            postureDataService.stopRecording()
                        }
                        onTrackingStateChange(false)
                        
                        // Show congratulations dialog
                        showCongratulationsDialog = true
                    }
                    .padding(horizontal = 24.dp * hScale, vertical = 16.dp * vScale),
                contentAlignment = Alignment.Center
            ) {
                Text("STOP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        
        // AirPods connection alert - iOS only
        if (showAirPodsAlert && getPlatformName() == "iOS") {
            AlertDialog(
                onDismissRequest = { showAirPodsAlert = false },
                icon = { Text("🎧", fontSize = 28.sp) },
                title = { 
                    Text(
                        "No compatible devices found",
                        color = Color(0xFF0F1931),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                text = {
                    Column {
                        Text(
                            "Connect one of these to track with AirPods:",
                            color = Color(0xFF0F1931),
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("• AirPods / AirPods Pro / AirPods Max", color = Color(0xFF0F1931), fontSize = 14.sp)
                        Text("• Beats Studio / Beats Solo / Powerbeats", color = Color(0xFF0F1931), fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Make sure Bluetooth is ON and open the case near your phone.",
                            color = Color(0xFF6B7280),
                            fontSize = 12.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showAirPodsAlert = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC233))
                    ) {
                        Text("Got it", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = Color(0xFFFFF0C0)
            )
        }
        
        // Congratulations dialog
        if (showCongratulationsDialog && sessionSummary != null) {
            AlertDialog(
                onDismissRequest = { showCongratulationsDialog = false },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "🎉 Congratulations!",
                            color = textPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .height(4.dp)
                                .width(56.dp)
                                .background(Color(0xFFFFC233), shape = RoundedCornerShape(2.dp))
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            "You tracked your posture for ${sessionSummary!!.sessionTime}.",
                            color = textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Today's progress: ${sessionSummary!!.totalProgress}",
                            color = textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Average score: ${sessionSummary!!.averageScore}",
                            color = Color(0xFF34C759),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showCongratulationsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC233))
                    ) {
                        Text("Great!", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = Color(0xFFFFF0C0)
            )
        }
    }
    }
}
