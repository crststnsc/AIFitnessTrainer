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
import com.example.aifitnesstrainer.datalayer.ml.KeyPoint

import com.example.aifitnesstrainer.datalayer.models.BoundingBox
import com.example.aifitnesstrainer.datalayer.models.Constants
import kotlin.math.acos
import kotlin.math.sqrt

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results = listOf<BoundingBox>()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    init {
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.WHITE
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
        Pair(2, 12), Pair(3, 13) // connect upper and lower body
    )

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        // Pre-compute scaled keypoints and joint angles
        val scaledKeyPoints = results.map { boundingBox ->
            boundingBox.keyPoints.map { keypoint ->
                Pair(keypoint.x * width, keypoint.y * height)
            }
        }

        val jointAngles = if (results.isNotEmpty()) {
            Constants.JOINTS_ANGLE_POINTS.map { joint ->
                val (start, middle, end) = joint.value
                val angle = getAngle(
                    results[0].keyPoints[start.value],
                    results[0].keyPoints[middle.value],
                    results[0].keyPoints[end.value]
                )
                Pair(middle.value, angle)
            }
        } else {
            emptyList()
        }

        // Draw keypoints
        scaledKeyPoints.forEach { boundingBox ->
            boundingBox.forEach { (keypointX, keypointY) ->
                canvas.drawCircle(keypointX, keypointY, KEYPOINT_RADIUS, keypointPaint)
            }
        }

        // Draw edges and joint angles
        if (results.isNotEmpty()) {
            val boundingBox = scaledKeyPoints[0]
            edges.forEach { edge ->
                val (startX, startY) = boundingBox[edge.first]
                val (endX, endY) = boundingBox[edge.second]
                canvas.drawLine(startX, startY, endX, endY, boxPaint)
            }

            jointAngles.forEach { (middleIndex, angle) ->
                val (x, y) = boundingBox[middleIndex]
                canvas.drawText("${angle}°", x, y, textPaint)
            }
        }
    }

    private fun getAngle(
        startKeyPoint: KeyPoint,
        midKeyPoint: KeyPoint,
        endKeyPoint: KeyPoint
    ): Int {
        val start = Pair(startKeyPoint.x, startKeyPoint.y)
        val mid = Pair(midKeyPoint.x, midKeyPoint.y)
        val end = Pair(endKeyPoint.x, endKeyPoint.y)

        val v1 = Pair(mid.first - start.first, mid.second - start.second)
        val v2 = Pair(mid.first - end.first, mid.second - end.second)

        val dotProduct = v1.first * v2.first + v1.second * v2.second
        val magnitudeV1 = sqrt((v1.first * v1.first + v1.second * v1.second).toDouble())
        val magnitudeV2 = sqrt((v2.first * v2.first + v2.second * v2.second).toDouble())

        val angle = Math.toDegrees(acos(dotProduct / (magnitudeV1 * magnitudeV2)))
        return angle.toInt()
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