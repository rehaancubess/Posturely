package com.example.posturelynew

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
import androidx.compose.foundation.Image
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import posturelynew.composeapp.generated.resources.Res
import posturelynew.composeapp.generated.resources.giraffeenew
import com.example.posturelynew.StatusRingLarge
import com.example.posturelynew.audio.AudioPlayer
import kotlinx.coroutines.delay

// Camera service used by desktop tracking
import com.example.posturelynew.DesktopCameraService
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

@OptIn(ExperimentalResourceApi::class)
@Composable
fun DesktopMainScreen(
    onNavigateToLiveTracking: () -> Unit,
    onLogout: () -> Unit,
    onStartTracking: () -> Unit
) {
    val pageBg = Color(0xFFFED867)
    val cardBg = Color(0xFFFFF0C0)
    val textPrimary = Color(0xFF0F1931)
    val accentBrown = Color(0xFFD69C2F)

    // --- Tracking state (runs in-place on the homepage) ---
    var isTracking by remember { mutableStateOf(false) }
    var sessionDurationSeconds by remember { mutableStateOf(0) }
    var postureRatio by remember { mutableStateOf(0.0) }
    var lastPostureRatio by remember { mutableStateOf(0.0) }
    var calibratedRatio by remember { mutableStateOf(0.0) }
    var isRatioCalibrated by remember { mutableStateOf(false) }
    var ratioStuckStartTime by remember { mutableStateOf(0L) }
    var isRatioStuck by remember { mutableStateOf(false) }
    var ratioChangeStreak by remember { mutableStateOf(0) }
    var isUserVisible by remember { mutableStateOf(true) }
    var currentScore by remember { mutableStateOf(0) }
    var smoothedScore by remember { mutableStateOf(0) }
    var updateCounter by remember { mutableStateOf(0) }
    var countdownValue by remember { mutableStateOf(0) }
    var showCountdown by remember { mutableStateOf(false) }
    var isAudioSequencePlaying by remember { mutableStateOf(false) }
    var hasSessionStarted by remember { mutableStateOf(false) }
    var showCongratulationsDialog by remember { mutableStateOf(false) }
    var lowScoreTicks by remember { mutableStateOf(0) }
    var beepTick by remember { mutableStateOf(0) } // 10 * 200ms = 2s

    val audioPlayer = remember { AudioPlayer() }
    val cameraService = remember { DesktopCameraService() }
    val scope = rememberCoroutineScope()
    var audioSeqJob: Job? by remember { mutableStateOf<Job?>(null) }

    // Toggle tracking with auto-select camera and audio/calibration (same as live screen)
    fun toggleTracking() {
        if (!isTracking) {
            // START
            isTracking = true
            audioSeqJob = scope.launch {
                // Ensure a clean camera start
                if (!cameraService.isRunning()) {
                    cameraService.stopCamera()
                    delay(200)
                    cameraService.selectCameraIndex(0)
                    delay(100)
                    cameraService.startCamera { /* no UI preview on homepage */ }
                    delay(500)
                }

                // Audio sequence → countdown
                isAudioSequencePlaying = true
                // Play sitstraight immediately, then wait ~11s and play countdown
                println("🎵 DesktopMain: Playing sitstraight.mp3")
                audioPlayer.playSound(0)
                delay(11000)
                println("🎵 DesktopMain: Playing countdown.mp3")
                audioPlayer.playSound(1)
                showCountdown = true
                for (i in 3 downTo 1) { countdownValue = i; delay(1000) }
                showCountdown = false
                isAudioSequencePlaying = false

                // Begin tracking only after audio
                cameraService.startTracking()
                // Allow time for first frames, then calibrate and mark session started
                delay(2000)
                if (postureRatio > 0 && !postureRatio.isNaN()) {
                    calibratedRatio = postureRatio
                    isRatioCalibrated = true
                } else {
                    // fallback baseline if first frames invalid
                    calibratedRatio = 1.0
                    isRatioCalibrated = true
                }
                hasSessionStarted = true
            }
        } else {
            cameraService.stopTracking()
            cameraService.stopCamera()
            // Cancel any running audio/countdown job
            audioSeqJob?.cancel()
            audioSeqJob = null
            isAudioSequencePlaying = false
            showCountdown = false
            countdownValue = 0
            if (hasSessionStarted) {
                showCongratulationsDialog = true
            }
            hasSessionStarted = false
            isRatioCalibrated = false
            // Fully reset session and tracking state so a new Start behaves cleanly
            sessionDurationSeconds = 0
            currentScore = 0
            smoothedScore = 0
            lowScoreTicks = 0
            beepTick = 0
            postureRatio = 0.0
            lastPostureRatio = 0.0
            isUserVisible = true
            ratioStuckStartTime = 0L
            isRatioStuck = false
            ratioChangeStreak = 0
            // Flip UI state back to not tracking so button text updates
            isTracking = false
            // Force ring to neutral
            isRatioCalibrated = false
            calibratedRatio = 0.0
        }
    }

    // Session timer
    LaunchedEffect(isTracking) {
        while (isTracking) {
            if (isUserVisible && hasSessionStarted) {
                sessionDurationSeconds += 1
            }
            delay(1000)
        }
    }

    // Live update loop similar to live screen but lightweight
    LaunchedEffect(isTracking) {
        if (isTracking) {
            while (true) {
                val landmarkCount = cameraService.getLandmarkCount()
                val newLandmarks = mutableListOf<Pair<Float, Float>>()
                for (i in 0 until kotlin.math.min(landmarkCount, 33)) {
                    val x = cameraService.getLandmarkX(i)
                    val y = cameraService.getLandmarkY(i)
                    newLandmarks.add(Pair(x, y))
                }

                var newRatioComputed = false
                if (landmarkCount >= 33 && newLandmarks.size >= 33) {
                    val data = calculatePostureRatioHome(newLandmarks)
                    if (data != null) {
                        postureRatio = data.ratio
                        newRatioComputed = true
                    }
                }

                val now = System.currentTimeMillis()
                val ratioDiff = kotlin.math.abs(postureRatio - lastPostureRatio)
                if (newRatioComputed) {
                    if (ratioDiff < 0.002) {
                        if (ratioStuckStartTime == 0L) ratioStuckStartTime = now else if (now - ratioStuckStartTime > 2000 && !isRatioStuck) isRatioStuck = true
                        ratioChangeStreak = 0
                    } else {
                        ratioChangeStreak = (ratioChangeStreak + 1).coerceAtMost(10)
                        if (ratioChangeStreak >= 3) {
                            ratioStuckStartTime = 0L
                            isRatioStuck = false
                            lastPostureRatio = postureRatio
                            ratioChangeStreak = 0
                        }
                    }
                }

                // Consider person visible if we detect enough landmarks
                isUserVisible = landmarkCount >= 4

                // If tracking is running, we already set baseline; keep it stable

                // Score from ratio (same mapping as live screen)
                currentScore = if (hasSessionStarted && isRatioCalibrated && calibratedRatio > 0) {
                    calculateRatioScoreHome(postureRatio, calibratedRatio)
                } else {
                    0
                }

                // Smoothing
                if (hasSessionStarted) {
                    updateCounter++
                    if (updateCounter % 3 == 0) {
                        val weight = 0.4
                        smoothedScore = (((currentScore * weight) + (smoothedScore * (1 - weight))).toInt()).coerceIn(0, 100)
                    }
                } else {
                    smoothedScore = 0
                }

                // Bad posture alert logic: after 5s below threshold, play a short beep every 2s
                val lowScoreThreshold = 80
                if (hasSessionStarted && isUserVisible && smoothedScore in 1..(lowScoreThreshold - 1)) {
                    if (lowScoreTicks < 25) {
                        lowScoreTicks++ // accumulate 5s (25 * 200ms)
                    } else {
                        beepTick++ // every 2s
                        if (beepTick >= 10) {
                            audioPlayer.playSound(2) // beep.mp3
                            beepTick = 0
                        }
                    }
                } else {
                    if (lowScoreTicks != 0 || beepTick != 0) {
                        lowScoreTicks = 0
                        beepTick = 0
                    }
                }

                delay(100)
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .padding(40.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Posturely", color = textPrimary, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onLogout, colors = ButtonDefaults.textButtonColors(contentColor = textPrimary)) {
                    Text("Logout", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(16.dp))

            // 2x2 responsive grid that fills available space
            Row(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left column (top-left and bottom-left cards)
                Column(
                modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top-left: Giraffe illustration
                    Card(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Image(painter = painterResource(Res.drawable.giraffeenew), contentDescription = "Giraffe")
                        }
                    }

                    // Bottom-left: Current Session time (pause when out of frame)
                    Card(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Current Session", color = textPrimary.copy(alpha = 0.8f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            val minutes = sessionDurationSeconds / 60
                            val seconds = sessionDurationSeconds % 60
                            val sessionText = String.format("%02d:%02d", minutes, seconds)
                            Text(sessionText, color = textPrimary, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                            if (isTracking && !isUserVisible) {
                                Spacer(Modifier.height(6.dp))
                                Text("PAUSED", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Right column (top-right and bottom-right cards)
    Column(
        modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top-right: Status ring (live)
                    Card(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            val label = if (!isUserVisible || !hasSessionStarted) "—" else when {
                                smoothedScore >= 80 -> "GOOD"
                                smoothedScore >= 60 -> "OK"
                                else -> "BAD"
                            }
                            val ringColor = if (!isUserVisible || !hasSessionStarted) Color(0xFFFFA000) else when {
                                smoothedScore >= 80 -> Color(0xFF34C759)
                                smoothedScore >= 60 -> Color(0xFFFFA000)
                                else -> Color(0xFFEF4444)
                            }
                            StatusRingLarge(
                                percent = (if (!isUserVisible || !hasSessionStarted) 0f else smoothedScore / 100f).coerceIn(0f, 1f),
                                label = label,
                                ringColor = ringColor,
                                textColor = textPrimary
                            )
                        }
                    }

                    // Bottom-right: Start tracking button (moved here)
                    Card(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Button(
                                onClick = { toggleTracking() },
                                modifier = Modifier.fillMaxWidth(0.7f).height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentBrown)
                            ) {
                                Text(if (isTracking) "Stop tracking" else "Start tracking", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // End-of-session popup
            if (showCongratulationsDialog) {
                AlertDialog(
                    onDismissRequest = { showCongratulationsDialog = false },
                    title = { Text("🎉 Session complete", color = textPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold) },
                    text = {
                        val minutes = sessionDurationSeconds / 60
                        val seconds = sessionDurationSeconds % 60
                        Column {
                            Text("You tracked for ${minutes}m ${seconds}s", color = textPrimary)
                            Spacer(Modifier.height(8.dp))
                            Text("Average score: $smoothedScore", color = Color(0xFF34C759), fontWeight = FontWeight.Bold)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showCongratulationsDialog = false }) { Text("Great!", color = textPrimary, fontWeight = FontWeight.Bold) }
                    }
                )
            }
        }
    }
}

// Lightweight helpers (duplicated to avoid coupling)
private data class RatioData(val ratio: Double)
private fun calculatePostureRatioHome(landmarks: List<Pair<Float, Float>>): RatioData? {
    return try {
        if (landmarks.size < 33) return null
        val nose = landmarks[0]
        val leftEye = landmarks[2]
        val rightEye = landmarks[5]
        val leftShoulder = landmarks[11]
        val rightShoulder = landmarks[12]
        val shoulderWidth = kotlin.math.sqrt(((leftShoulder.first - rightShoulder.first).toDouble().let { it * it } + (leftShoulder.second - rightShoulder.second).toDouble().let { it * it }))
        val faceCenterY = (leftEye.second + rightEye.second + nose.second) / 3.0
        val shoulderCenterY = (leftShoulder.second + rightShoulder.second) / 2.0
        val faceToShoulder = kotlin.math.abs(faceCenterY - shoulderCenterY)
        val ratio = faceToShoulder / shoulderWidth
        RatioData(ratio)
    } catch (_: Exception) { null }
}

private fun calculateRatioScoreHome(currentRatio: Double, calibratedRatio: Double): Int {
    if (calibratedRatio <= 0 || currentRatio <= 0 || calibratedRatio.isNaN() || currentRatio.isNaN()) return 50
    val drop = calibratedRatio - currentRatio
    if (drop <= 0) return 100
    val score = 100.0 - drop * 200.0
    return score.toInt().coerceIn(0, 100)
}

// Removed phone-like bottom bar and device tiles per request
