package com.mobil80.posturely.scan

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.mobil80.posturely.native.PoseLandmark

class ScanOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private var landmarks: List<PoseLandmark> = emptyList()
    
    private val skeletonPaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
    
    private val jointPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    // Define pose connections (same as iOS implementation)
    private val connections = listOf(
        // Face outline
        Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 7),
        Pair(0, 4), Pair(4, 5), Pair(5, 6), Pair(6, 8),
        
        // Shoulders and upper body
        Pair(9, 10),        // Mouth corners
        Pair(11, 12),       // Shoulders
        Pair(11, 13), Pair(13, 15), // Left arm
        Pair(12, 14), Pair(14, 16), // Right arm
        Pair(11, 23), Pair(12, 24), // Shoulder to hip
        
        // Torso
        Pair(23, 24),       // Hip line
        
        // Left leg
        Pair(23, 25), Pair(25, 27), Pair(27, 29), Pair(27, 31),
        
        // Right leg
        Pair(24, 26), Pair(26, 28), Pair(28, 30), Pair(28, 32),
        
        // Hand details
        Pair(15, 17), Pair(15, 19), Pair(15, 21), Pair(17, 19),
        Pair(16, 18), Pair(16, 20), Pair(16, 22), Pair(18, 20)
    )
    
    fun updateLandmarks(newLandmarks: List<PoseLandmark>) {
        landmarks = newLandmarks
        android.util.Log.d("ScanOverlayView", "Updating overlay with ${newLandmarks.size} landmarks, view size: ${width}x${height}")
        if (newLandmarks.isNotEmpty()) {
            val firstLandmark = newLandmarks[0]
            android.util.Log.d("ScanOverlayView", "First landmark: (${firstLandmark.x}, ${firstLandmark.y}), visibility: ${firstLandmark.visibility}")
        }
        invalidate() // Trigger redraw
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        android.util.Log.d("ScanOverlayView", "onDraw called, landmarks: ${landmarks.size}, canvas size: ${canvas.width}x${canvas.height}")
        
        if (landmarks.isEmpty()) {
            android.util.Log.d("ScanOverlayView", "No landmarks to draw")
            return
        }
        
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        
        if (viewWidth <= 0 || viewHeight <= 0) {
            android.util.Log.w("ScanOverlayView", "Invalid view dimensions: ${viewWidth}x${viewHeight}")
            return
        }
        
        // Mirror the entire canvas horizontally around the center (equivalent to rotationY(pi))
        canvas.save()
        canvas.scale(-1f, 1f, viewWidth / 2f, viewHeight / 2f)

        // Draw connections - MediaPipe normalized coords to screen coords (no manual mirroring)
        for ((startIdx, endIdx) in connections) {
            if (startIdx < landmarks.size && endIdx < landmarks.size) {
                val startLandmark = landmarks[startIdx]
                val endLandmark = landmarks[endIdx]

                val startX = startLandmark.x.coerceIn(0.0f, 1.0f) * viewWidth
                val startY = startLandmark.y.coerceIn(0.0f, 1.0f) * viewHeight
                val endX = endLandmark.x.coerceIn(0.0f, 1.0f) * viewWidth
                val endY = endLandmark.y.coerceIn(0.0f, 1.0f) * viewHeight

                if (landmarks.size >= 4) {
                    canvas.drawLine(startX, startY, endX, endY, skeletonPaint)
                }
            }
        }
        
        // Draw key joints as circles
        val keyJoints = listOf(0, 11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28) // Key landmarks
        for (jointIdx in keyJoints) {
            if (jointIdx < landmarks.size) {
                val landmark = landmarks[jointIdx]
                // Since MediaPipe visibility is always 0.0, use landmark count as proxy
                if (landmarks.size >= 4) { // Use landmark count instead of visibility
                    // Same coordinate transformation as connections (no manual mirror; canvas is mirrored)
                    val x = landmark.x.coerceIn(0.0f, 1.0f) * viewWidth
                    val y = landmark.y.coerceIn(0.0f, 1.0f) * viewHeight
                    canvas.drawCircle(x, y, 8f, jointPaint)
                }
            }
        }
        canvas.restore()
        
        // Debug: Draw a test line to verify the overlay is working (temporarily)
        // canvas.drawLine(100f, 100f, 200f, 200f, skeletonPaint)
        // android.util.Log.d("ScanOverlayView", "Drew test line at (100,100) to (200,200)")
    }
}
