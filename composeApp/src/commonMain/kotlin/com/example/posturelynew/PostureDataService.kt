package com.example.posturelynew

import com.example.posturelynew.supabase.Supa
import com.example.posturelynew.PostureRecord
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.ExperimentalTime

/**
 * Service to record posture data to Supabase every minute during phone tracking
 */
@OptIn(ExperimentalTime::class)
class PostureDataService {
    private var isRecording = false
    private var recordingJob: Job? = null
    private var userEmail: String = ""
    private var trackingSource: String = "phone" // Default to phone
    
    // Callback for progress refresh
    private var onProgressUpdate: (() -> Unit)? = null
    
    // No counter needed - using real datetime
    
    // Track scores for the current minute
    private val scoresForCurrentMinute = mutableListOf<Int>()
    private var lastMinuteRecorded = -1
    
    // State flows for UI updates
    private val _isRecordingFlow = MutableStateFlow(false)
    val isRecordingFlow: StateFlow<Boolean> = _isRecordingFlow.asStateFlow()
    
    private val _lastRecordedScore = MutableStateFlow(0)
    val lastRecordedScore: StateFlow<Int> = _lastRecordedScore.asStateFlow()
    
    private val _lastRecordedTime = MutableStateFlow("")
    val lastRecordedTime: StateFlow<String> = _lastRecordedTime.asStateFlow()
    
    /**
     * Set callback for progress updates
     */
    fun setProgressUpdateCallback(callback: () -> Unit) {
        onProgressUpdate = callback
    }
    
