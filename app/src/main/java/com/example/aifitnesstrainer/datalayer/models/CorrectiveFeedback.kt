package com.example.aifitnesstrainer.datalayer.models

import androidx.compose.runtime.currentCompositionErrors


class CorrectiveFeedback {
    private val feedbackTargetJoints = FeedbackConfig.feedbackTargetJoints
    private val indexToKeyPointMap = FeedbackConfig.indexToKeyPointMap
    private val feedbackPhrases = FeedbackConfig.feedbackPhrases
    private val cooldownTime = 8000L
    private val movementThreshold = 10

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
        lastAngles = currentAngles

        val targetJoint = feedbackTargetJoints[movement.name] ?: return ""

        val stateAngles = if (movement.currentState == Movement.State.UP) {
            movement.upStateAngles
        } else {
            movement.downStateAngles
        }

        for ((joint, expectedAngle) in stateAngles) {
            val currentAngle = currentAngles[joint] ?: continue

            if (kotlin.math.abs(currentAngle - expectedAngle) > movement.tolerance) {
                val correction = if (currentAngle < expectedAngle) {
                    if (movement.currentState == Movement.State.UP) "higher_up" else "higher_down"
                } else {
                    if (movement.currentState == Movement.State.UP) "lower_up" else "lower_down"
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

    private fun provideFeedback(joint: String, correction: String): String {
        val phrases = feedbackPhrases[correction] ?: listOf("Adjust your %s.")
        val phrase = phrases.random()
        return phrase.format(joint)
    }
}

