package com.example.posturelynew

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import posturelynew.composeapp.generated.resources.Res
import posturelynew.composeapp.generated.resources.giffygood
import com.example.posturelynew.AirPodsTracker

// Color definitions at file level
private val pageBg = Color(0xFFFFF9EA)
private val textPrimary = Color(0xFF0F1931)
private val subText = Color(0xFF6B7280)
private val accentGreen = Color(0xFF28A745)
private val accentBlue = Color(0xFF3B82F6)
private val accentOrange = Color(0xFFFFA000)
private val accentRed = Color(0xFFEF4444)

// Helper function to calculate angle from vertical
private fun calculateAngleFromVertical(x1: Float, y1: Float, x2: Float, y2: Float): Double {
    val dx = (x2 - x1).toDouble()
    val dy = (y2 - y1).toDouble()
    val angle = kotlin.math.atan2(dx, dy) * 180.0 / kotlin.math.PI
    return kotlin.math.abs(angle)
}

// Helper function to calculate shoulder tilt
private fun calculateShoulderTilt(leftShoulder: Pair<Float, Float>, rightShoulder: Pair<Float, Float>): Double {
    val heightDiff = (leftShoulder.second - rightShoulder.second).toDouble()
    return kotlin.math.abs(heightDiff) * 100.0 // Scale for better visibility
}

// Calculate real metrics from MediaPipe landmarks (like HTML implementation)
private fun calculateRealMetrics(landmarks: List<Pair<Float, Float>>, noseToShoulderRatio: Double = 0.0, shoulderWidth: Double = 0.0): PoseMetrics {
    if (landmarks.size < 33) return PoseMetrics(0.0, 0.0, 0.0, 0.0, 0.0)
    
    // Extract key landmarks (using MediaPipe pose landmark indices)
    val nose = landmarks[0]        // Landmark 0: Nose
    val leftShoulder = landmarks[11]  // Landmark 11: Left Shoulder
    val rightShoulder = landmarks[12] // Landmark 12: Right Shoulder
    val leftEar = landmarks[3]     // Landmark 3: Left Ear
    val rightEar = landmarks[4]    // Landmark 4: Right Ear
    val leftHip = landmarks[23]    // Landmark 23: Left Hip
    val rightHip = landmarks[24]   // Landmark 24: Right Hip
    
    // Calculate torso tilt (angle from vertical)
    val torsoCenterX = (leftHip.first + rightHip.first) / 2
    val torsoCenterY = (leftHip.second + rightHip.second) / 2
    val shoulderCenterX = (leftShoulder.first + rightShoulder.first) / 2
    val shoulderCenterY = (leftShoulder.second + rightShoulder.second) / 2
    
    val torsoTilt = calculateAngleFromVertical(
        shoulderCenterX, shoulderCenterY,
        torsoCenterX, torsoCenterY
    )
    
    // Calculate shoulder tilt (left vs right shoulder height difference)
    val shoulderTilt = calculateShoulderTilt(leftShoulder, rightShoulder)
    
    // Calculate neck flexion (head forward position)
    val headZDelta = (nose.second - shoulderCenterY).toDouble()
    
    // Calculate neck flexion angle
    val neckFlex = calculateAngleFromVertical(
        nose.first, nose.second,
        shoulderCenterX, shoulderCenterY
    )
    
    // Shoulder symmetry (height difference)
    val shoulderAsymY = kotlin.math.abs((leftShoulder.second - rightShoulder.second).toDouble())
    
    return PoseMetrics(
        torsoTilt = torsoTilt,
        shoulderTilt = shoulderTilt,
        neckFlex = neckFlex,
        headZDelta = headZDelta,
        shoulderAsymY = shoulderAsymY
    )
}

