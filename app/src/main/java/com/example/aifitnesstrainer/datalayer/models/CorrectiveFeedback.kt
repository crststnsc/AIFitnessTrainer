package com.example.aifitnesstrainer.datalayer.models


class CorrectiveFeedback {
    private val feedbackTargetJoints = FeedbackConfig.feedbackTargetJoints
    private val indexToKeyPointMap = FeedbackConfig.indexToKeyPointMap
    private val feedbackPhrases = FeedbackConfig.feedbackPhrases
    private val cooldownTime = 6000L

    private var lastFeedbackTime = System.currentTimeMillis()

    fun analyzeAngles(currentAngles: Map<Int, Int>, movement: Movement): String {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFeedbackTime < cooldownTime) {
            return ""
        }

        lastFeedbackTime = currentTime

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

    private fun provideFeedback(joint: String, correction: String): String {
        val phrases = feedbackPhrases[correction] ?: listOf("Adjust your %s.")
        val phrase = phrases.random()
        return phrase.format(joint)
    }
}

