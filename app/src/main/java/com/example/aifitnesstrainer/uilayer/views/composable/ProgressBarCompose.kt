package com.example.aifitnesstrainer.uilayer.views.composable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@Composable
fun MovementProgressBar(progress: Float) {
    Canvas(modifier = Modifier
        .height(150.dp)
        .width(150.dp)
    ) {
        val startAngle = 180f
        val maxSweepAngle = 180f
        val sweepAngle = progress * maxSweepAngle

        drawArc(
            color = Color.DarkGray,
            startAngle = startAngle,
            sweepAngle = maxSweepAngle,
            useCenter = false,
            style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Round)
        )

        drawArc(
            color = Color.LightGray,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}


