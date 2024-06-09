package com.example.aifitnesstrainer.uilayer.viewmodels

import com.example.aifitnesstrainer.datalayer.models.KEYPOINTS
import com.example.aifitnesstrainer.datalayer.models.Movement

object MainViewModelConfig{
    val movements = listOf(
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
            tolerance = 20,
        ),
        Movement(
            name = "Bicep Curl",
            upStateAngles = mapOf(
                KEYPOINTS.L_ELBOW.value to 90,
                KEYPOINTS.R_ELBOW.value to 90,
            ),
            downStateAngles = mapOf(
                KEYPOINTS.L_ELBOW.value to 180,
                KEYPOINTS.R_ELBOW.value to 180,
            ),
            tolerance = 20,
        ),
        Movement(
            name = "Shoulder Press",
            upStateAngles = mapOf(
                KEYPOINTS.L_ELBOW.value to 180,
                KEYPOINTS.R_ELBOW.value to 180,
            ),
            downStateAngles = mapOf(
                KEYPOINTS.L_ELBOW.value to 90,
                KEYPOINTS.R_ELBOW.value to 90,
            ),
            tolerance = 30,
        ),
    )
}