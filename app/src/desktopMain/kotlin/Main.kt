import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.gestures.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.text.font.FontWeight
import com.school.timetable.App
import com.school.timetable.data.TimetableRepository
import com.school.timetable.ui.TimetableViewModel
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import java.awt.image.BufferedImage
import java.awt.Font
import java.awt.RenderingHints
import java.awt.Color as AwtColor
import java.util.Calendar

fun main(args: Array<String>) {
    val isStartupLaunch = args.contains("--startup")
    val repository = TimetableRepository()
    val viewModel = TimetableViewModel(repository)
    
    application {
        var isOverlayActive by remember { mutableStateOf(true) }
        var isWindowVisible by remember { mutableStateOf(true) }
        var showTrayMenu by remember { mutableStateOf(false) }
    
    // Flow-driven States
    val isOverlayExpanded by viewModel.isOverlayExpanded.collectAsState()
    val isSmartHideVisible by viewModel.isSmartHideVisible.collectAsState()
    val currentPeriodId by viewModel.currentPeriodId.collectAsState()
    
    var lastPeriodId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Background Observer for Period Changes (Persistence Logic)
    val isAutoSlideEnabled by viewModel.isAutoSlideEnabled.collectAsState()
    
    LaunchedEffect(currentPeriodId) {
        if (currentPeriodId != null && currentPeriodId != lastPeriodId) {
            // Wake up and show expanded for 2 seconds on actual start
            // ONLY if auto-slide is enabled
            if (isOverlayActive && isAutoSlideEnabled) {
                viewModel.setOverlayExpanded(true)
                scope.launch {
                    delay(3500) 
                    viewModel.setOverlayExpanded(false)
                }
            }
            lastPeriodId = currentPeriodId
        }
    }

    val schedules by viewModel.schedules.collectAsState()
    val currentDay by viewModel.currentDay.collectAsState()

    val icon = painterResource("Icon.png")
    
    // Dynamic Icon Logic
    val trayIcon = remember(currentPeriodId, schedules, currentDay) {
        val today = schedules.find { it.dayOfWeek == currentDay }
        if (today == null || currentPeriodId == null) return@remember icon
        
        val index = today.subjects.indexOfFirst { it.id == currentPeriodId }
        if (index == -1) return@remember icon
        
        val subject = today.subjects[index]
        val isBreak = subject.name.uppercase().contains("BREAK")
        
        // Count non-break periods for P numbering
        var pNumber = 0
        for (i in 0..index) {
            if (!today.subjects[i].name.uppercase().contains("BREAK")) {
                pNumber++
            }
        }
        
        val text = if (isBreak) "B" else "P$pNumber"
        val backgroundColor = AwtColor(25, 118, 210) // Deep Material Blue for all
        
        try {
            val bi = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)
            val g2d = bi.createGraphics()
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            
            // Draw Background (Less rounding = more space for text)
            g2d.color = backgroundColor
            g2d.fillRoundRect(0, 0, 32, 32, 4, 4)
            
            // Draw Text (Maximized size)
            g2d.color = AwtColor.WHITE
            g2d.font = Font("SansSerif", Font.BOLD, if (text.length > 1) 22 else 28)
            val fm = g2d.fontMetrics
            val x = (32 - fm.stringWidth(text)) / 2
            val y = ((32 - fm.height) / 2) + fm.ascent
            g2d.drawString(text, x, y)
            g2d.dispose()
            
            BitmapPainter(bi.toComposeImageBitmap())
        } catch (e: Exception) {
            icon
        }
    }

    val trayState = rememberTrayState()

    Tray(
        icon = trayIcon,
        state = trayState,
        tooltip = "Smart Timetable",
        onAction = { showTrayMenu = !showTrayMenu },
        menu = {
            Item("Open Smart Controls", onClick = { showTrayMenu = true })
            Separator()
            Item("Exit App", onClick = ::exitApplication)
        }
    )

    if (isWindowVisible) {
        if (!isOverlayActive) {
            // Main App Window
            Window(
                onCloseRequest = { isWindowVisible = false }, 
                title = "Smart Timetable",
                icon = icon,
                state = rememberWindowState(placement = WindowPlacement.Maximized)
            ) {
                App(
                    viewModel = viewModel, 
                    onToggleOverlay = { isOverlayActive = true },
                    isOverlayActive = false
                )
            }
        } else {
            // Floating Slider Overlay Window
            val isSmartHideVisible by viewModel.isSmartHideVisible.collectAsState()
            var handleOffsetX by remember { mutableStateOf(0f) }
            var showCustomization by remember { mutableStateOf(false) }
            var handleAlpha by remember { mutableStateOf(1f) }
            val handleStyle by viewModel.handleStyle.collectAsState()
            val favoriteStyles by viewModel.favoriteStyles.collectAsState()
            val isAutoHideEnabled by viewModel.isAutoHideOverlayEnabled.collectAsState()
            var lastInteractionKey by remember { mutableStateOf(0L) }
            
            // Interaction: Reset timer on any interaction within the overlay
            val updateInteraction = { lastInteractionKey = System.currentTimeMillis() }

            // Smart Handle Fading Logic
            LaunchedEffect(isOverlayExpanded, lastInteractionKey, isAutoHideEnabled, isSmartHideVisible) {
                if (isOverlayExpanded || isSmartHideVisible) {
                    handleAlpha = 1f
                    updateInteraction()
                } else {
                    // Wait for 5 seconds of idle time
                    delay(5000)
                    if (isAutoHideEnabled) {
                        handleAlpha = 0f // Completely remove from screen
                    } else {
                        handleAlpha = 0.3f // Semi-transparent
                    }
                }
            }

            val isAutoCollapseEnabled by viewModel.isAutoCollapseEnabled.collectAsState()
            val autoCollapseDelay by viewModel.autoCollapseDelay.collectAsState()

            // Slider Auto-Collapse Logic
            LaunchedEffect(isOverlayExpanded, lastInteractionKey, isAutoCollapseEnabled, autoCollapseDelay) {
                if (isOverlayExpanded && isAutoCollapseEnabled) {
                    delay(autoCollapseDelay * 1000L)
                    viewModel.setOverlayExpanded(false)
                }
            }

            val animatedAlpha by animateFloatAsState(handleAlpha)

            val windowState = rememberWindowState(
                width = 1200.dp, 
                height = when {
                    isOverlayExpanded -> 400.dp
                    animatedAlpha > 0f -> 48.dp
                    else -> 0.dp
                }, 
                position = WindowPosition(Alignment.TopCenter)
            )

            // Sync minimization when Smart-Hide state changes
            LaunchedEffect(isSmartHideVisible) {
                windowState.isMinimized = !isSmartHideVisible
            }
            
            // Interaction: Clicking Taskbar icon to restore should expand the slider (if auto-slide is ON)
            LaunchedEffect(windowState.isMinimized) {
                if (!windowState.isMinimized && !isSmartHideVisible) {
                    // This happens when user clicks Taskbar icon to 'Restore'
                    if (isAutoSlideEnabled) {
                        viewModel.setSmartHideVisible(true)
                        viewModel.setOverlayExpanded(true)
                    } else {
                        // Just show the handle, don't expand
                        viewModel.setSmartHideVisible(true)
                    }
                }
            }
            
            LaunchedEffect(isOverlayExpanded, animatedAlpha) {
                windowState.size = windowState.size.copy(
                    height = when {
                        isOverlayExpanded -> 400.dp
                        animatedAlpha > 0f -> 48.dp
                        else -> 0.dp
                    }
                )
            }
            
            val overlayDialogState = rememberDialogState(
                width = windowState.size.width,
                height = windowState.size.height,
                position = windowState.position
            )

            // Keep dialog state synced with window state
            LaunchedEffect(windowState.size, windowState.position) {
                overlayDialogState.size = windowState.size
                overlayDialogState.position = windowState.position
            }

            @Suppress("DEPRECATION")
            Dialog(
                onCloseRequest = { isWindowVisible = false }, 
                title = "", // Empty title helps hide from taskbar
                undecorated = true,
                transparent = true, 
                state = overlayDialogState
            ) {
                // Ensure Always on Top
                SideEffect {
                    window.isAlwaysOnTop = true
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 40.dp)
                        .pointerInput(Unit) {
                            // Track ANY touch/mouse event to wake up the handle
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent()
                                    updateInteraction()
                                }
                            }
                        }, 
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedVisibility(
                            visible = isOverlayExpanded,
                            enter = slideInVertically(
                                initialOffsetY = { -it },
                                animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy)
                            ) + expandVertically(
                                animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy)
                            ),
                            exit = slideOutVertically(
                                targetOffsetY = { -it },
                                animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy)
                            ) + shrinkVertically(
                                animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp) 
                                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
                            ) {
                                App(
                                    viewModel = viewModel, 
                                    onToggleOverlay = { isOverlayActive = false },
                                    isOverlayActive = true
                                )
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(handleOffsetX.roundToInt(), 0) }
                                .graphicsLayer { alpha = animatedAlpha }
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            if (event.type == PointerEventType.Press && 
                                                event.buttons.isSecondaryPressed) {
                                                showCustomization = true
                                            }
                                        }
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures { change, dragAmount ->
                                        change.consume()
                                        handleOffsetX += dragAmount
                                    }
                                }
                        ) {
                            com.school.timetable.ui.DragHandle(
                                style = handleStyle,
                                onClick = { 
                                    viewModel.toggleOverlayExpanded() 
                                }
                            )
                            
                            DropdownMenu(
                                expanded = showCustomization,
                                onDismissRequest = { showCustomization = false }
                            ) {
                                Text("Fast Styles", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                favoriteStyles.forEach { style ->
                                    DropdownMenuItem(
                                        text = { Text(style.displayName) },
                                        onClick = { 
                                            viewModel.updateHandleStyle(style)
                                            showCustomization = false 
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
        
        // Custom Smart-Board Tray Menu
        if (showTrayMenu) {
            @Suppress("DEPRECATION")
            Dialog(
                onCloseRequest = { showTrayMenu = false },
                state = rememberDialogState(
                    width = 280.dp,
                    height = 300.dp,
                    position = WindowPosition(Alignment.BottomEnd)
                ),
                undecorated = true,
                transparent = true,
                resizable = false
            ) {
                // Adjust position and set always on top
                SideEffect {
                    window.isAlwaysOnTop = true
                    // Manual tweak to push it up from the very corner
                    val screen = java.awt.Toolkit.getDefaultToolkit().screenSize
                    window.setLocation(screen.width - 300, screen.height - 300)
                }

                Surface(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Close Button in Top Right
                        IconButton(
                            onClick = { showTrayMenu = false },
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close, 
                                contentDescription = "Close Menu",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Smart Controls",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        
                        // Option: Main App
                        Surface(
                            onClick = { 
                                isWindowVisible = true 
                                isOverlayActive = false 
                                showTrayMenu = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Home, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(Modifier.width(12.dp))
                                Text("Full Dashboard", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Option: Slider Overlay
                        Surface(
                            onClick = { 
                                isWindowVisible = true
                                isOverlayActive = true
                                viewModel.setOverlayExpanded(true)
                                showTrayMenu = false
                                scope.launch {
                                    delay(4000)
                                    viewModel.setOverlayExpanded(false)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.List, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(Modifier.width(12.dp))
                                Text("Show Slider", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Option: Auto-Slide Toggle
                        val isAutoSlideEnabled by viewModel.isAutoSlideEnabled.collectAsState()
                        Surface(
                            onClick = { viewModel.setAutoSlideEnabled(!isAutoSlideEnabled) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isAutoSlideEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Settings, // Use a small cog or similar
                                        contentDescription = null,
                                        tint = if (isAutoSlideEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text("Auto-Slide", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                                Switch(
                                    checked = isAutoSlideEnabled,
                                    onCheckedChange = { viewModel.setAutoSlideEnabled(it) },
                                    modifier = Modifier.scale(0.7f),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // Option: Exit
                        Surface(
                            onClick = ::exitApplication,
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(Modifier.width(8.dp))
                                Text("Exit App", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
}
