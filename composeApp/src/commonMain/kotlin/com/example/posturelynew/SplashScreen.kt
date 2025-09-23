package com.mobil80.posturely

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import posturelynew.composeapp.generated.resources.Res
import posturelynew.composeapp.generated.resources.logohorizontal
import kotlinx.coroutines.delay

@OptIn(ExperimentalResourceApi::class)
@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    val pageBg = Color(0xFFFED867) // App background color
    
    // Auto-navigate after 2 seconds
    LaunchedEffect(Unit) {
        delay(2000)
        onSplashComplete()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg),
        contentAlignment = Alignment.Center
    ) {
        // Horizontal logo centered
        Image(
            painter = painterResource(Res.drawable.logohorizontal),
            contentDescription = "Posturely Logo",
            modifier = Modifier
                .height(180.dp)
                .fillMaxWidth(),
            contentScale = ContentScale.Fit
        )
    }
}
