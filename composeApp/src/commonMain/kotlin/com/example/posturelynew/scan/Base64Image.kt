package com.mobil80.posturely.scan

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlin.io.encoding.ExperimentalEncodingApi

expect fun decodeBase64ToImageBitmap(base64: String): ImageBitmap?

@OptIn(ExperimentalEncodingApi::class)
@Composable
fun Base64Image(
    base64String: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val bitmap = decodeBase64ToImageBitmap(base64String)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}


