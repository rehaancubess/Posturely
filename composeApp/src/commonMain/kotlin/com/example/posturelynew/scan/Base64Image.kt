package com.example.posturelynew.scan

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import org.jetbrains.skia.Image

@OptIn(ExperimentalEncodingApi::class)
fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? {
    return try {
        val data = Base64.decode(base64)
        val skImage = Image.makeFromEncoded(data)
        skImage.toComposeImageBitmap()
    } catch (_: Throwable) {
        null
    }
}


