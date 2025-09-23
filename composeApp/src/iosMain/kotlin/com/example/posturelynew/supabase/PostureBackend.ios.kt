package com.mobil80.posturely.supabase

import com.mobil80.posturely.PostureRecord

actual object PostureBackend {
    actual suspend fun isAuthenticated(): Boolean {
        return try { Supa.isUserSignedIn() } catch (_: Exception) { false }
    }

    actual suspend fun insertRecord(record: PostureRecord) {
        Supa.insertPostureRecord(record)
    }
}


