package com.mobil80.posturely

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.platform.AndroidUiDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

actual fun PlatformOpenAppLockConfig() {
    // This function will be called from Composable context, but does not need to be @Composable itself.
    // Use a global coroutine if needed; here we directly start intents.
}

actual fun PlatformStartAppLock(fromTime: String, tillTime: String, exercise: String) {
    // Stub: to be implemented with a foreground service and overlay.
}


