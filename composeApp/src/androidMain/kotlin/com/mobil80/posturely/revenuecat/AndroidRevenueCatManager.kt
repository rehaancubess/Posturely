package com.mobil80.posturely.revenuecat

import android.content.Context
import com.mobil80.posturely.revenuecat.RevenueCatService
import kotlinx.coroutines.flow.StateFlow

/**
 * Android implementation of RevenueCat manager
 */
class AndroidRevenueCatManager(
    private val context: Context,
    private val apiKey: String = getRevenueCatApiKey()
) : RevenueCatManager {
    
    companion object {
        private fun getRevenueCatApiKey(): String {
            // Try to get from environment variable first (for CI/CD)
            //val envKey = System.getenv("goog_DuXINcnsjFAblGqJRCBNBiLgpdV")
            //if (!envKey.isNullOrBlank()) {
            //    return 
            //}
            
            // Fallback to build config (for local development)
            return "goog_DuXINcnsjFAblGqJRCBNBiLgpdV"
        }
    }
    
    private val revenueCatService = RevenueCatService.getInstance()
    
    override val isInitialized: StateFlow<Boolean>
        get() = revenueCatService.isInitialized
    
    override val hasActiveSubscription: StateFlow<Boolean>
        get() = revenueCatService.hasActiveSubscription
    
    override fun initialize() {
        revenueCatService.initialize(context, apiKey)
    }
    
    override fun refreshCustomerInfo() {
        revenueCatService.refreshCustomerInfo()
    }
    
    override fun onPurchaseCompleted() {
        // This will be handled by the paywall dialog listener
    }
    
    override fun onRestoreCompleted() {
        // This will be handled by the paywall dialog listener
    }
}
