package com.fc.app.data

import android.content.Context
import com.fc.app.data.model.OverlayTextField
import com.fc.app.util.AspectRatioOption
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ExportedPreset(val name: String, val fields: List<OverlayTextField>)

@Serializable
data class ExportedConfig(
    val presets: List<ExportedPreset> = emptyList(),
    val lastFields: List<OverlayTextField>? = null,
    val lastAspectRatio: String? = null,
    val fadeDurationSecs: Int = UserPreferences.DEFAULT_FADE_SECS,
    val fruitFilter1Enabled: Boolean = false,
    val fruitFilter2Enabled: Boolean = false,
    val captureAspectRatio: String? = null,
)

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

    // ─── Config export / import ───────────────────────────────────────────────

    /** Serialises all user preferences and named presets to a JSON string. */
    fun exportAllConfig(): String {
        val config = ExportedConfig(
            presets = loadPresets().map { ExportedPreset(it.name, it.fields) },
            lastFields = loadLastFields(),
            lastAspectRatio = prefs.getString(KEY_LAST_RATIO, null),
            fadeDurationSecs = loadFadeDurationSecs(),
            fruitFilter1Enabled = loadFruitFilter1Enabled(),
            fruitFilter2Enabled = loadFruitFilter2Enabled(),
            captureAspectRatio = prefs.getString(KEY_CAPTURE_RATIO, null),
        )
        return Json.encodeToString(config)
    }

    /**
     * Parses a JSON string produced by [exportAllConfig] and writes the
     * contained data into SharedPreferences.  Existing entries are replaced;
     * entries absent from the config are left unchanged.
     *
     * @return `true` on success, `false` if the JSON is invalid.
     */
    fun importAllConfig(json: String): Boolean {
        val config = runCatching { Json.decodeFromString<ExportedConfig>(json) }.getOrNull()
            ?: return false
        prefs.edit().also { editor ->
            // Presets – replace name-set and individual entries
            val names = config.presets.map { it.name }.toMutableSet()
            editor.putStringSet(KEY_PRESET_NAMES, names)
            config.presets.forEach { preset ->
                editor.putString(presetKey(preset.name), Json.encodeToString(preset.fields))
            }
            // Last-used settings
            config.lastFields?.let {
                editor.putString(KEY_LAST_FIELDS, Json.encodeToString(it))
            }
            config.lastAspectRatio?.let { editor.putString(KEY_LAST_RATIO, it) }
            editor.putInt(KEY_FADE_DURATION, config.fadeDurationSecs)
            editor.putBoolean(KEY_FRUIT_FILTER_1, config.fruitFilter1Enabled)
            editor.putBoolean(KEY_FRUIT_FILTER_2, config.fruitFilter2Enabled)
            config.captureAspectRatio?.let { editor.putString(KEY_CAPTURE_RATIO, it) }
        }.apply()
        return true
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
