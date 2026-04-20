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
import com.school.timetable.App
import com.school.timetable.data.TimetableRepository
import com.school.timetable.ui.TimetableViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.res.painterResource

fun main() {
    val repository = TimetableRepository()
    val viewModel = TimetableViewModel(repository)
    
    application {
        var isOverlayActive by remember { mutableStateOf(false) }
        var isWindowVisible by remember { mutableStateOf(true) }
    
    // Flow-driven States
    val isOverlayExpanded by viewModel.isOverlayExpanded.collectAsState()
    val isSmartHideVisible by viewModel.isSmartHideVisible.collectAsState()
    val currentPeriodId by viewModel.currentPeriodId.collectAsState()
    
    var lastPeriodId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Background Observer for Period Changes (Persistence Logic)
    LaunchedEffect(currentPeriodId) {
        if (currentPeriodId != null && currentPeriodId != lastPeriodId) {
            // Wake up and show expanded for 2 seconds on actual start
            if (isOverlayActive) {
                viewModel.setOverlayExpanded(true)
                scope.launch {
                    delay(2000) 
                    viewModel.setOverlayExpanded(false)
                }
            }
            lastPeriodId = currentPeriodId
        }
    }

    val trayState = rememberTrayState()
    val icon = painterResource("Icon.png")

    Tray(
        icon = icon,
        state = trayState,
        tooltip = "Smart Timetable",
        menu = {
            Item("Show App", onClick = { 
                isWindowVisible = true 
                isOverlayActive = false 
            })
            Item("Show Today's Timetable", onClick = {
                isOverlayActive = true
                viewModel.setOverlayExpanded(true)
                scope.launch {
                    delay(3000)
                    viewModel.setOverlayExpanded(false)
                }
            })
            Item("Exit", onClick = ::exitApplication)
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
            
            // Small handle-fading timer logic
            LaunchedEffect(isOverlayExpanded) {
                if (!isOverlayExpanded) {
                    delay(5000)
                    handleAlpha = 0.2f
                } else {
                    handleAlpha = 1f
                }
            }

            val windowState = rememberWindowState(
                width = 1200.dp, 
                height = if (isOverlayExpanded) 400.dp else 48.dp, 
                position = WindowPosition(Alignment.TopCenter)
            )

            // Sync minimization when Smart-Hide state changes
            LaunchedEffect(isSmartHideVisible) {
                windowState.isMinimized = !isSmartHideVisible
            }
            
            // Interaction: Clicking Taskbar icon to restore should expand the slider
            LaunchedEffect(windowState.isMinimized) {
                if (!windowState.isMinimized && !isSmartHideVisible) {
                    // This happens when user clicks Taskbar icon to 'Restore'
                    viewModel.setSmartHideVisible(true)
                    viewModel.setOverlayExpanded(true)
                }
            }
            
            LaunchedEffect(isOverlayExpanded) {
                windowState.size = windowState.size.copy(
                    height = if (isOverlayExpanded) 400.dp else 48.dp
                )
            }
            
            Window(
                onCloseRequest = { isWindowVisible = false }, 
                title = "Timetable Overlay",
                icon = icon,
                alwaysOnTop = true,
                undecorated = true,
                transparent = true, 
                state = windowState
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 40.dp), 
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
                                animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy)
                            ) + shrinkVertically(
                                animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy)
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
                        
                        val animatedAlpha by animateFloatAsState(handleAlpha)
                        
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
    }
}
