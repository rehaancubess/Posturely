# RevenueCat Paywall Integration

This document describes the RevenueCat paywall integration for Android in the Posturely app.

## Overview

The RevenueCat paywall is integrated to display whenever users navigate to the home screen of the Android app. The integration is Android-specific and will not affect iOS, desktop, or other platforms.

## Setup Instructions

### 1. RevenueCat Dashboard Configuration

1. **Create a RevenueCat Account**: Sign up at [RevenueCat](https://www.revenuecat.com/)
2. **Create a Project**: Set up a new project in the RevenueCat dashboard
3. **Configure Products**: Create your subscription products in the RevenueCat dashboard
4. **Create Entitlements**: Set up entitlements (e.g., "premium") that users will receive upon purchase
5. **Design Paywall**: Use the RevenueCat dashboard to design your paywall UI

### 2. API Key Configuration

**📋 See detailed setup guide**: [REVENUECAT_API_KEY_SETUP.md](./REVENUECAT_API_KEY_SETUP.md)

**Quick Setup**: Replace the placeholder in `composeApp/build.gradle.kts`:

```kotlin
defaultConfig {
    // ... other config
    buildConfigField("String", "REVENUECAT_API_KEY", "\"rcb_your_actual_api_key_here\"")
}
```

The API key is automatically loaded from BuildConfig or environment variables.

### 3. Paywall Configuration

**📋 See detailed paywall setup guide**: [REVENUECAT_PAYWALL_SETUP.md](./REVENUECAT_PAYWALL_SETUP.md)

#### Quick Setup:
1. **Design in Portal**: Create your paywall in RevenueCat dashboard
2. **Configure Entitlement**: Update the entitlement identifier in code
3. **Test**: Build and test on Android device

#### Update Entitlement Identifier:
**File**: `composeApp/src/androidMain/kotlin/com/mobil80/posturely/revenuecat/RevenueCatPaywall.kt`

```kotlin
.setRequiredEntitlementIdentifier("premium") // Replace with your entitlement ID from RevenueCat portal
```

**Important**: The entitlement identifier must match exactly what you configured in your RevenueCat dashboard under **Entitlements**.

## Architecture

### Platform-Specific Implementation

- **Android**: Full RevenueCat SDK integration with paywall display
- **iOS/Desktop/JVM**: No-op implementations that don't show paywalls

### Key Components

1. **RevenueCatService**: Android-specific service handling RevenueCat SDK operations
2. **RevenueCatManager**: Common interface for cross-platform compatibility
3. **PaywallComposable**: Platform-specific paywall UI components
4. **RevenueCatManagerFactory**: Factory for creating platform-specific managers

### Integration Points

The paywall is integrated in the `HomeScreen` composable in `App.kt`:

```kotlin
@Composable
fun HomeScreen(...) {
    // ... existing home screen code
    
    // RevenueCat Paywall - Android only
    PaywallComposable(
        revenueCatManager = revenueCatManager,
        onDismiss = { /* Paywall dismissed */ }
    )
}
```

## Behavior

- **Android**: Paywall displays automatically when users navigate to the home screen (if they don't have an active subscription)
- **Other Platforms**: No paywall is shown
- **Subscription Check**: The paywall only shows for users without active subscriptions
- **Dismissal**: Users can dismiss the paywall using the built-in close button

## Testing

### Test Scenarios

1. **Fresh Install**: Verify paywall appears on first home screen visit
2. **Subscription Active**: Verify paywall doesn't appear for subscribed users
3. **Purchase Flow**: Test the complete purchase flow
4. **Restore Purchases**: Test restore functionality
5. **Error Handling**: Test behavior when RevenueCat services are unavailable

### Test Configuration

For testing, you can use RevenueCat's sandbox environment:

1. Set up sandbox products in RevenueCat dashboard
2. Use test accounts for Google Play Console
3. Test purchases using sandbox mode

## Dependencies

The following dependencies are added for RevenueCat KMP integration:

```kotlin
// RevenueCat KMP SDK for cross-platform paywall integration
implementation("com.revenuecat.purchases:purchases-kmp-core:8.19.2")
implementation("com.revenuecat.purchases:purchases-kmp-datetime:8.19.2")
```

**Note**: This implementation uses the RevenueCat Kotlin Multiplatform SDK as recommended in the [official documentation](https://www.revenuecat.com/docs/getting-started/installation/kotlin-multiplatform).

## Notes

- The integration is designed to be non-intrusive and only affects Android users
- The paywall respects user subscription status and won't show for active subscribers
- All RevenueCat operations are handled asynchronously to avoid blocking the UI
- Error handling is implemented to gracefully handle RevenueCat service unavailability
