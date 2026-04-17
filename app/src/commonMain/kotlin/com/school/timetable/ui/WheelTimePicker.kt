package com.school.timetable.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    initialIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
    visibleItemsCount: Int = 3,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val itemHeight = height / visibleItemsCount
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }
    
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    
    val padCount = visibleItemsCount / 2
    val paddedItems = List(padCount) { "" } + items + List(padCount) { "" }
    
    val firstVisibleIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val firstVisibleScrollTarget by remember { derivedStateOf { listState.firstVisibleItemScrollOffset } }
    
    val centerIndex = remember(firstVisibleIndex, firstVisibleScrollTarget, itemHeightPx) {
        if (firstVisibleScrollTarget > itemHeightPx / 2) {
            firstVisibleIndex + 1
        } else {
            firstVisibleIndex
        }
    }

    LaunchedEffect(centerIndex) {
        val realIndex = centerIndex.coerceIn(0, items.lastIndex)
        onItemSelected(realIndex)
    }

    Box(
        modifier = modifier
            .height(height)
            .width(60.dp),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize()
        ) {
            items(paddedItems.size) { index ->
                val isSelected = index == centerIndex + padCount
                val targetAlpha = if (isSelected) 1f else 0.4f
                val fontSize = if (isSelected) 26.sp else 18.sp
                val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = paddedItems[index],
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.alpha(targetAlpha)
                    )
                }
            }
        }
        
        // Horizontal highlight bars
        Box(
            modifier = Modifier
                .height(itemHeight)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
        )
    }
}

@Composable
fun WheelTimePickerDialog(
    initialTime24h: String, // format "HH:mm"
    is24HourFormat: Boolean,
    onDismiss: () -> Unit,
    onTimeSelected: (String) -> Unit // returns "HH:mm"
) {
    val parts = initialTime24h.split(":")
    var hour24 by remember { mutableStateOf(parts.getOrNull(0)?.toIntOrNull() ?: 8) }
    var minute by remember { mutableStateOf(parts.getOrNull(1)?.toIntOrNull() ?: 0) }

    val hours12List = (1..12).map { it.toString().padStart(2, '0') }
    val hours24List = (0..23).map { it.toString().padStart(2, '0') }
    val minutesList = (0..59).map { it.toString().padStart(2, '0') }
    val amPmList = listOf("AM", "PM")

    // Derived states for 12-hour logic
    var isAm by remember { mutableStateOf(hour24 < 12) }
    var hour12 by remember {
        mutableStateOf(
            when {
                hour24 == 0 -> 12
                hour24 > 12 -> hour24 - 12
                else -> hour24
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Set Time", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (is24HourFormat) {
                        WheelPicker(
                            items = hours24List,
                            initialIndex = hour24,
                            onItemSelected = { hour24 = it }
                        )
                        Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        WheelPicker(
                            items = minutesList,
                            initialIndex = minute,
                            onItemSelected = { minute = it }
                        )
                    } else {
                        WheelPicker(
                            items = hours12List,
                            initialIndex = hour12 - 1, // 0-based index for 1..12
                            onItemSelected = { hour12 = it + 1 }
                        )
                        Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        WheelPicker(
                            items = minutesList,
                            initialIndex = minute,
                            onItemSelected = { minute = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        WheelPicker(
                            items = amPmList,
                            initialIndex = if (isAm) 0 else 1,
                            onItemSelected = { isAm = it == 0 }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalHour24 = if (is24HourFormat) {
                        hour24
                    } else {
                        if (isAm && hour12 == 12) 0
                        else if (!isAm && hour12 < 12) hour12 + 12
                        else hour12
                    }
                    val formatted = "${finalHour24.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
                    onTimeSelected(formatted)
                }
            ) {
                Text("Set Time", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
