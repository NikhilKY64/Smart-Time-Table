package com.school.timetable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.school.timetable.ui.TimetableScreen
import com.school.timetable.ui.TimetableViewModel
import com.school.timetable.ui.theme.SmartTimetableTheme

@Composable
fun App(
    viewModel: TimetableViewModel, 
    onToggleOverlay: () -> Unit = {},
    isOverlayActive: Boolean = false
) {
    val isDark by viewModel.isDarkMode.collectAsState()
    SmartTimetableTheme(darkTheme = isDark) {
        TimetableScreen(
            viewModel = viewModel,
            onToggleOverlay = onToggleOverlay,
            isOverlayActive = isOverlayActive
        )
    }
}
