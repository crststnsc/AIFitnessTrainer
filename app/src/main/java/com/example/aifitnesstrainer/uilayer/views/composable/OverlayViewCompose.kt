package com.example.aifitnesstrainer.uilayer.views.composable

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.aifitnesstrainer.datalayer.models.BoundingBox
import com.example.aifitnesstrainer.uilayer.views.OverlayView

@Composable
fun OverlayViewComposable(results: List<BoundingBox>, jointAngles: Map<Int, Int>) {
    AndroidView(
        modifier = Modifier.aspectRatio(3f/4f).fillMaxSize(),
        factory = { context ->
            OverlayView(context, null)
        },
        update = { view ->
            view.setResults(results, jointAngles)
        }
    )
}