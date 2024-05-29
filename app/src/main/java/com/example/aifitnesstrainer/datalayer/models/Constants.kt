package com.example.aifitnesstrainer.datalayer.models

enum class KEYPOINTS(val value: Int) {
    R_ANKLE(0),
    R_KNEE(1),
    R_HIP(2),
    L_HIP(3),
    L_KNEE(4),
    L_ANKLE(5),
    PELVIS(6),
    THORAX(7),
    UPPER_NECK(8),
    HEAD_TOP(9),
    R_WRIST(10),
    R_ELBOW(11),
    R_SHOULDER(12),
    L_SHOULDER(13),
    L_ELBOW(14),
    L_WRIST(15)
}

object Constants {
    val MODELS = arrayOf("yolov8n-pose-custom.tflite", "yolov8s-pose-custom.tflite")

    val JOINTS_ANGLE_POINTS = hashMapOf(
        "right_elbow" to Triple(KEYPOINTS.R_WRIST, KEYPOINTS.R_ELBOW, KEYPOINTS.R_SHOULDER),
        "left_elbow" to Triple(KEYPOINTS.L_WRIST, KEYPOINTS.L_ELBOW, KEYPOINTS.L_SHOULDER),
        "right_shoulder" to Triple(KEYPOINTS.R_ELBOW, KEYPOINTS.R_SHOULDER, KEYPOINTS.R_HIP),
        "left_shoulder" to Triple(KEYPOINTS.L_ELBOW, KEYPOINTS.L_SHOULDER, KEYPOINTS.L_HIP),
        "right_hip" to Triple(KEYPOINTS.R_KNEE, KEYPOINTS.R_HIP, KEYPOINTS.R_SHOULDER),
        "left_hip" to Triple(KEYPOINTS.L_KNEE, KEYPOINTS.L_HIP, KEYPOINTS.L_SHOULDER),
        "right_knee" to Triple(KEYPOINTS.R_ANKLE, KEYPOINTS.R_KNEE, KEYPOINTS.R_HIP),
        "left_knee" to Triple(KEYPOINTS.L_ANKLE, KEYPOINTS.L_KNEE, KEYPOINTS.L_HIP)
    )
}
