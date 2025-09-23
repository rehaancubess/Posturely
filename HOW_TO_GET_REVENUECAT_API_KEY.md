# How to Get Your RevenueCat API Key

## Step-by-Step Guide

### 1. Sign Up for RevenueCat
1. Go to [https://www.revenuecat.com/](https://www.revenuecat.com/)
2. Click "Get Started" or "Sign Up"
3. Create your account

### 2. Create a Project
1. After logging in, click "Create Project"
2. Enter your project name (e.g., "Posturely App")
3. Select "Android" as the platform
4. Click "Create Project"

### 3. Get Your API Key
1. In your RevenueCat dashboard, go to **Project Settings**
2. Click on **API Keys** tab
3. You'll see your **Public API Key** (starts with `rcb_` or similar)
4. Copy this key - you'll need it for your app

### 4. Configure Your App
Replace the placeholder in your `build.gradle.kts`:

```kotlin
defaultConfig {
    // Replace with your actual API key from RevenueCat dashboard
    buildConfigField("String", "REVENUECAT_API_KEY", "\"rcb_your_actual_key_here\"")
}
```

## Example API Key Format
RevenueCat API keys typically look like:
- `rcb_1234567890abcdef1234567890abcdef`
- `rcb_abcdef1234567890abcdef1234567890`

## Important Notes
- ✅ Use the **Public API Key** (safe for client-side apps)
- ❌ Never use the **Secret API Key** in your mobile app
- 🔒 Keep your API key secure and don't commit it to public repositories
- 🔄 You can regenerate API keys if needed from the dashboard

## Next Steps
After getting your API key:
1. Configure it in your build file
2. Set up your products and entitlements in RevenueCat dashboard
3. Test the paywall integration
4. Deploy to production

For detailed configuration options, see [REVENUECAT_API_KEY_SETUP.md](./REVENUECAT_API_KEY_SETUP.md)
