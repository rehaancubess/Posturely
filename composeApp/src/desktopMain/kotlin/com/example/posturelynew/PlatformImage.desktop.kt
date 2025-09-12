package com.example.posturelynew

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

actual fun getPlatformImage(): String = "desktop"

@Composable
actual fun PlatformImage(
    model: Any,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    // For desktop, we'll use a simple placeholder
    // You can replace this with a proper image loading library like Coil or Ktor if needed
    Box(
        modifier = modifier
            .size(100.dp)
            .background(Color.Gray),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Image",
            color = Color.White
        )
    }
}
