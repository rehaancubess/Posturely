package com.example.posturelynew

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import posturelynew.composeapp.generated.resources.Res
import posturelynew.composeapp.generated.resources.front
import kotlin.math.abs
import kotlin.math.round
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PostureCalibrationActiveScreen(
    onNavigateBack: () -> Unit,
    onComplete: () -> Unit
) {
    var isMonitoring by remember { mutableStateOf(false) }
    var isCalibrated by remember { mutableStateOf(false) }
    var normalPostureRatio by remember { mutableStateOf(0f) }
    var currentRatio by remember { mutableStateOf(0f) }
    var imageRotation by remember { mutableStateOf(0f) }
    var sessionStartTime by remember { mutableStateOf(0L) }
    var sessionDuration by remember { mutableStateOf(0L) }
    
    // Time-based threshold variables
    var poorPostureSince: TimeMark? by remember { mutableStateOf<TimeMark?>(null) }
    var goodPostureSince: TimeMark? by remember { mutableStateOf<TimeMark?>(null) }
    var isInPoorPosture by remember { mutableStateOf(false) }
    
    // Get pose data from the tracking interface
    val poseData = remember { mutableStateOf("") }
    val postureInterface = rememberPostureTrackingInterface()
    
    // Function to parse pose data and extract ratio
    fun parsePoseData(data: String): Float {
        return try {
            val lines = data.split("\n")
            for (line in lines) {
                if (line.contains("Nose to Shoulder Center:")) {
                    val ratioStr = line.split(":").lastOrNull()?.trim()
                    return ratioStr?.toFloatOrNull() ?: 0f
                }
            }
            0f
        } catch (e: Exception) {
            0f
        }
    }
    
    LaunchedEffect(Unit) {
        // Start monitoring when the screen loads
        isMonitoring = true
        sessionStartTime = TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds
        // Start the pose tracking
        postureInterface.startTracking()
    }
    
    // Update pose data and session time periodically
    LaunchedEffect(isMonitoring) {
        while (isMonitoring) {
            poseData.value = postureInterface.getPoseData()
            currentRatio = parsePoseData(poseData.value)
            sessionDuration = TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds - sessionStartTime
            kotlinx.coroutines.delay(100) // Update every 100ms
        }
    }
    
    // Stop tracking when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            postureInterface.stopTracking()
        }
    }
    
    // Calculate image rotation based on posture changes with time threshold
    LaunchedEffect(currentRatio, normalPostureRatio) {
        if (isCalibrated && normalPostureRatio > 0) {
            val ratioDifference = normalPostureRatio - currentRatio
            // Use monotonic time to avoid deprecation and clock issues across platforms
            
            if (ratioDifference >= 0.05) {
                // Poor posture detected
                if (!isInPoorPosture) {
                    // Just entered poor posture
                    poorPostureSince = TimeSource.Monotonic.markNow()
                    isInPoorPosture = true
                } else {
                    // Check if we've been in poor posture for 1 second
                    if (poorPostureSince?.elapsedNow() ?: 0.seconds >= 1.seconds) {
                        // Rotate image based on how much the posture has deteriorated
                        imageRotation = (ratioDifference * 100).coerceAtMost(45f) // Max 45 degrees
                    }
                }
                // Reset good posture timer
                goodPostureSince = null
            } else {
                // Good posture detected
                if (isInPoorPosture) {
                    // Just returned to good posture
                    goodPostureSince = TimeSource.Monotonic.markNow()
                    isInPoorPosture = false
                } else {
                    // Check if we've been in good posture for 1 second
                    if (goodPostureSince?.elapsedNow() ?: 0.seconds >= 1.seconds) {
                        // Return image to normal
                        imageRotation = 0f
                        goodPostureSince = null // Reset timer
                    }
                }
                // Reset poor posture timer
                poorPostureSince = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Front Posture Calibration",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Session time display - made bigger and prominent
        if (isMonitoring) {
            Text(
                text = formatSessionTime(sessionDuration),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
        
        // Status text
        Text(
            text = if (!isCalibrated) "Please sit straight and click 'Set Normal Posture'" else "Monitoring posture...",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Human figure image with rotation
        Image(
            painter = painterResource(Res.drawable.front),
            contentDescription = "Front posture reference",
            modifier = Modifier
                .size(200.dp)
                .rotate(imageRotation)
                .padding(bottom = 24.dp),
            contentScale = ContentScale.Fit
        )
        
        // Posture ratio display - no boxes, just clean text
        if (isCalibrated) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Normal: ${formatThreeDecimals(normalPostureRatio)}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Current: ${formatThreeDecimals(currentRatio)}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            if (imageRotation > 0) {
                Text(
                    text = "⚠️ Poor posture detected!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        }

        // Pose data display (for debugging) - removed box styling
        Text(
            text = poseData.value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Action buttons - removed box styling, using clean text buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isCalibrated) {
                TextButton(
                    onClick = {
                        // Set current posture as normal
                        normalPostureRatio = currentRatio
                        isCalibrated = true
                    },
                    enabled = currentRatio > 0,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        "Set Normal Posture",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                TextButton(
                    onClick = {
                        isMonitoring = false
                        postureInterface.stopTracking()
                        onComplete()
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        "Done",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            
            TextButton(
                onClick = onNavigateBack
            ) {
                Text(
                    "Back",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

private fun formatThreeDecimals(value: Float): String {
    val thousandths = round(value * 1000f).toInt()
    val sign = if (thousandths < 0) "-" else ""
    val absThousandths = abs(thousandths)
    val whole = absThousandths / 1000
    val frac = absThousandths % 1000
    val fracStr = frac.toString().padStart(3, '0')
    return "$sign$whole.$fracStr"
}

private fun formatSessionTime(durationMs: Long): String {
    val seconds = (durationMs / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return buildString {
        if (minutes < 10) append("0")
        append(minutes)
        append(":")
        if (remainingSeconds < 10) append("0")
        append(remainingSeconds)
    }
}

 