package com.mobil80.posturely

import platform.Foundation.NSNotificationCenter

actual fun PlatformOpenAppLockConfig() {
    // Notify native Swift to open Screen Time configuration
    NSNotificationCenter.defaultCenter.postNotificationName(
        aName = "OpenAppLockConfig",
        `object` = null,
        userInfo = null
    )
}

actual fun PlatformStartAppLock(fromTime: String, tillTime: String, exercise: String) {
    // Notify native Swift to schedule shields via ScreenTime APIs
    NSNotificationCenter.defaultCenter.postNotificationName(
        aName = "StartAppLock",
        `object` = null,
        userInfo = mapOf("from" to fromTime, "till" to tillTime, "exercise" to exercise)
    )
}


