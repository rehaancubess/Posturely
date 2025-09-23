package com.mobil80.posturely

// Shared data class for pose metrics (used by both common and desktop code)
data class PoseMetrics(
    val torsoTilt: Double,
    val shoulderTilt: Double,
    val neckFlex: Double,
    val headZDelta: Double,
    val shoulderAsymY: Double
)

// Shared data class for score results
data class ScoreResult(
    val score: Int,
    val status: String
)
