package com.school.timetable.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.school.timetable.data.Subject
import com.school.timetable.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(viewModel: TimetableViewModel, onToggleOverlay: () -> Unit = {}) {
    val schedules by viewModel.schedules.collectAsState()
    val currentDay by viewModel.currentDay.collectAsState()
    val currentTime by viewModel.currentTimeFormat.collectAsState()
    val currentPeriodId by viewModel.currentPeriodId.collectAsState()
    val isSlidePanelOpen by viewModel.isSlidePanelOpen.collectAsState()

    var subjectToEdit by remember { mutableStateOf<Subject?>(null) }
    var isEditingName by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showBellTimingDialog by remember { mutableStateOf(false) }
    val className by viewModel.className.collectAsState(initial = "XII-E")

    val dayName = when (currentDay) {
        Calendar.MONDAY -> "Monday"
        Calendar.TUESDAY -> "Tuesday"
        Calendar.WEDNESDAY -> "Wednesday"
        Calendar.THURSDAY -> "Thursday"
        Calendar.FRIDAY -> "Friday"
        Calendar.SATURDAY -> "Saturday"
        Calendar.SUNDAY -> "Sunday"
        else -> "Today"
    }

    val isDarkTheme by viewModel.isDarkMode.collectAsState()
    val is24HourFormatSetting by viewModel.is24HourFormat.collectAsState()
    val currentHandleStyle by viewModel.handleStyle.collectAsState()
    val favoriteStyles by viewModel.favoriteStyles.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()

    var showCreateProfileDialog by remember { mutableStateOf(false) }

    val formatTime = { time24: String ->
        if (is24HourFormatSetting) time24
        else {
            val parts = time24.split(":")
            if (parts.size == 2) {
                val h = parts[0].toIntOrNull() ?: 8
                val m = parts[1].toIntOrNull() ?: 0
                val amPm = if (h < 12) "AM" else "PM"
                val h12 = when {
                    h == 0 -> 12
                    h > 12 -> h - 12
                    else -> h
                }
                "${h12.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')} $amPm"
            } else time24
        }
    }
    
    val displayDay = if (currentDay == Calendar.SUNDAY) Calendar.MONDAY else currentDay
    val todaySchedule = schedules.find { it.dayOfWeek == displayDay }

    val mainListState = rememberLazyListState()
    val overlayWeeklyScrollState = rememberScrollState()
    
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenMultiplier = (configuration.screenWidthDp / 800f).coerceIn(1f, 1.2f)
    val cardWidthPx = with(density) { ((140 * screenMultiplier) + 12).dp.toPx() }.toInt()
    
    var firstLoad by remember { mutableStateOf(true) }
    var previousPeriod by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentPeriodId) {
        if (!firstLoad && currentPeriodId != previousPeriod && currentPeriodId != null) {
            val idx = todaySchedule?.subjects?.indexOfFirst { it.id == currentPeriodId } ?: -1
            if (idx >= 0) {
                mainListState.animateScrollToItem(idx)
                overlayWeeklyScrollState.animateScrollTo(idx * cardWidthPx)
            }
        }
        previousPeriod = currentPeriodId
        firstLoad = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main Content
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Custom Modern Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = dayName,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$className • $currentTime",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { isEditingName = true }.padding(vertical = 4.dp)
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            modifier = Modifier
                                .clickable { onToggleOverlay() }
                                .clip(RoundedCornerShape(8.dp)),
                            color = AccentSecondary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentSecondary.copy(0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Overlay", color = AccentSecondary, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Theme Toggle button
                        Surface(
                            modifier = Modifier
                                .clickable { viewModel.toggleTheme() }
                                .clip(RoundedCornerShape(8.dp)),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Theme", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Settings button
                        Surface(
                            modifier = Modifier
                                .clickable { showSettingsDialog = true }
                                .clip(RoundedCornerShape(8.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = Color.White
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Settings", 
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                if (todaySchedule != null) {
                    LazyRow(
                        state = mainListState,
                        modifier = Modifier.fillMaxWidth().weight(0.55f),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(todaySchedule.subjects, key = { it.id }) { subject ->
                            ModernTimetableCard(
                                subject = subject,
                                isCurrent = subject.id == currentPeriodId,
                                timeFormatter = formatTime,
                                onClick = { subjectToEdit = subject }
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.weight(0.55f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No schedule configuration found.", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // --------- Profiles Section ---------
                Column(
                    modifier = Modifier.fillMaxWidth().weight(0.45f).padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Timetable Profiles",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Surface(
                            modifier = Modifier
                                .clickable { showCreateProfileDialog = true }
                                .clip(RoundedCornerShape(8.dp)),
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Text("Create New", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(profiles, key = { it.id }) { profile ->
                            ProfileCard(
                                profile = profile,
                                onActiveClick = { viewModel.setActiveProfile(profile.id) },
                                onEditClick = {
                                    viewModel.setSlidePanel(true, profile.id)
                                },
                                onDeleteClick = {
                                    viewModel.deleteProfile(profile.id)
                                }
                            )
                        }
                    }
                }
                // ------------------------------------
            }

            // Slide Down Overlay Panel (Glassy/Overlay effect)
            AnimatedVisibility(
                visible = isSlidePanelOpen,
                enter = slideInVertically(
                    initialOffsetY = { -it }, 
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(250, easing = FastOutLinearInEasing)
                ) + fadeOut(tween(250)),
                modifier = Modifier.fillMaxSize()
            ) {
                var showDiscardDialog by remember { mutableStateOf(false) }
                
                if (showDiscardDialog) {
                    AlertDialog(
                        onDismissRequest = { showDiscardDialog = false },
                        title = { Text("Unsaved Changes") },
                        text = { Text("You have unsaved schedule changes. Do you want to save them before closing?") },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.commitWorkingSchedules()
                                showDiscardDialog = false
                                viewModel.setSlidePanel(false)
                            }) { Text("Save & Close") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                viewModel.discardWorkingSchedules()
                                showDiscardDialog = false
                                viewModel.setSlidePanel(false)
                            }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable { 
                            if (hasUnsavedChanges) showDiscardDialog = true
                            else viewModel.setSlidePanel(false)
                        }
                ) {
                    var draggedSubject by remember { mutableStateOf<Subject?>(null) }
                    var dragPosition by remember { mutableStateOf(Offset.Zero) }
                    val cellLayouts = remember { mutableMapOf<String, Rect>() }
                    var sliderBounds by remember { mutableStateOf(Rect.Zero) }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.9f)
                            .onGloballyPositioned { sliderBounds = it.boundsInWindow() }
                            .clickable { /* prevent touch passthrough */ },
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Panel Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Weekly Overview",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (hasUnsavedChanges) {
                                        Button(
                                            onClick = { viewModel.commitWorkingSchedules() },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text("Save Changes", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    IconButton(
                                        onClick = { 
                                            if (hasUnsavedChanges) showDiscardDialog = true
                                            else viewModel.setSlidePanel(false) 
                                        },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Close",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            
                            // Weekly Grid Structure
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                val days = listOf(
                                    Calendar.MONDAY to "Monday",
                                    Calendar.TUESDAY to "Tuesday",
                                    Calendar.WEDNESDAY to "Wednesday",
                                    Calendar.THURSDAY to "Thursday",
                                    Calendar.FRIDAY to "Friday",
                                    Calendar.SATURDAY to "Saturday"
                                )
                                
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    // Left fixed column for Days
                                    Column(
                                        modifier = Modifier.width(100.dp).padding(top = 58.dp), // offset for header
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        days.forEach { (_, label) -> 
                                             Box(modifier = Modifier.height((110 * screenMultiplier).dp), contentAlignment = Alignment.CenterStart) {
                                                 Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                             }
                                        }
                                    }
                                    
                                    // Right synchronized horizontally scrolling columns
                                    Column(
                                        modifier = Modifier.weight(1f).horizontalScroll(overlayWeeklyScrollState),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Top Header (Bell Timings)
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            viewModel.getGlobalBellTimings().forEach { timing ->
                                                Surface(
                                                     modifier = Modifier.width((140 * screenMultiplier).dp),
                                                     color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                     shape = RoundedCornerShape(8.dp)
                                                ) {
                                                     val periodLabel = if (timing.periodIndex == -1) "BREAK" else if(timing.periodIndex == 0) "Zero" else "P-${timing.periodIndex}"
                                                     Column(modifier = Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                         Text(periodLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                         Spacer(Modifier.height(4.dp))
                                                         Text("${formatTime(timing.startTime)} - ${formatTime(timing.endTime)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                     }
                                                }
                                            }
                                        }
                                        
                                        // Matrix Rows
                                        days.forEach { (calDay, _) ->
                                            val daySched = viewModel.workingSchedules.collectAsState().value.find { it.dayOfWeek == calDay }
                                            if (daySched != null) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                    daySched.subjects.forEach { subject ->
                                                        CompactTimetableCard(
                                                            subject = subject,
                                                            isCurrent = subject.id == currentPeriodId,
                                                            timeFormatter = formatTime,
                                                            onClick = { subjectToEdit = subject },
                                                            isDragged = draggedSubject?.id == subject.id,
                                                            modifier = Modifier
                                                                .onGloballyPositioned { cellLayouts[subject.id] = it.boundsInWindow() }
                                                                .pointerInput(subject) {
                                                                    detectDragGesturesAfterLongPress(
                                                                        onDragStart = { _ ->
                                                                            val bounds = cellLayouts[subject.id]
                                                                            if (bounds != null) {
                                                                                dragPosition = bounds.topLeft
                                                                                draggedSubject = subject
                                                                            }
                                                                        },
                                                                        onDrag = { change, dragAmount ->
                                                                            change.consume()
                                                                            dragPosition += dragAmount
                                                                        },
                                                                        onDragEnd = {
                                                                            val centerOfDrag = Offset(dragPosition.x + 90.dp.toPx(), dragPosition.y + 70.dp.toPx())
                                                                            val targetEntry = cellLayouts.entries.find { it.value.contains(centerOfDrag) }
                                                                            if (targetEntry != null && targetEntry.key != subject.id) {
                                                                                val workingLists = viewModel.workingSchedules.value
                                                                                val targetSubject = workingLists.flatMap { it.subjects }.find { it.id == targetEntry.key }
                                                                                if (targetSubject != null) viewModel.swapSubjects(subject, targetSubject)
                                                                            }
                                                                            draggedSubject = null
                                                                        },
                                                                        onDragCancel = { draggedSubject = null }
                                                                    )
                                                                }
                                                        )
                                                    }
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.height((110 * screenMultiplier).dp))
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(40.dp))
                            }
                        }
                    }

                    // Render Dragging Shadow/Overlay
                    draggedSubject?.let { subject ->
                        val localX = (dragPosition.x - sliderBounds.left).coerceAtLeast(0f)
                        val localY = (dragPosition.y - sliderBounds.top).coerceAtLeast(0f)
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(localX.roundToInt(), localY.roundToInt()) }
                                .scale(1.05f)
                        ) {
                            CompactTimetableCard(
                                subject = subject, 
                                isCurrent = subject.id == currentPeriodId, 
                                timeFormatter = formatTime, 
                                onClick = {}, 
                                isDragged = false,
                                modifier = Modifier
                            )
                        }
                    }
                }
            }
        }
        
        // Edit Dialog
        subjectToEdit?.let { sub ->
            EditCellDialog(
                subject = sub,
                is24HourFormat = is24HourFormatSetting,
                onDismiss = { subjectToEdit = null },
                onSave = { name, teacher, start, end ->
                    viewModel.updateSubject(sub, name, teacher, start, end)
                    subjectToEdit = null
                }
            )
        }
        
        if (isEditingName) {
            var newName by remember { mutableStateOf(className) }
            AlertDialog(
                onDismissRequest = { isEditingName = false },
                title = { Text("Edit Class Name") },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = { viewModel.updateClassName(newName); isEditingName = false }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { isEditingName = false }) { Text("Cancel") }
                }
            )
        }

        if (showCreateProfileDialog) {
            var newProfileName by remember { mutableStateOf("") }
            var copyCurrent by remember { mutableStateOf(false) }
            
            AlertDialog(
                onDismissRequest = { showCreateProfileDialog = false },
                title = { Text("New Timetable", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newProfileName,
                            onValueChange = { newProfileName = it },
                            label = { Text("Profile Name") }
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = copyCurrent, onCheckedChange = { copyCurrent = it })
                            Text("Copy from active timetable")
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (newProfileName.isNotBlank()) {
                            viewModel.createNewProfile(newProfileName, copyCurrent)
                            showCreateProfileDialog = false
                        }
                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateProfileDialog = false }) { Text("Cancel") }
                }
            )
        }
        
        // Settings Drawer Overlay
        AnimatedVisibility(
            visible = showSettingsDialog,
            enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)),
            exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)),
            modifier = Modifier.fillMaxSize()
        ) {
            var showAbout by remember { mutableStateOf(false) }
            var showHandleStyleSelector by remember { mutableStateOf(false) }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showSettingsDialog = false; showAbout = false; showHandleStyleSelector = false }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(360.dp)
                        .align(Alignment.CenterEnd)
                        .clickable { /* absorb taps */ },
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        if (showAbout) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f)) {
                                Text("About Smart TimeTable", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 16.dp))
                                Text(
                                    "Smart TimeTable is a simple and smart application designed to help students manage their daily class schedules efficiently.\n\nFeatures:\n• Displays today's timetable automatically\n• Fully editable timetable\n• Slide-down timetable panel\n• Highlights current period\n• Simple and user-friendly interface\n\nPurpose:\nThis application is developed to improve classroom management and student productivity using smart devices.\n\nDeveloped by:\nNikhil Kumar Yadav\n\nClass: 12-E\nSession: 2026–2027", 
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 22.sp
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { showAbout = false }, modifier = Modifier.fillMaxWidth()) { Text("Back to Settings") }
                        } else if (showHandleStyleSelector) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f)) {
                                Text("Handle Style", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 16.dp))
                                
                                HandleStyle.values().forEach { style ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                            .clickable { viewModel.updateHandleStyle(style) }
                                            .clip(RoundedCornerShape(8.dp)),
                                        color = if (style == currentHandleStyle) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (style == currentHandleStyle) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text(style.displayName, fontWeight = FontWeight.SemiBold, color = if (style == currentHandleStyle) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                                IconButton(onClick = { viewModel.toggleFavoriteStyle(style) }) {
                                                    val isFav = favoriteStyles.contains(style)
                                                    Icon(
                                                        imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.Star,
                                                        contentDescription = "Favorite",
                                                        tint = if (isFav) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            // Render visual preview
                                            Box(
                                                modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha=0.05f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                DragHandle(style = style, onClick = {})
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { showHandleStyleSelector = false }, modifier = Modifier.fillMaxWidth()) { Text("Back to Settings", color = Color.White) }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {

                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable { showHandleStyleSelector = true }.clip(RoundedCornerShape(8.dp)),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) { Text("Handle Style", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable { showSettingsDialog = false; isEditingName = true }.clip(RoundedCornerShape(8.dp)),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) { Text("Change Class Name", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable { showSettingsDialog = false; showBellTimingDialog = true }.clip(RoundedCornerShape(8.dp)),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) { Text("Edit Bell Timing", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable { showAbout = true }.clip(RoundedCornerShape(8.dp)),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) { Text("About Application", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleTheme() }.clip(RoundedCornerShape(8.dp)),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Switch Theme", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                        Text(if (isDarkTheme) "Dark" else "Light", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                                
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleTimeFormat() }.clip(RoundedCornerShape(8.dp)),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Time Format", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                        Text(if (is24HourFormatSetting) "24-Hour" else "12-Hour (AM/PM)", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text("Smart Timetable v1.0", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
                        }
                    }
                }
            }
        }
    }

    if (showBellTimingDialog) {
        val timings = viewModel.getGlobalBellTimings()
        BellTimingEditorDialog(
            timings = timings,
            onDismiss = { showBellTimingDialog = false },
            onSave = { updated ->
                updated.forEach { viewModel.updateGlobalBellTiming(it.periodIndex, it.startTime, it.endTime) }
                showBellTimingDialog = false
            }
        )
    }
}