// Function to parse real pose data and extract metrics (like in calibration screen)
private fun parsePoseData(data: String, landmarks: List<Pair<Float, Float>>): PoseMetrics? {
    return try {
        // If we have landmarks, calculate metrics directly from them
        if (landmarks.size >= 33) {
            val metrics = calculateRealMetrics(landmarks)
            metrics
        } else {
            // Fallback to parsing text data if landmarks aren't available
            val lines = data.split("\n")
            var noseToShoulderRatio = 0.0
            
            for (line in lines) {
                when {
                    line.contains("Nose to Shoulder Center:") -> {
                        val ratioStr = line.split(":").lastOrNull()?.trim()
                        noseToShoulderRatio = ratioStr?.toDoubleOrNull() ?: 0.0
                    }
                }
            }
            
            if (noseToShoulderRatio > 0) {
                // Calculate metrics using the ratio from text data
                val metrics = calculateRealMetrics(landmarks, noseToShoulderRatio, 1.0)
                metrics
            } else {
                null
            }
        }
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LiveTrackingScreen(
    onBackPressed: () -> Unit,
    isAirPodsTracking: Boolean = false,
    userEmail: String = ""
) {
    // Get stored email if not provided
    val storage = remember { PlatformStorage() }
    val effectiveUserEmail = remember(userEmail) {
        if (userEmail.isNotEmpty()) userEmail else storage.getString("userEmail", "")
    }
    var isTracking by remember { mutableStateOf(false) }
    var currentScore by remember { mutableStateOf(0) }
    var postureStatus by remember { mutableStateOf("NO SIGNAL") }
    var showCalibration by remember { mutableStateOf(false) }
    var isCalibrated by remember { mutableStateOf(false) }
    
    // Posture metrics from real MediaPipe data
    var torsoTilt by remember { mutableStateOf(0.0) }
    var shoulderTilt by remember { mutableStateOf(0.0) }
    var neckFlex by remember { mutableStateOf(0.0) }
    var headZDelta by remember { mutableStateOf(0.0) }
    var shoulderAsymY by remember { mutableStateOf(0.0) }
    var fps by remember { mutableStateOf(0) }
    
    // Calibration thresholds - start with defaults, will be personalized
    var calibratedThresholds by remember { mutableStateOf<Map<String, Double>?>(null) }
    
    // Real pose data from MediaPipe
    var poseData by remember { mutableStateOf("") }
    var landmarkCount by remember { mutableStateOf(0) }
    var landmarks by remember { mutableStateOf(List(33) { Pair(0.5f, 0.5f) }) }
    
    // Score smoothing mechanism
    var scoreHistory by remember { mutableStateOf(listOf<Int>()) }
    var smoothedScore by remember { mutableStateOf(0) }
    var updateCounter by remember { mutableStateOf(0) }
    
    // Visibility detection for phone tracking
    var isUserVisible by remember { mutableStateOf(true) }
    
    // AirPods tracking
    val airPodsTracker = remember { AirPodsTracker() }
    var airPodsTiltAngle by remember { mutableStateOf(0f) }
    
    // Posture data recording service
    val postureDataService = remember { PostureDataService() }
    val isRecording by postureDataService.isRecordingFlow.collectAsState()
    val lastRecordedScore by postureDataService.lastRecordedScore.collectAsState()
    val lastRecordedTime by postureDataService.lastRecordedTime.collectAsState()
    
    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "tracking")
    val pulseAnimation by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val postureInterface = rememberPostureTrackingInterface()
    
    // Function to smooth score changes
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
    
    // Start tracking automatically when screen loads (Live tracking from phone)
    LaunchedEffect(Unit) {
        delay(500)
        // Start tracking immediately when screen loads
        println("🔍 LiveTracking: Starting tracking, isAirPodsTracking=$isAirPodsTracking")
        if (isAirPodsTracking) {
            airPodsTracker.startTracking()
        } else {
            postureInterface.startTracking()
        }
        isTracking = true
        println("🔍 LiveTracking: isTracking set to true")
        
        // Start posture data recording (only for phone tracking, not desktop)
        println("🔍 PostureDataService: effectiveUserEmail='$effectiveUserEmail', isNotEmpty=${effectiveUserEmail.isNotEmpty()}")
        if (effectiveUserEmail.isNotEmpty()) {
            val source = if (isAirPodsTracking) "airpods" else "phone"
            println("📊 PostureDataService: Starting recording with email='$effectiveUserEmail', source='$source'")
            postureDataService.startRecording(effectiveUserEmail, source)
        } else {
            println("⚠️ PostureDataService: No user email provided, skipping recording")
        }
    }
    
    // Update pose data and calculate score in real-time
    LaunchedEffect(isTracking) {
        if (isTracking) {
            println("🔍 LiveTracking: Starting tracking loop")
            while (true) {
                if (isAirPodsTracking) {
                    // AirPods-based tracking path: use head pitch as neck flex proxy
                    airPodsTiltAngle = airPodsTracker.getCurrentTiltAngle()
                    val neckFlexAngle = kotlin.math.abs(airPodsTiltAngle.toDouble())
                    neckFlex = neckFlexAngle
                    
                    println("🔍 LiveTracking(AirPods): Tilt: ${airPodsTiltAngle}°, NeckFlex: ${neckFlexAngle}°")
                    
                    val scoreResult = calculateAirPodsScore(neckFlexAngle, calibratedThresholds)
                    currentScore = scoreResult.score
                    smoothedScore = smoothScore(currentScore)
                    postureStatus = when {
                        smoothedScore >= 80 -> "GOOD"
                        smoothedScore >= 60 -> "OK"
                        else -> "BAD"
                    }
                    println("🔍 LiveTracking(AirPods): Score: $currentScore, Smoothed: $smoothedScore, Status: $postureStatus")
                    postureDataService.addPostureScore(smoothedScore)
                    updateCounter++
                } else {
                    val newPoseData = postureInterface.getPoseData()
                    poseData = newPoseData
                    
                    // Get real landmark data
                    landmarkCount = postureInterface.getLandmarkCount()
                    println("🔍 LiveTracking: landmarkCount=$landmarkCount")
                    
                    // Check visibility for phone tracking
                    val wasVisible = isUserVisible
                    isUserVisible = landmarkCount >= 4 // User is visible if we have at least 4 landmarks
                    
                    // Log visibility changes
                    if (wasVisible != isUserVisible) {
                        if (isUserVisible) {
                            println("✅ LiveTracking: Person is back in frame (landmarks: $landmarkCount)")
                        } else {
                            println("❌ LiveTracking: Person is not in frame (landmarks: $landmarkCount)")
                        }
                    }
                    
                    if (landmarkCount > 0) {
                        val newLandmarks = mutableListOf<Pair<Float, Float>>()
                        for (i in 0 until minOf(landmarkCount, 33)) {
                            val x = postureInterface.getLandmarkX(i)
                            val y = postureInterface.getLandmarkY(i)
                            newLandmarks.add(Pair(x, y))
                        }
                        landmarks = newLandmarks
                        
                        // Calculate real metrics from landmarks for live tracking
                        val metrics = calculateRealMetrics(landmarks)
                        torsoTilt = metrics.torsoTilt
                        shoulderTilt = metrics.shoulderTilt
                        neckFlex = metrics.neckFlex
                        headZDelta = metrics.headZDelta
                        shoulderAsymY = metrics.shoulderAsymY
                        
                        // Calculate real-time score with live updates
                        val scoreResult = calculatePostureScore(metrics, calibratedThresholds)
                        currentScore = scoreResult.score
                        
                        // Apply smoothing to prevent wild fluctuations
                        smoothedScore = smoothScore(currentScore)
                        
                        // Update status based on smoothed score
                        postureStatus = when {
                            smoothedScore >= 80 -> "GOOD"
                            smoothedScore >= 60 -> "OK"
                            else -> "BAD"
                        }
                        
                        // Add score to posture data recording service
                        println("🔍 LiveTracking: Adding score $smoothedScore to PostureDataService")
                        postureDataService.addPostureScore(smoothedScore)
                    }
                }
                
                // Update FPS
                fps = (1000 / 100).toInt() // 100ms delay = 10 FPS
                updateCounter++ // Increment counter for smoothing
                delay(100) // Update every 100ms like HTML implementation
            }
        }
    }
    
    // Stop tracking when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            if (isAirPodsTracking) {
                airPodsTracker.stopTracking()
            } else {
                postureInterface.stopTracking()
            }
            isTracking = false
            
            // Stop posture data recording
            postureDataService.stopRecording()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        // Add safe space above header for status bar
        Spacer(modifier = Modifier.height(48.dp))
        
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onBackPressed,
                colors = ButtonDefaults.textButtonColors(contentColor = textPrimary)
            ) {
                Text("← Back", fontSize = 16.sp)
            }
            
            // Status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            when (postureStatus) {
                                "GOOD" -> accentGreen
                                "OK" -> accentOrange
                                else -> accentRed
                            },
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = postureStatus,
                    color = textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Title
        Text(
            text = "Live Tracking",
            color = textPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // AirPods tracking indicator
        if (isAirPodsTracking) {
            val deviceName = airPodsTracker.getConnectedDeviceName()
            Text(
                text = "Tracking with ${deviceName ?: "AirPods"}",
                color = accentBlue,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Current tilt angle display
            Text(
                text = "Current Tilt: ${(airPodsTiltAngle * 10).toInt() / 10.0}°",
                color = textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        // Instruction
        Text(
            text = if (isAirPodsTracking) 
                "Keep your head level while wearing AirPods." 
            else 
                "Place your phone on a stand or stable surface.",
            color = subText,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 48.dp)
        )
        
        // Central Content - Live Score Circle with Giraffe
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Live Score Circle Progress
            Canvas(
                modifier = Modifier.size(280.dp)
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2 - 20f
                
                // Background circle (light gray)
                drawCircle(
                    color = Color(0xFFE5E7EB),
                    radius = radius,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 16.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
                
                // Progress arc based on live score
                if (smoothedScore > 0) {
                    val sweepAngle = (smoothedScore / 100f) * 360f
                    val progressColor = when {
                        smoothedScore >= 80 -> accentGreen
                        smoothedScore >= 60 -> accentOrange  
                        else -> accentRed
                    }
                    
                    drawArc(
                        color = progressColor,
                        startAngle = -90f, // Start from top
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 16.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                }
            }
            
            // Giraffe centered within the progress circle
            Image(
                painter = painterResource(Res.drawable.giffygood),
                contentDescription = "Giraffe illustration",
                modifier = Modifier.size(200.dp),
                contentScale = ContentScale.Fit
            )
            
            // Live score text overlay
            if (smoothedScore > 0) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.9f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = smoothedScore.toString(),
                            color = textPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "LIVE",
                            color = when {
                                smoothedScore >= 80 -> accentGreen
                                smoothedScore >= 60 -> accentOrange  
                                else -> accentRed
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Posture Status and Score
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when {
                    !isAirPodsTracking && !isUserVisible -> "Person is not in frame"
                    postureStatus == "GOOD" -> "Good Posture"
                    postureStatus == "OK" -> "Fair Posture"
                    else -> "Poor Posture"
                },
                color = when {
                    !isAirPodsTracking && !isUserVisible -> accentRed
                    postureStatus == "GOOD" -> accentGreen
                    postureStatus == "OK" -> accentOrange
                    else -> accentRed
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = when {
                    !isAirPodsTracking && !isUserVisible -> "—"
                    smoothedScore > 0 -> smoothedScore.toString()
                    else -> "—"
                },
                color = textPrimary,
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold
            )
            
            // Show smoothing indicator
            if (smoothedScore > 0 && currentScore != smoothedScore) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Raw: $currentScore (Smoothed)",
                    color = subText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Calibrate Button - exactly like HTML implementation
        if (isTracking && smoothedScore > 0) {
            Button(
                onClick = {
                    // Apply current posture as GOOD baseline (like HTML calibrate)
                    calibratedThresholds = mapOf(
                        "torsoTilt" to (torsoTilt + 2.0), // Add margin like HTML
                        "shoulderTilt" to (shoulderTilt + 2.0),
                        "neckFlex" to (neckFlex + 2.0),
                        "headZDelta" to (headZDelta - 0.02), // Stricter for forward head
                        "shoulderAsymY" to (shoulderAsymY + 0.01)
                    )
                    isCalibrated = true
                    showCalibration = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentBlue)
            ) {
                Text(
                    "🎯 Calibrate (Current = GOOD)",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Action Buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            
            // Live tracking status indicator
            if (isTracking) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                when {
                                    !isAirPodsTracking && !isUserVisible -> accentRed
                                    else -> accentGreen
                                }, 
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        when {
                            !isAirPodsTracking && !isUserVisible -> "Person not in frame"
                            else -> "Live Tracking Active"
                        },
                        color = when {
                            !isAirPodsTracking && !isUserVisible -> accentRed
                            else -> accentGreen
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            // Posture data recording status
            if (isRecording) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(accentBlue, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Recording to Cloud",
                        color = accentBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Show last recorded data
                if (lastRecordedScore > 0) {
                    Text(
                        "Last recorded: $lastRecordedScore at $lastRecordedTime",
                        color = subText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
            
            // Start/Stop Tracking Button
            Button(
                onClick = { 
                    if (isTracking) {
                        if (isAirPodsTracking) {
                            airPodsTracker.stopTracking()
                        } else {
                            postureInterface.stopTracking()
                        }
                        isTracking = false
                        // Reset scores when stopping
                        smoothedScore = 0
                        currentScore = 0
                    } else {
                        if (isAirPodsTracking) {
                            airPodsTracker.startTracking()
                        } else {
                            postureInterface.startTracking()
                        }
                        isTracking = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTracking) accentRed else accentGreen
                )
            ) {
                Text(
                    if (isTracking) "⏹ Stop Live Tracking" else "▶ Resume Live Tracking",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Footer Hints
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "For best results, sit in a well-lit area.",
                    color = subText,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (isTracking && smoothedScore > 0 && !isAirPodsTracking) {
                    Text(
                        "Score updates in real-time • Today's progress: 82% (hardcoded)",
                        color = subText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// Data classes for pose metrics are now in shared PoseMetrics.kt

// Calculate posture score based on metrics
private fun calculatePostureScore(
    metrics: PoseMetrics,
    calibratedThresholds: Map<String, Double>?
): ScoreResult {
    // Use calibrated thresholds if available, otherwise use defaults
    val thresholds = calibratedThresholds ?: mapOf(
        "torsoTilt" to 10.0,
        "shoulderTilt" to 7.0,
        // Make front slouch more sensitive: lower neck flex threshold slightly
        "neckFlex" to 12.0,
        // Make forward-head more sensitive: raise threshold (less negative)
        "headZDelta" to -0.05,
        "shoulderAsymY" to 0.03
    )
    
    var score = 100
    val flags = mutableListOf<String>()
    
    // Apply penalties based on thresholds
    if (metrics.torsoTilt > thresholds["torsoTilt"]!!) {
        val penalty = ((metrics.torsoTilt - thresholds["torsoTilt"]!!) / 20.0 * 25).coerceAtMost(25.0)
        score -= penalty.toInt()
        flags.add("Torso tilt: ${metrics.torsoTilt.toString()}")
    }
    
    if (metrics.shoulderTilt > thresholds["shoulderTilt"]!!) {
        // Side slouch slightly less weighted than front slouch
        val penalty = ((metrics.shoulderTilt - thresholds["shoulderTilt"]!!) / 20.0 * 15).coerceAtMost(15.0)
        score -= penalty.toInt()
        flags.add("Shoulder tilt: ${metrics.shoulderTilt.toString()}")
    }
    
    if (metrics.neckFlex > thresholds["neckFlex"]!!) {
        // Increase penalty and slope for forward flexion
        val penalty = ((metrics.neckFlex - thresholds["neckFlex"]!!) / 20.0 * 35).coerceAtMost(35.0)
        score -= penalty.toInt()
        flags.add("Neck flexion: ${metrics.neckFlex.toString()}")
    }
    
    if (metrics.headZDelta < thresholds["headZDelta"]!!) {
        // Heavier and more sensitive forward-head penalty
        val penalty = ((thresholds["headZDelta"]!! - metrics.headZDelta) / 0.10 * 45).coerceAtMost(45.0)
        score -= penalty.toInt()
        flags.add("Forward head: ${metrics.headZDelta.toString()}")
    }
    
    if (metrics.shoulderAsymY > thresholds["shoulderAsymY"]!!) {
        val penalty = ((metrics.shoulderAsymY - thresholds["shoulderAsymY"]!!) / 0.07 * 15).coerceAtMost(15.0)
        score -= penalty.toInt()
        flags.add("Shoulder asymmetry: ${metrics.shoulderAsymY.toString()}")
    }
    
    score = score.coerceIn(0, 100)
    
    val status = when {
        score >= 85 -> "GOOD"
        score >= 70 -> "OK"
        else -> "BAD"
    }
    
    return ScoreResult(score, status)
} 

// Calculate posture score from AirPods pitch-only input (neck flex proxy)
private fun calculateAirPodsScore(
    neckFlexAngleDeg: Double,
    calibratedThresholds: Map<String, Double>?
): ScoreResult {
    val tiltThreshold = calibratedThresholds?.get("airPodsTilt") ?: 12.0
    val tiltDeviation = kotlin.math.abs(neckFlexAngleDeg)
    
    println("🔍 [DEBUG] calculateAirPodsScore: tiltDeviation=$tiltDeviation°, threshold=$tiltThreshold°")
    
    // Use the same improved scoring algorithm as HomeDashboardPage
    val score = when {
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
    
    val status = when {
        score >= 80 -> "GOOD"
        score >= 60 -> "OK"
        else -> "BAD"
    }
    
    println("🔍 [DEBUG] calculateAirPodsScore: score=$score, status=$status")
    
    return ScoreResult(score, status)
}