package com.mobil80.posturely

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
    onNavigateToWhyPostureMatters: () -> Unit
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(WindowInsets.navigationBars.asPaddingValues()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = contentPaddingX),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Horizontal logo
                Image(
                    painter = painterResource(Res.drawable.logohorizontal),
                    contentDescription = "Posturely Logo",
                    modifier = Modifier
                        .height(logoHeight)
                        .fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(midSpace * 2))

                // Get Started button
                Button(
                    onClick = onNavigateToWhyPostureMatters,
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
            }
        }
    }
}