@Composable
fun ModernTimetableCard(
    subject: Subject,
    isCurrent: Boolean,
    timeFormatter: (String) -> String,
    onClick: () -> Unit
) {
    val isBreak = subject.periodIndex == -1
    
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val scale by animateFloatAsState(
        targetValue = if (isCurrent) 1.05f else 1.0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing)
    )

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenMultiplier = (configuration.screenWidthDp / 800f).coerceIn(1f, 1.3f)

    val baseModifier = Modifier
        .scale(scale)
        .width((220 * screenMultiplier).dp)
        .height((280 * screenMultiplier).dp)
        .clip(RoundedCornerShape(24.dp))
        .clickable { onClick() }
        
    val styledModifier = if (isCurrent) {
        baseModifier
            .background(Brush.linearGradient(listOf(HighlightGradientStart, HighlightGradientEnd)))
            .border(3.dp, AccentSecondary.copy(alpha = pulseAlpha), RoundedCornerShape(24.dp))
    } else if (isBreak) {
        baseModifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
    } else {
        baseModifier
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
    }

    Box(
        modifier = styledModifier.padding(28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section (Period Info)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isCurrent) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text(
                        text = if (isBreak) "BREAK" else "P${subject.periodIndex}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = (14 * screenMultiplier).sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(AccentSecondary, CircleShape)
                    )
                }
            }

            // Middle Section (Subject Name)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {
                if (isBreak) {
                    Text(
                        text = "Take a Break",
                        fontSize = (32 * screenMultiplier).sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = (38 * screenMultiplier).sp,
                        color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = subject.name,
                        fontSize = (32 * screenMultiplier).sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = (38 * screenMultiplier).sp,
                        color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface,
                        maxLines = 3
                    )
                }
            }

            // Bottom Section (Teacher & Time)
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!isBreak) {
                    Text(
                        text = subject.teacher,
                        fontSize = (14 * screenMultiplier).sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isCurrent) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = "${timeFormatter(subject.startTime)} - ${timeFormatter(subject.endTime)}",
                    fontSize = (16 * screenMultiplier).sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isCurrent) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CompactTimetableCard(
    subject: Subject,
    isCurrent: Boolean,
    timeFormatter: (String) -> String,
    onClick: () -> Unit,
    isDragged: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isBreak = subject.periodIndex == -1
    
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val scale by animateFloatAsState(
        targetValue = if (isCurrent) 1.05f else 1.0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing)
    )

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenMultiplier = (configuration.screenWidthDp / 800f).coerceIn(1f, 1.2f)

    val baseModifier = modifier
        .scale(scale)
        .width((140 * screenMultiplier).dp)
        .height((110 * screenMultiplier).dp)
        .clip(RoundedCornerShape(16.dp))
        .clickable { onClick() }
        .then(if (isDragged) Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) else Modifier)
        
    val styledModifier = if (isDragged) {
        baseModifier.border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    } else if (isCurrent) {
        baseModifier
            .background(Brush.linearGradient(listOf(HighlightGradientStart, HighlightGradientEnd)))
            .border(2.dp, AccentSecondary.copy(alpha = pulseAlpha), RoundedCornerShape(16.dp))
    } else if (isBreak) {
        baseModifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    } else {
        baseModifier
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
    }

    Box(
        modifier = styledModifier.padding(16.dp).then(if (isDragged) Modifier.background(Color.Transparent) else Modifier)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBreak) "BREAK" else "P${subject.periodIndex}",
                    fontSize = (12 * screenMultiplier).sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${timeFormatter(subject.startTime)}",
                    fontSize = (11 * screenMultiplier).sp,
                    color = if (isCurrent) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = if (isBreak) "Break" else subject.name,
                fontSize = (18 * screenMultiplier).sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
            
            Text(
                text = if (isBreak) "" else subject.teacher,
                fontSize = (10 * screenMultiplier).sp,
                fontWeight = FontWeight.Medium,
                color = if (isCurrent) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
@Composable
fun ProfileCard(
    profile: com.school.timetable.data.TimetableProfile,
    onActiveClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val formatter = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
    val dateStr = formatter.format(java.util.Date(profile.createdAt))
    val updateStr = formatter.format(java.util.Date(profile.updatedAt))
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Set Active Profile") },
            text = { Text("Are you sure you want to set '${profile.name}' as the active timetable?") },
            confirmButton = {
                TextButton(onClick = { 
                    onActiveClick() 
                    showConfirmDialog = false 
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    Surface(
        modifier = Modifier
            .width(280.dp)
            .border(
                width = if (profile.isActive) 2.dp else 1.dp,
                color = if (profile.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (profile.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(profile.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                
                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Full Timetable", tint = MaterialTheme.colorScheme.primary)
                    }
                    if (!profile.isActive) {
                        var showDeleteConf by remember { mutableStateOf(false) }
                        IconButton(onClick = { showDeleteConf = true }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Profile", tint = MaterialTheme.colorScheme.error)
                        }
                        if (showDeleteConf) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConf = false },
                                title = { Text("Delete Profile") },
                                text = { Text("Are you sure you want to permanently delete '${profile.name}'?") },
                                confirmButton = {
                                    TextButton(onClick = { onDeleteClick(); showDeleteConf = false }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConf = false }) { Text("Cancel") }
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Created: $dateStr", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Updated: $updateStr", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = { if (!profile.isActive) showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (profile.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (profile.isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(if (profile.isActive) "ACTIVE" else "Set Active", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialogWrapper(initialTime: String, onTimeSelected: (String) -> Unit, onDismiss: () -> Unit) {
    val initialSplit = initialTime.split(":")
    val h = initialSplit.getOrNull(0)?.toIntOrNull() ?: 12
    val m = initialSplit.getOrNull(1)?.toIntOrNull() ?: 0
    val timePickerState = androidx.compose.material3.rememberTimePickerState(initialHour = h, initialMinute = m, is24Hour = true)
    
    AlertDialog(
         onDismissRequest = onDismiss,
         title = { Text("Select Time") },
         text = { androidx.compose.material3.TimePicker(state = timePickerState) },
         confirmButton = {
             TextButton(onClick = {
                 onTimeSelected(String.format(java.util.Locale.getDefault(), "%02d:%02d", timePickerState.hour, timePickerState.minute))
             }) { Text("OK") }
         },
         dismissButton = {
             TextButton(onClick = onDismiss) { Text("Cancel") }
         }
    )
}

@Composable
fun BellTimingEditorDialog(
    timings: List<Subject>,
    onDismiss: () -> Unit,
    onSave: (List<Subject>) -> Unit
) {
     var editedTimings by remember { mutableStateOf(timings) }
     var pickerContext by remember { mutableStateOf<Triple<Int, Boolean, String>?>(null) } // index, isStart, initialTime

     if (pickerContext != null) {
          TimePickerDialogWrapper(
               initialTime = pickerContext!!.third,
               onTimeSelected = { newTime ->
                   val ctx = pickerContext!!
                   editedTimings = editedTimings.toMutableList().apply {
                       val updated = if (ctx.second) this[ctx.first].copy(startTime = newTime) else this[ctx.first].copy(endTime = newTime)
                       this[ctx.first] = updated
                   }
                   pickerContext = null
               },
               onDismiss = { pickerContext = null }
          )
     }

     AlertDialog(
         onDismissRequest = onDismiss,
         title = { Text("Global Bell Timings", fontWeight = FontWeight.Bold) },
         text = {
             Column(modifier = Modifier.fillMaxWidth()) {
                 Text("Changes will synchronize across all days simultaneously.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                 Spacer(Modifier.height(16.dp))
                 androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxHeight(0.6f)) {
                     itemsIndexed(editedTimings) { index, timing ->
                          val periodLabel = if (timing.periodIndex == -1) "Break" else if (timing.periodIndex == 0) "Zero" else "P-${timing.periodIndex}"
                          Row(
                               verticalAlignment = Alignment.CenterVertically,
                               horizontalArrangement = Arrangement.SpaceBetween,
                               modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                          ) {
                               Text(periodLabel, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.3f))
                               
                               Button(
                                   onClick = { pickerContext = Triple(index, true, timing.startTime) },
                                   modifier = Modifier.weight(0.3f),
                                   contentPadding = PaddingValues(0.dp)
                               ) { Text(timing.startTime, fontSize = 12.sp) }
                               
                               Text(" - ", modifier = Modifier.padding(horizontal = 8.dp))
                               
                               Button(
                                   onClick = { pickerContext = Triple(index, false, timing.endTime) },
                                   modifier = Modifier.weight(0.3f),
                                   contentPadding = PaddingValues(0.dp)
                               ) { Text(timing.endTime, fontSize = 12.sp) }
                          }
                     }
                 }
             }
         },
         confirmButton = {
             Button(onClick = { onSave(editedTimings) }) { Text("Save") }
         },
         dismissButton = {
             TextButton(onClick = onDismiss) { Text("Cancel") }
         }
     )
}
