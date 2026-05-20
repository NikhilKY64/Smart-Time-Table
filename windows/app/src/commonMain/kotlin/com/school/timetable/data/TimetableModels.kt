package com.school.timetable.data

import kotlinx.serialization.Serializable

@Serializable
data class BellTiming(
    val periodIndex: Int, // 0 to 8, or -1 for BREAK
    val startTime: String,
    val endTime: String
)

@Serializable
data class Subject(
    val id: String,
    val name: String,
    val teacher: String,
    val periodIndex: Int, // 0 to 8, or -1 for BREAK
    val startTime: String,
    val endTime: String,
    val dayOfWeek: Int // 2 = Monday, 7 = Saturday
)

@Serializable
data class DaySchedule(
    val dayOfWeek: Int,
    val subjects: List<Subject>
)

@Serializable
data class TimetableProfile(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val schedules: List<DaySchedule>,
    val bellTimings: List<BellTiming>? = null,
    val isActive: Boolean = false,
    // v1.3.0: Dynamic period configuration
    // Backward-compatible: old profiles without these fields default to 8 periods, break after 4
    val totalPeriods: Int = 8,
    val breakAfterPeriods: List<Int> = listOf(4) // 1-based: e.g. 4 means break after period 4
)

// Helper to create a new profile (used by ViewModel)
fun createNewProfileData(
    name: String,
    schedules: List<DaySchedule>,
    totalPeriods: Int = 8,
    breakAfterPeriods: List<Int> = listOf(4)
): TimetableProfile {
    val time = System.currentTimeMillis()
    return TimetableProfile(
        id = java.util.UUID.randomUUID().toString(),
        name = name,
        createdAt = time,
        updatedAt = time,
        schedules = schedules,
        totalPeriods = totalPeriods,
        breakAfterPeriods = breakAfterPeriods,
        isActive = false
    )
}
