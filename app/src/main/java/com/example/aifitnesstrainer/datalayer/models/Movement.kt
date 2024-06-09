package com.example.aifitnesstrainer.datalayer.models

import android.util.Log

class Movement(
    val name: String,
    val upStateAngles: Map<Int, Int>,
    val downStateAngles: Map<Int, Int>,
    val tolerance: Int = 10,
) {
    enum class State {
        UP, DOWN, NONE
    }

    private var currentState = State.NONE
    private var correctReps = 0
    var onRepComplete: ((Int) -> Unit)? = null
    var onProgressUpdate: ((Float) -> Unit)? = null
    var onCorrectiveFeedback: ((String) -> Unit)? = null
    var onMovementStatusChange: ((String) -> Unit)? = null

    private var isMoving = false

    fun updateAngles(currentAngles: Map<Int, Int>, correctiveFeedback: CorrectiveFeedback) {
        val isUp = anglesWithinTolerance(currentAngles, upStateAngles)
        val isDown = anglesWithinTolerance(currentAngles, downStateAngles)

        val isSignificantlyMoving = correctiveFeedback.isSignificantlyMoving(currentAngles, this)
        if (isSignificantlyMoving && !isMoving) {
            isMoving = true
            onMovementStatusChange?.invoke("moving")
        } else if (!isSignificantlyMoving && isMoving) {
            isMoving = false
            onMovementStatusChange?.invoke("not moving")
        }

        when {
            isUp -> {
                if (currentState == State.DOWN) {
                    correctReps++
                }
                currentState = State.UP
            }
            isDown -> {
                if (currentState == State.UP) {
                    currentState = State.DOWN
                }
            }
        }
        val feedback = correctiveFeedback.analyzeAngles(currentAngles, this)
        if (feedback.isNotEmpty()) {
            onCorrectiveFeedback?.invoke(feedback)
        }

        val firstAngle = upStateAngles.keys.toList()[0]
        val angle = currentAngles[firstAngle]
        onProgressUpdate?.invoke(angle?.toFloat() ?: 0f)
    }

    private fun anglesWithinTolerance(
        currentAngles: Map<Int, Int>,
        targetAngles: Map<Int, Int>
    ): Boolean {
        for ((key, value) in targetAngles) {
            val currentAngle = currentAngles[key] ?: return false
            if (kotlin.math.abs(currentAngle - value) > tolerance) return false
        }
        return true
    }


    fun getFeedback(): String {
        return "$correctReps"
    }
}
