package com.school.timetable

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.school.timetable.data.TimetableRepository
import com.school.timetable.ui.CompactTimetableCard
import com.school.timetable.ui.theme.AccentPrimary
import com.school.timetable.ui.theme.SmartTimetableTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import com.school.timetable.ui.DragHandle
import com.school.timetable.ui.HandleStyle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.unit.IntOffset

class TimetableOverlayService : LifecycleService(), SavedStateRegistryOwner {
    
    private lateinit var windowManager: WindowManager
    private lateinit var composeView: ComposeView
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    companion object {
        var isRunning = false
    }

    @Suppress("DEPRECATION")
    override fun onCreate() {
        super.onCreate()
        isRunning = true
        savedStateRegistryController.performRestore(null)
        
        createNotificationChannel()
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "OVERLAY_CHANNEL")
                .setContentTitle("Timetable Overlay Active")
                .setSmallIcon(android.R.drawable.ic_menu_today)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("Timetable Overlay Active")
                .setSmallIcon(android.R.drawable.ic_menu_today)
                .build()
        }
        startForeground(1, notification)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        val repository = TimetableRepository(applicationContext)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@TimetableOverlayService)
            setViewTreeSavedStateRegistryOwner(this@TimetableOverlayService)
            
            setContent {
                val context = androidx.compose.ui.platform.LocalContext.current
                var expanded by remember { mutableStateOf(false) }
                var isDark by remember { mutableStateOf(repository.isDarkMode()) }

                SmartTimetableTheme(darkTheme = isDark) {
                    OverlayContent(
                        repository = repository,
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        onThemeToggle = {
                            isDark = !isDark
                            repository.setDarkMode(isDark)
                        },
                        context = context,
                        updateWindowParams = { isFullScreen ->
                            if (isFullScreen) {
                                params.height = WindowManager.LayoutParams.MATCH_PARENT
                                windowManager.updateViewLayout(this@apply, params)
                            } else {
                                params.height = WindowManager.LayoutParams.WRAP_CONTENT
                                windowManager.updateViewLayout(this@apply, params)
                            }
                        }
                    )
                }
            }
        }

        windowManager.addView(composeView, params)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "OVERLAY_CHANNEL",
                "Overlay Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        if (::composeView.isInitialized) {
            windowManager.removeView(composeView)
        }
    }
}

