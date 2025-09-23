package com.mobil80.posturely.revenuecat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * JVM implementation of RevenueCat manager factory
 * This is a stub implementation since RevenueCat is Android-only for this integration
 */
actual class RevenueCatManagerFactory {
    actual fun create(): RevenueCatManager {
        return NoOpRevenueCatManager()
    }
}

@Composable
actual fun rememberRevenueCatManager(): RevenueCatManager {
    return remember { NoOpRevenueCatManager() }
}
