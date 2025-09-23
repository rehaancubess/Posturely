package com.mobil80.posturely.scan

actual suspend fun postPostureReport(scanId: String, frontBase64: String, sideBase64: String): Result<Unit> {
    // TODO: Implement if needed; for now return success since we're using the Compose-based approach
    return Result.success(Unit)
}
