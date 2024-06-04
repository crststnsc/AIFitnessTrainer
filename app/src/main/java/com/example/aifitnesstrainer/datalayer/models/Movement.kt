package com.example.aifitnesstrainer.datalayer.models

import android.util.Log

class Movement(
    val name: String,
    val upStateAngles: Map<Int, Int>,
    val downStateAngles: Map<Int, Int>,
    val tolerance: Int = 10
) {
    enum class State {
        UP, DOWN, NONE
    }

    var currentState = State.NONE
    private var correctReps = 0
    private var totalReps = 0
    var onRepComplete: ((Int) -> Unit)? = null
    var onProgressUpdate: ((Float) -> Unit)? = null
    var onCorrectiveFeedback: ((String) -> Unit)? = null

    fun updateAngles(currentAngles: Map<Int, Int>, correctiveFeedback: CorrectiveFeedback) {
        val firstAngle = currentAngles.values.firstOrNull()
        onProgressUpdate?.invoke(firstAngle?.toFloat() ?: 0f)

        val isUp = anglesWithinTolerance(currentAngles, upStateAngles)
        val isDown = anglesWithinTolerance(currentAngles, downStateAngles)

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
        return if (correctReps > 0) {
            "$correctReps/$totalReps"
        } else {
            "Start moving to begin counting the reps!"
        }
    }
}
