package com.example.posturelynew

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun PostureTrackingScreen(
    onBackPressed: () -> Unit
) {
    val postureInterface = rememberPostureTrackingInterface()
    
    var poseData by remember { mutableStateOf(postureInterface.getPoseData()) }
    var showSkeleton by remember { mutableStateOf(false) }
    var landmarkCount by remember { mutableStateOf(0) }
    var landmarks by remember { mutableStateOf(List(33) { Pair(0.5f, 0.5f) }) }
    
    // Update pose data periodically
    LaunchedEffect(postureInterface) {
        while (true) {
            val newPoseData = postureInterface.getPoseData()
            poseData = newPoseData
            
            // Show skeleton if pose detection is active
            showSkeleton = newPoseData.contains("Pose detection active")
            
            // Get real landmark data
            if (showSkeleton) {
                landmarkCount = postureInterface.getLandmarkCount()
                if (landmarkCount > 0) {
                    val newLandmarks = mutableListOf<Pair<Float, Float>>()
                    for (i in 0 until minOf(landmarkCount, 33)) {
                        val x = postureInterface.getLandmarkX(i)
                        val y = postureInterface.getLandmarkY(i)
                        newLandmarks.add(Pair(x, y))
                    }
                    landmarks = newLandmarks
                }
            }
            
            kotlinx.coroutines.delay(30) // Increased to 30ms for very responsive updates
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Posture Tracking",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Skeleton overlay area (replaces camera preview)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(
                    color = if (showSkeleton) Color.Black else Color.DarkGray,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (postureInterface.isTracking()) {
                if (showSkeleton) {
                    // Show skeleton overlay with real landmark data
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        drawRealSkeleton(landmarks)
                    }
                    
                                                // Overlay text
                            Text(
                                text = "Pose Detected!\nLandmarks: $landmarkCount\nReal Skeleton Overlay\nHead: (${landmarks[0].first}, ${landmarks[0].second})",
                                color = Color.Green,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                } else {
                    Text(
                        text = "Initializing pose detection...\nCamera starting up",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = "Camera Preview\nTap Start to begin tracking\n\nNative iOS integration ready",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Pose data display
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Pose Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = poseData,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Status card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = if (postureInterface.isTracking()) {
                        if (showSkeleton) {
                            "✅ Tracking Active\n📱 Native iOS Camera Active\n🎯 Real Pose Detected - $landmarkCount Landmarks"
                        } else {
                            "✅ Tracking Active\n📱 Native iOS Camera Starting\n🔍 Initializing pose detection..."
                        }
                    } else {
                        "⏸️ Tracking Stopped\n📱 Native iOS Camera Ready\n🔧 MediaPipe Integration Active"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onBackPressed,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Back")
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Button(
                onClick = {
                    if (postureInterface.isTracking()) {
                        postureInterface.stopTracking()
                    } else {
                        postureInterface.startTracking()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (postureInterface.isTracking()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (postureInterface.isTracking()) "Stop" else "Start")
            }
        }
    }
}

private fun DrawScope.drawRealSkeleton(landmarks: List<Pair<Float, Float>>) {
    if (landmarks.isEmpty() || landmarks.size < 33) return
    
    val viewWidth = size.width
    val viewHeight = size.height
    
    // Simplified connections for better performance - only key body parts
    val connections = listOf(
        // Core body
        11 to 12, // left shoulder to right shoulder
        11 to 23, // left shoulder to left hip
        12 to 24, // right shoulder to right hip
        23 to 24, // left hip to right hip
        
        // Arms
        11 to 13, // left shoulder to left elbow
        13 to 15, // left elbow to left wrist
        12 to 14, // right shoulder to right elbow
        14 to 16, // right elbow to right wrist
        
        // Legs
        23 to 25, // left hip to left knee
        25 to 27, // left knee to left ankle
        24 to 26, // right hip to right knee
        26 to 28  // right knee to right ankle
    )
    
    // Draw connections with thicker lines (rotated for portrait mode)
    for ((start, end) in connections) {
        if (start < landmarks.size && end < landmarks.size) {
            val startPoint = Offset(
                x = landmarks[start].second * viewWidth,         // Rotate 90 degrees: x = y
                y = (1f - landmarks[start].first) * viewHeight  // Rotate 90 degrees: y = 1-x
            )
            val endPoint = Offset(
                x = landmarks[end].second * viewWidth,           // Rotate 90 degrees: x = y
                y = (1f - landmarks[end].first) * viewHeight    // Rotate 90 degrees: y = 1-x
            )
            
            drawLine(
                color = Color.Green,
                start = startPoint,
                end = endPoint,
                strokeWidth = 3f // Reduced stroke width for better performance
            )
        }
    }
    
    // Draw only key landmark points for better performance (rotated for portrait mode)
    val keyPoints = listOf(0, 11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28) // Only key body parts
    
    for (i in keyPoints) {
        if (i < landmarks.size) {
            val point = Offset(
                x = landmarks[i].second * viewWidth,         // Rotate 90 degrees: x = y
                y = (1f - landmarks[i].first) * viewHeight  // Rotate 90 degrees: y = 1-x
            )
            
            val color = when (i) {
                0 -> Color.Yellow  // nose
                11, 12 -> Color.Blue  // shoulders
                13, 14 -> Color.Green  // elbows
                15, 16 -> Color.Green  // wrists
                23, 24 -> Color.Cyan  // hips
                25, 26 -> Color.Magenta  // knees
                27, 28 -> Color.Magenta  // ankles
                else -> Color.Red
            }
            
            drawCircle(
                color = color,
                radius = 6f, // Consistent radius for better performance
                center = point
            )
        }
    }
} 