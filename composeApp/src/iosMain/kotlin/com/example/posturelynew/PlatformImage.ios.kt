package com.example.posturelynew

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
actual fun PlatformImage(
    model: Any,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    // For iOS, we'll use a simple placeholder since Coil is not available
    Box(
        modifier = modifier
            .background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Image Placeholder",
            color = Color.DarkGray
        )
    }
} 