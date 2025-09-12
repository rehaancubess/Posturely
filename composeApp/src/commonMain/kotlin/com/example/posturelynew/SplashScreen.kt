package com.example.posturelynew

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import posturelynew.composeapp.generated.resources.Res
import posturelynew.composeapp.generated.resources.giffygood
import kotlinx.coroutines.delay

@OptIn(ExperimentalResourceApi::class)
@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    val pageBg = Color(0xFFFFF9EA)
    val textPrimary = Color(0xFF0F1931)
    
    // Animation for the giraffe (simplified for cross-platform compatibility)
    val infiniteTransition = rememberInfiniteTransition(label = "giraffe_animation")
    val giraffeOpacity by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "giraffe_opacity"
    )
    
    // Auto-navigate after 2.5 seconds
    LaunchedEffect(Unit) {
        delay(2500)
        onSplashComplete()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Giraffe image with animation
            Image(
                painter = painterResource(Res.drawable.giffygood),
                contentDescription = "Giraffe",
                modifier = Modifier.size(200.dp),
                alpha = giraffeOpacity
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // App title
            Text(
                text = "Posturely",
                color = textPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Subtitle
            Text(
                text = "Monitor and correct your posture",
                color = textPrimary.copy(alpha = 0.7f),
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Loading indicator
            CircularProgressIndicator(
                color = Color(0xFF2ECC71),
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
