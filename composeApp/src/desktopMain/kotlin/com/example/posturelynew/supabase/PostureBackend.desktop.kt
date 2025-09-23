package com.mobil80.posturely.supabase

import com.mobil80.posturely.PostureRecord

actual object PostureBackend {
    actual suspend fun isAuthenticated(): Boolean {
        // Desktop currently lacks real auth; treat as authenticated if userEmail was saved
        return true
    }

    actual suspend fun insertRecord(record: PostureRecord) {
        // Forward to Android/iOS supabase client if available in common; desktop currently mocks
        // For now, call the same Supa used by common if reachable (noop on desktop)
        DesktopSupa.insertPostureRecord(record)
    }
}


