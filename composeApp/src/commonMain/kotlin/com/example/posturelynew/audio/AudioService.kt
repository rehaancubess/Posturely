package com.example.posturelynew.audio

expect class AudioPlayer {
    /** Plays a sound by id (see soundResList) */
    fun playSound(id: Int)
    
    /** Plays the complete audio sequence: sitstraight -> wait 1 second -> countdown */
    fun playAudioSequence()
    
    /** Plays beep sound continuously until stopped */
    fun playBeepSound()
    
    /** Stops the beep sound */
    fun stopBeepSound()

    /** Stops any ongoing sounds and cancels pending sequence playback */
    fun stopAllSounds()
    
    fun release()
}

val soundResList = listOf(
    "files/sitstraight.mp3",
    "files/countdown.mp3",
    "files/beep.mp3"
)
