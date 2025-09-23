package com.mobil80.posturely

import com.mobil80.posturely.supabase.Supa
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProgressData(
    val totalMinutes: Int = 0,
    val averageScore: Int = 0,
    // Week aggregates
    val weekTotalMinutes: Int = 0,
    val weekAverageScore: Int = 0,
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
            
            // Robust coercion helpers to avoid platform-specific type issues
            fun anyToInt(value: Any?): Int = when (value) {
                is Int -> value
                is Long -> value.toInt()
                is Double -> value.toInt()
                is Float -> value.toInt()
                is Number -> value.toInt()
                is String -> value.toIntOrNull() ?: 0
                else -> 0
            }
            fun mapAnyToIntMap(anyMap: Any?): Map<String, Int> {
                val src = anyMap as? Map<*, *> ?: return emptyMap()
                return src.entries.associate { (k, v) -> k.toString() to anyToInt(v) }
            }
            fun listAnyToStringList(anyList: Any?): List<String> {
                val src = anyList as? List<*> ?: return emptyList()
                return src.map { it.toString() }
            }

            val weekTotal = anyToInt(weekMap["totalMinutes"]) 
            val weekAvg = anyToInt(weekMap["averageScore"]) 

            val progressData = ProgressData(
                totalMinutes = anyToInt(todayMap["totalMinutes"]),
                averageScore = anyToInt(todayMap["averageScore"]),
                weekTotalMinutes = weekTotal,
                weekAverageScore = weekAvg,
                totalSamples = anyToInt(todayMap["totalSamples"]),
                bestScore = anyToInt(todayMap["bestScore"]),
                worstScore = anyToInt(todayMap["worstScore"]),
                goodMinutes = anyToInt(todayMap["goodMinutes"]),
                okMinutes = anyToInt(todayMap["okMinutes"]),
                badMinutes = anyToInt(todayMap["badMinutes"]),
                weekDays = mapAnyToIntMap(weekMap["days"]),
                weekScores = mapAnyToIntMap(weekMap["scores"]),
                weekOrder = listAnyToStringList(weekMap["dateOrder"]),
                monthWeeks = mapAnyToIntMap(monthMap["weeks"]),
                monthWeekScores = mapAnyToIntMap(monthMap["scores"]),
                monthWeekOrder = listAnyToStringList(monthMap["weekOrder"]),
                yearMonths = mapAnyToIntMap(yearMap["months"]),
                yearMonthScores = mapAnyToIntMap(yearMap["scores"]),
                yearMonthOrder = listAnyToStringList(yearMap["monthOrder"])
            )
            
            println("🔍 ProgressService: Raw todayMap data: $todayMap")
            println("🔍 ProgressService: Mapped progressData - totalMinutes: ${progressData.totalMinutes}, averageScore: ${progressData.averageScore}")
            
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
