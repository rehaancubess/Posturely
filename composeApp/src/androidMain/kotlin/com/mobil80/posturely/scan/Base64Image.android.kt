package com.mobil80.posturely.scan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
actual fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? {
    return try {
        val clean = base64.replace("\n", "").replace("\r", "")
        val bytes = Base64.decode(clean)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        bitmap?.asImageBitmap()
    } catch (_: Throwable) {
        null
    }
}


