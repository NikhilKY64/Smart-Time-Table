package com.school.timetable.data

import java.util.prefs.Preferences
import com.school.timetable.ui.HandleStyle
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar

actual class TimetableRepository {
    private val prefs = Preferences.userNodeForPackage(TimetableRepository::class.java)
    private val jsonHandler = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    actual fun getAllProfiles(): List<TimetableProfile> {
        val json = prefs.get("timetable_profiles", null)
        if (json != null) {
            return try {
                jsonHandler.decodeFromString<List<TimetableProfile>>(json)
            } catch (e: Exception) {
                emptyList()
            }
        }
        
        val defaultProfile = TimetableProfile(
            id = "default",
            name = "Main Timetable",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            schedules = generateMockData(),
            isActive = true
        )
        
        saveAllProfiles(listOf(defaultProfile))
        return listOf(defaultProfile)
    }

    actual fun saveAllProfiles(profiles: List<TimetableProfile>) {
        val json = jsonHandler.encodeToString(profiles)
        prefs.put("timetable_profiles", json)
    }
    
    actual fun getTimetable(): List<DaySchedule> {
        val profiles = getAllProfiles()
        val active = profiles.find { it.isActive } ?: profiles.firstOrNull()
        return active?.schedules ?: generateMockData()
    }
    
    actual fun getActiveProfileId(): String = getAllProfiles().find { it.isActive }?.id ?: "default"
    
    actual fun saveTimetable(profileId: String, schedules: List<DaySchedule>) {
        val profiles = getAllProfiles().toMutableList()
        val idx = profiles.indexOfFirst { it.id == profileId }
        if (idx != -1) {
            profiles[idx] = profiles[idx].copy(schedules = schedules, updatedAt = System.currentTimeMillis())
            saveAllProfiles(profiles)
        }
    }

    actual fun getClassName(): String = prefs.get("class_name", "XII-E")
    actual fun saveClassName(name: String) = prefs.put("class_name", name)
    actual fun isDarkMode(): Boolean = prefs.getBoolean("dark_mode", true)
    actual fun setDarkMode(dark: Boolean) = prefs.putBoolean("dark_mode", dark)
    actual fun is24HourFormat(): Boolean = prefs.getBoolean("is_24_hour", false)
    actual fun set24HourFormat(is24Hour: Boolean) = prefs.putBoolean("is_24_hour", is24Hour)
    
    actual fun getHandleStyle(): HandleStyle {
        val styleName = prefs.get("handle_style", HandleStyle.DEFAULT.name)
        return try { HandleStyle.valueOf(styleName) } catch (e: Exception) { HandleStyle.DEFAULT }
    }
    actual fun saveHandleStyle(style: HandleStyle) = prefs.put("handle_style", style.name)
    
    actual fun getFavoriteHandleStyles(): Set<HandleStyle> {
        val string = prefs.get("favorite_styles", HandleStyle.DEFAULT.name)
        return string.split(",").mapNotNull { try { HandleStyle.valueOf(it) } catch (e: Exception) { null } }.toSet()
    }
    actual fun saveFavoriteHandleStyles(favorites: Set<HandleStyle>) {
        prefs.put("favorite_styles", favorites.map { it.name }.joinToString(","))
    }
}
