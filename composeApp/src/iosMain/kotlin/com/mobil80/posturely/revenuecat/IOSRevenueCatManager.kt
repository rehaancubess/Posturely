package com.mobil80.posturely.revenuecat

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lightweight iOS RevenueCat manager placeholder.
 * Stores the Apple API key and exposes initialized/subscription state without linking the SDK.
 */
class IOSRevenueCatManager(
    private val apiKey: String = AppleRevenueCatConfig.API_KEY
) : RevenueCatManager {

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _hasActiveSubscription = MutableStateFlow(false)
    override val hasActiveSubscription: StateFlow<Boolean> = _hasActiveSubscription.asStateFlow()

    override fun initialize() {
        // In a full integration, we'd call Purchases.configure(apiKey) here.
        // For now, mark initialized so UI can proceed.
        _isInitialized.value = apiKey.isNotBlank()
    }

    override fun refreshCustomerInfo() {
        // No-op placeholder
    }

    override fun onPurchaseCompleted() {
        // No-op placeholder
    }

    override fun onRestoreCompleted() {
        // No-op placeholder
    }
}


