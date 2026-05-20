package com.school.timetable.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.*

@Composable
fun TimePickerContent(
    selectionKey: Any?,
    initialTime: String,
    is24Hour: Boolean,
    onBackRequest: (() -> Unit)? = null,
    onTimeCompleted: (String) -> Unit
) {
    val parts = initialTime.split(":")
    val initialH = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val initialM = parts.getOrNull(1)?.toIntOrNull() ?: 0
    
    var isPickingHour by remember(selectionKey) { mutableStateOf(true) }
    
    val initialDisplayH = remember(initialTime, is24Hour) {
        if (!is24Hour) {
            when {
                initialH == 0 -> 12
                initialH > 12 -> initialH - 12
                else -> initialH
            }
        } else initialH
    }

    var hour by remember(selectionKey) { mutableStateOf(initialDisplayH) }
    var minute by remember(selectionKey) { mutableStateOf(initialM) }
    var selectedAmPm by remember(selectionKey) { 
        mutableStateOf(if (initialH >= 12 && !is24Hour) "PM" else "AM") 
    }
    
    LaunchedEffect(selectionKey) {
        hour = initialDisplayH
        minute = initialM
        isPickingHour = true
    }
    
    val latestOnTimeCompleted by rememberUpdatedState(onTimeCompleted)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            TimeDisplayPart(
                value = hour.toString().padStart(2, '0'),
                isActive = isPickingHour,
                onClick = { isPickingHour = true }
            )
            Text(" : ", fontSize = 34.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            TimeDisplayPart(
                value = minute.toString().padStart(2, '0'),
                isActive = !isPickingHour,
                onClick = { isPickingHour = false }
            )
            
            if (!is24Hour) {
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AmPmToggle("AM", selectedAmPm == "AM") { selectedAmPm = "AM" }
                    AmPmToggle("PM", selectedAmPm == "PM") { selectedAmPm = "PM" }
                }
            }
        }
        
        Spacer(Modifier.height(20.dp))
        
        Box(
            modifier = Modifier
                .size(240.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AnalogClockFace(
                isHour = isPickingHour,
                selectedValue = if (isPickingHour) hour else minute,
                onValueChange = { newValue ->
                    if (isPickingHour) hour = newValue
                    else minute = newValue
                },
                onSelectionFinished = {
                    if (isPickingHour) isPickingHour = false
                }
            )
        }
        
        Spacer(Modifier.height(24.dp))
        
        ElevatedButton(
            onClick = {
                val finalH = if (!is24Hour) {
                    when {
                        selectedAmPm == "PM" && hour < 12 -> hour + 12
                        selectedAmPm == "AM" && hour == 12 -> 0
                        else -> hour
                    }
                } else hour
                latestOnTimeCompleted("${finalH.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}")
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 6.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text("CONFIRM TIME", fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
        
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun AnalogClockFace(
    isHour: Boolean,
    selectedValue: Int,
    onValueChange: (Int) -> Unit,
    onSelectionFinished: () -> Unit
) {
    val density = LocalDensity.current
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnFinished by rememberUpdatedState(onSelectionFinished)

    var dragAngle by remember(isHour) { 
        val anglePerPart = if (isHour) 30f else 6f
        mutableStateOf((selectedValue % if (isHour) 12 else 60) * anglePerPart) 
    }
    
    LaunchedEffect(isHour, selectedValue) {
        val anglePerPart = if (isHour) 30f else 6f
        dragAngle = (selectedValue % if (isHour) 12 else 60) * anglePerPart
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val center = Offset(constraints.maxWidth / 2f, constraints.maxHeight / 2f)
        val radius = center.x - with(density) { 32.dp.toPx() }
        val primaryColor = MaterialTheme.colorScheme.primary

        Box(
            modifier = Modifier.fillMaxSize()
                .pointerInput(isHour) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val angle = calculateAngle(offset, center)
                            dragAngle = angle
                            currentOnValueChange(calculateValue(angle, isHour))
                        },
                        onDrag = { change, _ ->
                            val angle = calculateAngle(change.position, center)
                            dragAngle = angle
                            currentOnValueChange(calculateValue(angle, isHour))
                        },
                        onDragEnd = { currentOnFinished() }
                    )
                }
                .pointerInput(isHour) {
                    detectTapGestures { offset ->
                        val angle = calculateAngle(offset, center)
                        dragAngle = angle
                        currentOnValueChange(calculateValue(angle, isHour))
                        currentOnFinished()
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val handAngleRad = (dragAngle - 90f) * PI / 180f
                val handEnd = Offset(center.x + radius * cos(handAngleRad).toFloat(), center.y + radius * sin(handAngleRad).toFloat())

                drawLine(
                    brush = Brush.linearGradient(listOf(primaryColor, primaryColor.copy(alpha = 0.3f))),
                    start = center, end = handEnd, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round
                )
                
                drawCircle(color = primaryColor, radius = 6.dp.toPx(), center = center)
                drawCircle(color = primaryColor, radius = 22.dp.toPx(), center = handEnd, style = Stroke(width = 2.dp.toPx()))
                drawCircle(color = primaryColor.copy(alpha = 0.2f), radius = 20.dp.toPx(), center = handEnd)
            }

            val indicatorCount = 12
            for (i in 0 until indicatorCount) {
                val angleDeg = i * 360f / indicatorCount - 90f
                val angleRad = angleDeg * PI / 180f
                val posX = center.x + radius * cos(angleRad).toFloat()
                val posY = center.y + radius * sin(angleRad).toFloat()
                
                val label = if (isHour) (if (i == 0) 12 else i).toString() else (i * 5).toString().padStart(2, '0')
                val isSelected = if (isHour) (selectedValue % 12) == (i % 12) else (selectedValue / 5) == i

                Box(
                    modifier = Modifier.offset { IntOffset((posX - with(density) { 15.dp.toPx() }).toInt(), (posY - with(density) { 15.dp.toPx() }).toInt()) }.size(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label, 
                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), 
                        fontSize = if (isSelected) 17.sp else 14.sp, 
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TimeDisplayPart(value: String, isActive: Boolean, onClick: () -> Unit) {
    val bgColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val textColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    
    Surface(onClick = onClick, color = bgColor, shape = RoundedCornerShape(14.dp), modifier = Modifier.width(74.dp).height(64.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(value, fontSize = 42.sp, fontWeight = FontWeight.Black, color = textColor)
        }
    }
}

@Composable
fun AmPmToggle(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick, 
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent, 
        shape = RoundedCornerShape(10.dp), 
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.width(52.dp).height(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun AnalogTimePickerDialog(
    initialTime24h: String,
    is24HourFormat: Boolean,
    onDismiss: () -> Unit,
    onTimeSelected: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.width(420.dp).wrapContentHeight().padding(12.dp), 
            shape = RoundedCornerShape(28.dp), 
            color = MaterialTheme.colorScheme.surface, 
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            TimePickerContent(selectionKey = initialTime24h, initialTime = initialTime24h, is24Hour = is24HourFormat, onTimeCompleted = onTimeSelected)
        }
    }
}

private fun calculateAngle(position: Offset, center: Offset): Float {
    val dx = position.x - center.x
    val dy = position.y - center.y
    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    angle += 90f
    if (angle < 0) angle += 360f
    return angle
}

private fun calculateValue(angle: Float, isHour: Boolean): Int {
    return if (isHour) {
        val h = (angle / 30f).roundToInt()
        if (h == 0) 12 else if (h > 12) h % 12 else h
    } else {
        val m = (angle / 6f).roundToInt()
        if (m >= 60) 0 else m
    }
}
