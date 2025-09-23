package com.mobil80.posturely

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import posturelynew.composeapp.generated.resources.Res
import posturelynew.composeapp.generated.resources.logohorizontal

// Color definitions matching the app's design
private val pageBg = Color(0xFFFED867) // App background color
private val textPrimary = Color(0xFF0F1931)
private val cardBg = Color(0xFFFFF0C0)
private val accentBrown = Color(0xFF7A4B00) // App brown color
private val goodPostureGreen = Color(0xFF4CAF50)
private val badPostureRed = Color(0xFFE53935)

@OptIn(ExperimentalResourceApi::class)
@Composable
fun WhyPostureMattersScreen(
    onNavigateToLogin: () -> Unit,
    onContinue: () -> Unit = onNavigateToLogin
) {
    val scope = rememberCoroutineScope()
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        val isCompact = maxHeight < 640.dp
        val isSmall = maxHeight < 560.dp
        val logoHeight = when {
            maxHeight < 560.dp -> 100.dp
            maxHeight < 720.dp -> 120.dp
            else -> 140.dp
        }
        val topSpace = if (isCompact) 16.dp else 40.dp
        val midSpace = if (isCompact) 16.dp else 24.dp
        val bottomSpace = if (isCompact) 24.dp else 32.dp
        val contentPaddingX = if (isCompact) 20.dp else 24.dp

        // Typography scaling for smaller devices
        val titleSizeSp = when {
            isSmall -> 24.sp
            isCompact -> 28.sp
            else -> 32.sp
        }
        val subtitleSizeSp = when {
            isSmall -> 14.sp
            isCompact -> 16.sp
            else -> 18.sp
        }
        val cardTitleSizeSp = when {
            isSmall -> 18.sp
            isCompact -> 20.sp
            else -> 22.sp
        }
        val benefitTextSizeSp = when {
            isSmall -> 14.sp
            isCompact -> 15.sp
            else -> 16.sp
        }
        val buttonHeight = if (isCompact) 52.dp else 56.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues()),
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
                    .fillMaxWidth()
                    .padding(horizontal = contentPaddingX),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(midSpace * 2))

            // Main title
            Text(
                text = "Why Posture Matters",
                color = textPrimary,
                fontSize = titleSizeSp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = contentPaddingX)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle
            Text(
                text = "You don't notice your posture... until it starts hurting.",
                color = textPrimary.copy(alpha = 0.7f),
                fontSize = subtitleSizeSp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = contentPaddingX)
            )

            Spacer(modifier = Modifier.height(midSpace * 2))

            // Swipeable Posture Cards
            val pagerState = rememberPagerState(pageCount = { 2 })
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) { page ->
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = contentPaddingX + 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        when (page) {
                            0 -> {
                                // Bad Posture Card
                                Text(
                                    text = "Bad Posture",
                                    color = textPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 18.dp, top = 10.dp)
                                )

                                Column(
                                    horizontalAlignment = Alignment.Start,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        Text(
                                            text = "⚡",
                                            fontSize = 24.sp,
                                            color = badPostureRed,
                                            modifier = Modifier.width(40.dp),
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Neck and Back Pain",
                                            color = textPrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        Text(
                                            text = "💨",
                                            fontSize = 24.sp,
                                            color = badPostureRed,
                                            modifier = Modifier.width(40.dp),
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Shallow Breathing",
                                            color = textPrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        Text(
                                            text = "🔋",
                                            fontSize = 24.sp,
                                            color = badPostureRed,
                                            modifier = Modifier.width(40.dp),
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Low Energy",
                                            color = textPrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        Text(
                                            text = "😔",
                                            fontSize = 24.sp,
                                            color = badPostureRed,
                                            modifier = Modifier.width(40.dp),
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Reduced Confidence",
                                            color = textPrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            1 -> {
                                // Good Posture Card
                                Text(
                                    text = "Good Posture",
                                    color = textPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 18.dp, top = 10.dp)
                                )

                                Column(
                                    horizontalAlignment = Alignment.Start,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        Text(
                                            text = "✨",
                                            fontSize = 24.sp,
                                            color = goodPostureGreen,
                                            modifier = Modifier.width(40.dp),
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Reduced Pain",
                                            color = textPrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        Text(
                                            text = "🫁",
                                            fontSize = 24.sp,
                                            color = goodPostureGreen,
                                            modifier = Modifier.width(40.dp),
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Better Breathing",
                                            color = textPrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        Text(
                                            text = "⚡",
                                            fontSize = 24.sp,
                                            color = goodPostureGreen,
                                            modifier = Modifier.width(40.dp),
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "High Energy",
                                            color = textPrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        Text(
                                            text = "💪",
                                            fontSize = 24.sp,
                                            color = goodPostureGreen,
                                            modifier = Modifier.width(40.dp),
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Improved Confidence",
                                            color = textPrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(midSpace))

            // Pagination dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(2) { index ->
                    val isSelected = pagerState.currentPage == index
                    val dotColor = if (isSelected) goodPostureGreen else textPrimary.copy(alpha = 0.3f)
                    val dotSize = if (isSelected) 10.dp else 8.dp
                    
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .background(dotColor, CircleShape)
                            .clickable {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(midSpace))

            // Next button
            Button(
            onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight)
                    .padding(horizontal = contentPaddingX),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentBrown)
            ) {
                Text(
                    "Next",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(bottomSpace))

            // Bottom inset spacing for devices with gesture nav
            Spacer(modifier = Modifier.padding(WindowInsets.navigationBars.asPaddingValues()))
        }
    }
}

