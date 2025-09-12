import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
}

repositories {
    google()
    mavenCentral()
}

kotlin {
    applyDefaultHierarchyTemplate()
    
    androidTarget()
    
    jvm("desktop")
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "ComposeApp"
            // Link AVFoundation framework for audio support
            linkerOpts("-framework", "AVFoundation")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                // KMP WebView for in-app web content
                implementation("io.github.shadmanadman:kwebview:1.60.0")
                
                // Supabase modules (v3)
                implementation("io.github.jan-tennert.supabase:supabase-kt:3.2.2")
                implementation("io.github.jan-tennert.supabase:auth-kt:3.2.2")
                implementation("io.github.jan-tennert.supabase:postgrest-kt:3.2.2")
                implementation("io.github.jan-tennert.supabase:realtime-kt:3.2.2")
                
                // Ktor dependencies (v3)
                implementation("io.ktor:ktor-client-core:3.2.2")
                
                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                
                // DateTime
                // Removed kotlinx-datetime - using our own DateTime object
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        
        val iosMain by getting {
            dependencies {
                // Ktor engine for iOS
                implementation("io.ktor:ktor-client-darwin:3.2.2")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.12.0")
                implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
                implementation("androidx.activity:activity-compose:1.8.2")
                implementation("androidx.camera:camera-core:1.3.1")
                implementation("androidx.camera:camera-camera2:1.3.1")
                implementation("androidx.camera:camera-lifecycle:1.3.1")
                implementation("androidx.camera:camera-view:1.3.1")
                implementation("com.google.mediapipe:tasks-vision:0.10.8")
                implementation("com.google.mediapipe:tasks-core:0.10.8")
                implementation("io.coil-kt:coil-compose:2.5.0")
                
                // Ktor engine for Android
                implementation("io.ktor:ktor-client-okhttp:3.2.2")
                
                // ExoPlayer for better audio handling
                implementation("androidx.media3:media3-exoplayer:1.3.1")
            }
        }
        
        val desktopMain by getting {
            dependencies {
                // Desktop-specific dependencies
                implementation(compose.desktop.currentOs)
                
                // Camera: bring FFmpeg + native presets
                implementation("org.bytedeco:javacv-platform:1.5.10")
                
                // MediaPipe for desktop (same as mobile)
                implementation("com.google.mediapipe:tasks-vision:0.10.8")
                implementation("com.google.mediapipe:tasks-core:0.10.8")

                // Ktor client for local WebSocket bridge
                implementation("io.ktor:ktor-client-cio:3.2.2")
                implementation("io.ktor:ktor-client-websockets:3.2.2")
                implementation("io.ktor:ktor-client-content-negotiation:3.2.2")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.2.2")
            }
        }
    }
}

android {
    namespace = "com.example.posturelynew"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.posturelynew"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.example.posturelynew.DesktopAppKt"
        
        from(kotlin.targets.getByName("desktop"))

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.example.posturelynew"
            packageVersion = "1.0.0"
            
            macOS {
                // Camera permissions will be handled by JavaCV
            }
        }
    }
}
