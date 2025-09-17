package com.example.posturelynew.scan

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
actual fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? {
    return try {
        val clean = base64.replace("\n", "").replace("\r", "")
        val bytes = Base64.decode(clean)
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    } catch (_: Throwable) {
        null
    }
}


