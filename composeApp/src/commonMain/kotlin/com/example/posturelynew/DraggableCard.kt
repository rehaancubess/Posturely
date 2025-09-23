package com.mobil80.posturely

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlinx.coroutines.launch

enum class SwipeResult {
  ACCEPTED,
  REJECTED
}

@Composable
fun DraggableCard(
  item: Any,
  modifier: Modifier = Modifier,
  onSwiped: (Any, Any) -> Unit,
  content: @Composable () -> Unit
) {
  val maxX = 1200f
  val swipeX = remember { Animatable(0f) }
  val swipeY = remember { Animatable(0f) }
  swipeX.updateBounds(-maxX, maxX)
  swipeY.updateBounds(-1000f, 1000f)
  if (abs(swipeX.value) < maxX - 50f) {
    val rotationFraction = (swipeX.value / 60).coerceIn(-40f, 40f)
    Card(
      elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
      modifier =
        modifier
          .dragContent(swipeX = swipeX, swipeY = swipeY, maxX = maxX, onSwiped = { _, _ -> })
          .graphicsLayer(
            translationX = swipeX.value,
            translationY = swipeY.value,
            rotationZ = rotationFraction,
          )
          .clip(RoundedCornerShape(16.dp))
    ) {
      content()
    }
  } else {
    val swipeResult = if (swipeX.value > 0) SwipeResult.ACCEPTED else SwipeResult.REJECTED
    onSwiped(swipeResult, item)
  }
}

fun Modifier.dragContent(
  swipeX: Animatable<Float, AnimationVector1D>,
  swipeY: Animatable<Float, AnimationVector1D>,
  maxX: Float,
  onSwiped: (Any, Any) -> Unit
): Modifier = composed {
  val coroutineScope = rememberCoroutineScope()
  pointerInput(Unit) {
    this.detectDragGestures(
      onDragCancel = {
        coroutineScope.apply {
          launch { swipeX.animateTo(0f) }
          launch { swipeY.animateTo(0f) }
        }
      },
      onDragEnd = {
        coroutineScope.apply {
          if (abs(swipeX.targetValue) < abs(maxX) / 4) {
            launch { swipeX.animateTo(0f, tween(300)) }
            launch { swipeY.animateTo(0f, tween(300)) }
          } else {
            launch {
              if (swipeX.targetValue > 0) {
                swipeX.animateTo(maxX, tween(300))
              } else {
                swipeX.animateTo(-maxX, tween(300))
              }
            }
          }
        }
      }
    ) { change, dragAmount ->
      change.consumePositionChange()
      coroutineScope.apply {
        launch { swipeX.animateTo(swipeX.targetValue + dragAmount.x) }
        launch { swipeY.animateTo(swipeY.targetValue + dragAmount.y) }
      }
    }
  }
}


