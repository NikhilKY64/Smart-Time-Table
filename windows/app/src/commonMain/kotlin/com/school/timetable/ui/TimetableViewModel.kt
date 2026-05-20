package com.school.timetable.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.timetable.data.DaySchedule
import com.school.timetable.data.Subject
import com.school.timetable.data.TimetableProfile
import com.school.timetable.data.TimetableRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import com.school.timetable.data.generateMockData
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

    val activeTotalPeriods: StateFlow<Int> = _profiles.map { profiles ->
        val activeProfile = profiles.find { it.isActive } ?: profiles.firstOrNull()
        activeProfile?.totalPeriods ?: 8
    }.stateIn(viewModelScope, SharingStarted.Lazily, 8)

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

    private val _isNextPeriodApproaching = MutableStateFlow(false)
    val isNextPeriodApproaching: StateFlow<Boolean> = _isNextPeriodApproaching.asStateFlow()

    private val _isAutoHideOverlayEnabled = MutableStateFlow(true)
    val isAutoHideOverlayEnabled: StateFlow<Boolean> = _isAutoHideOverlayEnabled.asStateFlow()

    private val _isOverlayExpanded = MutableStateFlow(false)
    val isOverlayExpanded: StateFlow<Boolean> = _isOverlayExpanded.asStateFlow()

    private val _isSmartHideVisible = MutableStateFlow(false)
    val isSmartHideVisible: StateFlow<Boolean> = _isSmartHideVisible.asStateFlow()

    private val _isAutoCollapseEnabled = MutableStateFlow(true)
    val isAutoCollapseEnabled: StateFlow<Boolean> = _isAutoCollapseEnabled.asStateFlow()

    private val _autoCollapseDelay = MutableStateFlow(5)
    val autoCollapseDelay: StateFlow<Int> = _autoCollapseDelay.asStateFlow()

    private val _isStartOnStartupEnabled = MutableStateFlow(false)
    val isStartOnStartupEnabled: StateFlow<Boolean> = _isStartOnStartupEnabled.asStateFlow()

    private val _isAutoSlideEnabled = MutableStateFlow(true)
    val isAutoSlideEnabled: StateFlow<Boolean> = _isAutoSlideEnabled.asStateFlow()

    private val _subjectTeachers = MutableStateFlow<Map<String, String>>(emptyMap())
    val subjectTeachers: StateFlow<Map<String, String>> = _subjectTeachers.asStateFlow()

    private var smartHideJob: kotlinx.coroutines.Job? = null
    private var lastTriggeredMinute43 = ""
    private var lastTriggeredMinute14 = ""

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
        _isAutoHideOverlayEnabled.value = repository.isAutoHideOverlayEnabled()
        _isAutoCollapseEnabled.value = repository.isAutoCollapseEnabled()
        _autoCollapseDelay.value = repository.getAutoCollapseDelay()
        _isStartOnStartupEnabled.value = repository.isStartOnStartupEnabled()
        _isAutoSlideEnabled.value = repository.isAutoSlideEnabled()
        
        _subjectTeachers.value = repository.getSubjectTeachers()
        autoScanSubjects()
    }
    private fun startClock() {
        viewModelScope.launch {
            while (true) {
                val cal = Calendar.getInstance()
                val day = cal.get(Calendar.DAY_OF_WEEK)
                _currentDay.value = day
                
                val sdf24Display = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val timeString24Display = sdf24Display.format(cal.time)
                
                val sdf24Logic = SimpleDateFormat("HH:mm", Locale.getDefault())
                currentTime24ForLogic = sdf24Logic.format(cal.time)
                
                // Pre-emptive 4.3-second detection to wake up window early (SS:55.7)
                val calPlus43 = (cal.clone() as Calendar).apply { add(Calendar.MILLISECOND, 4300) }
                val nextTime43 = sdf24Logic.format(calPlus43.time)
                if (calPlus43.get(Calendar.SECOND) == 0 && nextTime43 != lastTriggeredMinute43) {
                    val schedule = _schedules.value.find { it.dayOfWeek == day }
                    val startingSoon = schedule?.subjects?.any { it.startTime == nextTime43 } ?: false
                    if (startingSoon) {
                        lastTriggeredMinute43 = nextTime43
                        // ONLY wake up handle early if auto-slide is enabled
                        if (_isAutoSlideEnabled.value) {
                            setSmartHideVisible(true) // Silently create window
                        }
                    }
                }

                // Pre-emptive 1.4-second detection for the actual drop-down
                val calPlus14 = (cal.clone() as Calendar).apply { add(Calendar.MILLISECOND, 1400) }
                val nextTime14 = sdf24Logic.format(calPlus14.time)
                if (calPlus14.get(Calendar.SECOND) == 0 && nextTime14 != lastTriggeredMinute14) {
                    val schedule = _schedules.value.find { it.dayOfWeek == day }
                    val startingSoon = schedule?.subjects?.any { it.startTime == nextTime14 } ?: false
                    if (startingSoon && !_isNextPeriodApproaching.value) {
                        lastTriggeredMinute14 = nextTime14
                        _isNextPeriodApproaching.value = true
                        
                        // Drop down!
                        if (_isAutoSlideEnabled.value) {
                            setOverlayExpanded(true)
                        }

                        launch {
                            delay(4900) // Spans 1.4s before and 3.5s after start (Total 4.9s)
                            _isNextPeriodApproaching.value = false
                        }
                    }
                }

                val sdf12Display = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                val timeString12Display = sdf12Display.format(cal.time)
                
                _currentTimeFormat.value = if (_is24HourFormat.value) timeString24Display else timeString12Display

                calculateCurrentPeriod(day, currentTime24ForLogic)

                delay(100) // 100ms precision for 1.5s detection
            }
        }
    }

    private fun calculateCurrentPeriod(day: Int, currentTime: String) {
        val schedule = _schedules.value.find { it.dayOfWeek == day }
        if (schedule != null) {
            val period = schedule.subjects.find { 
                currentTime >= it.startTime && currentTime < it.endTime
            }
            _currentPeriodId.value = period?.id
        } else {
            _currentPeriodId.value = null
        }
    }

    private fun refreshSchedules() {
        _schedules.value = repository.getTimetable()
        _profiles.value = repository.getAllProfiles()
        calculateCurrentPeriod(_currentDay.value, currentTime24ForLogic)
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

    /** Copies only the name & teacher of [source] into the slot of [target], leaving source unchanged. */
    fun copySubjectTo(source: Subject, target: Subject) {
        val schedules = _workingSchedules.value.map { day ->
            val newSubjects = day.subjects.map { subj ->
                if (subj.id == target.id) {
                    subj.copy(name = source.name, teacher = source.teacher)
                } else subj
            }
            day.copy(subjects = newSubjects)
        }
        _workingSchedules.value = schedules
        _hasUnsavedChanges.value = true
    }

    /** Shifts an entire column left, swapping content and identity but keeping time slots chronologically intact. */
    fun shiftColumnLeft(columnIndex: Int) {
        if (columnIndex <= 0) return
        val currentSchedules = _workingSchedules.value
        if (currentSchedules.isEmpty() || currentSchedules.first().subjects.size <= columnIndex) return
        
        val newSchedules = currentSchedules.map { day ->
            val newSubjects = day.subjects.toMutableList()
            val subjectA = newSubjects[columnIndex]
            val subjectB = newSubjects[columnIndex - 1]
            
            newSubjects[columnIndex] = subjectB.copy(
                id = subjectA.id, startTime = subjectA.startTime, endTime = subjectA.endTime, dayOfWeek = subjectA.dayOfWeek
            )
            newSubjects[columnIndex - 1] = subjectA.copy(
                id = subjectB.id, startTime = subjectB.startTime, endTime = subjectB.endTime, dayOfWeek = subjectB.dayOfWeek
            )
            day.copy(subjects = newSubjects)
        }
        _workingSchedules.value = newSchedules
        _hasUnsavedChanges.value = true
    }

    /** Shifts an entire column right, swapping content and identity but keeping time slots chronologically intact. */
    fun shiftColumnRight(columnIndex: Int) {
        val currentSchedules = _workingSchedules.value
        if (currentSchedules.isEmpty() || columnIndex >= currentSchedules.first().subjects.size - 1) return
        
        val newSchedules = currentSchedules.map { day ->
            val newSubjects = day.subjects.toMutableList()
            val subjectA = newSubjects[columnIndex]
            val subjectB = newSubjects[columnIndex + 1]
            
            newSubjects[columnIndex] = subjectB.copy(
                id = subjectA.id, startTime = subjectA.startTime, endTime = subjectA.endTime, dayOfWeek = subjectA.dayOfWeek
            )
            newSubjects[columnIndex + 1] = subjectA.copy(
                id = subjectB.id, startTime = subjectB.startTime, endTime = subjectB.endTime, dayOfWeek = subjectB.dayOfWeek
            )
            day.copy(subjects = newSubjects)
        }
        _workingSchedules.value = newSchedules
        _hasUnsavedChanges.value = true
    }

    fun commitWorkingSchedules() {
        val pId = _viewingProfileId.value ?: repository.getActiveProfileId()
        _slidePanelSchedules.value = _workingSchedules.value
        repository.saveTimetable(pId, _workingSchedules.value)
        _hasUnsavedChanges.value = false
        refreshSchedules()
    }

    fun discardWorkingSchedules() {
        _workingSchedules.value = _slidePanelSchedules.value
        _hasUnsavedChanges.value = false
    }

    fun getGlobalBellTimings(): List<Subject> {
        val firstDay = _schedules.value.firstOrNull() ?: return emptyList()
        return firstDay.subjects
    }

    fun updateGlobalBellTiming(periodIndex: Int, newStartTime: String, newEndTime: String) {
        val timingMap = mapOf(periodIndex to Pair(newStartTime, newEndTime))
        applyBellTimings(timingMap)
    }

    fun saveAllBellTimings(timings: List<Subject>) {
        val timingMap = timings.associate { it.periodIndex to Pair(it.startTime, it.endTime) }
        applyBellTimings(timingMap)
    }

    private fun applyBellTimings(timingMap: Map<Int, Pair<String, String>>) {
        val updatedSchedules = _schedules.value.map { day ->
            day.copy(subjects = day.subjects.map { subject ->
                val newTiming = timingMap[subject.periodIndex]
                if (newTiming != null) {
                    subject.copy(startTime = newTiming.first, endTime = newTiming.second)
                } else {
                    subject
                }
            })
        }

        val activeProfileId = repository.getActiveProfileId()
        repository.saveTimetable(activeProfileId, updatedSchedules)

        if (_workingSchedules.value.isNotEmpty()) {
            val updatedWorking = _workingSchedules.value.map { day ->
                day.copy(subjects = day.subjects.map { subject ->
                    val newTiming = timingMap[subject.periodIndex]
                    if (newTiming != null) {
                        subject.copy(startTime = newTiming.first, endTime = newTiming.second)
                    } else {
                        subject
                    }
                })
            }
            _workingSchedules.value = updatedWorking
        }

        refreshSchedules()
    }

    fun createNewProfile(name: String, totalPeriods: Int = 8, breakAfterPeriods: List<Int> = listOf(4), copyFromActive: Boolean = false) {
        val currentProfiles = repository.getAllProfiles().toMutableList()
        val newSchedules = if (copyFromActive) {
            repository.getTimetable()
        } else {
            generateMockData(totalPeriods, breakAfterPeriods).map { day -> 
                day.copy(subjects = day.subjects.map { 
                    if (it.name != "BREAK") it.copy(name = "", teacher = "", startTime = "", endTime = "") else it 
                }) 
            }
        }
        
        val time = System.currentTimeMillis()
        val newProfile = TimetableProfile(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            createdAt = time,
            updatedAt = time,
            schedules = newSchedules,
            totalPeriods = totalPeriods,
            breakAfterPeriods = breakAfterPeriods,
            isActive = false
        )
        currentProfiles.add(newProfile)
        repository.saveAllProfiles(currentProfiles)
        _profiles.value = repository.getAllProfiles()
    }

    fun addPeriod() {
        val currentWorking = _workingSchedules.value
        if (currentWorking.isEmpty()) return
        
        val maxPeriodIndex = currentWorking.first().subjects.maxOfOrNull { it.periodIndex } ?: 0
        val newPeriodIndex = maxPeriodIndex + 1
        
        val newWorking = currentWorking.map { daySchedule ->
            val newSubject = Subject(
                id = "${daySchedule.dayOfWeek}_new_$newPeriodIndex",
                name = "",
                teacher = "",
                periodIndex = newPeriodIndex,
                startTime = "",
                endTime = "",
                dayOfWeek = daySchedule.dayOfWeek
            )
            daySchedule.copy(subjects = daySchedule.subjects + newSubject)
        }
        
        _workingSchedules.value = newWorking
        _hasUnsavedChanges.value = true
        
        updateViewingProfileTotalPeriods(newWorking)
    }
    
    fun removePeriod() {
        val currentWorking = _workingSchedules.value
        if (currentWorking.isEmpty()) return
        
        val maxPeriodIndex = currentWorking.first().subjects.maxOfOrNull { it.periodIndex } ?: 0
        if (maxPeriodIndex <= 1) return // Don't remove the last period
        
        val newWorking = currentWorking.map { daySchedule ->
            val newSubjects = daySchedule.subjects.filter { it.periodIndex != maxPeriodIndex }
            daySchedule.copy(subjects = newSubjects)
        }
        
        _workingSchedules.value = newWorking
        _hasUnsavedChanges.value = true
        
        updateViewingProfileTotalPeriods(newWorking)
    }

    private fun updateViewingProfileTotalPeriods(newWorking: List<DaySchedule>) {
        val pId = _viewingProfileId.value ?: repository.getActiveProfileId()
        val profilesList = _profiles.value.toMutableList()
        val index = profilesList.indexOfFirst { it.id == pId }
        if (index != -1) {
            val oldProfile = profilesList[index]
            val newTotal = newWorking.firstOrNull()?.subjects?.filter { it.periodIndex != -1 }?.size ?: oldProfile.totalPeriods
            
            // Also need to save this working schedule to the profile so totalPeriods stays in sync
            val updatedProfile = oldProfile.copy(totalPeriods = newTotal, schedules = newWorking)
            profilesList[index] = updatedProfile
            _profiles.value = profilesList
            repository.saveAllProfiles(profilesList)
        }
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
        refreshSchedules()
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

    fun setOverlayExpanded(expanded: Boolean) {
        _isOverlayExpanded.value = expanded
        if (expanded) {
            _isSmartHideVisible.value = true
        }
        
        smartHideJob?.cancel()
        if (!expanded && _isAutoHideOverlayEnabled.value) {
            smartHideJob = viewModelScope.launch {
                delay(5000)
                _isSmartHideVisible.value = false
            }
        }
    }

    fun toggleOverlayExpanded() {
        setOverlayExpanded(!_isOverlayExpanded.value)
    }

    fun setSmartHideVisible(visible: Boolean) {
        _isSmartHideVisible.value = visible
        if (visible) {
            if (!_isOverlayExpanded.value && _isAutoHideOverlayEnabled.value) {
                smartHideJob?.cancel()
                smartHideJob = viewModelScope.launch {
                    delay(5000)
                    _isSmartHideVisible.value = false
                }
            }
        }
    }

    fun toggleAutoHideOverlay() {
        val newState = !_isAutoHideOverlayEnabled.value
        _isAutoHideOverlayEnabled.value = newState
        repository.setAutoHideOverlayEnabled(newState)
        
        if (!newState) {
            _isSmartHideVisible.value = true
            smartHideJob?.cancel()
        } else if (!_isOverlayExpanded.value) {
            setOverlayExpanded(false)
        }
    }

    fun toggleTimeFormat() {
        val newFormat = !_is24HourFormat.value
        _is24HourFormat.value = newFormat
        repository.set24HourFormat(newFormat)
        
        val cal = Calendar.getInstance()
        val sdf24 = SimpleDateFormat("HH:mm", Locale.getDefault())
        val sdf12 = SimpleDateFormat("hh:mm a", Locale.getDefault())
        _currentTimeFormat.value = if (newFormat) sdf24.format(cal.time) else sdf12.format(cal.time)
    }

    fun toggleFavoriteStyle(style: HandleStyle) {
        val currentFavorites = _favoriteStyles.value.toMutableSet()
        if (currentFavorites.contains(style)) {
            currentFavorites.remove(style)
        } else {
            currentFavorites.add(style)
        }
        _favoriteStyles.value = currentFavorites
        repository.saveFavoriteHandleStyles(currentFavorites)
    }

    fun setAutoCollapseEnabled(enabled: Boolean) {
        _isAutoCollapseEnabled.value = enabled
        repository.setAutoCollapseEnabled(enabled)
    }

    fun setAutoCollapseDelay(seconds: Int) {
        _autoCollapseDelay.value = seconds
        repository.setAutoCollapseDelay(seconds)
    }

    fun setStartOnStartupEnabled(enabled: Boolean) {
        _isStartOnStartupEnabled.value = enabled
        repository.setStartOnStartupEnabled(enabled)
    }

    fun setAutoSlideEnabled(enabled: Boolean) {
        _isAutoSlideEnabled.value = enabled
        repository.setAutoSlideEnabled(enabled)
    }

    private fun autoScanSubjects() {
        val allSubjects = _profiles.value.flatMap { profile ->
            profile.schedules.flatMap { day ->
                day.subjects
            }
        }.filter { it.periodIndex != -1 }
        
        val currentTeachers = _subjectTeachers.value.toMutableMap()
        var changed = false
        
        allSubjects.forEach { subject ->
            if (subject.name.isNotBlank() && !currentTeachers.containsKey(subject.name)) {
                currentTeachers[subject.name] = subject.teacher
                changed = true
            }
        }
        
        if (changed) {
            _subjectTeachers.value = currentTeachers
            repository.saveSubjectTeachers(currentTeachers)
        }
    }

    fun updateTeacherForSubject(subjectName: String, teacherName: String) {
        val currentTeachers = _subjectTeachers.value.toMutableMap()
        currentTeachers[subjectName] = teacherName
        _subjectTeachers.value = currentTeachers
        repository.saveSubjectTeachers(currentTeachers)
        
        syncTeachersAcrossProfiles(subjectName, teacherName)
    }

    fun deleteSubjectTeacher(subjectName: String) {
        val currentTeachers = _subjectTeachers.value.toMutableMap()
        currentTeachers.remove(subjectName)
        _subjectTeachers.value = currentTeachers
        repository.saveSubjectTeachers(currentTeachers)
        
        // Reset these subjects to empty teacher in all profiles
        syncTeachersAcrossProfiles(subjectName, "")
    }

    fun addSubjectTeacher(subjectName: String, teacherName: String) {
        if (subjectName.isBlank()) return
        val currentTeachers = _subjectTeachers.value.toMutableMap()
        currentTeachers[subjectName] = teacherName
        _subjectTeachers.value = currentTeachers
        repository.saveSubjectTeachers(currentTeachers)
        
        syncTeachersAcrossProfiles(subjectName, teacherName)
    }

    private fun syncTeachersAcrossProfiles(subjectName: String, teacherName: String) {
        val updatedProfiles = _profiles.value.map { profile ->
            val updatedSchedules = profile.schedules.map { day ->
                val updatedSubjects = day.subjects.map { subject ->
                    if (subject.name == subjectName) {
                        subject.copy(teacher = teacherName)
                    } else {
                        subject
                    }
                }
                day.copy(subjects = updatedSubjects)
            }
            profile.copy(schedules = updatedSchedules)
        }
        
        _profiles.value = updatedProfiles
        repository.saveAllProfiles(updatedProfiles)
        
        val activeId = repository.getActiveProfileId()
        _schedules.value = updatedProfiles.find { it.id == activeId }?.schedules ?: emptyList()
        
        if (_viewingProfileId.value != null) {
            _slidePanelSchedules.value = updatedProfiles.find { it.id == _viewingProfileId.value }?.schedules ?: emptyList()
            _workingSchedules.value = _slidePanelSchedules.value
        }
    }
}

class TimetableViewModelFactory(private val repository: TimetableRepository) {
    fun create(): TimetableViewModel {
        return TimetableViewModel(repository)
    }
}
