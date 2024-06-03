package com.example.aifitnesstrainer.uilayer.views.composable

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aifitnesstrainer.uilayer.viewmodels.MainViewModel

@Composable
fun FeedbackView(viewModel: MainViewModel) {
    val feedback by viewModel.feedback.collectAsState()

    Text(
        text = feedback,
        modifier = Modifier.padding(16.dp),
        color = Color.Black,
        fontSize = 20.sp
    )
}