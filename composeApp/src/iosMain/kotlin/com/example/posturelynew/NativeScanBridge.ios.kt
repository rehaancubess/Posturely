package com.example.posturelynew

import platform.Foundation.NSNotificationCenter

actual fun openNativeScanCamera() {
    // Notify Swift to present the native camera overlay controller
    NSNotificationCenter.defaultCenter.postNotificationName(
        aName = "PresentScanCamera",
        `object` = null
    )
}


