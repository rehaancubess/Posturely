package com.example.posturelynew.scan

// Desktop stub implementation - no-op since posture reporting is not needed on desktop
actual suspend fun postPostureReport(scanId: String, frontBase64: String, sideBase64: String): Result<Unit> {
    // Return success for desktop - no actual posting needed
    return Result.success(Unit)
}
