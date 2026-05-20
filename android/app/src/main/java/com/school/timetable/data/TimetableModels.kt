package com.school.timetable.data

data class BellTiming(
    val periodIndex: Int, // 0 to 8, or -1 for BREAK
    val startTime: String,
    val endTime: String
)

data class Subject(
    val id: String,
    val name: String,
    val teacher: String,
    val periodIndex: Int, // 0 to 8, or -1 for BREAK
    val startTime: String,
    val endTime: String,
    val dayOfWeek: Int // 2 = Monday, 7 = Saturday
)

data class DaySchedule(
    val dayOfWeek: Int,
    val subjects: List<Subject>
)

data class TimetableProfile(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val schedules: List<DaySchedule>,
    val bellTimings: List<BellTiming>? = null,
    val isActive: Boolean = false
)
