package com.example.posturelynew

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import posturelynew.composeapp.generated.resources.Res
import posturelynew.composeapp.generated.resources.giffygood

// Color definitions at file level
private val pageBg = Color(0xFFFFF9EA)
private val textPrimary = Color(0xFF0F1931)
private val subText = Color(0xFF6B7280)
private val accentGreen = Color(0xFF28A745)
private val accentOrange = Color(0xFFFFA000)
private val accentBlue = Color(0xFF3B82F6)
private val accentPurple = Color(0xFF8B5CF6)
private val lightGreen = Color(0xFFE8F5E8)
private val lightOrange = Color(0xFFFFE4B3)
private val lightBlue = Color(0xFFE0F2FF)
private val lightPurple = Color(0xFFE8E0FF)

@OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)
@Composable
fun IntroScreen(
    onNavigateToLogin: () -> Unit
) {
    var currentSection by remember { mutableStateOf(0) }
    var showContent by remember { mutableStateOf(false) }
    
    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "intro")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle_rotation"
    )
    
    val wobble by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wobble"
    )
    
    LaunchedEffect(Unit) {
        delay(300)
        showContent = true
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        // Add safe space above header for status bar
        Spacer(modifier = Modifier.height(48.dp))
        
        // Header
        Text(
            text = "Welcome to Posturely",
            color = textPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "A playful coach for a healthier spine.",
            color = subText,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Section 1: Good Posture
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(animationSpec = tween(800)) + slideInVertically(
                animationSpec = tween(800),
                initialOffsetY = { 100 }
            )
        ) {
            IntroSection(
                title = "Good Posture — Why it helps",
                backgroundColor = lightGreen,
                icon = "🌟",
                content = listOf(
                    "Breathe better: more lung space, less fatigue",
                    "Less neck/back pain: distributes load evenly",
                    "Sharper focus: better blood flow & energy"
                ),
                giraffeModifier = Modifier
                    .size(120.dp)
                    .rotate(0f),
                showSparkles = true,
                sparkleRotation = rotation
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Section 2: Bad Posture
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(animationSpec = tween(800, delayMillis = 200)) + slideInVertically(
                animationSpec = tween(800, delayMillis = 200),
                initialOffsetY = { 100 }
            )
        ) {
            IntroSection(
                title = "Bad Posture — What to avoid",
                backgroundColor = lightOrange,
                icon = "⚠️",
                content = listOf(
                    "Forward head: sore neck & tight traps",
                    "Rounded shoulders: mid‑back strain",
                    "Side tilt: uneven load on spine"
                ),
                giraffeModifier = Modifier
                    .size(120.dp)
                    .rotate(wobble),
                showSparkles = false
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Section 3: How Posturely tracks
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(animationSpec = tween(800, delayMillis = 400)) + slideInVertically(
                animationSpec = tween(800, delayMillis = 400),
                initialOffsetY = { 100 }
            )
        ) {
            IntroSection(
                title = "How Posturely tracks",
                backgroundColor = lightBlue,
                icon = "📱",
                content = listOf(
                    "Phone camera (background, no preview)",
                    "Laptop camera (phone becomes dashboard)",
                    "AirPods (motion sensors)"
                ),
                giraffeModifier = Modifier
                    .size(120.dp)
                    .rotate(0f),
                showSparkles = false
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Status chips
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(animationSpec = tween(800, delayMillis = 600)) + slideInVertically(
                animationSpec = tween(800, delayMillis = 600),
                initialOffsetY = { 50 }
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusChip("Private (on‑device)", accentGreen)
                StatusChip("Real‑time", accentBlue)
                StatusChip("Low battery impact", accentPurple)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Primary CTA
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(animationSpec = tween(800, delayMillis = 800)) + slideInVertically(
                animationSpec = tween(800, delayMillis = 800),
                initialOffsetY = { 50 }
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onNavigateToLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentGreen)
                ) {
                    Text(
                        "Get Started",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Footer microcopy
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(animationSpec = tween(800, delayMillis = 1000))
                ) {
                    Text(
                        "Posture insights are wellness guidance, not medical advice.",
                        color = subText,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun IntroSection(
    title: String,
    backgroundColor: Color,
    icon: String,
    content: List<String>,
    giraffeModifier: Modifier,
    showSparkles: Boolean,
    sparkleRotation: Float = 0f
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        icon,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = title,
                        color = textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                content.forEach { item ->
                    Text(
                        text = "• $item",
                        color = textPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Removed CTA buttons
            }
            
            // Right side: Giraffe with optional sparkles
            Box(
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.giffygood),
                    contentDescription = "Giraffe illustration",
                    modifier = giraffeModifier,
                    contentScale = ContentScale.Fit
                )
                
                if (showSparkles) {
                    // Sparkle icons around the giraffe
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(sparkleRotation)
                            .background(Color(0xFFFFD700), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✨", fontSize = 12.sp)
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(-sparkleRotation)
                            .background(Color(0xFFFFD700), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⭐", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
} 