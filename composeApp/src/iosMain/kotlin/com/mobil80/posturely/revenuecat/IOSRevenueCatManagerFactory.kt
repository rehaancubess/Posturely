package com.mobil80.posturely.revenuecat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS implementation of RevenueCat manager factory
 */
actual class RevenueCatManagerFactory {
    actual fun create(): RevenueCatManager {
        return IOSRevenueCatManager()
    }
}

@Composable
actual fun rememberRevenueCatManager(): RevenueCatManager {
    return remember { IOSRevenueCatManager() }
}
