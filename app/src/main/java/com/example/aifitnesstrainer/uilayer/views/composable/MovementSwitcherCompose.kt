package com.example.aifitnesstrainer.uilayer.views.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.aifitnesstrainer.datalayer.models.Constants
import com.example.aifitnesstrainer.uilayer.viewmodels.MainViewModel


@Composable
fun MovementSwitcher(
    viewModel: MainViewModel
) {
    val movementsNames = viewModel.getMovementNames()

    LazyRow(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        items(movementsNames) { movement ->
            Button(onClick = { viewModel.switchActiveMovement(movement) }) {
                Text(movement)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}
