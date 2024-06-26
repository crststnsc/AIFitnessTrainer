package com.example.aifitnesstrainer.uilayer.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.aifitnesstrainer.datalayer.ml.KeyPoint
import com.example.aifitnesstrainer.datalayer.models.BoundingBox
import com.example.aifitnesstrainer.datalayer.models.Constants
import com.example.aifitnesstrainer.datalayer.models.CorrectiveFeedback
import com.example.aifitnesstrainer.datalayer.models.KEYPOINTS
import com.example.aifitnesstrainer.datalayer.models.Movement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _results = MutableStateFlow<List<BoundingBox>>(emptyList())
    val results: StateFlow<List<BoundingBox>> = _results

    private val _jointAngles = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val jointAngles: StateFlow<Map<Int, Int>> = _jointAngles

    private val _inferenceTime = MutableStateFlow(0L)
    val inferenceTime: StateFlow<Long> = _inferenceTime

    private val _feedback = MutableStateFlow("")
    val feedback: StateFlow<String> = _feedback

    private val _movementProgress = MutableStateFlow(0f)
    val movementProgress: StateFlow<Float> = _movementProgress

    private val correctiveFeedbackAnalyzer = CorrectiveFeedback()

    private val _movements = MutableStateFlow(MainViewModelConfig.movements)

    private var activeMovement: Movement? = null
    private var speakCallback: ((String) -> Unit)? = null

    var _activeMovementString = MutableStateFlow("")

    private val _movementStatus = MutableStateFlow("not moving")

    private fun updateMovementStatus(status: String) {
        _movementStatus.value = status
    }

    fun registerSpeakCallback(callback: (String) -> Unit) {
        speakCallback = callback
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

            activeMovement?.updateAngles(jointAngles, correctiveFeedbackAnalyzer)
            _feedback.value = activeMovement?.getFeedback() ?: "0"
        } else {
            _jointAngles.value = emptyMap()
        }
    }

    fun switchActiveMovement(movementName: String) {
        activeMovement = _movements.value.find { it.name == movementName }
        speakCallback?.invoke("Starting $movementName.")

        _activeMovementString.value = movementName

        activeMovement?.onRepComplete = { reps ->
            speakCallback?.invoke("You have completed $reps reps")
        }

        activeMovement?.onProgressUpdate = { progress ->
            val downAngle = activeMovement?.downStateAngles?.values?.first() ?: 0
            val upAngle = activeMovement?.upStateAngles?.values?.first() ?: 1

            val movementRange = abs(upAngle - downAngle)
            val normalizedProgress = if (downAngle < upAngle) {
                ((progress - downAngle).coerceIn(0f, movementRange.toFloat())) / movementRange.toFloat()
            } else {
                ((downAngle - progress).coerceIn(0f, movementRange.toFloat())) / movementRange.toFloat()
            }

            _movementProgress.value = normalizedProgress
        }

        activeMovement?.onCorrectiveFeedback = { feedback ->
            speakCallback?.invoke(feedback)
        }

        activeMovement?.onMovementStatusChange = { status ->
            updateMovementStatus(status)
        }
    }

    fun getMovementNames(): List<String> {
        return _movements.value.map { it.name }
    }

    fun clearResults() {
        _results.value = emptyList()
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
