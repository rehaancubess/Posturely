package com.mobil80.posturely

import android.content.Context
import com.mobil80.posturely.scan.ScanCameraActivity

// Global context storage for Android
private var appContext: Context? = null

fun setAppContext(context: Context) {
    appContext = context
}

actual fun openNativeScanCamera() {
    appContext?.let { context ->
        val intent = ScanCameraActivity.createIntent(context)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}


