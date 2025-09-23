package com.mobil80.posturely.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberAudioPlayer(): AudioPlayer {
    return remember { AudioPlayer() }
}
