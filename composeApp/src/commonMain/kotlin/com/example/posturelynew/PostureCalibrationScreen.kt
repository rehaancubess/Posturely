package com.example.posturelynew

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
// Icons will be handled differently for cross-platform compatibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PostureCalibrationScreen(
    onBackPressed: () -> Unit,
    onStartCalibration: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBackPressed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("← Back")
            }
            
            Text(
                text = "Posture Calibration",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Introduction card
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
                        text = "ℹ️ Perfect Posture Guide",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Follow these steps to achieve and maintain perfect posture. Imagine there's an invisible thread pulling you up from the crown of your head.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Start
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Step-by-step instructions
            val steps = listOf(
                PostureStep(
                    number = 1,
                    title = "Head Position",
                    description = "Imagine a thread pulling you up from the crown of your head. Keep your chin parallel to the ground, not tilted up or down.",
                    details = "• Ears should be directly above your shoulders\n• Eyes looking straight ahead\n• Neck should feel long and relaxed"
                ),
                PostureStep(
                    number = 2,
                    title = "Shoulder Alignment",
                    description = "Roll your shoulders back and down. They should be relaxed, not hunched forward or raised up.",
                    details = "• Shoulder blades should be gently pulled together\n• Arms should hang naturally at your sides\n• Avoid shrugging or tensing"
                ),
                PostureStep(
                    number = 3,
                    title = "Spine Position",
                    description = "Maintain the natural curves of your spine. Stand tall but don't over-arch your back.",
                    details = "• Keep your back straight but not rigid\n• Natural curve in your lower back should be maintained\n• Avoid slouching or over-arching"
                ),
                PostureStep(
                    number = 4,
                    title = "Core Engagement",
                    description = "Gently engage your core muscles. Think of pulling your belly button toward your spine.",
                    details = "• Don't suck in your stomach too hard\n• Maintain natural breathing\n• Core should feel engaged but not tense"
                ),
                PostureStep(
                    number = 5,
                    title = "Hip Position",
                    description = "Keep your hips level and aligned with your shoulders. Avoid tilting forward or backward.",
                    details = "• Hips should be directly under your shoulders\n• Avoid sticking your butt out or tucking it under\n• Pelvis should be in neutral position"
                ),
                PostureStep(
                    number = 6,
                    title = "Legs and Feet",
                    description = "Stand with your feet shoulder-width apart, knees slightly bent, and weight evenly distributed.",
                    details = "• Feet should point straight ahead\n• Weight evenly distributed on both feet\n• Knees should not be locked"
                )
            )
            
            steps.forEach { step ->
                PostureStepCard(step = step)
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Tips section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Pro Tips",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    val tips = listOf(
                        "• Take regular breaks to check and adjust your posture",
                        "• Use a mirror to check your alignment",
                        "• Practice in front of a wall - your head, shoulders, and hips should touch it",
                        "• Remember to breathe naturally while maintaining posture",
                        "• Start with short periods and gradually increase duration",
                        "• Be patient - good posture takes time to develop"
                    )
                    
                    tips.forEach { tip ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "✅",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = tip,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Start Calibration button
            Button(
                onClick = onStartCalibration,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Start Calibration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PostureStepCard(step: PostureStep) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = step.number.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
            
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = step.details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}

private data class PostureStep(
    val number: Int,
    val title: String,
    val description: String,
    val details: String
) 