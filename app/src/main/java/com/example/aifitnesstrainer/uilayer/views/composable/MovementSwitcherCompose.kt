package com.example.aifitnesstrainer.uilayer.views.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.aifitnesstrainer.datalayer.models.Constants
import com.example.aifitnesstrainer.uilayer.viewmodels.MainViewModel
import kotlin.math.exp

@Composable
fun MovementSwitcher(viewModel: MainViewModel) {
    val movements = viewModel.getMovementNames()
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        NewMovementDialog(
            onDismiss = { showDialog = false },
        )
    }

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
            Button(onClick = { showDialog = true }) {
                Text("+")
            }
        }
    }
}


@Composable
fun NewMovementDialog(
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var upStateAngles by remember { mutableStateOf("") }
    var downStateAngles by remember { mutableStateOf("") }
    var tolerance by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                Spacer(modifier = Modifier.height(8.dp))

                val options = Constants.JOINTS_TO_INDEX_MAP.keys.toList()

                Row {
                    DropDownMenu(options = options, selectedOption = options.first()) {
                    }

                    var value by remember { mutableStateOf("0") }
                    OutlinedTextField(value = value, onValueChange = {s: String -> value = s })
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Button(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onDismiss){
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun DropDownMenu(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
){
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(selectedOption) }

    Box(modifier = Modifier.width(200.dp)) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Joint") },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, Modifier.clickable { expanded = true })
            },
            modifier = Modifier
                .wrapContentSize()
                .clickable { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .wrapContentSize()
        ) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option)}, onClick = {
                    onOptionSelected(option)
                    selected = option
                    expanded = false
                })
            }
        }
    }
}
