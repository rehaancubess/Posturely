package com.mobil80.posturely

import kotlinx.serialization.Serializable

@Serializable
data class PostureRecord(
    val user_email: String,
    val date: String,
    val time: String,
    val average_posture_score: Int,
    val tracking_source: String,
    val timestamp: Long,
    val samples_count: Int
)
