@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.posturelynew.audio

import kotlinx.cinterop.*
import platform.AVFAudio.*
import platform.Foundation.*
import platform.darwin.*
import kotlin.native.concurrent.ThreadLocal
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * iOS audio player with:
 * - Proper AVAudioSession setup
 * - Safe NSError** handling
 * - Strong references so AVAudioPlayer isn't GC'd
 * - Main-thread execution (AV APIs prefer main)
 * - Completion callbacks for proper sequencing
 */
actual class AudioPlayer {
    
    private var onSitStraightComplete: (() -> Unit)? = null
    private var sitStraightPlayer: AVAudioPlayer? = null
    private var beepPlayer: AVAudioPlayer? = null
    private var countdownTimer: dispatch_source_t? = null
    private var countdownNSTimer: NSTimer? = null

    @OptIn(ExperimentalResourceApi::class)
    actual fun playSound(id: Int) {
        val path = when (id) {
            0 -> "files/sitstraight.mp3"
            1 -> "files/countdown.mp3"
            2 -> "files/beep.mp3"
            else -> {
                println("❌ iOS: Invalid sound id: $id")
                return
            }
        }
        
        runOnMain {
            // 1) Prepare session
            if (!prepareSession()) {
                println("❌ iOS: Failed to prepare AVAudioSession")
                return@runOnMain
            }

            val url = urlForResourcePath(path)
            if (url == null) {
                println("❌ iOS: Resource not found: $path")
                return@runOnMain
            }
            println("🎵 iOS: Media item URL: ${url.absoluteString}")

            memScoped {
                val errVar = alloc<ObjCObjectVar<NSError?>>()
                val player = AVAudioPlayer(contentsOfURL = url, error = errVar.ptr)
                val err = errVar.value
                if (err != null) {
                    println("❌ iOS: AVAudioPlayer init error: ${err.domain} (${err.code}) ${err.localizedDescription}")
                    return@runOnMain
                }
                if (player == null) {
                    // Extremely rare, but guard anyway
                    println("❌ iOS: AVAudioPlayer is null after init")
                    return@runOnMain
                }

                player.numberOfLoops = 0
                player.prepareToPlay()

                // Set up completion delegate for this specific player
                if (id == 0) { // sitstraight.mp3
                    println("🎵 iOS: Setting up completion delegate for sitstraight.mp3")
                    sitStraightPlayer = player
                    player.setDelegate(object : NSObject(), AVAudioPlayerDelegateProtocol {
                        override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
                            println("🎵 iOS: sitstraight.mp3 delegate called - success: $successfully")
                            if (successfully) {
                                println("🎵 iOS: sitstraight.mp3 finished successfully, triggering callback")
                                // Trigger the completion callback
                                onSitStraightComplete?.invoke()
                            } else {
                                println("❌ iOS: sitstraight.mp3 failed to play")
                            }
                        }

                        override fun audioPlayerDecodeErrorDidOccur(player: AVAudioPlayer, error: NSError?) {
                            println("❌ iOS: Decode error for sitstraight.mp3: ${error?.localizedDescription}")
                        }
                    })
                    println("🎵 iOS: Completion delegate set successfully")
                }

                // Keep a strong reference so it doesn't get collected mid-play
                GlobalAudioPlayers.hold(player)

                val ok = player.play()
                if (!ok) {
                    println("❌ iOS: AVAudioPlayer.play() returned false")
                    GlobalAudioPlayers.release(player)
                    if (id == 0) sitStraightPlayer = null
                } else {
                    println("🎵 iOS: Sound started playing successfully")
                }
            }
        }
    }
    
    /**
     * Play the complete audio sequence: sitstraight -> wait 11 seconds -> countdown
     */
    @OptIn(ExperimentalResourceApi::class)
    actual fun playAudioSequence() {
        println("🎵 iOS: Starting audio sequence...")
        
        // Start with sitstraight
        println("🎵 iOS: Playing sitstraight.mp3...")
        playSound(0) // sitstraight.mp3
        
        // Wait 11 seconds, then play countdown
        println("🎵 iOS: Waiting 11 seconds before playing countdown...")
        // Use a cancellable NSTimer for simplicity
        countdownNSTimer?.invalidate()
        countdownNSTimer = NSTimer.scheduledTimerWithTimeInterval(11.0, repeats = false) { _ ->
            println("🎵 iOS: 11 seconds elapsed, playing countdown.mp3...")
            playSound(1) // countdown.mp3
            countdownNSTimer?.invalidate()
            countdownNSTimer = null
        }
    }
    
    /**
     * Play beep sound continuously until stopped
     */
    @OptIn(ExperimentalResourceApi::class)
    actual fun playBeepSound() {
        println("🔔 iOS: Starting beep sound...")
        
        runOnMain {
            // 1) Prepare session
            if (!prepareSession()) {
                println("❌ iOS: Failed to prepare AVAudioSession for beep")
                return@runOnMain
            }

            val url = urlForResourcePath("files/beep.mp3")
            if (url == null) {
                println("❌ iOS: Beep resource not found")
                return@runOnMain
            }
            println("🔔 iOS: Beep URL: ${url.absoluteString}")

            memScoped {
                val errVar = alloc<ObjCObjectVar<NSError?>>()
                val player = AVAudioPlayer(contentsOfURL = url, error = errVar.ptr)
                val err = errVar.value
                if (err != null) {
                    println("❌ iOS: Beep AVAudioPlayer init error: ${err.domain} (${err.code}) ${err.localizedDescription}")
                    return@runOnMain
                }
                if (player == null) {
                    println("❌ iOS: Beep AVAudioPlayer is null after init")
                    return@runOnMain
                }

                // Set to loop continuously
                player.numberOfLoops = -1 // -1 means infinite loop
                player.prepareToPlay()

                // Keep a strong reference
                GlobalAudioPlayers.hold(player)
                beepPlayer = player

                val ok = player.play()
                if (!ok) {
                    println("❌ iOS: Beep AVAudioPlayer.play() returned false")
                    GlobalAudioPlayers.release(player)
                    beepPlayer = null
                } else {
                    println("🔔 iOS: Beep sound started playing continuously")
                }
            }
        }
    }
    
    /**
     * Stop the beep sound
     */
    actual fun stopBeepSound() {
        println("🔔 iOS: Stopping beep sound...")
        
        runOnMain {
            beepPlayer?.let { player ->
                player.stop()
                GlobalAudioPlayers.release(player)
                beepPlayer = null
                println("🔔 iOS: Beep sound stopped")
            }
        }
    }

    actual fun stopAllSounds() {
        println("🎵 iOS: Stopping all sounds and pending timers...")
        runOnMain {
            countdownTimer?.let { dispatch_source_cancel(it) }
            countdownTimer = null
            countdownNSTimer?.invalidate()
            countdownNSTimer = null
            sitStraightPlayer?.stop()
            sitStraightPlayer = null
            beepPlayer?.let { player ->
                player.stop()
                beepPlayer = null
            }
            GlobalAudioPlayers.stopAll()
        }
    }

    actual fun release() {
        runOnMain {
            GlobalAudioPlayers.stopAll()
        }
        sitStraightPlayer = null
        beepPlayer = null
        onSitStraightComplete = null
        countdownTimer?.let { dispatch_source_cancel(it) }
        countdownTimer = null
        countdownNSTimer?.invalidate()
        countdownNSTimer = null
        println("🎵 iOS: AudioPlayer released")
    }

    // ---- Helpers ----

    private fun urlForResourcePath(path: String): NSURL? {
        val lastSlash = path.lastIndexOf('/')
        val fileOnly = if (lastSlash >= 0) path.substring(lastSlash + 1) else path
        val dot = fileOnly.lastIndexOf('.')
        val name = if (dot > 0) fileOnly.substring(0, dot) else fileOnly
        val ext = if (dot > 0 && dot < fileOnly.length - 1) fileOnly.substring(dot + 1) else null

        // Try standard bundle lookup by name/ext
        if (ext != null) {
            NSBundle.mainBundle.URLForResource(name, ext)?.let { return it }
        }

        // Fallback: search bundle for any mp3 matching filename
        if (ext != null) {
            val candidates = NSBundle.mainBundle.URLsForResourcesWithExtension(ext, subdirectory = null)
            val list = candidates as? List<*>
            if (list != null) {
                val targetLower = fileOnly.lowercase()
                for (obj in list) {
                    val url = obj as? NSURL ?: continue
                    val last = url.lastPathComponent ?: continue
                    if (last.lowercase() == targetLower) return url
                    // Also match by basename (beep.*)
                    val base = last.substringBeforeLast('.')
                    if (base.lowercase() == name.lowercase()) return url
                }
                // Debug: list found MP3s if exact match failed
                val names = list.mapNotNull { (it as? NSURL)?.lastPathComponent }
                println("🎵 iOS: Could not match $fileOnly. Bundle MP3s: ${names.joinToString()} ")
            }
        }

        // Final fallbacks: try direct bundle paths (both flattened and nested)
        val bundlePath = NSBundle.mainBundle.resourcePath ?: return null
        val flatPath = "$bundlePath/$fileOnly"
        val nestedPath = "$bundlePath/$path"
        val flatUrl = NSURL.fileURLWithPath(flatPath)
        if (NSFileManager.defaultManager.fileExistsAtPath(flatPath)) return flatUrl
        val nestedUrl = NSURL.fileURLWithPath(nestedPath)
        if (NSFileManager.defaultManager.fileExistsAtPath(nestedPath)) return nestedUrl

        return null
    }

    private fun prepareSession(): Boolean = memScoped {
        val session = AVAudioSession.sharedInstance()
        val errVar = alloc<ObjCObjectVar<NSError?>>()

        // Category: Playback (so it plays even with silent switch; tweak as you prefer)
        session.setCategory(AVAudioSessionCategoryPlayback, error = errVar.ptr)
        errVar.value?.let {
            println("⚠️ iOS: setCategory error: ${it.localizedDescription}")
            return false
        }

        // Route to speaker by default (optional)
        session.overrideOutputAudioPort(AVAudioSessionPortOverrideSpeaker, error = errVar.ptr)
        errVar.value?.let {
            // Not fatal if it fails; just log
            println("ℹ️ iOS: overrideOutputAudioPort error: ${it.localizedDescription}")
            errVar.value = null
        }

        // Activate session
        session.setActive(true, errVar.ptr)
        errVar.value?.let {
            println("⚠️ iOS: setActive error: ${it.localizedDescription}")
            return false
        }

        true
    }

    private inline fun runOnMain(crossinline block: () -> Unit) {
        if (NSThread.isMainThread) {
            block()
        } else {
            dispatch_async(dispatch_get_main_queue()) { block() }
        }
    }
}

/**
 * Holds strong references to players to prevent premature GC.
 * Also lets you stop / release all easily.
 */
@ThreadLocal
object GlobalAudioPlayers {
    private val players = mutableSetOf<AVAudioPlayer>()

    fun hold(p: AVAudioPlayer) {
        // Stop and release any finished players
        players.removeAll { !it.playing && it.currentTime > 0.0 }
        players.add(p)
    }

    fun release(p: AVAudioPlayer) {
        p.stop()
        players.remove(p)
    }

    fun stopAll() {
        players.forEach { it.stop() }
        players.clear()
    }
}
