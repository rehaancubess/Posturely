package com.mobil80.posturely

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import posturelynew.composeapp.generated.resources.Res
import posturelynew.composeapp.generated.resources.giffygood
import posturelynew.composeapp.generated.resources.study1
import posturelynew.composeapp.generated.resources.study2
import posturelynew.composeapp.generated.resources.study3

// Theme-aligned colors (match Home/Stats)
private val pageBg = Color(0xFFFED867)
private val cardBg = Color(0xFFFFF0C0)
private val textPrimary = Color(0xFF0F1931)
private val subText = Color(0xFF6B7280)
private val accentBrown = Color(0xFF7A4B00)
private val chipUnselected = Color(0xFFFFF7DA)

// TEMPORARILY DISABLED - Replaced with Stats tab
@OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen() {
    var showLockDialog by remember { mutableStateOf(false) }
    val exerciseOptions = listOf("Sit Tall & Breathe", "Neck Rotation (Left–Right)", "Neck Tilt (Up–Down)")
    var selectedExercise by remember { mutableStateOf(exerciseOptions.first()) }
    var fromTime by remember { mutableStateOf("09:00") }
    var tillTime by remember { mutableStateOf("10:00") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        // Header Section - centered
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(
                text = "Exercises",
                color = textPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }
        
        // Two primary options
        ExerciseOptionCard(
            title = "Sit Tall & Breathe",
            subtitle = "(Posture Meditation)",
            duration = "3 min"
        )
        Spacer(Modifier.height(16.dp))
        ExerciseOptionCard(
            title = "Neck Rotation",
            subtitle = "Stretch",
            duration = "7 reps"
        )
        Spacer(Modifier.height(16.dp))
        ExerciseOptionCard(
            title = "Neck Tilt (Up–Down)",
            subtitle = "Stretch",
            duration = "7 reps"
        )

        Spacer(Modifier.height(24.dp))
        Text(
            text = "Tap any exercise to do it or Lock chosen apps for a time you set — until you complete selected exercise",
            color = textPrimary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
        )
        Button(
            onClick = { showLockDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentBrown)
        ) {
            Text("Lock Apps", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        
        if (showLockDialog) {
            AlertDialog(
                onDismissRequest = { showLockDialog = false },
                confirmButton = {
                    Button(
                        onClick = {
                            PlatformStartAppLock(fromTime, tillTime, selectedExercise)
                            showLockDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentBrown),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Start Lock", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { showLockDialog = false }) { Text("Cancel", color = textPrimary) }
                },
                title = { Text("Lock Apps", color = textPrimary, fontWeight = FontWeight.SemiBold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Time range
                        Text("Time window", color = textPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("From", color = textPrimary.copy(alpha = 0.8f))
                                OutlinedTextField(
                                    value = fromTime,
                                    onValueChange = { fromTime = it },
                                    singleLine = true
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Till", color = textPrimary.copy(alpha = 0.8f))
                                OutlinedTextField(
                                    value = tillTime,
                                    onValueChange = { tillTime = it },
                                    singleLine = true
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        // Exercise selection
                        Text("Exercise to complete", color = textPrimary)
                        exerciseOptions.forEach { option ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedExercise == option, onClick = { selectedExercise = option })
                                Text(option, color = textPrimary)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        // Configure apps button
                        OutlinedButton(
                            onClick = { PlatformOpenAppLockConfig() },
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("Configure Locked Apps", color = textPrimary) }
                    }
                },
                containerColor = cardBg,
                tonalElevation = 0.dp,
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                if (isSelected) cardBg else chipUnselected,
                RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = null,
                indication = null,
                enabled = true,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) textPrimary else subText,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun CategoryCard(
    title: String,
    subtitle: String,
    details: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    // Deprecated
} 

// Placeholder: platform-specific hooks can be added via expect/actual APIs if needed.

@Composable
private fun ExerciseOptionCard(
    title: String,
    subtitle: String,
    duration: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = textPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = textPrimary.copy(alpha = 0.8f), fontSize = 16.sp, fontWeight = FontWeight.Normal)
                if (duration != null) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFD36B), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(duration, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            val img = when {
                title.contains("Sit Tall", ignoreCase = true) -> Res.drawable.study1
                title.contains("Tilt", ignoreCase = true) -> Res.drawable.study2
                else -> Res.drawable.study3
            }
            Image(
                painter = painterResource(img),
                contentDescription = title,
                modifier = Modifier.size(110.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}