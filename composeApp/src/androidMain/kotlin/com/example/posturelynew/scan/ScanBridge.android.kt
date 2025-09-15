package com.example.posturelynew.scan

actual suspend fun postPostureReport(scanId: String, frontBase64: String, sideBase64: String): Result<Unit> {
    // TODO: Implement if Android native capture is added; for now return success
    return Result.success(Unit)
}


