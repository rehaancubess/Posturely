package com.mobil80.posturely.audio

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberAudioPlayer(): AudioPlayer {
    val context = LocalContext.current
    return remember { AudioPlayer(context) }
}
