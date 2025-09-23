package com.mobil80.posturely.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ApiResponse<T>(
    val data: T? = null,
    val error: String? = null,
    val success: Boolean = true
)

@Serializable
data class PostureData(
    val score: Int,
    val timestamp: Long,
    val landmarks: List<Float>
)

// Modern API client using Ktor
class DesktopApiClient {
    
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 10000
        }
        
        install(DefaultRequest) {
            contentType(ContentType.Application.Json)
        }
    }
    
    // Example: Posture data API
    suspend fun savePostureData(data: PostureData): Result<ApiResponse<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val response: HttpResponse = client.post("https://your-api.com/posture") {
                    setBody(data)
                }
                
                if (response.status.isSuccess()) {
                    val result: ApiResponse<String> = response.body()
                    Result.success(result)
                } else {
                    Result.failure(Exception("API Error: ${response.status}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // Example: Get user data
    suspend fun getUserData(userId: String): Result<ApiResponse<UserData>> {
        return withContext(Dispatchers.IO) {
            try {
                val response: HttpResponse = client.get("https://your-api.com/users/$userId")
                
                if (response.status.isSuccess()) {
                    val result: ApiResponse<UserData> = response.body()
                    Result.success(result)
                } else {
                    Result.failure(Exception("API Error: ${response.status}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // Example: Upload file
    suspend fun uploadFile(fileData: ByteArray, fileName: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response: HttpResponse = client.post("https://your-api.com/upload") {
                    setBody(fileData)
                    headers {
                        append("Content-Type", "application/octet-stream")
                        append("X-File-Name", fileName)
                    }
                }
                
                if (response.status.isSuccess()) {
                    val fileUrl: String = response.body()
                    Result.success(fileUrl)
                } else {
                    Result.failure(Exception("Upload failed: ${response.status}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    fun close() {
        client.close()
    }
}

@Serializable
data class UserData(
    val id: String,
    val email: String,
    val name: String,
    val createdAt: Long
)
