package com.mobil80.posturely

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import posturelynew.composeapp.generated.resources.Res
import posturelynew.composeapp.generated.resources.study1
import posturelynew.composeapp.generated.resources.study2
import posturelynew.composeapp.generated.resources.study3

data class DemoCard(val title: String, val body: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun CaseStudiesScreen(
    onSkip: () -> Unit,
    onFinish: () -> Unit
) {
  Surface(modifier = Modifier.fillMaxSize()) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
      val constraintsMaxHeight = maxHeight
      val cards = remember {
        mutableStateListOf(
          DemoCard(
            "Lower Back Pain & Sitting",
            "A Harvard Medical School study found that people sitting with poor posture for more than 4 hours/day reported 2× higher incidence of lower back pain compared to those who maintained neutral spine posture."
          ),
          DemoCard(
            "Breathing Efficiency",
            "A University of California study showed that slouched posture can reduce lung capacity by up to 30%, directly impacting oxygen intake, focus, and energy levels."
          ),
          DemoCard(
            "Workplace Productivity",
            "A 2018 ergonomics case study at a Japanese IT company reported a 17% increase in productivity after employees received posture coaching and ergonomic seating adjustments."
          ),
          DemoCard(
            "Confidence & Mood",
            "Ohio State University research found that people who sat upright reported higher self-esteem and mood, while slouched posture was linked to negative emotions and increased stress."
          ),
          DemoCard(
            "Digital Device Usage",
            "“Text neck” has become a clinical term. A biomechanics case study revealed that tilting the head forward 60° while looking at a phone increases cervical spine stress from 10–12 lbs to 60 lbs—equivalent to carrying a child on your neck."
          ),
          DemoCard(
            "Children & Posture",
            "A UK study (Royal Society for Public Health) found that 68% of children aged 8–12 already show early signs of postural strain from device use, predicting future musculoskeletal problems."
          ),
          DemoCard(
            "Sports & Posture",
            "Case studies on athletes show that posture correction programs improve balance and muscle activation efficiency, reducing injury risk and improving performance consistency."
          ),
        )
      }
      val totalCount = remember { cards.size }
      val images = remember { listOf(Res.drawable.study1, Res.drawable.study2, Res.drawable.study3) }

      // Colors to match earlier style
      val pageBg = Color(0xFFFED867)
      val textPrimary = Color(0xFF0F1931)
      val accentBrown = Color(0xFF7A4B00)
      val cardBg = Color(0xFFFFF0C0)

      Column(
        modifier = Modifier
          .fillMaxSize()
          .background(pageBg)
      ) {
        // App bar
        CenterAlignedTopAppBar(
          title = {
            Text(
              text = "Case Studies",
              color = textPrimary,
              fontSize = 30.sp,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center
            )
          },
          actions = {
            Text(
              text = "Skip",
              color = textPrimary,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier
                .padding(end = 12.dp)
                .clickable { onSkip() }
            )
          },
          colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = pageBg,
            titleContentColor = textPrimary
          )
        )

        // Card stack area
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        ) {
          cards.forEachIndexed { index, card ->
            val imgRes = images[index % images.size]
            val isTop = index == cards.lastIndex
            DraggableCard(
              item = card,
              modifier =
                Modifier
                  .align(Alignment.TopCenter)
                  .fillMaxWidth(0.86f)
                  .height((constraintsMaxHeight - 300.dp).coerceAtLeast(200.dp))
                  .padding(top = (if (isTop) 24.dp else 8.dp) + (index + 1).dp, bottom = 12.dp, end = if (isTop) 8.dp else 0.dp),
              onSwiped = { _, swiped ->
                cards.remove(swiped)
                if (cards.isEmpty()) onFinish()
              }
          ) {
            Surface(
              modifier = Modifier
                .fillMaxSize()
                .border(2.dp, accentBrown, RoundedCornerShape(16.dp)),
              color = cardBg,
              shape = RoundedCornerShape(16.dp)
            ) {
               Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
              ) {
                  Image(
                    painter = painterResource(imgRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentScale = ContentScale.Fit
                  )
                  Text(
                    text = card.title,
                    color = textPrimary,
                    fontSize = if (isTop) 26.sp else 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                  )
                  Text(
                    text = card.body,
                    color = textPrimary,
                    fontSize = if (isTop) 18.sp else 17.sp,
                    lineHeight = if (isTop) 26.sp else 24.sp,
                    letterSpacing = 0.3.sp
                  )
              }
            }
          }
          }
        }

        // Bottom next button
        Row(
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
          val currentIndex = ((totalCount - cards.size) + 1).coerceIn(1, totalCount)
          Button(
            onClick = {
              if (cards.isNotEmpty()) {
                if (cards.size == 1) {
                  cards.removeAt(0)
                  onFinish()
                } else {
                  cards.removeAt(0)
                }
              } else onFinish()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentBrown)
          ) {
            Text(
              "Next (${currentIndex}/${totalCount})",
              color = Color.White,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

