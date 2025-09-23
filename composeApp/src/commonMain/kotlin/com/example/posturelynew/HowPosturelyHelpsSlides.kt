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
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import posturelynew.composeapp.generated.resources.Res
import posturelynew.composeapp.generated.resources.giraffeenew
import posturelynew.composeapp.generated.resources.scans
import posturelynew.composeapp.generated.resources.study1
import posturelynew.composeapp.generated.resources.study2
import posturelynew.composeapp.generated.resources.study3

private val pageBg = Color(0xFFFED867)
private val textPrimary = Color(0xFF0F1931)
private val accentBrown = Color(0xFF7A4B00)

@OptIn(ExperimentalResourceApi::class)
@Composable
fun HowPosturelyHelpsSlides(
    onFinish: () -> Unit
) {
    val scope = rememberCoroutineScope()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        val isCompact = maxHeight < 640.dp
        val contentPaddingX = if (isCompact) 20.dp else 24.dp
        val headerTitleSizeSp = if (isCompact) 26.sp else 30.sp
        val titleSizeSp = if (isCompact) 24.sp else 28.sp
        val subtitleSizeSp = if (isCompact) 13.sp else 15.sp
        val imageHeight = if (isCompact) 200.dp else 240.dp
        val buttonHeight = if (isCompact) 52.dp else 56.dp

        val slides = listOf(
            SlideData(
                title = "Real-Time Monitoring",
                description = "Posturely keeps an eye on you while you work, study, or scroll. The app gives instant feedback the moment you start slouching, so you can correct your posture before discomfort builds up.",
                image = Res.drawable.giraffeenew
            ),
            SlideData(
                title = "Take Full Body Scans",
                description = "Beyond real-time checks, you can run full-body posture scans to analyze spine alignment, shoulder balance, and head tilt. These scans provide a deeper picture of your posture health over time.",
                image = Res.drawable.scans
            ),
            SlideData(
                title = "Track Anywhere with Device Sync",
                description = "Whether you’re at your desk, on your laptop, or just using your phone with AirPods, Posturely syncs data seamlessly across devices. You’ll always have posture tracking wherever you go.",
                image = Res.drawable.study2
            ),
            SlideData(
                title = "Personalized Monitored Exercises",
                description = "Based on your posture patterns, Posturely suggests custom stretches and strengthening routines. These exercises are designed to target your weak areas and build lasting posture improvements.",
                image = Res.drawable.study3
            ),
            SlideData(
                title = "Valuable Insights",
                description = "The app compiles your posture history into clear daily, weekly, and monthly insights. You’ll see trends, progress scores, and actionable tips, helping you stay consistent and motivated.",
                image = Res.drawable.study1
            )
        )

        val pagerState = rememberPagerState(pageCount = { slides.size })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(if (isCompact) 8.dp else 12.dp))
            Text(
                text = "How Posturely Helps",
                color = textPrimary,
                fontSize = headerTitleSizeSp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = contentPaddingX)
            )
            Spacer(Modifier.height(if (isCompact) 8.dp else 12.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val s = slides[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = contentPaddingX),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(s.image),
                        contentDescription = s.title,
                        modifier = Modifier
                            .height(imageHeight)
                            .fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.height(if (isCompact) 12.dp else 16.dp))
                    Text(s.title, color = textPrimary, fontSize = titleSizeSp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(if (isCompact) 8.dp else 12.dp))
                    Text(
                        s.description,
                        color = textPrimary.copy(alpha = 0.8f),
                        fontSize = subtitleSizeSp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(if (isCompact) 8.dp else 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(slides.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (selected) 10.dp else 8.dp)
                            .background(if (selected) textPrimary else textPrimary.copy(alpha = 0.3f), CircleShape)
                            .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                    )
                }
            }

            Spacer(Modifier.height(if (isCompact) 16.dp else 20.dp))

            Button(
                onClick = {
                    if (pagerState.currentPage < slides.lastIndex) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else onFinish()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight)
                    .padding(horizontal = contentPaddingX),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentBrown)
            ) {
                Text(if (pagerState.currentPage < slides.lastIndex) "Next" else "Get started", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            // Bottom spacing and navigation bar inset, mirroring stats screen
            Spacer(modifier = Modifier.height(if (isCompact) 24.dp else 32.dp))
            Spacer(modifier = Modifier.padding(WindowInsets.navigationBars.asPaddingValues()))
        }
    }
}

private data class SlideData(
    val title: String,
    val description: String,
    val image: DrawableResource
)


