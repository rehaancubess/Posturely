package com.mobil80.posturely.supabase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter

@Serializable
data class SupabaseOTPRequest(
    val email: String
)

@Serializable
data class SupabaseOTPVerifyRequest(
    val email: String,
    val token: String,
    val type: String = "email"
)

@Serializable
data class SupabaseResponse(
    val message: String? = null,
    val error: String? = null
)

// Desktop-specific Supabase client using HttpURLConnection (more reliable)
object DesktopSupa {
    
    private const val SUPABASE_URL = "https://lexlrxlvmbpfzenzzgld.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxleGxyeGx2bWJwZnplbnp6Z2xkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTU3NTQ5NDUsImV4cCI6MjA3MTMzMDk0NX0.WkLk4yF9-wSGejf2-JM4OhLb-Sf5N4AYCEqfWwkE-IY"
    
    private val json = Json { ignoreUnknownKeys = true }
    
    suspend fun sendEmailOtp(email: String) {
        withContext(Dispatchers.IO) {
            try {
                println("🔧 [DESKTOP] Sending OTP to: $email")
                
                val url = URL("$SUPABASE_URL/auth/v1/otp")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                connection.setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                connection.doOutput = true
                
                val requestBody = json.encodeToString(SupabaseOTPRequest.serializer(), SupabaseOTPRequest(email))
                
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody)
                    writer.flush()
                }
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    println("✅ [DESKTOP] OTP sent successfully to $email")
                } else {
                    val errorMessage = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                    println("❌ [DESKTOP] Failed to send OTP: $errorMessage")
                    throw Exception("Failed to send OTP: HTTP $responseCode")
                }
                
            } catch (e: Exception) {
                println("❌ [DESKTOP] OTP send error: ${e.message}")
                throw e
            }
        }
    }
    
    suspend fun verifyEmailOtp(email: String, code: String) {
        withContext(Dispatchers.IO) {
            try {
                println("🔧 [DESKTOP] Verifying OTP for: $email with code: $code")
                
                val url = URL("$SUPABASE_URL/auth/v1/verify")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                connection.setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                connection.doOutput = true
                
                // Manually construct JSON to ensure correct format
                val jsonBody = """
                    {
                        "email": "$email",
                        "token": "$code",
                        "type": "email"
                    }
                """.trimIndent()
                
                println("🔧 [DESKTOP] Manual JSON body: $jsonBody")
                
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(jsonBody)
                    writer.flush()
                }
                
                val responseCode = connection.responseCode
                println("🔧 [DESKTOP] Response code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseBody = connection.inputStream.bufferedReader().readText()
                    println("✅ [DESKTOP] OTP verified successfully for $email")
                    println("🔧 [DESKTOP] Response: $responseBody")
                } else {
                    val errorMessage = connection.errorStream?.bufferedReader()?.readText() ?: "Invalid OTP"
                    println("❌ [DESKTOP] OTP verification failed: $errorMessage")
                    throw Exception("Invalid OTP: HTTP $responseCode - $errorMessage")
                }
                
            } catch (e: Exception) {
                println("❌ [DESKTOP] OTP verification error: ${e.message}")
                throw e
            }
        }
    }
    
    // Mock session status - return a simple flow
    val sessionStatus: kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.flowOf("NotAuthenticated")
    
    suspend fun getCurrentSession() = null
    
    suspend fun refreshSession() {
        println("🔧 [DESKTOP] Mock session refresh")
    }
    
    fun isUserSignedIn(): Boolean = true
    
    suspend fun insertPostureRecord(record: com.mobil80.posturely.PostureRecord) {
        withContext(Dispatchers.IO) {
            try {
                // REST insert to PostgREST
                val url = URL("$SUPABASE_URL/rest/v1/posture_records")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                connection.setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                connection.setRequestProperty("Prefer", "return=representation")
                connection.doOutput = true

                val jsonBody = """
                    {
                      "user_email": "${record.user_email}",
                      "date": "${record.date}",
                      "time": "${record.time}",
                      "average_posture_score": ${record.average_posture_score},
                      "tracking_source": "${record.tracking_source}",
                      "timestamp": ${record.timestamp},
                      "samples_count": ${record.samples_count}
                    }
                """.trimIndent()

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(jsonBody)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode in listOf(HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_CREATED)) {
                    println("✅ [DESKTOP] Inserted posture record to Supabase for ${record.user_email} on ${record.date} ${record.time}")
                } else {
                    val errorMessage = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                    println("❌ [DESKTOP] Failed to insert posture record: $responseCode - $errorMessage")
                    throw Exception("Insert failed: HTTP $responseCode")
                }
            } catch (e: Exception) {
                println("❌ [DESKTOP] Insert error: ${e.message}")
                throw e
            }
        }
    }
    
    suspend fun getTodaysPostureRecords(userEmail: String): List<com.mobil80.posturely.PostureRecord> {
        println("🔧 [DESKTOP] Mock posture records retrieval for: $userEmail")
        return emptyList()
    }
}