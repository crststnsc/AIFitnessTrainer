package com.example.aifitnesstrainer.uilayer.views.composable

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aifitnesstrainer.uilayer.viewmodels.MainViewModel

@Composable
fun MovementSwitcher(viewModel: MainViewModel) {
    val movements = viewModel.getMovementNames()

    LazyRow(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        items(movements) { movement ->
            Button(onClick = { viewModel.switchActiveMovement(movement) }) {
                Text(movement)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        item {
            Button(onClick = { /*TODO*/ }) {
                Text("+")
            }
        }
    }
}
