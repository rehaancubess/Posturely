package com.mobil80.posturely

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController {
    // Touch singletons so their init blocks register native observers
    NativeScanImagesBridge
    NativeSupabaseBridge
    App()
}