package com.example.aifitnesstrainer.uilayer.viewmodels

import androidx.lifecycle.ViewModel
import com.example.aifitnesstrainer.datalayer.ml.KeyPoint
import com.example.aifitnesstrainer.datalayer.models.BoundingBox
import com.example.aifitnesstrainer.datalayer.models.Constants
import com.example.aifitnesstrainer.datalayer.models.KEYPOINTS
import com.example.aifitnesstrainer.datalayer.models.Movement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.acos
import kotlin.math.sqrt

class MainViewModel : ViewModel() {
    private val _results = MutableStateFlow<List<BoundingBox>>(emptyList())
    val results: StateFlow<List<BoundingBox>> = _results

    private val _jointAngles = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val jointAngles: StateFlow<Map<Int, Int>> = _jointAngles

    private val _inferenceTime = MutableStateFlow(0L)
    val inferenceTime: StateFlow<Long> = _inferenceTime

    private val movements = mutableListOf<Movement>()

    private val _feedback = MutableStateFlow("")
    val feedback: StateFlow<String> = _feedback


    init {
        // Define the movements you want to track
        movements.add(
            Movement(
                name = "Squat",
                upStateAngles = mapOf(
                    KEYPOINTS.L_KNEE.value to 180,
                    KEYPOINTS.R_KNEE.value to 180,
                ),
                downStateAngles = mapOf(
                    KEYPOINTS.L_KNEE.value to 90,
                    KEYPOINTS.R_KNEE.value to 90,
                ),
                tolerance = 20
            )
//            Movement(
//                name = "Bicep Flex",
//                upStateAngles = mapOf(
//                    KEYPOINTS.R_ELBOW.value to 90
//                ),
//                downStateAngles = mapOf(
//                    KEYPOINTS.R_ELBOW.value to 180
//                ),
//                tolerance = 20
//            )
        )
    }

    fun updateResults(newResults: List<BoundingBox>, inferenceTime: Long) {
        _results.value = newResults
        _inferenceTime.value = inferenceTime

        if (newResults.isNotEmpty()) {
            val jointAngles = Constants.JOINTS_ANGLE_POINTS.map { joint ->
                val (start, middle, end) = joint.value
                val angle = getAngle(
                    newResults[0].keyPoints[start.value],
                    newResults[0].keyPoints[middle.value],
                    newResults[0].keyPoints[end.value]
                )
                middle.value to angle
            }.toMap()
            _jointAngles.value = jointAngles

            // Update movements with the current angles
            movements.forEach { it.updateAngles(jointAngles) }

            _feedback.value = movements.joinToString("\n") { it.getFeedback() }

        } else {
            _jointAngles.value = emptyMap()
        }
    }

    fun clearResults() {
        _results.value = emptyList()
    }

    private fun calculateAngles(bboxes: List<BoundingBox>): List<Pair<Int, Int>> {
        val results = bboxes
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
        return jointAngles
    }

    fun getMovementFeedback(): String {
        return movements.joinToString("\n") { it.getFeedback() }
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
