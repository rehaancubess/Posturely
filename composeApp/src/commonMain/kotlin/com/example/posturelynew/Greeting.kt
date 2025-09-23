package com.mobil80.posturely

class Greeting {
    private val platform = getPlatformName()

    fun greet(): String {
        return "Hello, $platform!"
    }
}