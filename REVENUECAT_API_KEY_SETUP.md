# RevenueCat API Key Configuration Guide

## Where to Put Your RevenueCat API Key

You have several options for configuring your RevenueCat API key. Here are the recommended approaches:

## Option 1: Build Configuration (Recommended for Development)

### Step 1: Get Your API Key
1. Go to [RevenueCat Dashboard](https://app.revenuecat.com/)
2. Navigate to **Project Settings** → **API Keys**
3. Copy your **Public API Key** (starts with `rcb_` or similar)

### Step 2: Configure in Build File
**File**: `composeApp/build.gradle.kts`

Replace the placeholder in the `defaultConfig` block:

```kotlin
defaultConfig {
    applicationId = "com.mobil80.posturely"
    minSdk = libs.versions.android.minSdk.get().toInt()
    targetSdk = libs.versions.android.targetSdk.get().toInt()
    versionCode = 1
    versionName = "1.0"
    
    // Replace with your actual RevenueCat API key
    buildConfigField("String", "REVENUECAT_API_KEY", "\"rcb_your_actual_api_key_here\"")
}
```

## Option 2: Environment Variables (Recommended for Production)

### For Local Development
Create a `.env` file in your project root (add to `.gitignore`):

```bash
# .env file
REVENUECAT_API_KEY=rcb_your_actual_api_key_here
```

### For CI/CD (GitHub Actions, etc.)
Set the environment variable in your CI/CD pipeline:

```yaml
# GitHub Actions example
env:
  REVENUECAT_API_KEY: ${{ secrets.REVENUECAT_API_KEY }}
```

## Option 3: Properties File (Alternative)

Create a `revenuecat.properties` file in your project root:

```properties
# revenuecat.properties
revenuecat.api.key=rcb_your_actual_api_key_here
```

Then modify the build.gradle.kts to read from this file:

```kotlin
// Load RevenueCat API key from properties file
val revenueCatProps = Properties().apply {
    val propsFile = rootProject.file("revenuecat.properties")
    if (propsFile.exists()) {
        load(FileInputStream(propsFile))
    }
}

defaultConfig {
    // ... other config
    buildConfigField("String", "REVENUECAT_API_KEY", "\"${revenueCatProps.getProperty("revenuecat.api.key", "your_api_key_here")}\"")
}
```

## Security Best Practices

### ✅ DO:
- Use environment variables for production builds
- Add sensitive files to `.gitignore`
- Use different API keys for development/production
- Rotate API keys regularly

### ❌ DON'T:
- Commit API keys to version control
- Use the same API key for development and production
- Hardcode API keys in source code
- Share API keys in public repositories

## Verification

After setting up your API key, you can verify it's working by:

1. **Build the project**: `./gradlew assembleDebug`
2. **Check logs**: Look for RevenueCat initialization messages
3. **Test paywall**: The paywall should appear on Android home screen

## Troubleshooting

### Common Issues:

1. **"Invalid API Key" Error**
   - Verify you're using the correct Public API Key
   - Check for extra spaces or quotes in the configuration

2. **Paywall Not Showing**
   - Ensure the API key is correctly configured
   - Check that you're testing on Android
   - Verify the entitlement identifier matches your RevenueCat dashboard

3. **Build Errors**
   - Make sure `buildFeatures { buildConfig = true }` is enabled
   - Clean and rebuild: `./gradlew clean build`

## Example Configuration

Here's a complete example of how your configuration should look:

**build.gradle.kts**:
```kotlin
android {
    buildFeatures {
        compose = true
        buildConfig = true  // Enable BuildConfig
    }
    
    defaultConfig {
        // ... other config
        buildConfigField("String", "REVENUECAT_API_KEY", "\"rcb_1234567890abcdef\"")
    }
}
```

**AndroidRevenueCatManager.kt**:
```kotlin
class AndroidRevenueCatManager(
    private val context: Context,
    private val apiKey: String = getRevenueCatApiKey()
) : RevenueCatManager {
    
    companion object {
        private fun getRevenueCatApiKey(): String {
            // Try environment variable first
            val envKey = System.getenv("REVENUECAT_API_KEY")
            if (!envKey.isNullOrBlank()) {
                return envKey
            }
            
            // Fallback to build config
            return BuildConfig.REVENUECAT_API_KEY ?: "your_api_key_here"
        }
    }
    // ... rest of implementation
}
```

This setup provides flexibility for different deployment scenarios while maintaining security best practices.
