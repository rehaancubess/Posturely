package com.mobil80.posturely.revenuecat

import androidx.compose.runtime.Composable

/**
 * Android implementation of the common paywall composable
 */
@Composable
actual fun PaywallComposable(
    revenueCatManager: RevenueCatManager,
    onDismiss: () -> Unit
) {
    RevenueCatPaywall(
        revenueCatManager = revenueCatManager,
        onDismiss = onDismiss
    )
}
