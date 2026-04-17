package com.school.timetable.data

import java.util.Calendar

fun generateMockData(): List<DaySchedule> {
    val days = listOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
    )

    fun createSubj(d: Int, i: Int, p: Int, name: String, teacher: String, start: String, end: String): Subject {
        return Subject("${d}_$i", name.replace("\n", " "), teacher.replace("\n", " "), p, start, end, d)
    }

    val timings = listOf(
        "08:25" to "09:00", // 1
        "09:00" to "09:35", // 2
        "09:35" to "10:10", // 3
        "10:10" to "10:45", // 4
        "10:45" to "11:00", // BREAK
        "11:05" to "11:45", // 5
        "11:45" to "12:20", // 6
        "12:20" to "13:55", // 7
        "13:55" to "13:30"  // 8
    )

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
        var idx = 0
        val todayClasses = rawData[day] ?: emptyList()
        val subjects = timings.mapIndexed { index, timing ->
            if (index == 4) {
                createSubj(day, index, -1, "BREAK", "", timing.first, timing.second)
            } else {
                val pIdx = if (index < 4) index + 1 else index
                val data = todayClasses.getOrNull(idx++) ?: ("" to "")
                createSubj(day, index, pIdx, data.first, data.second, timing.first, timing.second)
            }
        }
        DaySchedule(dayOfWeek = day, subjects = subjects)
    }
}
