package com.school.timetable.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.school.timetable.data.DaySchedule
import com.school.timetable.data.Subject
import com.school.timetable.data.TimetableProfile
import com.school.timetable.data.TimetableRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TimetableViewModel(private val repository: TimetableRepository) : ViewModel() {
    private val _schedules = MutableStateFlow<List<DaySchedule>>(emptyList())
    val schedules: StateFlow<List<DaySchedule>> = _schedules.asStateFlow()

    private val _profiles = MutableStateFlow<List<TimetableProfile>>(emptyList())
    val profiles: StateFlow<List<TimetableProfile>> = _profiles.asStateFlow()

    private val _viewingProfileId = MutableStateFlow<String?>(null)
    val viewingProfileId: StateFlow<String?> = _viewingProfileId.asStateFlow()

    private val _slidePanelSchedules = MutableStateFlow<List<DaySchedule>>(emptyList())
    val slidePanelSchedules: StateFlow<List<DaySchedule>> = _slidePanelSchedules.asStateFlow()

    private val _workingSchedules = MutableStateFlow<List<DaySchedule>>(emptyList())
    val workingSchedules: StateFlow<List<DaySchedule>> = _workingSchedules.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

    private val _currentDay = MutableStateFlow(Calendar.MONDAY)
    val currentDay: StateFlow<Int> = _currentDay.asStateFlow()

    private val _currentTimeFormat = MutableStateFlow("")
    val currentTimeFormat: StateFlow<String> = _currentTimeFormat.asStateFlow()

    private var currentTime24ForLogic = ""

    private val _currentPeriodId = MutableStateFlow<String?>(null)
    val currentPeriodId: StateFlow<String?> = _currentPeriodId.asStateFlow()

    private val _isSlidePanelOpen = MutableStateFlow(false)
    val isSlidePanelOpen: StateFlow<Boolean> = _isSlidePanelOpen.asStateFlow()

    private val _className = MutableStateFlow("")
    val className: StateFlow<String> = _className.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _is24HourFormat = MutableStateFlow(false)
    val is24HourFormat: StateFlow<Boolean> = _is24HourFormat.asStateFlow()

    private val _handleStyle = MutableStateFlow(HandleStyle.DEFAULT)
    val handleStyle: StateFlow<HandleStyle> = _handleStyle.asStateFlow()

    private val _favoriteStyles = MutableStateFlow<Set<HandleStyle>>(emptySet())
    val favoriteStyles: StateFlow<Set<HandleStyle>> = _favoriteStyles.asStateFlow()

    init {
        loadData()
        startClock()
    }

    private fun loadData() {
        _profiles.value = repository.getAllProfiles()
        _schedules.value = repository.getTimetable()
        _className.value = repository.getClassName()
        _isDarkMode.value = repository.isDarkMode()
        _is24HourFormat.value = repository.is24HourFormat()
        _handleStyle.value = repository.getHandleStyle()
        _favoriteStyles.value = repository.getFavoriteHandleStyles()
    }

    private fun startClock() {
        viewModelScope.launch {
            while (true) {
                val cal = Calendar.getInstance()
                _currentDay.value = cal.get(Calendar.DAY_OF_WEEK)
                
                val sdf24Display = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val timeString24Display = sdf24Display.format(cal.time)
                
                val sdf24Logic = SimpleDateFormat("HH:mm", Locale.getDefault())
                currentTime24ForLogic = sdf24Logic.format(cal.time)
                
                val sdf12Display = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                val timeString12Display = sdf12Display.format(cal.time)
                
                _currentTimeFormat.value = if (_is24HourFormat.value) timeString24Display else timeString12Display

                calculateCurrentPeriod(cal.get(Calendar.DAY_OF_WEEK), currentTime24ForLogic)

                delay(1000) // Update every second to reflect seconds
            }
        }
    }

    private fun calculateCurrentPeriod(day: Int, currentTime: String) {
        val schedule = _schedules.value.find { it.dayOfWeek == day }
        if (schedule != null) {
            val period = schedule.subjects.find { 
                currentTime >= it.startTime && currentTime <= it.endTime
            }
            _currentPeriodId.value = period?.id
        } else {
            _currentPeriodId.value = null
        }
    }

    fun updateSubject(subject: Subject, newName: String, newTeacher: String, newStart: String, newEnd: String) {
        val updatedSubject = subject.copy(name = newName, teacher = newTeacher, startTime = newStart, endTime = newEnd)
        
        val newSchedules = _workingSchedules.value.map { daySchedule ->
            if (daySchedule.dayOfWeek == updatedSubject.dayOfWeek) {
                daySchedule.copy(subjects = daySchedule.subjects.map { subj ->
                    if (subj.id == updatedSubject.id) updatedSubject else subj
                })
            } else {
                daySchedule
            }
        }
        
        _workingSchedules.value = newSchedules
        _hasUnsavedChanges.value = true
    }

    fun swapSubjects(subjectA: Subject, subjectB: Subject) {
        val schedules = _workingSchedules.value.map { day ->
            val newSubjects = day.subjects.map { subj ->
                when (subj.id) {
                    subjectA.id -> subjectB.copy(id = subjectA.id, periodIndex = subjectA.periodIndex, startTime = subjectA.startTime, endTime = subjectA.endTime, dayOfWeek = subjectA.dayOfWeek)
                    subjectB.id -> subjectA.copy(id = subjectB.id, periodIndex = subjectB.periodIndex, startTime = subjectB.startTime, endTime = subjectB.endTime, dayOfWeek = subjectB.dayOfWeek)
                    else -> subj
                }
            }
            day.copy(subjects = newSubjects)
        }
        _workingSchedules.value = schedules
        _hasUnsavedChanges.value = true
    }

    fun commitWorkingSchedules() {
        val pId = _viewingProfileId.value ?: repository.getActiveProfileId()
        _slidePanelSchedules.value = _workingSchedules.value
        
        if (pId == repository.getActiveProfileId()) {
            _schedules.value = _workingSchedules.value
            calculateCurrentPeriod(_currentDay.value, currentTime24ForLogic)
        }
        repository.saveTimetable(pId, _workingSchedules.value)
        _profiles.value = repository.getAllProfiles()
        _hasUnsavedChanges.value = false
    }

    fun discardWorkingSchedules() {
        _workingSchedules.value = _slidePanelSchedules.value
        _hasUnsavedChanges.value = false
    }

    fun getGlobalBellTimings(): List<Subject> {
        // Extract one representative list of timings from Monday (or first available day)
        // Since timings are global, they align across days.
        val firstDay = _workingSchedules.value.firstOrNull() ?: return emptyList()
        return firstDay.subjects
    }

    fun updateGlobalBellTiming(periodIndex: Int, newStartTime: String, newEndTime: String) {
        val updatedMap = _workingSchedules.value.map { day ->
            day.copy(subjects = day.subjects.map { subject ->
                if (subject.periodIndex == periodIndex) {
                    subject.copy(startTime = newStartTime, endTime = newEndTime)
                } else {
                    subject
                }
            })
        }
        
        _workingSchedules.value = updatedMap
        _hasUnsavedChanges.value = true
    }

    fun createNewProfile(name: String, copyFromActive: Boolean = false) {
        val currentProfiles = repository.getAllProfiles().toMutableList()
        val newSchedules = if (copyFromActive) {
            repository.getTimetable() // copies active
        } else {
            // generate empty structured data
            repository.getTimetable().map { day -> 
                day.copy(subjects = day.subjects.map { it.copy(name = "", teacher = "") }) 
            }
        }
        
        val newProfile = TimetableProfile(
            name = name,
            schedules = newSchedules,
            isActive = false
        )
        currentProfiles.add(newProfile)
        repository.saveAllProfiles(currentProfiles)
        _profiles.value = repository.getAllProfiles()
    }

    fun deleteProfile(profileId: String) {
        val currentProfiles = repository.getAllProfiles().toMutableList()
        val index = currentProfiles.indexOfFirst { it.id == profileId }
        if (index != -1 && !currentProfiles[index].isActive) {
            currentProfiles.removeAt(index)
            repository.saveAllProfiles(currentProfiles)
            _profiles.value = repository.getAllProfiles()
        }
    }

    fun setActiveProfile(profileId: String) {
        val currentProfiles = repository.getAllProfiles().map {
            it.copy(isActive = it.id == profileId)
        }
        repository.saveAllProfiles(currentProfiles)
        _profiles.value = repository.getAllProfiles()
        _schedules.value = repository.getTimetable()
        calculateCurrentPeriod(_currentDay.value, currentTime24ForLogic)
    }

    fun toggleSlidePanel() {
        setSlidePanel(!_isSlidePanelOpen.value)
    }
    
    fun setSlidePanel(open: Boolean, profileId: String? = null) {
        if (open) {
            val pId = profileId ?: repository.getActiveProfileId()
            _viewingProfileId.value = pId
            val profile = repository.getAllProfiles().find { it.id == pId }
            val scheds = profile?.schedules ?: emptyList()
            _slidePanelSchedules.value = scheds
            _workingSchedules.value = scheds
            _hasUnsavedChanges.value = false
        } else {
            // we do not naturally reset without prompt, UI will handle prompts
        }
        _isSlidePanelOpen.value = open
    }

    fun updateClassName(name: String) {
        _className.value = name
        repository.saveClassName(name)
    }

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
        repository.setDarkMode(_isDarkMode.value)
    }

    fun updateHandleStyle(style: HandleStyle) {
        _handleStyle.value = style
        repository.saveHandleStyle(style)
    }

    fun toggleTimeFormat() {
        val newFormat = !_is24HourFormat.value
        _is24HourFormat.value = newFormat
        repository.set24HourFormat(newFormat)
        
        // Refresh immediately using current clock
        val cal = Calendar.getInstance()
        val sdf24 = SimpleDateFormat("HH:mm", Locale.getDefault())
        val sdf12 = SimpleDateFormat("hh:mm a", Locale.getDefault())
        _currentTimeFormat.value = if (newFormat) sdf24.format(cal.time) else sdf12.format(cal.time)
    }

    fun toggleFavoriteStyle(style: HandleStyle) {
        val currentFavorites = _favoriteStyles.value.toMutableSet()
        if (currentFavorites.contains(style)) {
            // Cannot remove the last favorite, maybe allow it? Yes, we allow empty or default.
            currentFavorites.remove(style)
        } else {
            currentFavorites.add(style)
        }
        _favoriteStyles.value = currentFavorites
        repository.saveFavoriteHandleStyles(currentFavorites)
    }
}

class TimetableViewModelFactory(private val repository: TimetableRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimetableViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimetableViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
