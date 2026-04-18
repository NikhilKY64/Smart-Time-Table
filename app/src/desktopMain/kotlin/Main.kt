import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
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

fun main() = application {
    val repository = TimetableRepository()
    val viewModel = TimetableViewModel(repository)
    var isOverlayActive by remember { mutableStateOf(false) }
    
    if (!isOverlayActive) {
        // Main App Window - Maximized by default for Feature 1
        Window(
            onCloseRequest = ::exitApplication, 
            title = "Smart Timetable",
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
        var isExpanded by remember { mutableStateOf(true) }
        var handleOffsetX by remember { mutableStateOf(0f) }
        var showCustomization by remember { mutableStateOf(false) }
        var handleAlpha by remember { mutableStateOf(1f) }
        
        val handleStyle by viewModel.handleStyle.collectAsState()
        val favoriteStyles by viewModel.favoriteStyles.collectAsState()
        
        LaunchedEffect(Unit) {
            delay(3000)
            isExpanded = false
        }

        // Feature 3: Auto-transparency after delay when collapsed
        LaunchedEffect(isExpanded) {
            if (!isExpanded) {
                delay(5000) // Wait 5 seconds after collapsing
                handleAlpha = 0.2f // Fade to very subtle
            } else {
                handleAlpha = 1f // Restore full visibility
            }
        }

        // Use a dynamic Window State to physically resize the window
        val windowState = rememberWindowState(
            width = 1200.dp, 
            height = if (isExpanded) 400.dp else 65.dp, 
            position = WindowPosition(Alignment.TopCenter)
        )
        
        // Sync window height with expansion state to allow click-through
        LaunchedEffect(isExpanded) {
            // Add a small delay for window sync during animation
            windowState.size = windowState.size.copy(
                height = if (isExpanded) 400.dp else 65.dp
            )
        }
        
        Window(
            onCloseRequest = { isOverlayActive = false }, 
            title = "Timetable Overlay",
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
                    // Feature: Adaptive Content visibility with Sliding Animation
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = slideInVertically(initialOffsetY = { -it }) + expandVertically(),
                        exit = slideOutVertically(targetOffsetY = { -it }) + shrinkVertically()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp) 
                                .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
                        ) {
                            App(
                                viewModel = viewModel, 
                                onToggleOverlay = { isOverlayActive = false },
                                isOverlayActive = true
                            )
                        }
                    }
                    
                    // The Handle Section with dynamic alpha
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
                            onClick = { isExpanded = !isExpanded }
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
