package com.mobil80.posturely.revenuecat

import kotlinx.coroutines.flow.StateFlow

/**
 * Common interface for RevenueCat functionality across platforms
 */
interface RevenueCatManager {
    val isInitialized: StateFlow<Boolean>
    val hasActiveSubscription: StateFlow<Boolean>
    
    fun initialize()
    fun refreshCustomerInfo()
    fun onPurchaseCompleted()
    fun onRestoreCompleted()
}
