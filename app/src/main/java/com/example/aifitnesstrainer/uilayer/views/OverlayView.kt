package com.example.aifitnesstrainer.uilayer.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

import com.example.aifitnesstrainer.datalayer.models.BoundingBox

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results = listOf<BoundingBox>()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    init {
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        boxPaint.color = Color.WHITE
        boxPaint.strokeWidth = 10F
        boxPaint.style = Paint.Style.STROKE
    }

    private val KEYPOINT_RADIUS = 10.0f // Adjust the size as needed
    private val keypointPaint = Paint().apply {
        color = Color.WHITE // Set your desired color
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }

    private val edges = listOf(
        Pair(0, 1), Pair(1, 2), // right leg
        Pair(4, 5), Pair(3, 4), // left leg
        Pair(2, 6), Pair(6,3), // connect hips
        Pair(10, 11), Pair(11, 12), // right arm
        Pair(13, 14), Pair(14, 15), // left arm
        Pair(13, 7), Pair(7, 12), // connect shoulders
        Pair(6, 7) // connect upper and lower body
    )

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        results.forEach { boundingBox ->
            boundingBox.keyPoints.forEach { keypoint ->
                // Scale the keypoint coordinates considering the aspect ratio
                val keypointX = keypoint.x * width
                val keypointY = keypoint.y * height

                // Draw a circle at each keypoint
                canvas.drawCircle(keypointX, keypointY, KEYPOINT_RADIUS, keypointPaint)
            }
        }

        if (results.isNotEmpty()){
            results.forEach { boundingBox ->
                for (edge in edges) {
                    val start = boundingBox.keyPoints[edge.first]
                    val end = boundingBox.keyPoints[edge.second]
                    canvas.drawLine(
                        start.x * width,
                        start.y * height,
                        end.x * width,
                        end.y * height,
                        boxPaint
                    )
                }
            }
        }
    }

    fun setResults(boundingBoxes: List<BoundingBox>) {
        results = boundingBoxes
        postInvalidate()
    }

}

@Composable
fun OverlayViewComposable(results: List<BoundingBox>) {
    AndroidView(
        modifier = Modifier.aspectRatio(3f/4f).fillMaxSize(),
        factory = { context ->
            OverlayView(context, null)
        },
        update = { view ->
            Log.d("OverlayViewComposable", "Updating OverlayView with ${results.size} results")
            view.setResults(results)
        }
    )
}