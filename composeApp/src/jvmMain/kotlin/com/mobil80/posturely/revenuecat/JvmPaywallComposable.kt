package com.mobil80.posturely.revenuecat

import androidx.compose.runtime.Composable

/**
 * JVM implementation of the common paywall composable
 * This is a no-op implementation since RevenueCat is Android-only for this integration
 */
@Composable
actual fun PaywallComposable(
    revenueCatManager: RevenueCatManager,
    onDismiss: () -> Unit
) {
    // No-op for JVM - paywall will not be shown
}
