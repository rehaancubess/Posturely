package com.mobil80.posturely.revenuecat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import platform.Foundation.NSNotificationCenter

/**
 * iOS implementation of the paywall composable.
 * Triggers a native RevenueCatUI paywall via NotificationCenter.
 */
@Composable
actual fun PaywallComposable(
    revenueCatManager: RevenueCatManager,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        NSNotificationCenter.defaultCenter.postNotificationName(
            aName = "PresentRevenueCatPaywall",
            `object` = null,
            userInfo = null
        )
    }
}
