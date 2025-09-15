package com.example.posturelynew

import platform.Foundation.NSNotificationCenter
import com.example.posturelynew.scan.ScanImagesBridge

object NativeScanImagesBridge {
    init {
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = "ScanImagesReady",
            `object` = null,
            queue = null
        ) { notif ->
            val info = notif?.userInfo
            val scanId = info?.get("scanId") as? String ?: return@addObserverForName
            val front = info.get("frontBase64") as? String ?: ""
            val side = info.get("sideBase64") as? String ?: ""
            // Forward to common bridge used by Compose UI
            ScanImagesBridge.onImagesReady?.invoke(scanId, front, side)
        }
    }
}

