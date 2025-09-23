package com.mobil80.posturely.audio

import org.jetbrains.compose.resources.ExperimentalResourceApi
import java.io.File
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.DataLine
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.UnsupportedAudioFileException
import kotlin.concurrent.thread

/**
 * Desktop audio player using Java Sound API
 */
@OptIn(ExperimentalResourceApi::class)
actual class AudioPlayer {
    
    private var currentClip: Clip? = null
    private var beepClip: Clip? = null
    private var countdownThread: Thread? = null

    @OptIn(ExperimentalResourceApi::class)
    actual fun playSound(id: Int) {
        val filename = when (id) {
            0 -> "sitstraight.mp3"
            1 -> "countdown.mp3"
            2 -> "beep.mp3"
            else -> {
                println("❌ Desktop: Invalid sound id: $id")
                return
            }
        }
        
        println("🎵 Desktop: Playing $filename...")
        
        // For desktop, we'll try to find the audio files in the resources
        // Since we're on desktop, we'll use a simple approach
        try {
            // Try to play using Java Sound API
            playAudioFile(filename)
        } catch (e: Exception) {
            println("❌ Desktop: Error playing $filename: ${e.message}")
            // Fallback: just log that we would play it
            println("🎵 Desktop: Simulating audio playback for $filename")
        }
    }
    
    /**
     * Play the complete audio sequence: sitstraight -> wait 11 seconds -> countdown
     */
    @OptIn(ExperimentalResourceApi::class)
    actual fun playAudioSequence() {
        println("🎵 Desktop: Starting audio sequence...")
        
        // Play sitstraight first
        println("🎵 Desktop: Playing sitstraight.mp3...")
        playSound(0) // sitstraight.mp3
        
        // Wait 11 seconds, then play countdown
        println("🎵 Desktop: Waiting 11 seconds before playing countdown...")
        countdownThread = thread {
            try {
                Thread.sleep(11000) // 11 seconds delay
                println("🎵 Desktop: 11 seconds elapsed, playing countdown.mp3...")
                playSound(1) // countdown.mp3
            } catch (_: InterruptedException) {
                println("🎵 Desktop: Countdown thread interrupted, aborting countdown play")
            } finally {
                countdownThread = null
            }
        }
    }

    actual fun playBeepSound() {
        println("🔔 Desktop: Starting beep sound...")
        
        try {
            val audioFile = findAudioFile("beep.mp3")
            if (audioFile != null) {
                val audioStream = AudioSystem.getAudioInputStream(audioFile)
                val format = audioStream.format
                val info = DataLine.Info(Clip::class.java, format)
                
                if (AudioSystem.isLineSupported(info)) {
                    val clip = AudioSystem.getLine(info) as Clip
                    clip.open(audioStream)
                    clip.loop(Clip.LOOP_CONTINUOUSLY) // Loop continuously
                    clip.start()
                    
                    // Store reference to prevent garbage collection
                    beepClip = clip
                    
                    println("🔔 Desktop: Beep sound started playing continuously")
                } else {
                    println("❌ Desktop: Line not supported for beep.mp3")
                }
            } else {
                println("❌ Desktop: Beep audio file not found")
            }
        } catch (e: Exception) {
            println("❌ Desktop: Error playing beep sound: ${e.message}")
        }
    }
    
    actual fun stopBeepSound() {
        println("🔔 Desktop: Stopping beep sound...")
        
        beepClip?.let { clip ->
            clip.stop()
            clip.close()
            beepClip = null
            println("🔔 Desktop: Beep sound stopped")
        }
    }

    actual fun release() {
        currentClip?.close()
        currentClip = null
        beepClip?.close()
        beepClip = null
        countdownThread?.interrupt()
        countdownThread = null
        println("🎵 Desktop: AudioPlayer released")
    }

    actual fun stopAllSounds() {
        println("🎵 Desktop: Stopping all sounds and pending timers...")
        currentClip?.stop(); currentClip?.close(); currentClip = null
        beepClip?.stop(); beepClip?.close(); beepClip = null
        countdownThread?.interrupt(); countdownThread = null
    }
    
    private fun playAudioFile(filename: String) {
        try {
            // Try to find the audio file in the classpath or resources
            val audioFile = findAudioFile(filename)
            if (audioFile != null) {
                // Use system audio player instead of Java Sound API for MP3 support
                val os = System.getProperty("os.name").lowercase()
                val command = when {
                    os.contains("mac") -> arrayOf("afplay", audioFile.absolutePath)
                    os.contains("win") -> arrayOf("powershell", "-c", "(New-Object Media.SoundPlayer '$audioFile').PlaySync();")
                    else -> arrayOf("aplay", audioFile.absolutePath) // Linux
                }
                
                println("🎵 Desktop: Playing $filename using system audio player: ${command.joinToString(" ")}")
                
                val process = ProcessBuilder(*command).start()
                
                // Don't wait for completion - let it play in background
                println("🎵 Desktop: Audio started playing successfully")
                
            } else {
                println("❌ Desktop: Audio file not found: $filename")
            }
        } catch (e: Exception) {
            println("❌ Desktop: Error playing $filename: ${e.message}")
            // Fallback: just log that we would play it
            println("🎵 Desktop: Simulating audio playback for $filename")
        }
    }
    
    private fun findAudioFile(filename: String): File? {
        // Try to find the audio file in various locations
        val possiblePaths = listOf(
            "composeApp/build/processedResources/desktop/main/files/$filename",
            "composeApp/src/commonMain/resources/files/$filename",
            "src/commonMain/resources/files/$filename",
            "build/processedResources/desktop/main/files/$filename",
            filename
        )
        
        for (path in possiblePaths) {
            val file = File(path)
            if (file.exists()) {
                println("🎵 Desktop: Found audio file at: ${file.absolutePath}")
                return file
            }
        }
        
        // Try absolute paths from current working directory
        val currentDir = System.getProperty("user.dir")
        val absolutePaths = listOf(
            "$currentDir/composeApp/build/processedResources/desktop/main/files/$filename",
            "$currentDir/build/processedResources/desktop/main/files/$filename"
        )
        
        for (path in absolutePaths) {
            val file = File(path)
            if (file.exists()) {
                println("🎵 Desktop: Found audio file at absolute path: ${file.absolutePath}")
                return file
            }
        }
        
        println("❌ Desktop: Audio file not found in any location: $filename")
        println("❌ Desktop: Current working directory: $currentDir")
        return null
    }
}
