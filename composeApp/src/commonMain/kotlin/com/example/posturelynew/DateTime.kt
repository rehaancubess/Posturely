package com.mobil80.posturely

expect object DateTime {
    fun getFormattedDate(
        timestamp: String,
        inputFormat: String,
        outputFormat: String
    ): String

    fun formatTimeStamp(
        timeStamp: Long,
        outputFormat: String = "yyyy-MM-dd"
    ): String

    fun getDateInMilliSeconds(timeStamp: String, inputFormat: String): Long

    fun getCurrentTimeInMilliSeconds(): Long

    fun getForwardedDate(
        forwardedDaya: Int = 0,
        forwardedMonth: Int = 0,
        outputFormat: String = "yyyy-MM-dd'T'HH:mm:ss"
    ): String
}
