package com.example.posturelynew.home

import com.example.posturelynew.PoseMetrics

// Helper function to calculate real metrics from landmarks (imported from LiveTrackingScreen logic)
internal fun calculateRealMetrics(landmarks: List<Pair<Float, Float>>): PoseMetrics {
    if (landmarks.size < 33) return PoseMetrics(0.0, 0.0, 0.0, 0.0, 0.0)
    
    // Extract key landmarks (using MediaPipe pose landmark indices)
    val nose = landmarks[0]        // Landmark 0: Nose
    val leftShoulder = landmarks[11]  // Landmark 11: Left Shoulder
    val rightShoulder = landmarks[12] // Landmark 12: Right Shoulder
    val leftHip = landmarks[23]    // Landmark 23: Left Hip
    val rightHip = landmarks[24]   // Landmark 24: Right Hip
    
    // Calculate torso tilt (angle from vertical)
    val torsoCenterX = (leftHip.first + rightHip.first) / 2
    val torsoCenterY = (leftHip.second + rightHip.second) / 2
    val shoulderCenterX = (leftShoulder.first + rightShoulder.first) / 2
    val shoulderCenterY = (leftShoulder.second + rightShoulder.second) / 2
    
    val torsoTilt = calculateAngleFromVertical(
        shoulderCenterX, shoulderCenterY,
        torsoCenterX, torsoCenterY
    )
    
    // Calculate shoulder tilt (left vs right shoulder height difference)
    val shoulderTilt = calculateShoulderTilt(leftShoulder, rightShoulder)
    
    // Calculate neck flexion (head forward position)
    val headZDelta = (nose.second - shoulderCenterY).toDouble()
    
    // Calculate neck flexion angle
    val neckFlex = calculateAngleFromVertical(
        nose.first, nose.second,
        shoulderCenterX, shoulderCenterY
    )
    
    // Shoulder symmetry (height difference)
    val shoulderAsymY = kotlin.math.abs((leftShoulder.second - rightShoulder.second).toDouble())
    
    return PoseMetrics(
        torsoTilt = torsoTilt,
        shoulderTilt = shoulderTilt,
        neckFlex = neckFlex,
        headZDelta = headZDelta,
        shoulderAsymY = shoulderAsymY
    )
}

// Helper function to calculate angle from vertical
internal fun calculateAngleFromVertical(x1: Float, y1: Float, x2: Float, y2: Float): Double {
    val dx = (x2 - x1).toDouble()
    val dy = (y2 - y1).toDouble()
    val angle = kotlin.math.atan2(dx, dy) * 180.0 / kotlin.math.PI
    return kotlin.math.abs(angle)
}

// Helper function to calculate shoulder tilt
internal fun calculateShoulderTilt(leftShoulder: Pair<Float, Float>, rightShoulder: Pair<Float, Float>): Double {
    val heightDiff = (leftShoulder.second - rightShoulder.second).toDouble()
    return kotlin.math.abs(heightDiff) * 100.0 // Scale for better visibility
}

// Score calculation function
internal data class ScoreResult(val score: Int, val flags: List<String>)

internal fun calculatePostureScore(
    metrics: PoseMetrics,
    calibratedThresholds: Map<String, Double>?
): ScoreResult {
    // Use calibrated thresholds if available, otherwise use defaults
    val thresholds = calibratedThresholds ?: mapOf(
        "torsoTilt" to 10.0,
        "shoulderTilt" to 7.0,
        "neckFlex" to 12.0,
        "headZDelta" to -0.05,
        "shoulderAsymY" to 0.03
    )
    
    var score = 100
    val flags = mutableListOf<String>()
    
    // Apply penalties based on thresholds
    if (metrics.torsoTilt > thresholds["torsoTilt"]!!) {
        val penalty = ((metrics.torsoTilt - thresholds["torsoTilt"]!!) / 20.0 * 25).coerceAtMost(25.0)
        score -= penalty.toInt()
        flags.add("Torso tilt")
    }
    
    if (metrics.shoulderTilt > thresholds["shoulderTilt"]!!) {
        val penalty = ((metrics.shoulderTilt - thresholds["shoulderTilt"]!!) / 20.0 * 15).coerceAtMost(15.0)
        score -= penalty.toInt()
        flags.add("Shoulder tilt")
    }
    
    if (metrics.neckFlex > thresholds["neckFlex"]!!) {
        val penalty = ((metrics.neckFlex - thresholds["neckFlex"]!!) / 20.0 * 35).coerceAtMost(35.0)
        score -= penalty.toInt()
        flags.add("Neck flexion")
    }
    
    if (metrics.headZDelta < thresholds["headZDelta"]!!) {
        val penalty = ((thresholds["headZDelta"]!! - metrics.headZDelta) / 0.10 * 45).coerceAtMost(45.0)
        score -= penalty.toInt()
        flags.add("Forward head")
    }
    
    if (metrics.shoulderAsymY > thresholds["shoulderAsymY"]!!) {
        val penalty = ((metrics.shoulderAsymY - thresholds["shoulderAsymY"]!!) / 0.10 * 20).coerceAtMost(20.0)
        score -= penalty.toInt()
        flags.add("Shoulder asymmetry")
    }
    
    return ScoreResult(score.coerceIn(0, 100), flags)
}

// Smoothing function for score 
internal fun smoothScore(newScore: Int, previousSmoothedScore: Int): Int {
    if (previousSmoothedScore == 0) return newScore
    
    val weight = 0.3 // 30% new data, 70% previous
    val smoothed = ((newScore * weight) + (previousSmoothedScore * (1 - weight))).toInt()
    return smoothed.coerceIn(0, 100)
}
