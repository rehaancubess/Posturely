package com.example.posturelynew

import com.example.posturelynew.supabase.Supa
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProgressData(
    val totalMinutes: Int = 0,
    val averageScore: Int = 0,
    val totalSamples: Int = 0,
    val bestScore: Int = 0,
    val worstScore: Int = 100,
    val goodMinutes: Int = 0,
    val okMinutes: Int = 0,
    val badMinutes: Int = 0,
    // Weekly breakdown for charts (yyyy-MM-dd -> minutes)
    val weekDays: Map<String, Int> = emptyMap(),
    val weekScores: Map<String, Int> = emptyMap(),
    val weekOrder: List<String> = emptyList(),
    // Monthly breakdown for charts (W1, W2, etc. -> minutes)
    val monthWeeks: Map<String, Int> = emptyMap(),
    val monthWeekScores: Map<String, Int> = emptyMap(),
    val monthWeekOrder: List<String> = emptyList(),
    // Yearly breakdown for charts (J, F, M, etc. -> minutes)
    val yearMonths: Map<String, Int> = emptyMap(),
    val yearMonthScores: Map<String, Int> = emptyMap(),
    val yearMonthOrder: List<String> = emptyList()
)

class ProgressService {
    private var progressJob: Job? = null
    private var userEmail: String = ""
    
    private val _progressData = MutableStateFlow(ProgressData())
    val progressData: StateFlow<ProgressData> = _progressData.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    fun startProgressTracking(email: String) {
        userEmail = email
        println("📊 ProgressService: Starting progress tracking for $email")
        
        progressJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            while (true) {
                try {
                    refreshProgress()
                    delay(120000) // Refresh every 2 minutes (reduced frequency)
                } catch (e: Exception) {
                    println("❌ ProgressService: Error refreshing progress: ${e.message}")
                    delay(300000) // Wait 5 minutes before retrying on error
                }
            }
        }
    }
    
    suspend fun refreshProgress() {
        if (userEmail.isEmpty()) return
        
        try {
            _isLoading.value = true
            println("🔄 ProgressService: Refreshing progress data for $userEmail...")
            
            // Use withTimeout to prevent hanging requests
            val todayMap = withTimeout(15000) { Supa.getTodaysProgress(userEmail) }
            val weekMap = withTimeout(15000) { Supa.getWeeksProgress(userEmail) }
            val monthMap = withTimeout(15000) { Supa.getMonthsProgress(userEmail) }
            val yearMap = withTimeout(15000) { Supa.getYearsProgress(userEmail) }
            
            val progressData = ProgressData(
                totalMinutes = weekMap["totalMinutes"] as? Int ?: 0,
                averageScore = weekMap["averageScore"] as? Int ?: 0,
                totalSamples = todayMap["totalSamples"] as? Int ?: 0,
                bestScore = todayMap["bestScore"] as? Int ?: 0,
                worstScore = todayMap["worstScore"] as? Int ?: 100,
                goodMinutes = todayMap["goodMinutes"] as? Int ?: 0,
                okMinutes = todayMap["okMinutes"] as? Int ?: 0,
                badMinutes = todayMap["badMinutes"] as? Int ?: 0,
                weekDays = (weekMap["days"] as? Map<String, Int>) ?: emptyMap(),
                weekScores = (weekMap["scores"] as? Map<String, Int>) ?: emptyMap(),
                weekOrder = (weekMap["dateOrder"] as? List<String>) ?: emptyList(),
                monthWeeks = (monthMap["weeks"] as? Map<String, Int>) ?: emptyMap(),
                monthWeekScores = (monthMap["scores"] as? Map<String, Int>) ?: emptyMap(),
                monthWeekOrder = (monthMap["weekOrder"] as? List<String>) ?: emptyList(),
                yearMonths = (yearMap["months"] as? Map<String, Int>) ?: emptyMap(),
                yearMonthScores = (yearMap["scores"] as? Map<String, Int>) ?: emptyMap(),
                yearMonthOrder = (yearMap["monthOrder"] as? List<String>) ?: emptyList()
            )
            
            _progressData.value = progressData
            println("✅ ProgressService: Progress updated - ${progressData.totalMinutes} minutes, avg: ${progressData.averageScore}, samples: ${progressData.totalSamples}")
            
        } catch (e: Exception) {
            println("❌ ProgressService: Failed to refresh progress: ${e.message}")
            // Don't update data on error, keep existing data
        } finally {
            _isLoading.value = false
        }
    }
    
    fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
        println("📊 ProgressService: Stopped progress tracking")
    }
    
    fun getCurrentProgress(): ProgressData = _progressData.value
}
