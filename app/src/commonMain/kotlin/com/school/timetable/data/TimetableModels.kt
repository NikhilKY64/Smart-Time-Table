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
    val isActive: Boolean = false
) {
    // Companion or secondary constructor for defaults if needed, 
    // but better to handle it in construction to keep it serializable easily.
}

// Helper to create profile since randomUUID is JVM specific
fun createNewProfileData(name: String, schedules: List<DaySchedule>): TimetableProfile {
    val time = System.currentTimeMillis()
    return TimetableProfile(
        id = java.util.UUID.randomUUID().toString(),
        name = name,
        createdAt = time,
        updatedAt = time,
        schedules = schedules,
        isActive = false
    )
}
