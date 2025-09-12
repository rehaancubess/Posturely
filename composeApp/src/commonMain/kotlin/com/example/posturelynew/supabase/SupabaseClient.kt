package com.example.posturelynew.supabase

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.flow.Flow
import com.example.posturelynew.PostureRecord
import com.example.posturelynew.DateTime

// TODO: move these to BuildConfig / expect/actual per target
private const val SUPABASE_URL = "https://lexlrxlvmbpfzenzzgld.supabase.co"
private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxleGxyeGx2bWJwZnplbnp6Z2xkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTU3NTQ5NDUsImV4cCI6MjA3MTMzMDk0NX0.WkLk4yF9-wSGejf2-JM4OhLb-Sf5N4AYCEqfWwkE-IY"

// Deep link config removed for OTP-only authentication
// If you need deep links later (e.g., for OAuth), uncomment and configure:
// private const val DEEPLINK_SCHEME = "posturely"
// private const val DEEPLINK_HOST = "auth"

object Supa {

    // Explicit type helps the Kotlin type checker on iOS
    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        println("🔧 [DEBUG] Creating Supabase client with URL: $SUPABASE_URL")
        install(Auth) {
            // ❌ No deep-link config needed for OTP-only
            // scheme = DEEPLINK_SCHEME
            // host = DEEPLINK_HOST
            println("🔧 [DEBUG] Auth plugin installed WITHOUT deep link config")
            // Enable session persistence
            autoLoadFromStorage = true
            println("🔧 [DEBUG] Auth plugin: Session persistence enabled")
        }
        install(Postgrest)   // ✅ instead of postgrest()
        install(Realtime)    // ✅ instead of realtime()
        println("🔧 [DEBUG] Postgrest and Realtime plugins installed")
    }

    // -------- Email OTP --------

    //  Send the email (now your template shows the code)
    suspend fun sendEmailOtp(email: String) {
        try {
            println("🔍 [DEBUG] Sending OTP to: $email")
            println("🔍 [DEBUG] Supabase URL: $SUPABASE_URL")
            println("🔍 [DEBUG] Auth config - scheme: not set, host: not set")
            
            client.auth.signInWith(io.github.jan.supabase.auth.providers.builtin.OTP) {
                this.email = email
                createUser = true
            }
            println("✅ [DEBUG] OTP sent successfully")
        } catch (e: Exception) {
            println("❌ [DEBUG] OTP send failed: ${e.message}")
            println("❌ [DEBUG] Exception type: ${e::class.simpleName}")
            println("❌ [DEBUG] Stack trace: ${e.stackTraceToString()}")
            throw e
        }
    }

    //  Verify the code the user typed
    suspend fun verifyEmailOtp(email: String, code: String) {
        client.auth.verifyEmailOtp(
            type = io.github.jan.supabase.auth.OtpType.Email.EMAIL,
            email = email,
            token = code
        )
        println("✅ Supabase: User authenticated successfully")
        println("🔍 Supabase: Session will be persisted automatically")
    }

    // -------- Session observation --------

    // Expose the Flow<SessionStatus> for UI layers to collect.
    // Do NOT try to return a SessionStatus by calling onAuthStateChange.
    val sessionStatus: Flow<SessionStatus>
        get() = client.auth.sessionStatus
        
    // -------- Posture Data Functions --------
    
    suspend fun getCurrentSession() = client.auth.currentSessionOrNull()
    
    suspend fun refreshSession() {
        try {
            println("🔍 Supabase: Attempting to refresh session")
            client.auth.refreshCurrentSession()
            println("✅ Supabase: Session refreshed successfully")
        } catch (e: Exception) {
            println("❌ Supabase: Failed to refresh session: ${e.message}")
        }
    }
    
    fun isUserSignedIn(): Boolean {
        return try {
            val session = client.auth.currentSessionOrNull()
            session != null
        } catch (e: Exception) {
            println("❌ Supabase: Error checking session: ${e.message}")
            false
        }
    }
    
    suspend fun insertPostureRecord(record: PostureRecord) {
        client.postgrest.from("posture_records").insert(record)
    }
    
    suspend fun getTodaysPostureRecords(userEmail: String): List<PostureRecord> {
        val today = DateTime.formatTimeStamp(DateTime.getCurrentTimeInMilliSeconds(), "yyyy-MM-dd")
        return client.postgrest.from("posture_records")
            .select {
                filter {
                    eq("user_email", userEmail)
                    eq("date", today)
                }
            }
            .decodeList<PostureRecord>()
    }
    
    suspend fun getTodaysProgress(userEmail: String): Map<String, Any> {
        val records = getTodaysPostureRecords(userEmail)
        
        if (records.isEmpty()) {
            return mapOf(
                "totalMinutes" to 0,
                "averageScore" to 0,
                "totalSamples" to 0,
                "bestScore" to 0,
                "worstScore" to 100,
                "goodMinutes" to 0,
                "okMinutes" to 0,
                "badMinutes" to 0
            )
        }
        
        val totalMinutes = records.size
        val totalSamples = records.sumOf { it.samples_count }
        val averageScore = records.map { it.average_posture_score }.average().toInt()
        val bestScore = records.maxOf { it.average_posture_score }
        val worstScore = records.minOf { it.average_posture_score }
        
        val goodMinutes = records.count { it.average_posture_score >= 80 }
        val okMinutes = records.count { it.average_posture_score in 60..79 }
        val badMinutes = records.count { it.average_posture_score < 60 }
        
        return mapOf(
            "totalMinutes" to totalMinutes,
            "averageScore" to averageScore,
            "totalSamples" to totalSamples,
            "bestScore" to bestScore,
            "worstScore" to worstScore,
            "goodMinutes" to goodMinutes,
            "okMinutes" to okMinutes,
            "badMinutes" to badMinutes
        )
    }

    suspend fun getWeeksProgress(userEmail: String): Map<String, Any> {
        val todayMillis = DateTime.getCurrentTimeInMilliSeconds()
        val weekStartMillis = todayMillis - 6L * 24L * 60L * 60L * 1000L // 7 days ago
        
        val weekStartDate = DateTime.formatTimeStamp(weekStartMillis, "yyyy-MM-dd")
        val todayDate = DateTime.formatTimeStamp(todayMillis, "yyyy-MM-dd")
        
        // Single query for all records in the date range
        val allRecords = try {
            client.postgrest.from("posture_records")
                .select {
                    filter {
                        eq("user_email", userEmail)
                        gte("date", weekStartDate)
                        lte("date", todayDate)
                    }
                }
                .decodeList<PostureRecord>()
        } catch (e: Exception) {
            println("❌ Supabase: Error fetching weekly data: ${e.message}")
            emptyList()
        }

        // Generate expected dates for the week
        val dates = (0..6).map { offset ->
            val millis = todayMillis - offset * 24L * 60L * 60L * 1000L
            DateTime.formatTimeStamp(millis, "yyyy-MM-dd")
        }.reversed()

        if (allRecords.isEmpty()) {
            return mapOf(
                "totalMinutes" to 0,
                "averageScore" to 0,
                "days" to dates.associateWith { 0 },
                "scores" to dates.associateWith { 0 },
                "dateOrder" to dates
            )
        }

        val grouped = allRecords.groupBy { it.date }
        val perDayMinutes = dates.associateWith { day -> grouped[day]?.size ?: 0 }
        val perDayAvgScores = dates.associateWith { day ->
            val recs = grouped[day].orEmpty()
            if (recs.isEmpty()) 0 else recs.map { it.average_posture_score }.average().toInt()
        }
        val averageScore = allRecords.map { it.average_posture_score }.average().toInt()
        val totalMinutes = allRecords.size

        return mapOf(
            "totalMinutes" to totalMinutes,
            "averageScore" to averageScore,
            "days" to perDayMinutes,
            "scores" to perDayAvgScores,
            "dateOrder" to dates
        )
    }

    suspend fun getMonthsProgress(userEmail: String): Map<String, Any> {
        val todayMillis = DateTime.getCurrentTimeInMilliSeconds()
        val monthStartMillis = todayMillis - 35L * 24L * 60L * 60L * 1000L // 5 weeks ago
        
        val monthStartDate = DateTime.formatTimeStamp(monthStartMillis, "yyyy-MM-dd")
        val todayDate = DateTime.formatTimeStamp(todayMillis, "yyyy-MM-dd")
        
        // Single query for all records in the date range
        val allRecords = try {
            client.postgrest.from("posture_records")
                .select {
                    filter {
                        eq("user_email", userEmail)
                        gte("date", monthStartDate)
                        lte("date", todayDate)
                    }
                }
                .decodeList<PostureRecord>()
        } catch (e: Exception) {
            println("❌ Supabase: Error fetching monthly data: ${e.message}")
            emptyList()
        }

        // Generate week labels and date ranges
        val weekLabels = listOf("W1", "W2", "W3", "W4", "W5")
        val weekRanges = (0..4).map { weekOffset ->
            val weekStartMillis = todayMillis - weekOffset * 7L * 24L * 60L * 60L * 1000L
            val weekEndMillis = weekStartMillis + 6L * 24L * 60L * 60L * 1000L
            
            val weekStartDate = DateTime.formatTimeStamp(weekStartMillis, "yyyy-MM-dd")
            val weekEndDate = DateTime.formatTimeStamp(weekEndMillis, "yyyy-MM-dd")
            
            Pair(weekLabels[4 - weekOffset], Pair(weekStartDate, weekEndDate))
        }.reversed()

        if (allRecords.isEmpty()) {
            return mapOf(
                "totalMinutes" to 0,
                "averageScore" to 0,
                "weeks" to weekLabels.associateWith { 0 },
                "scores" to weekLabels.associateWith { 0 },
                "weekOrder" to weekLabels
            )
        }

        val grouped = allRecords.groupBy { record ->
            weekRanges.find { (_, dateRange) -> 
                record.date >= dateRange.first && record.date <= dateRange.second 
            }?.first ?: "Unknown"
        }
        
        val perWeekMinutes = weekLabels.associate { weekLabel -> 
            weekLabel to (grouped[weekLabel]?.size ?: 0)
        }
        
        val perWeekAvgScores = weekLabels.associate { weekLabel ->
            val recs = grouped[weekLabel].orEmpty()
            weekLabel to if (recs.isEmpty()) 0 else recs.map { it.average_posture_score }.average().toInt()
        }
        
        val averageScore = allRecords.map { it.average_posture_score }.average().toInt()
        val totalMinutes = allRecords.size

        return mapOf(
            "totalMinutes" to totalMinutes,
            "averageScore" to averageScore,
            "weeks" to perWeekMinutes,
            "scores" to perWeekAvgScores,
            "weekOrder" to weekLabels
        )
    }

    suspend fun getYearsProgress(userEmail: String): Map<String, Any> {
        val todayMillis = DateTime.getCurrentTimeInMilliSeconds()
        val yearStartMillis = todayMillis - 365L * 24L * 60L * 60L * 1000L // 1 year ago
        
        val yearStartDate = DateTime.formatTimeStamp(yearStartMillis, "yyyy-MM-dd")
        val todayDate = DateTime.formatTimeStamp(todayMillis, "yyyy-MM-dd")
        
        // Single query for all records in the date range
        val allRecords = try {
            client.postgrest.from("posture_records")
                .select {
                    filter {
                        eq("user_email", userEmail)
                        gte("date", yearStartDate)
                        lte("date", todayDate)
                    }
                }
                .decodeList<PostureRecord>()
        } catch (e: Exception) {
            println("❌ Supabase: Error fetching yearly data: ${e.message}")
            emptyList()
        }

        // Generate month labels and date ranges (30-day periods)
        val monthLabels = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
        val monthRanges = (0..11).map { monthOffset ->
            val monthStartMillis = todayMillis - monthOffset * 30L * 24L * 60L * 60L * 1000L
            val monthEndMillis = monthStartMillis + 29L * 24L * 60L * 60L * 1000L
            
            val monthStartDate = DateTime.formatTimeStamp(monthStartMillis, "yyyy-MM-dd")
            val monthEndDate = DateTime.formatTimeStamp(monthEndMillis, "yyyy-MM-dd")
            
            Pair(monthLabels[11 - monthOffset], Pair(monthStartDate, monthEndDate))
        }.reversed()

        if (allRecords.isEmpty()) {
            return mapOf(
                "totalMinutes" to 0,
                "averageScore" to 0,
                "months" to monthLabels.associateWith { 0 },
                "scores" to monthLabels.associateWith { 0 },
                "monthOrder" to monthLabels
            )
        }

        val grouped = allRecords.groupBy { record ->
            monthRanges.find { (_, dateRange) -> 
                record.date >= dateRange.first && record.date <= dateRange.second 
            }?.first ?: "Unknown"
        }
        
        val perMonthMinutes = monthLabels.associate { monthLabel -> 
            monthLabel to (grouped[monthLabel]?.size ?: 0)
        }
        
        val perMonthAvgScores = monthLabels.associate { monthLabel ->
            val recs = grouped[monthLabel].orEmpty()
            monthLabel to if (recs.isEmpty()) 0 else recs.map { it.average_posture_score }.average().toInt()
        }
        
        val averageScore = allRecords.map { it.average_posture_score }.average().toInt()
        val totalMinutes = allRecords.size

        return mapOf(
            "totalMinutes" to totalMinutes,
            "averageScore" to averageScore,
            "months" to perMonthMinutes,
            "scores" to perMonthAvgScores,
            "monthOrder" to monthLabels
        )
    }
}
