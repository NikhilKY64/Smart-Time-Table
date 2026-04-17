package com.school.timetable.data

import com.school.timetable.ui.HandleStyle

expect class TimetableRepository {
    fun getAllProfiles(): List<TimetableProfile>
    fun saveAllProfiles(profiles: List<TimetableProfile>)
    fun getTimetable(): List<DaySchedule>
    fun getActiveProfileId(): String
    fun saveTimetable(profileId: String, schedules: List<DaySchedule>)
    fun getClassName(): String
    fun saveClassName(name: String)
    fun isDarkMode(): Boolean
    fun setDarkMode(dark: Boolean)
    fun is24HourFormat(): Boolean
    fun set24HourFormat(is24Hour: Boolean)
    fun getHandleStyle(): HandleStyle
    fun saveHandleStyle(style: HandleStyle)
    fun getFavoriteHandleStyles(): Set<HandleStyle>
    fun saveFavoriteHandleStyles(favorites: Set<HandleStyle>)
}
