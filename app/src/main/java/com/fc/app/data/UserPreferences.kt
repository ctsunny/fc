package com.fc.app.data

import android.content.Context
import com.fc.app.data.model.OverlayTextField
import com.fc.app.util.AspectRatioOption
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Thin SharedPreferences wrapper for persisting user defaults and named presets.
 */
class UserPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    // ─── Last-used defaults ───────────────────────────────────────────────────

    fun saveLastFields(fields: List<OverlayTextField>) {
        prefs.edit()
            .putString(KEY_LAST_FIELDS, Json.encodeToString(fields))
            .apply()
    }

    fun loadLastFields(): List<OverlayTextField>? =
        prefs.getString(KEY_LAST_FIELDS, null)?.let { json ->
            runCatching { Json.decodeFromString<List<OverlayTextField>>(json) }.getOrNull()
        }

    fun saveLastAspectRatio(option: AspectRatioOption) {
        prefs.edit().putString(KEY_LAST_RATIO, option.name).apply()
    }

    fun loadLastAspectRatio(): AspectRatioOption =
        prefs.getString(KEY_LAST_RATIO, null)
            ?.let { runCatching { AspectRatioOption.valueOf(it) }.getOrNull() }
            ?: AspectRatioOption.ORIGINAL

    fun saveFadeDurationSecs(secs: Int) {
        prefs.edit().putInt(KEY_FADE_DURATION, secs).apply()
    }

    fun loadFadeDurationSecs(): Int =
        prefs.getInt(KEY_FADE_DURATION, DEFAULT_FADE_SECS)

    // ─── Named presets ────────────────────────────────────────────────────────

    /** Returns all saved user presets as a list of (name, fields) pairs. */
    fun loadPresets(): List<UserPreset> {
        val names = prefs.getStringSet(KEY_PRESET_NAMES, emptySet()) ?: emptySet()
        return names.mapNotNull { name ->
            val json = prefs.getString(presetKey(name), null) ?: return@mapNotNull null
            val fields = runCatching { Json.decodeFromString<List<OverlayTextField>>(json) }.getOrNull()
                ?: return@mapNotNull null
            UserPreset(name, fields)
        }.sortedBy { it.name }
    }

    fun savePreset(name: String, fields: List<OverlayTextField>) {
        val names = getOrMutablePresetNames()
        names.add(name)
        prefs.edit()
            .putStringSet(KEY_PRESET_NAMES, names)
            .putString(presetKey(name), Json.encodeToString(fields))
            .apply()
    }

    fun deletePreset(name: String) {
        val names = getOrMutablePresetNames()
        names.remove(name)
        prefs.edit()
            .putStringSet(KEY_PRESET_NAMES, names)
            .remove(presetKey(name))
            .apply()
    }

    // ─── Fruit filter toggles ─────────────────────────────────────────────────

    fun saveFruitFilter1Enabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FRUIT_FILTER_1, enabled).apply()
    }

    fun loadFruitFilter1Enabled(): Boolean =
        prefs.getBoolean(KEY_FRUIT_FILTER_1, false)

    fun saveFruitFilter2Enabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FRUIT_FILTER_2, enabled).apply()
    }

    fun loadFruitFilter2Enabled(): Boolean =
        prefs.getBoolean(KEY_FRUIT_FILTER_2, false)

    // ─── Preferred capture aspect ratio ──────────────────────────────────────

    fun savePreferredCaptureRatio(option: AspectRatioOption) {
        prefs.edit().putString(KEY_CAPTURE_RATIO, option.name).apply()
    }

    fun loadPreferredCaptureRatio(): AspectRatioOption =
        prefs.getString(KEY_CAPTURE_RATIO, null)
            ?.let { runCatching { AspectRatioOption.valueOf(it) }.getOrNull() }
            ?: AspectRatioOption.PORTRAIT_9_16

    // ─── Draft project ────────────────────────────────────────────────────────

    fun saveDraft(videoUriString: String, fields: List<OverlayTextField>, aspectRatio: AspectRatioOption, fadeSecs: Int, fruitFilter1Enabled: Boolean = false, fruitFilter2Enabled: Boolean = false) {
        val draft = DraftProject(videoUriString, fields, aspectRatio.name, fadeSecs, fruitFilter1Enabled, fruitFilter2Enabled)
        prefs.edit()
            .putString(KEY_DRAFT, Json.encodeToString(draft))
            .apply()
    }

    fun loadDraft(): DraftProject? =
        prefs.getString(KEY_DRAFT, null)?.let { json ->
            runCatching { Json.decodeFromString<DraftProject>(json) }.getOrNull()
        }

    fun hasDraft(): Boolean = prefs.contains(KEY_DRAFT)

    fun clearDraft() {
        prefs.edit().remove(KEY_DRAFT).apply()
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun getOrMutablePresetNames(): MutableSet<String> =
        prefs.getStringSet(KEY_PRESET_NAMES, emptySet())?.toMutableSet() ?: mutableSetOf()

    private fun presetKey(name: String) = "preset_$name"

    companion object {
        private const val KEY_LAST_FIELDS = "last_fields"
        private const val KEY_LAST_RATIO = "last_ratio"
        private const val KEY_FADE_DURATION = "fade_duration"
        private const val KEY_PRESET_NAMES = "preset_names"
        private const val KEY_CAPTURE_RATIO = "capture_ratio"
        private const val KEY_DRAFT = "draft_project"
        private const val KEY_FRUIT_FILTER_1 = "fruit_filter_1"
        private const val KEY_FRUIT_FILTER_2 = "fruit_filter_2"
        const val DEFAULT_FADE_SECS = 3
    }
}

data class UserPreset(val name: String, val fields: List<OverlayTextField>)

@Serializable
data class DraftProject(
    val videoUriString: String,
    val fields: List<OverlayTextField>,
    val aspectRatioName: String,
    val fadeSecs: Int,
    val fruitFilter1Enabled: Boolean = false,
    val fruitFilter2Enabled: Boolean = false,
) {
    fun aspectRatioOption(): AspectRatioOption =
        runCatching { AspectRatioOption.valueOf(aspectRatioName) }.getOrDefault(AspectRatioOption.ORIGINAL)
}
