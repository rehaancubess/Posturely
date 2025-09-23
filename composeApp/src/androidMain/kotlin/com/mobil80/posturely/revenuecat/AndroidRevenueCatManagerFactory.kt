package com.mobil80.posturely.revenuecat

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Android implementation of RevenueCat manager factory
 */
actual class RevenueCatManagerFactory {
    actual fun create(): RevenueCatManager {
        // This will be initialized with context in the Android-specific implementation
        throw NotImplementedError("Use rememberRevenueCatManager() instead")
    }
}

@Composable
actual fun rememberRevenueCatManager(): RevenueCatManager {
    val context = LocalContext.current
    return remember {
        AndroidRevenueCatManager(context)
    }
}