    /**
     * Start recording posture data every minute
     */
    fun startRecording(email: String, source: String = "phone") {
        println("🔍 PostureDataService: startRecording called with email='$email', source='$source', isRecording=$isRecording")
        if (isRecording) {
            println("⚠️ PostureDataService: Already recording, ignoring start request")
            return
        }
        
        userEmail = email
        trackingSource = when (source.lowercase()) {
            "phone", "laptop", "airpods" -> source.lowercase()
            else -> "phone" // Default fallback
        }
        isRecording = true
        _isRecordingFlow.value = true
        scoresForCurrentMinute.clear()
        lastMinuteRecorded = -1
        
        println("📊 PostureDataService: Started recording for user: $email with source: $trackingSource")
        
        recordingJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            println("🔍 PostureDataService: Recording loop started")
            var lastRecordedMinute = -1
            
            while (isRecording) {
                try {
                    // Get current minute using our DateTime object
                    val currentTimeMillis = DateTime.getCurrentTimeInMilliSeconds()
                    val currentDateTimeStr = DateTime.formatTimeStamp(currentTimeMillis, "yyyy-MM-dd HH:mm:ss")
                    val currentMinute = currentDateTimeStr.substring(14, 16).toInt()
                    
                    // Check if we've moved to a new minute and have scores to record
                    if (currentMinute != lastRecordedMinute && scoresForCurrentMinute.isNotEmpty()) {
                        // Record the average score for the previous minute
                        val averageScore = scoresForCurrentMinute.average().toInt()
                        val sampleCount = scoresForCurrentMinute.size
                        println("🔍 PostureDataService: Recording minute $lastRecordedMinute with $averageScore average from $sampleCount scores")
                        
                        // Record the data
                            recordPostureData(averageScore)
                        
                        // Reset for new minute
                        scoresForCurrentMinute.clear()
                        lastRecordedMinute = currentMinute
                    }
                    
                    delay(1000) // Check every second
                } catch (e: Exception) {
                    println("❌ PostureDataService: Error in recording loop: ${e.message}")
                    delay(5000) // Wait 5 seconds before retrying
                }
            }
        }
    }
    
    /**
     * Stop recording posture data
     */
    fun stopRecording() {
        if (!isRecording) return
        
        isRecording = false
        _isRecordingFlow.value = false
        recordingJob?.cancel()
        recordingJob = null
        
        // Record any remaining scores for the current minute
        if (scoresForCurrentMinute.isNotEmpty()) {
            val averageScore = scoresForCurrentMinute.average().toInt()
            CoroutineScope(Dispatchers.IO).launch {
                recordPostureData(averageScore)
            }
        }
        
        println("📊 PostureDataService: Stopped recording")
    }
    
    /**
     * Add a new posture score to the current minute's tracking
     */
    fun addPostureScore(score: Int) {
        if (!isRecording || score <= 0) {
            println("⚠️ PostureDataService: Not recording or invalid score ($score), isRecording=$isRecording")
            return
        }
        
        scoresForCurrentMinute.add(score)
        println("📊 PostureDataService: Added score $score, total scores: ${scoresForCurrentMinute.size}")
        
        // Keep only the last 60 scores (1 minute at 1 score per second)
        if (scoresForCurrentMinute.size > 60) {
            scoresForCurrentMinute.removeAt(0)
        }
    }
    
    /**
     * Record posture data to Supabase
     */
    private suspend fun recordPostureData(averageScore: Int) {
        try {
            // Get current date and time using our DateTime object
            val currentTimeMillis = DateTime.getCurrentTimeInMilliSeconds()
            println("🔍 PostureDataService: currentTimeMillis=$currentTimeMillis")
            val currentDateTimeStr = DateTime.formatTimeStamp(currentTimeMillis, "yyyy-MM-dd HH:mm:ss")
            val currentDate = DateTime.formatTimeStamp(currentTimeMillis, "yyyy-MM-dd")
            val currentTime = DateTime.formatTimeStamp(currentTimeMillis, "HH:mm:ss")
            val timestamp = currentTimeMillis
            println("🔍 PostureDataService: currentDate=$currentDate, currentTime=$currentTime")
            
            // Store the sample count before clearing the list
            val sampleCount = scoresForCurrentMinute.size
            
            val record = PostureRecord(
                user_email = userEmail,
                date = currentDate,
                time = currentTime,
                average_posture_score = averageScore,
                tracking_source = trackingSource,
                timestamp = timestamp,
                samples_count = sampleCount
            )
            
            // Log what we're about to send to Supabase
            println("📤 PostureDataService: Sending to Supabase:")
            println("   - user_email: ${record.user_email}")
            println("   - date: ${record.date}")
            println("   - time: ${record.time}")
            println("   - average_posture_score: ${record.average_posture_score}")
            println("   - tracking_source: ${record.tracking_source}")
            println("   - timestamp: ${record.timestamp}")
            println("   - samples_count: ${record.samples_count}")
            
            // Insert into Supabase
            try {
                // Check if user is authenticated (RLS requires this)
                // Add a longer delay to ensure session is loaded from storage
                kotlinx.coroutines.delay(500)
                
                // Try multiple times to get the session
                var session = Supa.getCurrentSession()
                var attempts = 0
                while (session == null && attempts < 3) {
                    println("🔍 PostureDataService: Session attempt ${attempts + 1}/3 - session=${session != null}")
                    kotlinx.coroutines.delay(200)
                    session = Supa.getCurrentSession()
                    attempts++
                }
                
                println("🔍 PostureDataService: Final authentication check - session=${session != null}")
                if (session == null) {
                    println("⚠️ PostureDataService: User not authenticated after multiple attempts, cannot save to Supabase")
                    println("📊 PostureDataService: Recorded score $averageScore (local only - not authenticated)")
                    return
                }
                println("✅ PostureDataService: User authenticated, proceeding with Supabase insertion")
                println("🔍 PostureDataService: Session user: ${session.user?.email ?: "unknown"}")
                
                // Add retry logic for network timeouts
                var retryCount = 0
                val maxRetries = 3
                var success = false
                
                while (!success && retryCount < maxRetries) {
                    try {
                        Supa.insertPostureRecord(record)
                        println("✅ PostureDataService: Successfully inserted into Supabase")
                        success = true
                        
                        // Trigger progress update
                        onProgressUpdate?.invoke()
                        println("🔄 PostureDataService: Progress update triggered after successful Supabase insertion")
                    } catch (e: Exception) {
                        retryCount++
                        println("❌ PostureDataService: Attempt $retryCount/$maxRetries failed: ${e.message}")
                        println("❌ PostureDataService: Error type: ${e::class.simpleName}")
                        
                        if (retryCount < maxRetries) {
                            println("🔄 PostureDataService: Retrying in 2 seconds...")
                            kotlinx.coroutines.delay(2000)
                        } else {
                            println("❌ PostureDataService: All retry attempts failed")
                            println("📊 PostureDataService: Recorded score $averageScore (local only due to Supabase error)")
                        }
                    }
                }
            } catch (e: Exception) {
                println("❌ PostureDataService: Failed to insert into Supabase: ${e.message}")
                println("❌ PostureDataService: Error type: ${e::class.simpleName}")
                // Continue with local logging even if Supabase fails
                println("📊 PostureDataService: Recorded score $averageScore (local only due to Supabase error)")
            }
            
            // Update UI state
            _lastRecordedScore.value = averageScore
            _lastRecordedTime.value = "${record.date} ${record.time}"
            
        } catch (e: Exception) {
            println("❌ PostureDataService: Failed to record posture data: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Get recording status
     */
    fun isRecording(): Boolean = isRecording
    
    /**
     * Get current minute's average score
     */
    fun getCurrentMinuteAverage(): Int {
        return if (scoresForCurrentMinute.isNotEmpty()) {
            scoresForCurrentMinute.average().toInt()
        } else {
            0
        }
    }
    
    /**
     * Get current minute's sample count
     */
    fun getCurrentMinuteSampleCount(): Int = scoresForCurrentMinute.size
}
