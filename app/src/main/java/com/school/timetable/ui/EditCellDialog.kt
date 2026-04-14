package com.school.timetable.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import com.school.timetable.data.Subject

@Composable
fun EditCellDialog(
    subject: Subject,
    is24HourFormat: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    if (subject.periodIndex == -1) {
        // Just dismiss if they try to edit the break
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("BREAK") },
            text = { Text("Break times and names cannot be edited.") },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            }
        )
        return
    }

    var name by remember { mutableStateOf(if (subject.name.contains("Subject")) "" else subject.name) }
    var teacher by remember { mutableStateOf(if (subject.teacher == "Teacher") "" else subject.teacher) }
    var start by remember { mutableStateOf(subject.startTime) }
    var end by remember { mutableStateOf(subject.endTime) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    
    val formatTime = { time24: String ->
        if (is24HourFormat) time24
        else {
            val parts = time24.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val amPm = if (h < 12) "AM" else "PM"
            val h12 = when {
                h == 0 -> 12
                h > 12 -> h - 12
                else -> h
            }
            "${h12.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')} $amPm"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Period ${subject.periodIndex}")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("Teacher Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Start Time", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { showStartPicker = true },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatTime(start), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Edit", tint = MaterialTheme.colorScheme.outline) // placeholder icon 
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("End Time", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { showEndPicker = true },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatTime(end), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Edit", tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            if (showStartPicker) {
                WheelTimePickerDialog(
                    initialTime24h = start,
                    is24HourFormat = is24HourFormat,
                    onDismiss = { showStartPicker = false },
                    onTimeSelected = { time -> start = time; showStartPicker = false }
                )
            }
            if (showEndPicker) {
                WheelTimePickerDialog(
                    initialTime24h = end,
                    is24HourFormat = is24HourFormat,
                    onDismiss = { showEndPicker = false },
                    onTimeSelected = { time -> end = time; showEndPicker = false }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name.ifEmpty { "Empty" }, teacher.ifEmpty { "-" }, start.ifEmpty { "00:00" }, end.ifEmpty { "00:00" })
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
