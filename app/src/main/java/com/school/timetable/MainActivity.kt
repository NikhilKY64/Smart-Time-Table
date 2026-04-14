package com.school.timetable

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.school.timetable.data.TimetableRepository
import com.school.timetable.ui.TimetableScreen
import com.school.timetable.ui.TimetableViewModel
import com.school.timetable.ui.TimetableViewModelFactory
import com.school.timetable.ui.theme.SmartTimetableTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            startOverlayService()
        } else {
            Toast.makeText(this, "Overlay permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val repository = TimetableRepository(applicationContext)
        val viewModelFactory = TimetableViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[TimetableViewModel::class.java]

        setContent {
            val isDark by viewModel.isDarkMode.collectAsState()
            SmartTimetableTheme(darkTheme = isDark) {
                TimetableScreen(
                    viewModel = viewModel,
                    onToggleOverlay = { toggleOverlay() }
                )
            }
        }
    }
    
    private fun toggleOverlay() {
        if (TimetableOverlayService.isRunning) {
            stopService(Intent(this, TimetableOverlayService::class.java))
            Toast.makeText(this, "Overlay Disabled", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        } else {
            startOverlayService()
            Toast.makeText(this, "Overlay Enabled! You can minimise the app now.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startOverlayService() {
        val intent = Intent(this, TimetableOverlayService::class.java)
        startForegroundService(intent)
    }
}
