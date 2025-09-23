package com.mobil80.posturely

import java.io.File
import java.util.Properties

actual fun getPlatformName(): String = "JVM"

actual fun getPlatformImage(): String = "desktop"

actual class PlatformStorage {
    private val propertiesFile = File(System.getProperty("user.home"), ".posturely_prefs")
    private val properties = Properties()
    
    init {
        if (propertiesFile.exists()) {
            properties.load(propertiesFile.inputStream())
        }
    }
    
    actual fun saveBoolean(key: String, value: Boolean) {
        properties.setProperty(key, value.toString())
        properties.store(propertiesFile.outputStream(), "Posturely Preferences")
    }
    
    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return properties.getProperty(key)?.toBoolean() ?: defaultValue
    }
    
    actual fun saveString(key: String, value: String) {
        properties.setProperty(key, value)
        properties.store(propertiesFile.outputStream(), "Posturely Preferences")
    }
    
    actual fun getString(key: String, defaultValue: String): String {
        return properties.getProperty(key) ?: defaultValue
    }
    
    actual fun clear() {
        properties.clear()
        if (propertiesFile.exists()) {
            propertiesFile.delete()
        }
    }
}