package com.example.posturelynew

import platform.Foundation.*

actual object DateTime {
    actual fun getFormattedDate(
        timestamp: String,
        inputFormat: String,
        outputFormat: String
    ): String {
        val df = NSDateFormatter().apply {
            dateFormat = inputFormat
            timeZone = NSTimeZone.timeZoneWithAbbreviation("GMT") ?: NSTimeZone.timeZoneForSecondsFromGMT(0)
        }

        val date = df.dateFromString(timestamp)
        df.timeZone = NSTimeZone.localTimeZone
        df.dateFormat = outputFormat

        return df.stringFromDate(date!!)
    }

    actual fun formatTimeStamp(timeStamp: Long, outputFormat: String): String {
        val formatter = NSDateFormatter().apply {
            dateFormat = outputFormat
            timeZone = NSTimeZone.localTimeZone
        }
        // timeStamp is in milliseconds, NSDate expects seconds since 1970
        val date = NSDate(timeStamp.toDouble() / 1000.0)
        return formatter.stringFromDate(date)
    }

    actual fun getDateInMilliSeconds(timeStamp: String, inputFormat: String): Long {
        if (timeStamp.trim().isEmpty()) return getCurrentTimeInMilliSeconds()

        val df = NSDateFormatter().apply {
            dateFormat = inputFormat
        }
        val date = df.dateFromString(timeStamp)
        return (date!!.timeIntervalSince1970 * 1000).toLong()
    }

    actual fun getCurrentTimeInMilliSeconds(): Long {
        val currentTime = NSDate().timeIntervalSince1970
        val currentTimeMillis = (currentTime * 1000).toLong()
        println("🔍 iOS DateTime: currentTime=$currentTime, currentTimeMillis=$currentTimeMillis")
        return currentTimeMillis
    }

    actual fun getForwardedDate(
        forwardedDaya: Int,
        forwardedMonth: Int,
        outputFormat: String
    ): String {
        val calendar = NSCalendar.currentCalendar
        val currentDate = NSDate()
        val components = NSDateComponents().apply {
            day = forwardedDaya.toLong()
            month = forwardedMonth.toLong()
        }

        val forwardDate = calendar.dateByAddingComponents(components, currentDate, NSCalendarUnitDay)
        val dateFormatter = NSDateFormatter().apply {
            dateFormat = outputFormat
        }

        return dateFormatter.stringFromDate(forwardDate ?: currentDate)
    }
}
