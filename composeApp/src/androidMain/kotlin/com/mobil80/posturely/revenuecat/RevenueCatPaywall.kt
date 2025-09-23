package com.mobil80.posturely.revenuecat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.mobil80.posturely.revenuecat.RevenueCatManager
import com.revenuecat.purchases.kmp.ui.revenuecatui.Paywall
import com.revenuecat.purchases.kmp.ui.revenuecatui.PaywallOptions

/**
 * Android-specific paywall composable using RevenueCat KMP SDK
 * Uses the Paywall composable from RevenueCat UI
 */
@Composable
fun RevenueCatPaywall(
    revenueCatManager: RevenueCatManager,
    onDismiss: () -> Unit = {}
) {
    val hasActiveSubscription by revenueCatManager.hasActiveSubscription.collectAsState()
    val isInitialized by revenueCatManager.isInitialized.collectAsState()
    
    // Initialize RevenueCat if not already done
    LaunchedEffect(Unit) {
        if (!isInitialized) {
            revenueCatManager.initialize()
        }
    }
    
    // Only show paywall if user doesn't have an active subscription
    if (isInitialized && !hasActiveSubscription) {
        val options = remember {
            PaywallOptions(dismissRequest = onDismiss)
        }
        
        Paywall(options)
    }
}
