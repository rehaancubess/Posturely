package com.mobil80.posturely.supabase

import com.mobil80.posturely.PostureRecord

// Cross-platform bridge to insert posture records using the correct client per target
expect object PostureBackend {
    suspend fun isAuthenticated(): Boolean
    suspend fun insertRecord(record: PostureRecord)
}


