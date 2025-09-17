package com.example.posturelynew

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import org.jetbrains.compose.resources.painterResource
import posturelynew.composeapp.generated.resources.Res
import posturelynew.composeapp.generated.resources.nostats
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource

@Composable
fun StatsScreen() {
    // Colors to match the design
    val pageBg = Color(0xFFFED867)
    val cardBg = Color(0xFFFFF0C0)
    val textPrimary = Color(0xFF0F1931)
    val accentDonut = Color(0xFF7A4B00) // Same brown as start tracking button
    val accentDonutLight = Color(0xFFD2B48C) // Light brown for background ring
    val legendGood = Color(0xFF7A4B00) // Same brown as start tracking button
    val legendBad = Color(0xFFA0522D) // Darker brown for legend

    // Load progress data (same service used on Home)
    val storage = remember { PlatformStorage() }
    val userEmail = storage.getString("userEmail", "")
    val progressService = remember { ProgressService() }
    val progressData by progressService.progressData.collectAsState()
    
    // Coroutine scope for refresh button
    val scope = rememberCoroutineScope()

    LaunchedEffect(userEmail) {
        if (userEmail.isNotEmpty()) {
            progressService.startProgressTracking(userEmail)
        }
    }

    // Time range tabs
    var selectedRange by remember { mutableStateOf(0) } // 0=Week, 1=Month, 2=Year

    // Derived values
    val totalLabeledMinutes = (progressData.goodMinutes + progressData.okMinutes + progressData.badMinutes)
    val percentGood = if (totalLabeledMinutes > 0) {
        (progressData.goodMinutes.toFloat() / totalLabeledMinutes.toFloat())
    } else 0f
    
    // Average score for the donut chart
    val averageScore = if (selectedRange == 0) {
        progressData.weekAverageScore.takeIf { it > 0 } ?: 0
    } else {
        progressData.averageScore.takeIf { it > 0 } ?: 0
    }
    val averageScorePercent = (averageScore / 100f).coerceIn(0f, 1f)
    
    // Check if month or year view should show nostats (temporarily disabled)
    val hasInsufficientMonthData = selectedRange == 1 // Always show nostats for month
    val hasInsufficientYearData = selectedRange == 2 // Always show nostats for year

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Title with refresh button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Empty space to balance the layout
            Spacer(modifier = Modifier.width(60.dp))
            
            Text(
                text = "Stats",
                color = textPrimary,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            
            // Refresh button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(cardBg, CircleShape)
                    .clickable { 
                        // Trigger refresh in coroutine scope
                        scope.launch {
                            progressService.refreshProgress()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "↻",
                    color = textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Range tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatsRangeChip(label = "Week", selected = selectedRange == 0) { selectedRange = 0 }
            StatsRangeChip(label = "Month", selected = selectedRange == 1) { selectedRange = 1 }
            StatsRangeChip(label = "Year", selected = selectedRange == 2) { selectedRange = 2 }
        }

        // Show nostats image if month or year view, otherwise show normal content
        if (hasInsufficientMonthData || hasInsufficientYearData) {
            // Show nostats image for month/year data (temporarily disabled)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(Res.drawable.nostats),
                    contentDescription = "No stats available",
                    modifier = Modifier
                        .size(500.dp)
                        .padding(16.dp)
                )
            }
        } else {
            // Donut + Legend + Line chart row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Donut chart
                DonutChart(
                    percent = averageScorePercent,
                    size = 140.dp,
                    ringColor = accentDonut,
                    bgRingColor = accentDonutLight,
                    labelPrimary = averageScore.toString(),
                    labelSecondary = "Average Score",
                    textColor = textPrimary
                )

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Legend
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).background(legendGood, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text("Good", color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).background(legendBad, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text("Bad", color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(16.dp))

                    // Weekly trend from real data, zero-filled if empty
                    LineChart(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        values = generateWeeklyTrend(progressData)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("M","T","W","T","F","S","S").forEach { d ->
                            Text(d, color = textPrimary.copy(alpha = 0.9f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Only show cards and bar chart if not insufficient month/year data
        if (!hasInsufficientMonthData && !hasInsufficientYearData) {
            // Cards row: Total Time, Average Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total Time",
                    value = if (selectedRange == 0) formatMinutes(progressData.weekTotalMinutes) else formatMinutes(progressData.totalMinutes),
                    modifier = Modifier.weight(1f),
                    cardBg = cardBg,
                    textPrimary = textPrimary
                )
                StatCard(
                    title = "Average Score",
                    value = (if (selectedRange == 0) progressData.weekAverageScore.takeIf { it > 0 } ?: 0 else progressData.averageScore.takeIf { it > 0 } ?: 0).toString(),
                    modifier = Modifier.weight(1f),
                    cardBg = cardBg,
                    textPrimary = textPrimary
                )
            }

            Spacer(Modifier.height(12.dp))

            // Total Minutes + Bar chart
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Text("Total Minutes", color = textPrimary.copy(alpha = 0.8f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                val totalMinutesForRange = if (selectedRange == 0) progressData.weekTotalMinutes else progressData.totalMinutes
                Text(totalMinutesForRange.toString(), color = textPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(8.dp))
                BarChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(start = 20.dp),
                    values = generateBars(progressData, selectedRange)
                )
                Spacer(Modifier.height(6.dp))
                // Labels under the bars based on selected range
                val barLabels = when (selectedRange) {
                    0 -> listOf("M","T","W","T","F","S","S")
                    1 -> listOf("W1","W2","W3","W4","W5")
                    else -> listOf("J","F","M","A","M","J","J","A","S","O","N","D")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    barLabels.forEach { label ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = textPrimary.copy(alpha = 0.9f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.StatsRangeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Color(0xFFFFE08A) else Color(0xFFFFF0C0)
    val text = Color(0xFF0F1931)
    Box(
        modifier = Modifier
            .height(40.dp)
            .weight(1f)
            .background(bg, RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFFF5D37A), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp)
            .wrapContentHeight(align = Alignment.CenterVertically)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DonutChart(
    percent: Float,
    size: androidx.compose.ui.unit.Dp,
    ringColor: Color,
    bgRingColor: Color,
    labelPrimary: String,
    labelSecondary: String,
    textColor: Color
) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 20.dp.toPx()
            // Background ring
            drawArc(
                color = bgRingColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // Foreground ring
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * percent,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(labelPrimary, color = textColor, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(labelSecondary, color = textColor.copy(alpha = 0.8f), fontSize = 14.sp)
        }
    }
}

@Composable
private fun LineChart(modifier: Modifier, values: List<Float>) {
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val min = values.minOrNull() ?: 0f
        val max = values.maxOrNull() ?: 1f
        val range = (max - min).takeIf { it != 0f } ?: 1f
        val stepX = size.width / (values.size - 1)
        var prev: Offset? = null
        values.forEachIndexed { index, v ->
            val x = stepX * index
            val y = size.height - ((v - min) / range) * size.height
            val p = Offset(x, y)
            prev?.let { drawLine(color = Color(0xFF7A4B00), start = it, end = p, strokeWidth = 6f) }
            prev = p
        }
    }
}

@Composable
private fun BarChart(modifier: Modifier, values: List<Float>) {
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val max = values.maxOrNull() ?: 1f
        val barWidth = size.width / (values.size * 1.8f)
        values.forEachIndexed { index, v ->
            val height = (v / max) * size.height
            val left = (index * (barWidth * 1.8f))
            val top = size.height - height
            drawRoundRect(
                color = Color(0xFF7A4B00),
                topLeft = Offset(left, top),
                size = Size(barWidth, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    cardBg: Color,
    textPrimary: Color
) {
    Column(
        modifier = modifier
            .background(cardBg, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(title, color = textPrimary.copy(alpha = 0.8f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(value, color = textPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
    }
}

private fun formatMinutes(totalMinutes: Int): String {
    if (totalMinutes <= 0) return "0m"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun generateWeeklyTrend(progressData: ProgressData): List<Float> {
    val weekOrder = progressData.weekOrder
    val weekScores = progressData.weekScores
    if (weekOrder.isEmpty()) return List(7) { 0f }
    return weekOrder
        .sorted()
        .map { original -> original to normalizeYear(original) }
        .map { (original, normalized) ->
            (weekScores[normalized] ?: weekScores[original] ?: 0).toFloat()
        }
}

// Map yyyy-MM-dd weekOrder into short day labels matching local week
private fun weekLabelsFromOrder(weekOrder: List<String>): List<String> {
    if (weekOrder.isEmpty()) return listOf("M","T","W","T","F","S","S")
    return weekOrder.sorted().map { dateStr ->
        // Expect yyyy-MM-dd; derive day-of-week using DateTime helpers and the same reference used in SupabaseClient
        val millis = DateTime.getDateInMilliSeconds(normalizeYear(dateStr), "yyyy-MM-dd")
        val dayCode = DateTime.formatTimeStamp(millis, "EEE") // Mon, Tue, ...
        when (dayCode.lowercase()) {
            "mon" -> "M"
            "tue" -> "T"
            "wed" -> "W"
            "thu" -> "T"
            "fri" -> "F"
            "sat" -> "S"
            "sun" -> "S"
            else -> ""
        }
    }
}

// Supabase might send wrong year (e.g., 2056 instead of 2025). Normalize to 2025 for mapping keys.
private fun normalizeYear(dateStr: String): String {
    // yyyy-MM-dd
    if (dateStr.length < 10) return dateStr
    val year = dateStr.substring(0, 4)
    // TEMP: Align to 2056 to match iOS-stored rows
    return if (year != "2056") "2056" + dateStr.substring(4) else dateStr
}

private fun generateBars(progressData: ProgressData, range: Int): List<Float> {
    return when (range) {
        // Week: 7 bars (S..S) - use real weekly data
        0 -> {
            val weekOrder = progressData.weekOrder
            val weekDays = progressData.weekDays
            
            if (weekOrder.isEmpty()) return List(7) { 0f }
            // Use real data in the correct order (zero-filled), checking normalized and original keys
            weekOrder
                .sorted()
                .map { original -> original to normalizeYear(original) }
                .map { (original, normalized) ->
                    (weekDays[normalized] ?: weekDays[original] ?: 0).toFloat()
                }
        }
        // Month: 5 bars (W1..W5) - temporarily disabled
        1 -> {
            // Commented out for later use
            /*
            val monthWeekOrder = progressData.monthWeekOrder
            val monthWeeks = progressData.monthWeeks
            
            if (monthWeekOrder.isEmpty() || monthWeeks.isEmpty()) {
                // Fallback to mock data
                val total = progressData.totalMinutes.coerceAtLeast(0)
                val base = (total / 5f).coerceAtLeast(1f)
                listOf(0.7f, 0.8f, 1.1f, 0.9f, 1.0f).map { it * base }
            } else {
                // Use real data in the correct order
                monthWeekOrder.map { week ->
                    monthWeeks[week]?.toFloat() ?: 0f
                }
            }
            */
            // Temporary mock data for month
            val total = progressData.totalMinutes.coerceAtLeast(0)
            val base = (total / 5f).coerceAtLeast(1f)
            listOf(0.7f, 0.8f, 1.1f, 0.9f, 1.0f).map { it * base }
        }
        // Year: 12 bars (J..D) - temporarily disabled
        else -> {
            // Commented out for later use
            /*
            val yearMonthOrder = progressData.yearMonthOrder
            val yearMonths = progressData.yearMonths
            
            if (yearMonthOrder.isEmpty() || yearMonths.isEmpty()) {
                // Fallback to mock data
                val total = progressData.totalMinutes.coerceAtLeast(0)
                val base = (total / 12f).coerceAtLeast(1f)
                listOf(0.6f,0.7f,0.8f,0.9f,1.0f,1.1f,1.0f,0.95f,0.9f,0.85f,0.8f,0.75f).map { it * base }
            } else {
                // Use real data in the correct order
                yearMonthOrder.map { month ->
                    yearMonths[month]?.toFloat() ?: 0f
                }
            }
            */
            // Temporary mock data for year
            val total = progressData.totalMinutes.coerceAtLeast(0)
            val base = (total / 12f).coerceAtLeast(1f)
            listOf(0.6f,0.7f,0.8f,0.9f,1.0f,1.1f,1.0f,0.95f,0.9f,0.85f,0.8f,0.75f).map { it * base }
        }
    }
}
