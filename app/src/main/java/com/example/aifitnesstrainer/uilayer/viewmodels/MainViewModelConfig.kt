package com.example.aifitnesstrainer.uilayer.viewmodels

import com.example.aifitnesstrainer.datalayer.models.KEYPOINTS
import com.example.aifitnesstrainer.datalayer.models.Movement

object MainViewModelConfig{
    val movements = listOf(
        Movement(
            name = "Lateral Arm Raises",
            upStateAngles = mapOf(
                KEYPOINTS.L_SHOULDER.value to 90,
                KEYPOINTS.R_SHOULDER.value to 90,
            ),
            downStateAngles = mapOf(
                KEYPOINTS.L_SHOULDER.value to 20,
                KEYPOINTS.R_SHOULDER.value to 20,
            ),
        ),
        Movement(
            name = "Shoulder Press",
            upStateAngles = mapOf(
                KEYPOINTS.L_ELBOW.value to 160,
                KEYPOINTS.R_ELBOW.value to 160,
            ),
            downStateAngles = mapOf(
                KEYPOINTS.L_ELBOW.value to 70,
                KEYPOINTS.R_ELBOW.value to 70,
            ),
            tolerance = 30,
        ),
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
            tolerance = 30,
        ),
    )
}