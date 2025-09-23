package com.mobil80.posturely.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
actual class AudioPlayer(private val context: Context) {

    private val mediaPlayer = ExoPlayer.Builder(context).build()
    private val mediaItems = listOf(
        MediaItem.fromUri("android.resource://${context.packageName}/raw/sitstraight"),
        MediaItem.fromUri("android.resource://${context.packageName}/raw/countdown"),
        MediaItem.fromUri("android.resource://${context.packageName}/raw/beep")
    )
    
    private var beepPlayer: ExoPlayer? = null
    private var countdownPosted: Boolean = false

    init {
        mediaPlayer.prepare()
    }

    @OptIn(ExperimentalResourceApi::class)
    actual fun playSound(id: Int) {
        mediaPlayer.setMediaItem(mediaItems[id])
        mediaPlayer.play()
    }
    
    /**
     * Play the complete audio sequence: sitstraight -> wait 11 seconds -> countdown
     */
    @OptIn(ExperimentalResourceApi::class)
    actual fun playAudioSequence() {
        println("🎵 Android: Starting audio sequence...")
        
        // Play sitstraight first
        println("🎵 Android: Playing sitstraight.mp3...")
        playSound(0) // sitstraight.mp3
        
        // Wait 11 seconds, then play countdown
        println("🎵 Android: Waiting 11 seconds before playing countdown...")
        countdownPosted = true
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (countdownPosted) {
                println("🎵 Android: 11 seconds elapsed, playing countdown.mp3...")
                playSound(1) // countdown.mp3
            }
            countdownPosted = false
        }, 11000) // 11 seconds delay
    }

    actual fun playBeepSound() {
        println("🔔 Android: Starting beep sound...")
        
        // Create a separate player for beep to loop continuously
        beepPlayer = ExoPlayer.Builder(context).build()
        beepPlayer?.let { player ->
            player.setMediaItem(mediaItems[2]) // beep.mp3
            player.repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
            player.prepare()
            player.play()
            println("🔔 Android: Beep sound started playing continuously")
        }
    }
    
    actual fun stopBeepSound() {
        println("🔔 Android: Stopping beep sound...")
        
        beepPlayer?.let { player ->
            player.stop()
            player.release()
            beepPlayer = null
            println("🔔 Android: Beep sound stopped")
        }
    }

    actual fun release() {
        mediaPlayer.release()
        beepPlayer?.release()
        beepPlayer = null
    }

    actual fun stopAllSounds() {
        println("🎵 Android: Stopping all sounds and pending timers...")
        countdownPosted = false
        mediaPlayer.stop()
        beepPlayer?.let { it.stop() }
    }
}
