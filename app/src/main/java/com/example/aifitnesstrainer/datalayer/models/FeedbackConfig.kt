package com.example.aifitnesstrainer.datalayer.models

object FeedbackConfig {
    val feedbackTargetJoints = mapOf(
        "Squat" to KEYPOINTS.R_HIP.value,
        "Bicep Flex" to KEYPOINTS.L_WRIST.value,
    )

    val indexToKeyPointMap = mapOf(
        0 to "ankles",
        1 to "knees",
        2 to "hips",
        3 to "hips",
        4 to "knees",
        5 to "ankles",
        6 to "pelvis",
        7 to "thorax",
        8 to "upper neck",
        9 to "head top",
        10 to "wrists",
        11 to "elbows",
        12 to "shoulders",
        13 to "shoulders",
        14 to "elbows",
        15 to "wrists"
    )

    val feedbackPhrases = mapOf(
        "higher_up" to listOf(
            "Try raising your %s higher.",
            "Lift your %s up more.",
            "Elevate your %s a bit higher."
        ),
        "lower_up" to listOf(
            "Your %s should go lower.",
            "Drop your %s a bit.",
            "Lower your %s slightly."
        ),
        "higher_down" to listOf(
            "Raise your %s higher during the descent.",
            "Lift your %s up more as you go down.",
            "Keep your %s elevated a bit higher while descending."
        ),
        "lower_down" to listOf(
            "Your %s should go lower during the descent.",
            "Drop your %s a bit as you go down.",
            "Lower your %s slightly while descending."
        ),
        "good_form" to listOf(
            "Good form! Keep it up!",
            "Great job!",
            "You're doing great!",
            ""
        )
    )
}
