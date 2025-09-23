package com.mobil80.posturely

import androidx.compose.runtime.Composable

actual fun getPlatformName(): String = "Desktop"

actual class AirPodsTracker {
    actual fun isConnected(): Boolean = false
    actual fun startTracking() { /* Not supported on desktop */ }
    actual fun stopTracking() { /* Not supported on desktop */ }
    actual fun getCurrentTiltAngle(): Float = 0f
    actual fun isTracking(): Boolean = false
    actual fun getConnectedDeviceName(): String? = null
}

actual class PlatformStorage {
    private val storageFile = java.io.File(System.getProperty("user.home"), ".posturely_desktop_storage.json")
    private val storage = loadStorage()
    
    private fun loadStorage(): MutableMap<String, Any> {
        return try {
            if (storageFile.exists()) {
                val content = storageFile.readText()
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val map: Map<String, kotlinx.serialization.json.JsonElement> = json.decodeFromString(content)
                map.mapValues { entry ->
                    val element = entry.value
                    when {
                        element is kotlinx.serialization.json.JsonPrimitive && element.isString -> element.content
                        element is kotlinx.serialization.json.JsonPrimitive -> {
                            // Try to parse as different types
                            try {
                                element.content.toBooleanStrictOrNull() ?: element.content.toDoubleOrNull() ?: element.content.toLongOrNull() ?: element.content
                            } catch (e: Exception) {
                                element.content
                            }
                        }
                        else -> element.toString()
                    }
                }.toMutableMap()
            } else {
                mutableMapOf()
            }
        } catch (e: Exception) {
            println("🔧 [DESKTOP] Failed to load storage: ${e.message}")
            mutableMapOf()
        }
    }
    
    private fun saveStorage() {
        try {
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            val content = json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), 
                kotlinx.serialization.json.buildJsonObject {
                    storage.forEach { (key, value) ->
                        put(key, kotlinx.serialization.json.JsonPrimitive(value.toString()))
                    }
                })
            storageFile.writeText(content)
        } catch (e: Exception) {
            println("🔧 [DESKTOP] Failed to save storage: ${e.message}")
        }
    }
    
    actual fun saveBoolean(key: String, value: Boolean) {
        storage[key] = value
        saveStorage()
    }
    
    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return when (val value = storage[key]) {
            is Boolean -> value
            is String -> value.toBooleanStrictOrNull() ?: defaultValue
            else -> defaultValue
        }
    }
    
    actual fun saveString(key: String, value: String) {
        storage[key] = value
        saveStorage()
    }
    
    actual fun getString(key: String, defaultValue: String): String {
        return (storage[key] as? String) ?: defaultValue
    }
    
    actual fun clear() {
        storage.clear()
        if (storageFile.exists()) {
            storageFile.delete()
        }
    }
}

actual fun openEmailClient(to: String, subject: String, body: String) {
    try {
        val desktop = java.awt.Desktop.getDesktop()
        if (desktop.isSupported(java.awt.Desktop.Action.MAIL)) {
            val mailto = "mailto:$to?subject=${java.net.URLEncoder.encode(subject, "UTF-8")}&body=${java.net.URLEncoder.encode(body, "UTF-8")}"
            val uri = java.net.URI(mailto)
            desktop.mail(uri)
        } else {
            println("🔧 [DESKTOP] Mail not supported on this system")
        }
    } catch (e: Exception) {
        println("🔧 [DESKTOP] Failed to open email client: ${e.message}")
    }
}

actual fun openUrl(url: String) {
    try {
        val desktop = java.awt.Desktop.getDesktop()
        if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
            val uri = java.net.URI(url)
            desktop.browse(uri)
        } else {
            println("🔧 [DESKTOP] Browse not supported on this system")
        }
    } catch (e: Exception) {
        println("🔧 [DESKTOP] Failed to open URL: ${e.message}")
    }
}

actual fun requestPhoneTrackingPermissions() { /* no-op on desktop */ }
actual fun requestAirPodsPermissions() { /* no-op on desktop */ }
