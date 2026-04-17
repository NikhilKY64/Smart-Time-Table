package com.school.timetable.data

import android.content.Context
import android.content.SharedPreferences
import com.school.timetable.ui.HandleStyle
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar

actual class TimetableRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("timetable_prefs_v2", Context.MODE_PRIVATE)
    private val jsonHandler = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    actual fun getAllProfiles(): List<TimetableProfile> {
        val json = prefs.getString("timetable_profiles", null)
        if (json != null) {
            return try {
                jsonHandler.decodeFromString<List<TimetableProfile>>(json)
            } catch (e: Exception) {
                emptyList()
            }
        }
        
        // Simple migration if needed, but for now we'll just return generated
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
        prefs.edit().putString("timetable_profiles", json).apply()
    }
    
    actual fun getTimetable(): List<DaySchedule> {
        val profiles = getAllProfiles()
        val active = profiles.find { it.isActive } ?: profiles.firstOrNull()
        return active?.schedules ?: generateMockData()
    }
    
    actual fun getActiveProfileId(): String {
        return getAllProfiles().find { it.isActive }?.id ?: "default"
    }
    
    actual fun saveTimetable(profileId: String, schedules: List<DaySchedule>) {
        val profiles = getAllProfiles().toMutableList()
        val idx = profiles.indexOfFirst { it.id == profileId }
        if (idx != -1) {
            profiles[idx] = profiles[idx].copy(schedules = schedules, updatedAt = System.currentTimeMillis())
            saveAllProfiles(profiles)
        }
    }

    actual fun getClassName(): String = prefs.getString("class_name", "XII-E") ?: "XII-E"
    actual fun saveClassName(name: String) = prefs.edit().putString("class_name", name).apply()
    actual fun isDarkMode(): Boolean = prefs.getBoolean("dark_mode", true)
    actual fun setDarkMode(dark: Boolean) = prefs.edit().putBoolean("dark_mode", dark).apply()
    actual fun is24HourFormat(): Boolean = prefs.getBoolean("is_24_hour", false)
    actual fun set24HourFormat(is24Hour: Boolean) = prefs.edit().putBoolean("is_24_hour", is24Hour).apply()
    
    actual fun getHandleStyle(): HandleStyle {
        val styleName = prefs.getString("handle_style", HandleStyle.DEFAULT.name)
        return try { HandleStyle.valueOf(styleName!!) } catch (e: Exception) { HandleStyle.DEFAULT }
    }
    actual fun saveHandleStyle(style: HandleStyle) = prefs.edit().putString("handle_style", style.name).apply()
    
    actual fun getFavoriteHandleStyles(): Set<HandleStyle> {
        val stringSet = prefs.getStringSet("favorite_styles", setOf(HandleStyle.DEFAULT.name)) ?: setOf(HandleStyle.DEFAULT.name)
        return stringSet.mapNotNull { try { HandleStyle.valueOf(it) } catch (e: Exception) { null } }.toSet()
    }
    actual fun saveFavoriteHandleStyles(favorites: Set<HandleStyle>) {
        prefs.edit().putStringSet("favorite_styles", favorites.map { it.name }.toSet()).apply()
    }
}
