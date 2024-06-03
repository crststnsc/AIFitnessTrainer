package com.example.aifitnesstrainer.uilayer.views.composable

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aifitnesstrainer.uilayer.viewmodels.MainViewModel

@Composable
fun MovementSwitcher(viewModel: MainViewModel) {
    val movements = viewModel.getMovementNames()

    Row(modifier = Modifier.padding(16.dp)) {
        movements.forEach { movement ->
            Button(onClick = { viewModel.switchActiveMovement(movement) }) {
                Text(movement)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}