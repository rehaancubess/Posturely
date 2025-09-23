package com.mobil80.posturely.scan

import com.mobil80.posturely.supabase.Supa
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.*
import platform.darwin.NSObject
import kotlin.coroutines.resume

private fun makeRequest(url: String, token: String, jsonBody: String, cb: (Int, String?) -> Unit) {
    val nsUrl = NSURL.URLWithString(url) ?: run { cb(0, "bad_url"); return }
    val req = NSMutableURLRequest.requestWithURL(nsUrl)
    req.setHTTPMethod("POST")
    req.setValue("Bearer $token", forHTTPHeaderField = "Authorization")
    req.setValue("application/json", forHTTPHeaderField = "Content-Type")
    val bodyData = (jsonBody as NSString).dataUsingEncoding(NSUTF8StringEncoding)
    req.setHTTPBody(bodyData)

    val task = NSURLSession.sharedSession.dataTaskWithRequest(req) { _, response, error ->
        if (error != null) {
            cb(0, error.localizedDescription)
        } else {
            val code = (response as? NSHTTPURLResponse)?.statusCode?.toInt() ?: 0
            cb(code, null)
        }
    }
    task.resume()
}

actual suspend fun postPostureReport(scanId: String, frontBase64: String, sideBase64: String): Result<Unit> = suspendCancellableCoroutine { cont ->
    val token = Supa.currentAccessTokenOrEmpty()
    if (token.isEmpty()) {
        cont.resume(Result.failure(IllegalStateException("No Supabase session token")))
        return@suspendCancellableCoroutine
    }
    val url = "https://lexlrxlvmbpfzenzzgld.supabase.co/functions/v1/posture_report"
    val json = "{" +
        "\"scanId\":\"$scanId\"," +
        "\"frontBase64\":\"${frontBase64}\"," +
        "\"sideBase64\":\"${sideBase64}\"" +
        "}"
    makeRequest(url, token, json) { code, err ->
        if (code in 200..299) cont.resume(Result.success(Unit)) else cont.resume(Result.failure(IllegalStateException("HTTP $code ${err ?: ""}")))
    }
}