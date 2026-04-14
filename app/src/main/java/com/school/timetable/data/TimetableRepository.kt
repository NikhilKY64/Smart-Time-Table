package com.school.timetable.data

import android.content.Context
import android.content.SharedPreferences
import com.school.timetable.ui.HandleStyle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

class TimetableRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("timetable_prefs_v2", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getAllProfiles(): List<TimetableProfile> {
        val json = prefs.getString("timetable_profiles", null)
        if (json != null) {
            val type = object : TypeToken<List<TimetableProfile>>() {}.type
            return gson.fromJson(json, type)
        }
        
        // Migrate old `timetable_data` to a Default profile
        val oldJson = prefs.getString("timetable_data", null)
        val defaultSchedules = if (oldJson != null) {
            val type = object : TypeToken<List<DaySchedule>>() {}.type
            gson.fromJson<List<DaySchedule>>(oldJson, type) ?: generateMockData()
        } else {
            generateMockData()
        }
        
        val defaultProfile = TimetableProfile(
            id = "default",
            name = "Main Timetable",
            createdAt = System.currentTimeMillis(),
            schedules = defaultSchedules,
            isActive = true
        )
        
        saveAllProfiles(listOf(defaultProfile))
        prefs.edit().remove("timetable_data").apply() // clean up old data
        return listOf(defaultProfile)
    }

    fun saveAllProfiles(profiles: List<TimetableProfile>) {
        val json = gson.toJson(profiles)
        prefs.edit().putString("timetable_profiles", json).apply()
    }
    
    fun getTimetable(): List<DaySchedule> {
        val profiles = getAllProfiles()
        val active = profiles.find { it.isActive } ?: profiles.firstOrNull()
        return active?.schedules ?: generateMockData()
    }
    
    fun getActiveProfileId(): String {
        return getAllProfiles().find { it.isActive }?.id ?: "default"
    }

    fun saveTimetable(profileId: String, schedules: List<DaySchedule>) {
        val profiles = getAllProfiles().toMutableList()
        val idx = profiles.indexOfFirst { it.id == profileId }
        if (idx != -1) {
            profiles[idx] = profiles[idx].copy(schedules = schedules, updatedAt = System.currentTimeMillis())
            saveAllProfiles(profiles)
        }
    }

    fun getClassName(): String {
        return prefs.getString("class_name", "XII-E") ?: "XII-E"
    }

    fun saveClassName(name: String) {
        prefs.edit().putString("class_name", name).apply()
    }

    fun isDarkMode(): Boolean {
        return prefs.getBoolean("dark_mode", true)
    }

    fun setDarkMode(dark: Boolean) {
        prefs.edit().putBoolean("dark_mode", dark).apply()
    }

    fun is24HourFormat(): Boolean {
        return prefs.getBoolean("is_24_hour", false)
    }

    fun set24HourFormat(is24Hour: Boolean) {
        prefs.edit().putBoolean("is_24_hour", is24Hour).apply()
    }

    fun getHandleStyle(): HandleStyle {
        val styleName = prefs.getString("handle_style", HandleStyle.DEFAULT.name)
        return try {
            HandleStyle.valueOf(styleName ?: HandleStyle.DEFAULT.name)
        } catch (e: Exception) {
            HandleStyle.DEFAULT
        }
    }

    fun saveHandleStyle(style: HandleStyle) {
        prefs.edit().putString("handle_style", style.name).apply()
    }

    fun getFavoriteHandleStyles(): Set<HandleStyle> {
        val stringSet = prefs.getStringSet("favorite_styles", setOf(HandleStyle.DEFAULT.name)) ?: setOf(HandleStyle.DEFAULT.name)
        return stringSet.mapNotNull {
            try { HandleStyle.valueOf(it) } catch (e: Exception) { null }
        }.toSet()
    }

    fun saveFavoriteHandleStyles(favorites: Set<HandleStyle>) {
        val stringSet = favorites.map { it.name }.toSet()
        prefs.edit().putStringSet("favorite_styles", stringSet).apply()
    }

    private fun generateMockData(): List<DaySchedule> {
        val days = listOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
        )

        fun createSubj(d: Int, i: Int, p: Int, name: String, teacher: String, start: String, end: String): Subject {
            return Subject("${d}_$i", name.replace("\n", " "), teacher.replace("\n", " "), p, start, end, d)
        }

        val timings = listOf(
            "08:00" to "08:40", // 1
            "08:40" to "09:20", // 2
            "09:20" to "10:00", // 3
            "10:00" to "10:40", // 4
            "10:40" to "11:10", // BREAK
            "11:10" to "11:50", // 5
            "11:50" to "12:30", // 6
            "12:30" to "13:10", // 7
            "13:10" to "13:50"  // 8
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
}
