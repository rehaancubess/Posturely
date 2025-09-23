# Quick Start: Using Your RevenueCat Portal Paywall

## What You Need to Do

### 1. ✅ API Key (Already Done)
Your API key is configured: `goog_DuXINcnsjFAblGqJRCBNBiLgpdV`

### 2. 🎨 Design Your Paywall in RevenueCat Portal
1. Go to [RevenueCat Dashboard](https://app.revenuecat.com/)
2. Navigate to **Paywalls** → **Create Paywall**
3. Design your custom paywall with:
   - Your app branding and colors
   - Subscription plans and pricing
   - Feature highlights
   - Call-to-action buttons

### 3. 🔧 Configure Entitlement
1. Go to **Entitlements** in RevenueCat dashboard
2. Create/edit your entitlement (e.g., "premium", "pro", "unlimited")
3. Link it to your Google Play Console products
4. Note the exact entitlement identifier

### 4. 📝 Update Code
Replace `"premium"` in this file with your actual entitlement ID:

**File**: `composeApp/src/androidMain/kotlin/com/mobil80/posturely/revenuecat/RevenueCatPaywall.kt`
```kotlin
.setRequiredEntitlementIdentifier("YOUR_ENTITLEMENT_ID_HERE")
```

### 5. 🚀 Test
1. Build your Android app
2. Navigate to home screen
3. Your custom-designed paywall should appear!

## How It Works

- **Android Only**: Paywall only shows on Android devices
- **Smart Display**: Only appears for users without active subscriptions
- **Custom Design**: Uses your paywall design from RevenueCat portal
- **Automatic**: Shows whenever users reach the home screen

## Need Help?

- **Detailed Setup**: See [REVENUECAT_PAYWALL_SETUP.md](./REVENUECAT_PAYWALL_SETUP.md)
- **API Key Guide**: See [REVENUECAT_API_KEY_SETUP.md](./REVENUECAT_API_KEY_SETUP.md)
- **RevenueCat Docs**: [https://docs.revenuecat.com/](https://docs.revenuecat.com/)

## Example Entitlement IDs
- `premium`
- `pro` 
- `unlimited`
- `full_access`
- `posturely_premium`

Choose one that matches what you set up in your RevenueCat dashboard!
