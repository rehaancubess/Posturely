package com.mobil80.posturely.revenuecat

import androidx.compose.runtime.Composable

/**
 * Common paywall composable that delegates to platform-specific implementations
 */
@Composable
expect fun PaywallComposable(
    revenueCatManager: RevenueCatManager,
    onDismiss: () -> Unit
)
