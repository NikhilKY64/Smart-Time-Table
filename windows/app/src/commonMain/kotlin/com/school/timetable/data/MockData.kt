package com.school.timetable.data

import java.util.Calendar

fun generateMockData(totalPeriods: Int = 8, breakAfterPeriods: List<Int> = listOf(4)): List<DaySchedule> {
    val days = listOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
    )

    fun createSubj(d: Int, i: Int, p: Int, name: String, teacher: String, start: String, end: String): Subject {
        return Subject("${d}_$i", name.replace("\n", " "), teacher.replace("\n", " "), p, start, end, d)
    }

    // Default timings based on a standard 40-min period.
    // In a real scenario, this would be customizable or generated.
    fun getDummyTiming(index: Int): Pair<String, String> {
        val startHour = 7 + (index * 40) / 60
        val startMin = (10 + (index * 40)) % 60
        val endHour = 7 + ((index + 1) * 40) / 60
        val endMin = (10 + ((index + 1) * 40)) % 60
        return String.format("%02d:%02d", startHour, startMin) to String.format("%02d:%02d", endHour, endMin)
    }

    val rawData = mapOf(
        Calendar.MONDAY to listOf(
            "CHEMISTRY" to "AKHILESH CHEMISTRY", "MATHS" to "DEVENDRA PGT MATHS", "ENGLISH" to "TRIYAMBAK", "CS/PE" to "PGT CS-1 / JAGRITI",
            "ENGLISH" to "TRIYAMBAK", "CS/PE" to "PGT CS-1 / JAGRITI", "PHYSICS" to "SONALI GUPTA", "MATHS" to "DEVENDRA PGT MATHS"
        ),
        Calendar.TUESDAY to listOf(
            "MATHS" to "DEVENDRA PGT MATHS", "ENGLISH" to "TRIYAMBAK", "MATHS" to "DEVENDRA PGT MATHS", "PHYSICS" to "SONALI GUPTA",
            "CHEM/PHY_PRACTIC" to "SONALI GUPTA / PHY/CHEM LABPM", "CHEM/PHY_PRACTIC" to "SONALI GUPTA / PHY/CHEM LABPM", "CHEMISTRY" to "AKHILESH CHEMISTRY", "CS/PE" to "PGT CS-1 / JAGRITI"
        ),
        Calendar.WEDNESDAY to listOf(
            "IT/AI/IFM/NCC/PAT/DS/MULTIMEDIA" to "", "CHEMISTRY" to "AKHILESH CHEMISTRY", "ENGLISH" to "TRIYAMBAK", "SPORTS" to "MAHENDRA DUBEY",
            "MATHS" to "DEVENDRA PGT MATHS", "CS/PE" to "PGT CS-1 / JAGRITI", "MATHS" to "DEVENDRA PGT MATHS", "PHYSICS" to "SONALI GUPTA"
        ),
        Calendar.THURSDAY to listOf(
            "ENGLISH" to "TRIYAMBAK", "CS/PE" to "PGT CS-1 / JAGRITI", "MATHS" to "DEVENDRA PGT MATHS", "CHEMISTRY" to "AKHILESH CHEMISTRY",
            "CHEM/PHY_PRACTIC" to "SONALI GUPTA / PHY/CHEM LABPM", "CHEM/PHY_PRACTIC" to "SONALI GUPTA / PHY/CHEM LABPM", "CS/PE" to "PGT CS-1 / JAGRITI", "PHYSICS" to "SONALI GUPTA"
        ),
        Calendar.FRIDAY to listOf(
            "CS/PE" to "PGT CS-1 / JAGRITI", "CHEMISTRY" to "AKHILESH CHEMISTRY", "CS/PE" to "PGT CS-1 / JAGRITI", "MATHS" to "DEVENDRA PGT MATHS",
            "LIBRARY" to "GUNJAN BHATT", "ENGLISH" to "TRIYAMBAK", "PHYSICS" to "SONALI GUPTA", "MATHS" to "DEVENDRA PGT MATHS"
        ),
        Calendar.SATURDAY to listOf(
            "ACTIVITY/PAT" to "", "ACTIVITY/PAT" to "", "CHEMISTRY" to "AKHILESH CHEMISTRY", "PHYSICS" to "SONALI GUPTA",
            "CHEM/PHY_PRACTIC" to "SONALI GUPTA / PHY/CHEM LABPM", "CHEM/PHY_PRACTIC" to "SONALI GUPTA / PHY/CHEM LABPM", "ENGLISH" to "TRIYAMBAK", "CS/PE" to "PGT CS-1 / JAGRITI"
        )
    )

    return days.map { day ->
        var dataIdx = 0
        var periodCounter = 1
        val todayClasses = rawData[day] ?: emptyList()
        val subjects = mutableListOf<Subject>()
        
        var currentIndex = 0
        while (periodCounter <= totalPeriods) {
            val timing = getDummyTiming(currentIndex)
            
            // Check if a break should happen BEFORE this period starts
            // e.g. breakAfterPeriods = listOf(4) means break happens when periodCounter is about to be 5
            if (breakAfterPeriods.contains(periodCounter - 1) && subjects.lastOrNull()?.name != "BREAK") {
                subjects.add(createSubj(day, currentIndex, -1, "BREAK", "", timing.first, timing.second))
                currentIndex++
                continue
            }
            
            val data = todayClasses.getOrNull(dataIdx++) ?: ("" to "")
            val timingForPeriod = getDummyTiming(currentIndex) // recalculate timing for period
            subjects.add(createSubj(day, currentIndex, periodCounter, data.first, data.second, timingForPeriod.first, timingForPeriod.second))
            
            periodCounter++
            currentIndex++
        }
        DaySchedule(dayOfWeek = day, subjects = subjects)
    }
}
