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

    private var currentState = State.NONE
    private var correctReps = 0
    private var totalReps = 0
    private var startTime: Long = 0
    private var endTime: Long = 0
    var onRepComplete: ((Int) -> Unit)? = null // Callback for rep completion
    var onProgressUpdate: ((Float) -> Unit)? = null // Callback for progress update

    fun updateAngles(currentAngles: Map<Int, Int>) {
        val firstAngle = currentAngles.values.firstOrNull()
        onProgressUpdate?.invoke(firstAngle?.toFloat() ?: 0f)

        val isUp = anglesWithinTolerance(currentAngles, upStateAngles)
        val isDown = anglesWithinTolerance(currentAngles, downStateAngles)

        when {
            isUp -> {
                if (currentState != State.UP) {
                    currentState = State.UP
                    totalReps++
                    if (totalReps == 1) startTime = System.currentTimeMillis()
                }
            }
            isDown -> {
                if (currentState == State.UP) {
                    currentState = State.DOWN
                    correctReps++
                    onRepComplete?.invoke(correctReps) // Notify rep completion
                    if (correctReps == 1) endTime = System.currentTimeMillis()
                }
            }
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
        val duration = (endTime - startTime) / 1000 // Duration in seconds
        return if (correctReps > 0) {
            "Good job! You completed $correctReps of $name reps in $duration seconds."
        } else {
            "Keep going! Try to reach the correct form."
        }
    }
}
