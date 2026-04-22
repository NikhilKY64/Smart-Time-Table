package com.school.timetable.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.school.timetable.data.Subject
import com.school.timetable.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    viewModel: TimetableViewModel,
    onToggleOverlay: () -> Unit,
    isOverlayActive: Boolean = false
) {
    val schedules by viewModel.schedules.collectAsState()
    val currentDay by viewModel.currentDay.collectAsState()
    val currentTime by viewModel.currentTimeFormat.collectAsState()
    val currentPeriodId by viewModel.currentPeriodId.collectAsState()
    val isSlidePanelOpen by viewModel.isSlidePanelOpen.collectAsState()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()

    var subjectToEdit by remember { mutableStateOf<Subject?>(null) }
    var isEditingName by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showBellTimingDialog by remember { mutableStateOf(false) }
    val className by viewModel.className.collectAsState(initial = "XII-E")
    
    // Feature: Temporary Day Navigation
    var dayOffset by remember { mutableStateOf(0) }
    
    // Reset offset when curtain toggles
    LaunchedEffect(isSlidePanelOpen) {
        dayOffset = 0
    }
    
    val effectiveDay = remember(currentDay, dayOffset) {
        val cal = Calendar.getInstance()
        // We calculate day relative to currentDay
        var target = currentDay + dayOffset
        while (target <= 0) target += 7
        while (target > 7) target -= 7
        target
    }

    val dayName = when (effectiveDay) {
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
    val isStartOnStartupEnabled by viewModel.isStartOnStartupEnabled.collectAsState()

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
    
    val todaySchedule = schedules.find { it.dayOfWeek == effectiveDay }

    val mainListState = rememberLazyListState()
    val overlayWeeklyScrollState = rememberScrollState()
    
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenMultiplier = 1.1f 
    val cardWidthPx = with(density) { ((140 * screenMultiplier) + 12).dp.toPx() }.toInt()
    
    var firstLoad by remember { mutableStateOf(true) }
    var previousPeriod by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentPeriodId) {
        // Feature 5: Auto-Scroll on open/load
        if (currentPeriodId != null) {
            val idx = todaySchedule?.subjects?.indexOfFirst { it.id == currentPeriodId } ?: -1
            if (idx >= 0) {
                val currentPeriod = todaySchedule?.subjects?.get(idx)
                // Feature 5: Auto-Scroll on open/load
                // Restricted in slider to only start from 5th period (after break)
                if (isOverlayActive) {
                    if ((currentPeriod?.periodIndex ?: 0) >= 5) {
                        mainListState.scrollToItem(idx)
                        overlayWeeklyScrollState.scrollTo(idx * cardWidthPx)
                    }
                } else {
                    // In main app, always handle auto-scroll on change
                    if (!firstLoad && currentPeriodId != previousPeriod) {
                        mainListState.animateScrollToItem(idx)
                        // Smoother scroll for weekly overview
                        overlayWeeklyScrollState.animateScrollTo(
                            value = idx * cardWidthPx,
                            animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy)
                        )
                    } else if (firstLoad) {
                        // Immediate scroll on first load
                        mainListState.scrollToItem(idx)
                        overlayWeeklyScrollState.scrollTo(idx * cardWidthPx)
                    }
                }
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
                        .padding(horizontal = 24.dp, vertical = if (isOverlayActive) 12.dp else 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (dayOffset == 0) dayName else if (dayOffset == 1) "Tomorrow" else if (dayOffset == -1) "Yesterday" else dayName,
                            fontSize = if (isOverlayActive) 20.sp else 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (dayOffset == 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                        )
                        if (dayOffset != 0) {
                            Text(
                                text = if (dayOffset > 0) "($dayName)" else "($dayName)",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        AnimatedContent(
                            targetState = className to currentPeriodId,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(600))
                            }
                        ) { (name, _) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { dayOffset-- }, modifier = Modifier.size(if (isOverlayActive) 18.dp else 24.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.width(4.dp))
                                
                                Text(
                                    text = "$name • $currentTime",
                                    fontSize = if (isOverlayActive) 14.sp else 16.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { 
                                            isEditingName = true 
                                        }
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .padding(vertical = 2.dp),
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                                )
                                
                                Spacer(Modifier.width(4.dp))
                                IconButton(onClick = { dayOffset++ }, modifier = Modifier.size(if (isOverlayActive) 18.dp else 24.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            modifier = Modifier
                                .clickable { onToggleOverlay() }
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clip(RoundedCornerShape(8.dp)),
                            color = if (isOverlayActive) AccentSecondary else AccentSecondary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentSecondary.copy(0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = if (isOverlayActive) 8.dp else 12.dp, 
                                    vertical = if (isOverlayActive) 6.dp else 10.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isOverlayActive) "Turn Off" else "Overlay", 
                                    color = if (isOverlayActive) Color.White else AccentSecondary, 
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isOverlayActive) 10.sp else 12.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                                )
                            }
                        }

                        val isAutoHideEnabled by viewModel.isAutoHideOverlayEnabled.collectAsState()
                        Surface(
                            modifier = Modifier
                                .clickable { viewModel.toggleAutoHideOverlay() }
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clip(RoundedCornerShape(8.dp)),
                            color = if (isAutoHideEnabled) MaterialTheme.colorScheme.secondaryContainer else AccentPrimary.copy(0.2f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isAutoHideEnabled) MaterialTheme.colorScheme.secondary else AccentPrimary.copy(0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = if (isOverlayActive) 10.dp else 14.dp, 
                                    vertical = if (isOverlayActive) 6.dp else 10.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isAutoHideEnabled) Icons.AutoMirrored.Filled.List else Icons.Default.Star,
                                    contentDescription = "Toggle Auto-Hide",
                                    tint = if (isAutoHideEnabled) MaterialTheme.colorScheme.onSecondaryContainer else AccentPrimary,
                                    modifier = Modifier.size(if (isOverlayActive) 12.dp else 14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (isAutoHideEnabled) "Auto-Hide ON" else "Auto-Hide OFF", 
                                    color = if (isAutoHideEnabled) MaterialTheme.colorScheme.onSecondaryContainer else AccentPrimary, 
                                    fontWeight = FontWeight.ExtraBold, 
                                    fontSize = if (isOverlayActive) 10.sp else 12.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                                )
                            }
                        }

                        if (isOverlayActive) {
                            Surface(
                                modifier = Modifier
                                    .clickable { onToggleOverlay() }
                                    .clip(RoundedCornerShape(8.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Maximize",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Maximize View", 
                                        color = Color.White, 
                                        fontWeight = FontWeight.ExtraBold, 
                                        fontSize = 10.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                                    )
                                }
                            }
                        }

                        if (!isOverlayActive) {
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

                            Surface(
                                modifier = Modifier
                                    .clickable { showSettingsDialog = true }
                                    .pointerHoverIcon(PointerIcon.Hand)
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
                                    Text("Settings", color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                if (todaySchedule != null) {
                    LazyRow(
                        state = mainListState,
                        modifier = Modifier.fillMaxWidth().weight(if (isOverlayActive) 1f else 0.55f),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(todaySchedule.subjects, key = { it.id }) { subject ->
                            if (isOverlayActive) {
                                CompactTimetableCard(
                                    subject = subject,
                                    isCurrent = subject.id == currentPeriodId,
                                    timeFormatter = formatTime,
                                    onClick = { subjectToEdit = subject }
                                )
                            } else {
                                ModernTimetableCard(
                                    subject = subject,
                                    isCurrent = subject.id == currentPeriodId,
                                    timeFormatter = formatTime,
                                    onClick = { subjectToEdit = subject }
                                )
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().weight(if (isOverlayActive) 1f else 0.55f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val sundayMessage = if (currentDay == Calendar.SUNDAY) {
                                "Enjoy your Sunday! \uD83C\uDF1F\nNo classes scheduled."
                            } else {
                                "No schedule configuration found for today."
                            }
                            Text(
                                sundayMessage, 
                                fontSize = 20.sp, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 28.sp
                            )
                        }
                    }
                }

                if (!isOverlayActive) {
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
                                    onEditClick = { viewModel.setSlidePanel(true, profile.id) },
                                    onDeleteClick = { viewModel.deleteProfile(profile.id) }
                                )
                            }
                        }
                    }
                }
            }

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

                Box(modifier = Modifier.fillMaxSize()) {
                    // Scrim (Background Dimming)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f))
                            .clickable { 
                                if (hasUnsavedChanges) showDiscardDialog = true
                                else viewModel.setSlidePanel(false)
                            }
                    )

                    var draggedSubject by remember { mutableStateOf<Subject?>(null) }
                    var dragPosition by remember { mutableStateOf(Offset.Zero) }
                    val cellLayouts = remember { mutableMapOf<String, Rect>() }
                    var sliderBounds by remember { mutableStateOf(Rect.Zero) }

                    // Menu Surface
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.92f)
                            .onGloballyPositioned { sliderBounds = it.boundsInWindow() }
                            .pointerInput(Unit) {
                                // 2. Drag-to-Close gesture: Slide it UP to close
                                detectDragGestures(
                                    onDragEnd = {
                                        // If they leave it as is or pull it UP, snap it closed
                                        viewModel.setSlidePanel(false)
                                    },
                                    onDrag = { change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: androidx.compose.ui.geometry.Offset ->
                                        // If they drag UP significantly, close it immediately
                                        if (dragAmount.y < -30) {
                                            viewModel.setSlidePanel(false)
                                            change.consume()
                                        }
                                    }
                                )
                            }
                            .clickable { /* prevent touch passthrough */ },
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        shadowElevation = 12.dp
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { dayOffset-- }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                    }
                                    
                                    Spacer(Modifier.width(8.dp))
                                    
                                    Text(
                                        text = if (dayOffset == 0) "Weekly Overview" else "Overview • $dayName",
                                        fontSize = 28.sp, // Maintained large size
                                        fontWeight = FontWeight.Bold,
                                        color = if (dayOffset == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                                    )
                                    
                                    Spacer(Modifier.width(8.dp))
                                    
                                    IconButton(onClick = { dayOffset++ }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                    }
                                }
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
                                Spacer(modifier = Modifier.height(30.dp))
                                Box(
                                    modifier = Modifier
                                        .size(width = 40.dp, height = 4.dp)
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), CircleShape)
                                        .align(Alignment.CenterHorizontally)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
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
                onDismiss = { subjectToEdit = null },
                onSave = { name, teacher ->
                    // Times come from the subject unchanged — only name/teacher are editable here.
                    viewModel.updateSubject(sub, name, teacher, sub.startTime, sub.endTime)
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
            
            Box(modifier = Modifier.fillMaxSize()) {
                // Scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { showSettingsDialog = false; showAbout = false; showHandleStyleSelector = false }
                )

                // Settings Pane Content
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(360.dp)
                        .align(Alignment.CenterEnd)
                        .clickable { /* absorb taps */ },
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 16.dp 
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
                            Column(
                                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Section 1: Personalization
                                SettingSectionHeader("Visuals & Style")
                                SettingItem(
                                    title = "Handle Style",
                                    subtitle = "Change overlay grabber look",
                                    icon = Icons.Default.Settings,
                                    iconColor = Color(0xFF6200EE),
                                    onClick = { showHandleStyleSelector = true }
                                )
                                SettingItem(
                                    title = "Switch Theme",
                                    subtitle = if (isDarkTheme) "Dark Mode" else "Light Mode",
                                    icon = Icons.Default.Settings,
                                    iconColor = Color(0xFF03DAC6),
                                    onClick = { viewModel.toggleTheme() }
                                )

                                // Section 2: Core Configuration
                                SettingSectionHeader("Data & Schedule")
                                SettingItem(
                                    title = "Class Name",
                                    subtitle = "Current: ${viewModel.className.value}",
                                    icon = Icons.Default.Edit,
                                    iconColor = Color(0xFFBB86FC), // Light Purple
                                    onClick = { showSettingsDialog = false; isEditingName = true }
                                )
                                SettingItem(
                                    title = "Bell Timings",
                                    subtitle = "Adjust period durations",
                                    icon = Icons.Default.Settings,
                                    iconColor = Color(0xFFCF6679), // Coral
                                    onClick = { showSettingsDialog = false; showBellTimingDialog = true }
                                )

                                // Section 3: System & Behavior
                                SettingSectionHeader("System & Behavior")
                                SettingItem(
                                    title = "Launch on Startup",
                                    subtitle = if (isStartOnStartupEnabled) "App starts with Windows" else "Starts only when opened",
                                    icon = Icons.Default.Settings,
                                    iconColor = Color(0xFFFDD835), // Gold
                                    onClick = { viewModel.setStartOnStartupEnabled(!isStartOnStartupEnabled) }
                                )
                                SettingItem(
                                    title = "Time Format",
                                    subtitle = if (is24HourFormatSetting) "24-Hour (14:00)" else "12-Hour (02:00 PM)",
                                    icon = Icons.Default.Settings,
                                    iconColor = Color(0xFF42A5F5), // Blue
                                    onClick = { viewModel.toggleTimeFormat() }
                                )

                                // Sub-Section: Slider Behavior
                                Surface(
                                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Slider Behavior", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.height(12.dp))
                                        
                                        val isAutoCollapseEnabled by viewModel.isAutoCollapseEnabled.collectAsState()
                                        val autoCollapseDelay by viewModel.autoCollapseDelay.collectAsState()
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("Auto-Collapse", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Switch(
                                                checked = isAutoCollapseEnabled,
                                                onCheckedChange = { viewModel.setAutoCollapseEnabled(it) },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                                )
                                            )
                                        }
                                        
                                        if (isAutoCollapseEnabled) {
                                            Spacer(Modifier.height(16.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Delay:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(50.dp))
                                                Slider(
                                                    value = autoCollapseDelay.toFloat(),
                                                    onValueChange = { viewModel.setAutoCollapseDelay(it.toInt()) },
                                                    valueRange = 2f..10f,
                                                    steps = 7,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text("${autoCollapseDelay}s", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(30.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                            }
                                        }
                                    }
                                }

                                // Section 4: Information
                                SettingSectionHeader("Information")
                                SettingItem(
                                    title = "About Application",
                                    subtitle = "App info & Developer credits",
                                    icon = Icons.Default.Info,
                                    iconColor = Color(0xFFB0BEC5), // Grey
                                    onClick = { showAbout = true }
                                )
                                
                                Spacer(Modifier.height(24.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.weight(0.0001f)) // Always use a tiny positive weight to avoid division by zero crashes
                        
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Smart Timetable Version 1.2.2", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
        
        // Settings Dialogs
        if (showBellTimingDialog) {
            val timings = viewModel.getGlobalBellTimings()
            BellTimingEditorDialog(
                timings = timings,
                is24HourFormat = is24HourFormatSetting,
                onDismiss = { showBellTimingDialog = false },
                onSave = { updated ->
                    viewModel.saveAllBellTimings(updated)
                    showBellTimingDialog = false
                }
            )
        }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
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

    var isHovered by remember { mutableStateOf(false) }
    val screenMultiplier = 1.1f

    val baseModifier = Modifier
        .scale(scale)
        .width((220 * screenMultiplier).dp)
        .height((220 * screenMultiplier).dp) // Reduced height for main app
        .clip(RoundedCornerShape(24.dp))
        .onPointerEvent(PointerEventType.Enter) { isHovered = true }
        .onPointerEvent(PointerEventType.Exit) { isHovered = false }
        .pointerHoverIcon(PointerIcon.Hand)
        .clickable { onClick() }
        
    val styledModifier = if (isCurrent) {
        baseModifier
            .background(Brush.linearGradient(listOf(HighlightGradientStart, HighlightGradientEnd)))
            .border(3.dp, AccentSecondary.copy(alpha = pulseAlpha), RoundedCornerShape(24.dp))
    } else if (isBreak) {
        baseModifier
            .background(if (isHovered) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (isHovered) 0.6f else 0.3f), RoundedCornerShape(24.dp))
    } else {
        baseModifier
            .background(if (isHovered) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (isHovered) 1.0f else 0.8f), RoundedCornerShape(24.dp))
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

            // Middle Section (Subject Name) - Optimized for Smart Boards
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                if (isBreak) {
                    Text(
                        text = "Take a Break",
                        fontSize = (24 * screenMultiplier).sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = (30 * screenMultiplier).sp,
                        color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                    )
                } else {
                    Text(
                        text = subject.name,
                        fontSize = (22 * screenMultiplier).sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = (28 * screenMultiplier).sp,
                        color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                    )
                }
            }

            // Bottom Section (Teacher & Time)
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!isBreak) {
                    Text(
                        text = subject.teacher,
                        fontSize = (12 * screenMultiplier).sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = "${timeFormatter(subject.startTime)} - ${timeFormatter(subject.endTime)}",
                    fontSize = (13 * screenMultiplier).sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isCurrent) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
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

    var isHovered by remember { mutableStateOf(false) }
    val screenMultiplier = 1.1f

    val baseModifier = modifier
        .scale(scale)
        .width((140 * screenMultiplier).dp)
        .height((125 * screenMultiplier).dp)
        .clip(RoundedCornerShape(16.dp))
        .onPointerEvent(PointerEventType.Enter) { isHovered = true }
        .onPointerEvent(PointerEventType.Exit) { isHovered = false }
        .pointerHoverIcon(PointerIcon.Hand)
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
            .background(if (isHovered) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (isHovered) 0.6f else 0.3f), RoundedCornerShape(16.dp))
    } else {
        baseModifier
            .background(if (isHovered) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (isHovered) 1.0f else 0.8f), RoundedCornerShape(16.dp))
    }

    Box(
        modifier = styledModifier.padding(12.dp).then(if (isDragged) Modifier.background(Color.Transparent) else Modifier)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
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
            
            Spacer(Modifier.height(4.dp))

            Text(
                text = if (isBreak) "Break" else subject.name,
                fontSize = (18 * screenMultiplier).sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
            
            Spacer(modifier = Modifier.weight(1f)) // Push teacher name to bottom
            
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


@Composable
fun BellTimingEditorDialog(
    timings: List<Subject>,
    is24HourFormat: Boolean,
    onDismiss: () -> Unit,
    onSave: (List<Subject>) -> Unit
) {
    var editedTimings by remember(timings) { mutableStateOf(timings) }
    var activeSelection by remember { mutableStateOf<Pair<Int, Boolean>?>(null) } // index, isStart

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.width(850.dp).wrapContentHeight(),
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Smart Bell Dashboard", fontWeight = FontWeight.Black, fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(4.dp)) {
                    Text("Auto-Chain Active", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        },
        text = {
            Row(modifier = Modifier.fillMaxWidth().height(480.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                // Left Side: The List
                Column(modifier = Modifier.weight(1.1f)) {
                    Text("Select a time to adjust on the clock", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Spacer(Modifier.height(12.dp))
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp)).padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(editedTimings) { index, timing ->
                            val isRunning = activeSelection?.first == index
                            val periodLabel = if (timing.periodIndex == -1) "Break" else if (timing.periodIndex == 0) "Zero" else "P-${timing.periodIndex}"
                            
                            Surface(
                                color = if (isRunning) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Text(periodLabel, fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.width(60.dp))
                                    
                                    // Start Time Selector
                                    TimeChip(
                                        time = timing.startTime,
                                        isSelected = isRunning && activeSelection?.second == true,
                                        onClick = { activeSelection = index to true },
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    Text(" to ", modifier = Modifier.padding(horizontal = 8.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    
                                    // End Time Selector
                                    TimeChip(
                                        time = timing.endTime,
                                        isSelected = isRunning && activeSelection?.second == false,
                                        onClick = { activeSelection = index to false },
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Right Side: The Interactive Clock
                Surface(
                    modifier = Modifier.weight(0.9f).fillMaxHeight(),
                    color = Color.Black.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    if (activeSelection != null) {
                        val (idx, isStart) = activeSelection!!
                        val currentTime = if (isStart) editedTimings[idx].startTime else editedTimings[idx].endTime
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                            Text(
                                if (isStart) "Set Start Time" else "Set End Time",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                            
                            TimePickerContent(
                                selectionKey = activeSelection,
                                initialTime = currentTime,
                                is24Hour = is24HourFormat,
                                onTimeCompleted = { newTime ->
                                    val currentIdx = idx
                                    val currentIsStart = isStart
                                    
                                    editedTimings = editedTimings.toMutableList().apply {
                                        // 1. Update the current value
                                        this[currentIdx] = if (currentIsStart) {
                                            this[currentIdx].copy(startTime = newTime)
                                        } else {
                                            this[currentIdx].copy(endTime = newTime)
                                        }
                                        
                                        // 2. APPLY CHAIN LOGIC: If we updated END time, update next period START time
                                        if (!currentIsStart && currentIdx < size - 1) {
                                            this[currentIdx + 1] = this[currentIdx + 1].copy(startTime = newTime)
                                        }
                                    }
                                    
                                    // 3. AUTO-ADVANCE: Move to the next logical step
                                    activeSelection = if (currentIsStart) {
                                        currentIdx to false // Move to end time of same period
                                    } else if (currentIdx < editedTimings.size - 1) {
                                        (currentIdx + 1) to false // Move to end time of NEXT period (since start was auto-synced)
                                    } else null
                                }
                            )
                        }
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("Select a time to\nstart editing", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color.White.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(editedTimings) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(8.dp)) {
                Text("Save Full Schedule", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.padding(8.dp)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TimeChip(time: String, isSelected: Boolean, onClick: () -> Unit, color: Color) {
    Surface(
        onClick = onClick,
        color = if (isSelected) color else color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) color else color.copy(alpha = 0.3f)),
        modifier = Modifier.width(100.dp).height(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(time, fontSize = 16.sp, fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold, color = if (isSelected) Color.White else color)
        }
    }
 }

@Composable
private fun SettingSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun SettingItem(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, modifier = Modifier.size(20.dp), tint = iconColor)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                    if (subtitle != null) {
                        Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                    }
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
        }
    }
}
