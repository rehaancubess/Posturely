package com.mobil80.posturely

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReportsScreen(
    onBack: () -> Unit = {}
) {
    val pageBg = Color(0xFFFED867)
    val cardBg = Color(0xFFFFF0C0)
    val textPrimary = Color(0xFF0F1931)
    val accentBrown = Color(0xFF7A4B00)

    // TODO: Replace with real reports from Supabase
    val items = remember {
        List(2) { idx ->
            ReportListItem(
                id = "scan-${1000 + idx}",
                date = "2025-09-${(10 + idx).toString().padStart(2, '0')}",
                score = listOf(78, 82, 88, 91, 73, 85, 90, 76, 83, 87, 92, 80)[idx % 12],
                summary = "Balanced posture with mild forward head and rounded shoulders"
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp)
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text("←", color = textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "Reports",
                color = textPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                ReportCard(item = item, cardBg = cardBg, textPrimary = textPrimary, accentBrown = accentBrown)
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

private data class ReportListItem(
    val id: String,
    val date: String,
    val score: Int,
    val summary: String
)

@Composable
private fun ReportCard(
    item: ReportListItem,
    cardBg: Color,
    textPrimary: Color,
    accentBrown: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Scan • ${item.date}", color = textPrimary.copy(alpha = 0.9f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(item.summary, color = textPrimary, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(64.dp)
                    .background(Color(0xFFFFE0B2), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("${item.score}", color = accentBrown, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}


