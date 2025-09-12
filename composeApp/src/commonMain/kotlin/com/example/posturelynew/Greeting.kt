package com.example.posturelynew

class Greeting {
    private val platform = getPlatformName()

    fun greet(): String {
        return "Hello, $platform!"
    }
}