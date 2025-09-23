package com.mobil80.posturely.scan

import androidx.compose.ui.graphics.ImageBitmap

// Desktop stub implementation - returns null since image decoding is not needed on desktop
actual fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? {
    // Return null for desktop - no image processing needed
    return null
}
