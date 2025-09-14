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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import posturelynew.composeapp.generated.resources.Res
import posturelynew.composeapp.generated.resources.logohorizontal

// Color definitions matching the app's design
private val pageBg = Color(0xFFFED867) // App background color
private val textPrimary = Color(0xFF0F1931)
private val cardBg = Color(0xFFFFF0C0)
private val accentBrown = Color(0xFF7A4B00) // App brown color

@OptIn(ExperimentalResourceApi::class)
@Composable
fun IntroScreen(
    onNavigateToLogin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top spacing for status bar
        Spacer(modifier = Modifier.height(60.dp))
        
        // Horizontal logo
        Image(
            painter = painterResource(Res.drawable.logohorizontal),
            contentDescription = "Posturely Logo",
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth(),
            contentScale = ContentScale.Fit
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Features list - following the UI style from the image
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Feature 1: Real-time Tracking
            FeatureItem(
                icon = "📱",
                title = "Real-time Tracking",
                description = "Monitor your posture continuously using your phone camera or AirPods sensors"
            )
            
            // Feature 2: Gentle Reminders
            FeatureItem(
                icon = "🔔",
                title = "Gentle Reminders",
                description = "Get subtle notifications to adjust your posture without interrupting your work"
            )
            
            // Feature 3: Progress Insights
            FeatureItem(
                icon = "📊",
                title = "Progress Insights",
                description = "View detailed statistics and trends to understand your posture habits over time"
            )
            
            // Feature 4: Privacy First
            FeatureItem(
                icon = "🔒",
                title = "Privacy First",
                description = "All data stays on your device - no cloud storage, complete privacy protection"
            )
            
            // Feature 5: Multiple Devices
            FeatureItem(
                icon = "💻",
                title = "Multi-Device Support",
                description = "Works seamlessly across phone, laptop, and AirPods for comprehensive tracking"
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Get Started button
        Button(
            onClick = onNavigateToLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentBrown)
        ) {
            Text(
                "GET STARTED",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Footer text
        Text(
            text = "Posture insights are wellness guidance, not medical advice.",
            color = textPrimary.copy(alpha = 0.6f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun FeatureItem(
    icon: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Simple brown icon
        Text(
            text = icon,
            fontSize = 28.sp,
            color = accentBrown,
            modifier = Modifier.padding(end = 20.dp)
        )
        
        // Content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = description,
                color = textPrimary.copy(alpha = 0.7f),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}