@Composable
fun OverlayContent(
    repository: TimetableRepository,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onThemeToggle: () -> Unit,
    context: android.content.Context,
    updateWindowParams: (Boolean) -> Unit
) {
    var schedules by remember { mutableStateOf(repository.getTimetable()) }
    var currentDay by remember { mutableStateOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) }
    var currentTime by remember { mutableStateOf("") }
    var currentPeriodId by remember { mutableStateOf<String?>(null) }
    var handleStyle by remember { mutableStateOf(repository.getHandleStyle()) }
    var favoriteStyles by remember { mutableStateOf(repository.getFavoriteHandleStyles()) }
    var is24HourFormat by remember { mutableStateOf(repository.is24HourFormat()) }
    var showQuickSettings by remember { mutableStateOf(false) }

    var previousPeriod by remember { mutableStateOf<String?>(null) }
    var firstLoad by remember { mutableStateOf(true) }
    var handleOffsetX by remember { mutableStateOf(0f) }
    var isHandleIdle by remember { mutableStateOf(false) }
    
    // Smooth Translation Animation properties
    var contentHeightPx by remember { mutableStateOf(1500f) }
    
    val panelOffset by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (expanded) 0f else -contentHeightPx,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 350, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "PanelOffset"
    )
    
    val isFullyClosed = !expanded && panelOffset <= -contentHeightPx + 1f
    
    LaunchedEffect(expanded) {
        if (expanded) {
            updateWindowParams(true)
        }
    }
    
    LaunchedEffect(isFullyClosed, showQuickSettings) {
        if (showQuickSettings) {
             updateWindowParams(true)
        } else if (isFullyClosed) {
            updateWindowParams(false)
        }
    }

    LaunchedEffect(isFullyClosed, handleOffsetX, expanded) {
        if (isFullyClosed && !expanded) {
            isHandleIdle = false
            delay(1500)
            isHandleIdle = true
        } else {
            isHandleIdle = false
        }
    }

    val handleAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isHandleIdle) 0.5f else 1f,
        animationSpec = androidx.compose.animation.core.tween(500),
        label = "handleAlpha"
    )

    LaunchedEffect(Unit) {
        val sdf24 = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        val sdf12 = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault())
        val logicFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())

        while(true) {
            val updatedSchedules = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { repository.getTimetable() }
            if (schedules != updatedSchedules) schedules = updatedSchedules
            
            val updatedStyle = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { repository.getHandleStyle() }
            if (handleStyle != updatedStyle) handleStyle = updatedStyle

            val updatedFavorites = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { repository.getFavoriteHandleStyles() }
            if (favoriteStyles != updatedFavorites) favoriteStyles = updatedFavorites

            val updatedFormat = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { repository.is24HourFormat() }
            if (is24HourFormat != updatedFormat) is24HourFormat = updatedFormat

            val cal = Calendar.getInstance()
            currentDay = cal.get(Calendar.DAY_OF_WEEK)
            
            val timeString24 = sdf24.format(cal.time)
            currentTime = if (is24HourFormat) timeString24 else sdf12.format(cal.time)

            val logicTime24 = logicFormat.format(cal.time)
            
            val displayDay = if (currentDay == Calendar.SUNDAY) Calendar.MONDAY else currentDay
            val schedule = schedules.find { it.dayOfWeek == displayDay }
            if (schedule != null) {
                val period = schedule.subjects.find { 
                    logicTime24 >= it.startTime && logicTime24 < it.endTime
                }
                currentPeriodId = period?.id
            } else {
                currentPeriodId = null
            }
            
            kotlinx.coroutines.delay(1000) // Update every second to live-sync handle/UI
        }
    }

    // Only render the animating block if not fully closed
    if (!isFullyClosed) {
        val alphaVal = (1f - (Math.abs(panelOffset) / contentHeightPx.coerceAtLeast(1f))).coerceIn(0f, 1f)
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f * alphaVal))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { onExpandedChange(false) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = panelOffset
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* absorb clicks */ }
                    .onGloballyPositioned { coordinates ->
                        if (coordinates.size.height > 0) {
                            contentHeightPx = coordinates.size.height.toFloat()
                        }
                    },
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                shape = RoundedCornerShape(0.dp),
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    val displayDay = if (currentDay == Calendar.SUNDAY) Calendar.MONDAY else currentDay
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dayName = when(displayDay) {
                            Calendar.MONDAY -> "Monday"
                            Calendar.TUESDAY -> "Tuesday"
                            Calendar.WEDNESDAY -> "Wednesday"
                            Calendar.THURSDAY -> "Thursday"
                            Calendar.FRIDAY -> "Friday"
                            else -> "Saturday"
                        }
                        Text("$dayName • $currentTime", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                modifier = Modifier.clickable {
                                    val intent = Intent(context, MainActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    }
                                    context.startActivity(intent)
                                    onExpandedChange(false)
                                }.clip(RoundedCornerShape(8.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Open App", modifier = Modifier.padding(8.dp), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
                            }
                            
                            Surface(
                                modifier = Modifier.clickable { onThemeToggle() }.clip(RoundedCornerShape(8.dp)),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Switch Theme", modifier = Modifier.padding(8.dp), color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    val todaySchedule = schedules.find { it.dayOfWeek == displayDay }
                    val listState = rememberLazyListState()

                    LaunchedEffect(currentPeriodId) {
                        if (!firstLoad && currentPeriodId != previousPeriod && currentPeriodId != null) {
                            val idx = todaySchedule?.subjects?.indexOfFirst { it.id == currentPeriodId } ?: -1
                            if (idx >= 0) {
                                listState.animateScrollToItem(idx)
                            }
                        }
                        previousPeriod = currentPeriodId
                        firstLoad = false
                    }
                    
                    if (todaySchedule != null) {
                        LazyRow(
                            state = listState,
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(todaySchedule.subjects) { subject ->
                                val formatTime = { time24: String ->
                                    if (is24HourFormat) time24
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

                                CompactTimetableCard(
                                    subject = subject,
                                    isCurrent = subject.id == currentPeriodId,
                                    timeFormatter = formatTime,
                                    onClick = {}
                                )
                            }
                        }
                    }
                }
            }
            
                // Drag handle tab permanently attached to the bottom of the sliding body
                Box(
                    modifier = Modifier
                        .offset { IntOffset(handleOffsetX.roundToInt(), 0) }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                change.consume()
                                handleOffsetX += dragAmount
                                isHandleIdle = false
                            }
                        }
                        .alpha(handleAlpha)
                ) {
                    DragHandle(
                        style = handleStyle,
                        onClick = { onExpandedChange(false) },
                        onLongClick = { showQuickSettings = true }
                    )
                    
                    DropdownMenu(
                        expanded = showQuickSettings,
                        onDismissRequest = { showQuickSettings = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        Text("Favorite Styles", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        favoriteStyles.forEach { favStyle ->
                            DropdownMenuItem(
                                text = { Text(favStyle.displayName, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = { 
                                    repository.saveHandleStyle(favStyle)
                                    handleStyle = favStyle
                                    showQuickSettings = false
                                }
                            )
                        }
                        if (favoriteStyles.isEmpty()) {
                            Text("No favorites saved.", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    } else {
        // When completely closed, we surrender all height bounds and only render the isolated tab.
        Column(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(handleOffsetX.roundToInt(), 0) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            handleOffsetX += dragAmount
                            isHandleIdle = false
                        }
                    }
                    .alpha(handleAlpha)
            ) {
                DragHandle(
                    style = handleStyle,
                    onClick = { onExpandedChange(true) },
                    onLongClick = { showQuickSettings = true }
                )
                
                DropdownMenu(
                    expanded = showQuickSettings,
                    onDismissRequest = { showQuickSettings = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    Text("Favorite Styles", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    favoriteStyles.forEach { favStyle ->
                        DropdownMenuItem(
                            text = { Text(favStyle.displayName, color = MaterialTheme.colorScheme.onSurface) },
                            onClick = { 
                                repository.saveHandleStyle(favStyle)
                                handleStyle = favStyle
                                showQuickSettings = false
                            }
                        )
                    }
                    if (favoriteStyles.isEmpty()) {
                        Text("No favorites saved.", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
