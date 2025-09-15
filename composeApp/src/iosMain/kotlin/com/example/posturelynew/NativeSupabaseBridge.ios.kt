package com.example.posturelynew

import com.example.posturelynew.supabase.Supa
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNotification

@Suppress("unused")
object NativeSupabaseBridge {
    init {
        // Respond to token requests from Swift
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = "RequestSupabaseToken",
            `object` = null,
            queue = null
        ) { _: NSNotification? ->
            val token = Supa.currentAccessTokenOrEmpty()
            val userId = Supa.currentUserIdOrEmpty()
            val userInfo: Map<Any?, Any?> = mapOf("token" to token, "userId" to userId)
            NSNotificationCenter.defaultCenter.postNotificationName(
                aName = "SupabaseToken",
                `object` = null,
                userInfo = userInfo
            )
        }
    }
}


