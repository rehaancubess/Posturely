package com.mobil80.posturely.revenuecat

import android.content.Context
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.PurchasesError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android-specific RevenueCat service for handling paywall integration
 */
class RevenueCatService private constructor() {
    
    private var _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    
    private var _customerInfo = MutableStateFlow<CustomerInfo?>(null)
    val customerInfo: StateFlow<CustomerInfo?> = _customerInfo.asStateFlow()
    
    private var _hasActiveSubscription = MutableStateFlow(false)
    val hasActiveSubscription: StateFlow<Boolean> = _hasActiveSubscription.asStateFlow()
    
    companion object {
        @Volatile
        private var INSTANCE: RevenueCatService? = null
        
        fun getInstance(): RevenueCatService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RevenueCatService().also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Initialize RevenueCat with the app context
     * your actual RevenueCat API key
     */
    fun initialize(context: Context, apiKey: String = "goog_DuXINcnsjFAblGqJRCBNBiLgpdV") {
        try {
            // Initialize RevenueCat with KMP SDK
            val configuration = PurchasesConfiguration.Builder(apiKey).build()
            Purchases.configure(configuration)
            
            // Get initial customer info
            Purchases.sharedInstance.getCustomerInfo(
                onSuccess = { customerInfo ->
                    _customerInfo.value = customerInfo
                    _hasActiveSubscription.value = hasActiveEntitlement(customerInfo)
                    _isInitialized.value = true
                },
                onError = { error ->
                    _isInitialized.value = true
                }
            )
        } catch (e: Exception) {
            // Handle initialization error
            _isInitialized.value = true
        }
    }
    
    /**
     * Check if user has an active entitlement
     * Replace "premium" with your actual entitlement identifier
     */
    private fun hasActiveEntitlement(customerInfo: CustomerInfo?): Boolean {
        return customerInfo?.entitlements?.active?.containsKey("premium") == true
    }
    
    /**
     * Refresh customer info
     */
    fun refreshCustomerInfo() {
        Purchases.sharedInstance.getCustomerInfo(
            onSuccess = { customerInfo ->
                _customerInfo.value = customerInfo
                _hasActiveSubscription.value = hasActiveEntitlement(customerInfo)
            },
            onError = { error ->
                // Handle error silently
            }
        )
    }
    
    /**
     * Handle successful purchase
     */
    fun onPurchaseCompleted(customerInfo: CustomerInfo) {
        _customerInfo.value = customerInfo
        _hasActiveSubscription.value = hasActiveEntitlement(customerInfo)
    }
    
    /**
     * Handle successful restore
     */
    fun onRestoreCompleted(customerInfo: CustomerInfo) {
        _customerInfo.value = customerInfo
        _hasActiveSubscription.value = hasActiveEntitlement(customerInfo)
    }
}
