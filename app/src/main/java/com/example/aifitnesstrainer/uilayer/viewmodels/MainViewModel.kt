package com.example.aifitnesstrainer.uilayer.viewmodels

import androidx.lifecycle.ViewModel
import com.example.aifitnesstrainer.datalayer.ml.KeyPoint
import com.example.aifitnesstrainer.datalayer.models.BoundingBox
import com.example.aifitnesstrainer.datalayer.models.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.acos
import kotlin.math.sqrt

class MainViewModel() : ViewModel() {
    private val _results = MutableStateFlow<List<BoundingBox>>(emptyList())
    val results: StateFlow<List<BoundingBox>> = _results

    private val _jointAngles = MutableStateFlow(emptyList<Pair<Int, Int>>())
    val jointAngles: StateFlow<List<Pair<Int, Int>>> = _jointAngles

    private val _inferenceTime = MutableStateFlow(0L)
    val inferenceTime: StateFlow<Long> = _inferenceTime


    fun updateResults(newResults: List<BoundingBox>, inferenceTime: Long) {
        _results.value = newResults
        calculateAngles()
        _inferenceTime.value = inferenceTime
    }

    fun clearResults() {
        _results.value = emptyList()
    }

    private fun calculateAngles(){
        val results = _results.value
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
        _jointAngles.value = jointAngles
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
}
