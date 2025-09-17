package com.example.posturelynew

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        val isCompact = maxHeight < 640.dp
        val isSmall = maxHeight < 560.dp
        val logoHeight = when {
            maxHeight < 560.dp -> 120.dp
            maxHeight < 720.dp -> 160.dp
            else -> 200.dp
        }
        val topSpace = if (isCompact) 16.dp else 40.dp
        val midSpace = if (isCompact) 16.dp else 24.dp
        val bottomSpace = if (isCompact) 24.dp else 32.dp
        val featureSpacing = if (isCompact) 12.dp else 16.dp
        val contentPaddingX = if (isCompact) 20.dp else 24.dp

        // Typography scaling for smaller devices
        val iconSizeSp = when {
            isSmall -> 22.sp
            isCompact -> 24.sp
            else -> 28.sp
        }
        val titleSizeSp = when {
            isSmall -> 14.sp
            isCompact -> 15.sp
            else -> 16.sp
        }
        val descSizeSp = when {
            isSmall -> 12.sp
            isCompact -> 13.sp
            else -> 14.sp
        }
        val descLineHeightSp = when {
            isSmall -> 18.sp
            isCompact -> 19.sp
            else -> 20.sp
        }
        val buttonHeight = if (isCompact) 52.dp else 56.dp
        val footerSizeSp = if (isCompact) 11.sp else 12.sp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(horizontal = contentPaddingX)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top spacing for status bar / larger screens
            Spacer(modifier = Modifier.height(topSpace))

            // Horizontal logo
            Image(
                painter = painterResource(Res.drawable.logohorizontal),
                contentDescription = "Posturely Logo",
                modifier = Modifier
                    .height(logoHeight)
                    .fillMaxWidth(),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(midSpace))

            // Features list - responsive spacing
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(featureSpacing)
            ) {
                FeatureItem(
                    icon = "📱",
                    title = "Real-time Tracking",
                    description = "Monitor your posture continuously using your phone camera or AirPods sensors",
                    iconSize = iconSizeSp,
                    titleSize = titleSizeSp,
                    descriptionSize = descSizeSp,
                    descriptionLineHeight = descLineHeightSp
                )
                FeatureItem(
                    icon = "🔔",
                    title = "Gentle Reminders",
                    description = "Get subtle notifications to adjust your posture without interrupting your work",
                    iconSize = iconSizeSp,
                    titleSize = titleSizeSp,
                    descriptionSize = descSizeSp,
                    descriptionLineHeight = descLineHeightSp
                )
                FeatureItem(
                    icon = "📊",
                    title = "Progress Insights",
                    description = "View detailed statistics and trends to understand your posture habits over time",
                    iconSize = iconSizeSp,
                    titleSize = titleSizeSp,
                    descriptionSize = descSizeSp,
                    descriptionLineHeight = descLineHeightSp
                )
                FeatureItem(
                    icon = "🔒",
                    title = "Privacy First",
                    description = "All data stays on your device - no cloud storage, complete privacy protection",
                    iconSize = iconSizeSp,
                    titleSize = titleSizeSp,
                    descriptionSize = descSizeSp,
                    descriptionLineHeight = descLineHeightSp
                )
                FeatureItem(
                    icon = "💻",
                    title = "Multi-Device Support",
                    description = "Works seamlessly across phone, laptop, and AirPods for comprehensive tracking",
                    iconSize = iconSizeSp,
                    titleSize = titleSizeSp,
                    descriptionSize = descSizeSp,
                    descriptionLineHeight = descLineHeightSp
                )
            }

            Spacer(modifier = Modifier.height(midSpace))

            // Get Started button
            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight),
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

            Spacer(modifier = Modifier.height(bottomSpace))

            // Footer text
            Text(
                text = "Posture insights are wellness guidance, not medical advice.",
                color = textPrimary.copy(alpha = 0.6f),
                fontSize = footerSizeSp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Bottom inset spacing for devices with gesture nav
            Spacer(modifier = Modifier.padding(WindowInsets.navigationBars.asPaddingValues()))
        }
    }
}

@Composable
private fun FeatureItem(
    icon: String,
    title: String,
    description: String,
    iconSize: androidx.compose.ui.unit.TextUnit = 28.sp,
    titleSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    descriptionSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    descriptionLineHeight: androidx.compose.ui.unit.TextUnit = 20.sp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Simple brown icon
        Text(
            text = icon,
            fontSize = iconSize,
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
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = description,
                color = textPrimary.copy(alpha = 0.7f),
                fontSize = descriptionSize,
                lineHeight = descriptionLineHeight
            )
        }
    }
}
