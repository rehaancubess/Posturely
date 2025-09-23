# RevenueCat Paywall Setup Guide

## How to Design Your Paywall in RevenueCat Portal

### Step 1: Access Paywalls Section
1. Log in to your [RevenueCat Dashboard](https://app.revenuecat.com/)
2. Navigate to **Paywalls** in the left sidebar
3. Click **"Create Paywall"** or edit an existing paywall

### Step 2: Configure Paywall Settings
1. **Paywall Name**: Give your paywall a descriptive name (e.g., "Posturely Premium")
2. **Platform**: Select "Android" 
3. **App**: Choose your Android app from the dropdown

### Step 3: Design Your Paywall
#### Visual Elements:
- **Header Image**: Upload your app logo or hero image
- **Background**: Choose background color or image
- **Typography**: Select fonts and text colors
- **Button Styling**: Customize CTA button appearance

#### Content:
- **Title**: Main headline (e.g., "Unlock Premium Features")
- **Subtitle**: Supporting text explaining benefits
- **Feature List**: Highlight key premium features
- **Pricing Display**: Show subscription options

#### Subscription Plans:
1. **Add Products**: Link your Google Play Console products
2. **Pricing**: Set up monthly/yearly pricing
3. **Trial Periods**: Configure free trials if desired
4. **Promotional Offers**: Set up introductory pricing

### Step 4: Configure Entitlements
1. Go to **Entitlements** in RevenueCat dashboard
2. Create or edit your entitlement (e.g., "premium")
3. Link it to your subscription products
4. Note the entitlement identifier (you'll need this in your code)

### Step 5: Test Your Paywall
1. **Preview**: Use the preview feature to see how it looks
2. **Test Mode**: Enable test mode for development
3. **Sandbox Testing**: Test with sandbox Google Play accounts

### Step 6: Deploy to Production
1. **Publish**: Make your paywall live
2. **A/B Testing**: Set up different paywall variants if desired
3. **Analytics**: Monitor conversion rates and user behavior

## Code Configuration

### Update Entitlement Identifier
In your Android code, update the entitlement identifier to match your RevenueCat setup:

```kotlin
// File: RevenueCatPaywall.kt
PaywallDialog(
    paywallDialogOptions = PaywallDialogOptions.Builder()
        .setRequiredEntitlementIdentifier("premium") // Replace with your actual entitlement ID
        .setListener(object : PaywallListener {
            override fun onPurchaseCompleted(customerInfo: CustomerInfo) {
                // Handle successful purchase
                revenueCatManager.onPurchaseCompleted()
                onDismiss()
            }
            
            override fun onRestoreCompleted(customerInfo: CustomerInfo) {
                // Handle successful restore
                revenueCatManager.onRestoreCompleted()
                onDismiss()
            }
            
            override fun onError(error: PurchasesError) {
                // Handle error
                println("RevenueCat Paywall Error: ${error.message}")
            }
        })
        .build()
)
```

### Common Entitlement Identifiers:
- `premium`
- `pro`
- `unlimited`
- `full_access`
- `posturely_premium`

## Best Practices

### Design Tips:
- **Keep it Simple**: Don't overwhelm users with too many options
- **Clear Value Prop**: Explain what users get with premium
- **Social Proof**: Include testimonials or user counts
- **Urgency**: Use limited-time offers sparingly
- **Mobile-First**: Design for mobile screens first

### Technical Tips:
- **Test Thoroughly**: Test on different devices and screen sizes
- **Handle Errors**: Implement proper error handling
- **Offline Support**: Consider offline scenarios
- **Analytics**: Track paywall performance
- **A/B Testing**: Test different designs and copy

## Troubleshooting

### Common Issues:
1. **Paywall Not Showing**: Check entitlement identifier matches dashboard
2. **Products Not Loading**: Verify Google Play Console integration
3. **Purchase Errors**: Check API key and product configuration
4. **Styling Issues**: Ensure images are optimized for mobile

### Debug Steps:
1. Check RevenueCat dashboard for errors
2. Verify API key is correct
3. Test with sandbox accounts
4. Check device logs for error messages
5. Ensure products are approved in Google Play Console

## Next Steps

After setting up your paywall:
1. **Test the Integration**: Build and test on Android device
2. **Monitor Performance**: Use RevenueCat analytics
3. **Iterate**: Make improvements based on user feedback
4. **Scale**: Consider A/B testing different designs

For more detailed information, visit the [RevenueCat Documentation](https://docs.revenuecat.com/).
