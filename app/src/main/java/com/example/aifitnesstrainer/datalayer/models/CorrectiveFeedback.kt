package com.example.aifitnesstrainer.datalayer.models

import kotlin.math.pow


class CorrectiveFeedback {
    private val feedbackTargetJoints = FeedbackConfig.feedbackTargetJoints
    private val indexToKeyPointMap = FeedbackConfig.indexToKeyPointMap
    private val feedbackPhrases = FeedbackConfig.feedbackPhrases
    private val cooldownTime = 6000L
    private val movementThreshold = 15

    private var lastFeedbackTime = System.currentTimeMillis()
    private var lastAngles: Map<Int, Int>? = null

    fun analyzeAngles(currentAngles: Map<Int, Int>, movement: Movement): String {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFeedbackTime < cooldownTime) {
            return ""
        }

        if (isSignificantlyMoving(currentAngles, movement)) {
            lastAngles = currentAngles
            return ""
        }

        lastFeedbackTime = currentTime

        val targetJoint = feedbackTargetJoints[movement.name] ?: return ""
        val state = checkMovementDirection(currentAngles, movement)

        lastAngles = currentAngles

        val stateAngles = if (state == Movement.State.UP) {
            movement.upStateAngles
        } else {
            movement.downStateAngles
        }

        for ((joint, expectedAngle) in stateAngles) {
            val currentAngle = currentAngles[joint] ?: continue

            if (kotlin.math.abs(currentAngle - expectedAngle) > movement.tolerance) {
                val correction = if (currentAngle < expectedAngle) {
                    if (state == Movement.State.UP) "higher_up" else "higher_down"
                } else {
                    if (state == Movement.State.UP) "lower_up" else "lower_down"
                }
                lastFeedbackTime = currentTime
                return provideFeedback(indexToKeyPointMap[targetJoint] ?: "joint", correction)
            }
        }

        lastFeedbackTime = currentTime
        return feedbackPhrases["good_form"]?.random() ?: ""
    }

    fun isSignificantlyMoving(currentAngles: Map<Int, Int>, movement: Movement): Boolean {
        val previousAngles = lastAngles ?: return false
        val relevantJoints = movement.upStateAngles.keys

        for (joint in relevantJoints) {
            val currentAngle = currentAngles[joint] ?: continue
            val previousAngle = previousAngles[joint] ?: continue
            if (kotlin.math.abs(currentAngle - previousAngle) > movementThreshold) {
                return true
            }
        }
        return false
    }

    private fun checkNearestState(
        currentAngles: Map<Int, Int>,
        upAngles: Map<Int, Int>,
        downAngles: Map<Int, Int>
    ): Movement.State{
        val upDistance = calculateDistance(currentAngles, upAngles)
        val downDistance = calculateDistance(currentAngles, downAngles)

        if (upDistance < downDistance) {
            return Movement.State.UP
        }

        return Movement.State.DOWN
    }

    private fun checkMovementDirection(
        currentAngles: Map<Int, Int>,
        movement: Movement
    ): Movement.State {
        for ((key, _) in movement.upStateAngles) {
            val currentAngle = currentAngles[key] ?: return Movement.State.NONE
            val previousAngle = lastAngles?.get(key) ?: return Movement.State.NONE

            if(currentAngle > previousAngle) return Movement.State.UP
        }

        return Movement.State.DOWN
    }


    private fun calculateDistance(
        currentAngles: Map<Int, Int>,
        targetAngles: Map<Int, Int>
    ): Double {
        var distance = 0.0
        for ((joint, targetAngle) in targetAngles) {
            val currentAngle = currentAngles[joint] ?: continue
            distance += kotlin.math.abs(currentAngle - targetAngle).toDouble().pow(2)
        }
        return kotlin.math.sqrt(distance)
    }

    private fun provideFeedback(joint: String, correction: String): String {
        val phrases = feedbackPhrases[correction] ?: listOf("Adjust your %s.")
        val phrase = phrases.random()
        return phrase.format(joint)
    }
}

