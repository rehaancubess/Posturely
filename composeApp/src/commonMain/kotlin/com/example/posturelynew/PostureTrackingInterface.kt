package com.example.posturelynew

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// Interface for posture tracking functionality
interface PostureTrackingInterface {
    fun startTracking()
    fun stopTracking()
    fun isTracking(): Boolean
    fun getPoseData(): String
    
    // Methods to get landmark data
    fun getLandmarkCount(): Int
    fun getLandmarkX(index: Int): Float
    fun getLandmarkY(index: Int): Float
}

// Platform-specific implementations will be provided in platform-specific files
expect fun createPostureTrackingInterface(): PostureTrackingInterface

// Composable function to get posture tracking interface
@Composable
fun rememberPostureTrackingInterface(): PostureTrackingInterface {
    return remember { createPostureTrackingInterface() }
} 