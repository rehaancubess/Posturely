package com.example.posturelynew.scan

// Compose can set a callback to receive images from native (iOS)
object ScanImagesBridge {
    var onImagesReady: ((scanId: String, frontBase64: String, sideBase64: String) -> Unit)? = null
}

// Expect/actual for posting to Supabase edge function
expect suspend fun postPostureReport(scanId: String, frontBase64: String, sideBase64: String): Result<Unit>


