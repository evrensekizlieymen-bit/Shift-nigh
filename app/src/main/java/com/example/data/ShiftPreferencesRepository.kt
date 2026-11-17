package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.shiftDataStore: DataStore<Preferences> by preferencesDataStore(name = "night_shift_prefs")

data class ShiftStats(
    val bestHorrorSeconds: Int? = null,
    val bestExploreSeconds: Int? = null,
    val totalRepairsCompleted: Int = 0,
    val audioEnabled: Boolean = true,
    val horrorModeEnabled: Boolean = true,
    val lookSensitivity: Float = 1.0f
)

class ShiftPreferencesRepository(private val context: Context) {

    private object Keys {
        val BEST_HORROR_SECONDS = intPreferencesKey("best_horror_seconds")
        val BEST_EXPLORE_SECONDS = intPreferencesKey("best_explore_seconds")
        val TOTAL_REPAIRS = intPreferencesKey("total_repairs")
        val AUDIO_ENABLED = booleanPreferencesKey("audio_enabled")
        val HORROR_MODE = booleanPreferencesKey("horror_mode")
        val LOOK_SENSITIVITY = floatPreferencesKey("look_sensitivity")
    }

    val statsFlow: Flow<ShiftStats> = context.shiftDataStore.data.map { prefs ->
        ShiftStats(
            bestHorrorSeconds = prefs[Keys.BEST_HORROR_SECONDS],
            bestExploreSeconds = prefs[Keys.BEST_EXPLORE_SECONDS],
            totalRepairsCompleted = prefs[Keys.TOTAL_REPAIRS] ?: 0,
            audioEnabled = prefs[Keys.AUDIO_ENABLED] ?: true,
            horrorModeEnabled = prefs[Keys.HORROR_MODE] ?: true,
            lookSensitivity = prefs[Keys.LOOK_SENSITIVITY] ?: 1.0f
        )
    }

    suspend fun recordShiftVictory(clearSeconds: Int, horrorMode: Boolean) {
        context.shiftDataStore.edit { prefs ->
            val currentTotal = prefs[Keys.TOTAL_REPAIRS] ?: 0
            prefs[Keys.TOTAL_REPAIRS] = currentTotal + 1
            if (horrorMode) {
                val prev = prefs[Keys.BEST_HORROR_SECONDS]
                if (prev == null || clearSeconds < prev) {
                    prefs[Keys.BEST_HORROR_SECONDS] = clearSeconds
                }
            } else {
                val prev = prefs[Keys.BEST_EXPLORE_SECONDS]
                if (prev == null || clearSeconds < prev) {
                    prefs[Keys.BEST_EXPLORE_SECONDS] = clearSeconds
                }
            }
        }
    }

    suspend fun setAudioEnabled(enabled: Boolean) {
        context.shiftDataStore.edit { prefs ->
            prefs[Keys.AUDIO_ENABLED] = enabled
        }
    }

    suspend fun setHorrorModeEnabled(enabled: Boolean) {
        context.shiftDataStore.edit { prefs ->
            prefs[Keys.HORROR_MODE] = enabled
        }
    }

    suspend fun setLookSensitivity(sensitivity: Float) {
        context.shiftDataStore.edit { prefs ->
            prefs[Keys.LOOK_SENSITIVITY] = sensitivity.coerceIn(0.5f, 2.0f)
        }
    }
}
