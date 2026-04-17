import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.school.timetable.App
import com.school.timetable.data.TimetableRepository
import com.school.timetable.ui.TimetableViewModel

fun main() = application {
    val repository = TimetableRepository()
    val viewModel = TimetableViewModel(repository)
    var isOverlayActive by remember { mutableStateOf(false) }
    
    if (!isOverlayActive) {
        // Main App Window
        Window(
            onCloseRequest = ::exitApplication, 
            title = "Smart Timetable",
            alwaysOnTop = false
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
        
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(3000)
            isExpanded = false
        }

        val animationProgress by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (isExpanded) 1f else 0f,
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 400)
        )
        
        // We set the window height to be enough for full view
        Window(
            onCloseRequest = { isOverlayActive = false }, 
            title = "Timetable Overlay",
            alwaysOnTop = true,
            undecorated = true,
            transparent = true,
            state = rememberWindowState(
                width = 800.dp, 
                height = 300.dp, // slightly more height for handle
                position = WindowPosition(Alignment.TopCenter)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                // The actual content that slides
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-250 * (1f - animationProgress)).dp) // Slides up by 250dp
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Main App Content
                    Box(modifier = Modifier.height(250.dp).fillMaxWidth()) {
                        App(
                            viewModel = viewModel, 
                            onToggleOverlay = { isOverlayActive = false },
                            isOverlayActive = true
                        )
                    }
                    
                    // The Handle
                    com.school.timetable.ui.DragHandle(
                        style = com.school.timetable.ui.HandleStyle.DEFAULT,
                        onClick = { isExpanded = !isExpanded }
                    )
                }
            }
        }
    }
}
