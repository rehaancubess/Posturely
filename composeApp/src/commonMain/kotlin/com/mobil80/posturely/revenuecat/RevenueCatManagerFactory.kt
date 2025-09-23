package com.mobil80.posturely.revenuecat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Platform-specific factory for RevenueCat manager
 */
expect class RevenueCatManagerFactory {
    fun create(): RevenueCatManager
}

@Composable
expect fun rememberRevenueCatManager(): RevenueCatManager
