package com.school.timetable.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.clickable
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.school.timetable.data.Subject

@Composable
fun EditCellDialog(
    subject: Subject,
    subjectTeachers: Map<String, String>,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
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
    var showSubjectDropdown by remember { mutableStateOf(false) }

    // Sync teacher whenever name changes from dropdown
    androidx.compose.runtime.LaunchedEffect(name) {
        subjectTeachers[name]?.let { teacher = it }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth(0.9f),
        title = {
            Text("Edit Period ${subject.periodIndex}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Subject (Pick from List)") },
                        modifier = Modifier.fillMaxWidth().clickable { showSubjectDropdown = true },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { showSubjectDropdown = !showSubjectDropdown }) {
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                    )
                    
                    DropdownMenu(
                        expanded = showSubjectDropdown,
                        onDismissRequest = { showSubjectDropdown = false },
                        modifier = Modifier.width(280.dp)
                    ) {
                        val allSubjects = subjectTeachers.keys.toList().sorted()
                        if (allSubjects.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No subjects in Manage Teachers", fontSize = 12.sp, color = MaterialTheme.colorScheme.error) },
                                onClick = { showSubjectDropdown = false }
                            )
                        } else {
                            allSubjects.forEach { subName ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(subName, fontWeight = FontWeight.Bold)
                                            subjectTeachers[subName]?.let { 
                                                Text("Teacher: $it", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary) 
                                            }
                                        }
                                    },
                                    onClick = {
                                        name = subName
                                        showSubjectDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Teacher Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Bell timings are locked here. Change them in Settings.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name.ifEmpty { "Empty" }, teacher.ifEmpty { "-" })
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
