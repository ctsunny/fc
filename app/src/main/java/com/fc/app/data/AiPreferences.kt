package com.fc.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.fc.app.data.model.AiPreset
import com.fc.app.data.model.AiProvider
import com.fc.app.data.model.SubtitleLang
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * AI 编辑模块的全局设置 + 预设列表持久化。
 *
 * 普通设置（Base URL、默认服务商、模型、Token 预算、字幕语言）存入普通 SharedPreferences。
 * API Key 存入 EncryptedSharedPreferences（Android Keystore 加密），且**不**序列化到导出 JSON。
 */
class AiPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 加密存储 API Key；若 Keystore 不可用则回退到内存变量。 */
    private val encryptedPrefs: SharedPreferences? = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }.getOrElse { e ->
        Log.w(TAG, "EncryptedSharedPreferences unavailable, API key won't be persisted: $e")
        null
    }

    // ─── API Key（加密） ───────────────────────────────────────────────────────

    fun saveApiKey(key: String) {
        encryptedPrefs?.edit()?.putString(KEY_API_KEY, key)?.apply()
    }

    fun loadApiKey(): String =
        encryptedPrefs?.getString(KEY_API_KEY, "") ?: ""

    // ─── 全局设置 ──────────────────────────────────────────────────────────────

    fun saveApiProvider(provider: AiProvider) {
        prefs.edit().putString(KEY_PROVIDER, provider.name).apply()
    }

    fun loadApiProvider(): AiProvider =
        prefs.getString(KEY_PROVIDER, null)
            ?.let { runCatching { AiProvider.valueOf(it) }.getOrNull() }
            ?: AiProvider.QWEN

    fun saveCustomBaseUrl(url: String) {
        prefs.edit().putString(KEY_BASE_URL, url).apply()
    }

    fun loadCustomBaseUrl(): String =
        prefs.getString(KEY_BASE_URL, "") ?: ""

    fun saveDefaultModel(model: String) {
        prefs.edit().putString(KEY_MODEL, model).apply()
    }

    fun loadDefaultModel(): String {
        val saved = prefs.getString(KEY_MODEL, null)
        if (!saved.isNullOrBlank()) return saved
        return loadApiProvider().defaultModel
    }

    fun saveMaxTokenBudget(tokens: Int) {
        prefs.edit().putInt(KEY_TOKEN_BUDGET, tokens).apply()
    }

    fun loadMaxTokenBudget(): Int =
        prefs.getInt(KEY_TOKEN_BUDGET, DEFAULT_TOKEN_BUDGET)

    fun saveSubtitleLang(lang: SubtitleLang) {
        prefs.edit().putString(KEY_SUBTITLE_LANG, lang.name).apply()
    }

    fun loadSubtitleLang(): SubtitleLang =
        prefs.getString(KEY_SUBTITLE_LANG, null)
            ?.let { runCatching { SubtitleLang.valueOf(it) }.getOrNull() }
            ?: SubtitleLang.ZH

    // ─── 并发限制（预留，暂未暴露到 UI） ────────────────────────────────────────

    fun saveMaxConcurrentCalls(n: Int) {
        prefs.edit().putInt(KEY_CONCURRENT_CALLS, n.coerceIn(1, 4)).apply()
    }

    fun loadMaxConcurrentCalls(): Int =
        prefs.getInt(KEY_CONCURRENT_CALLS, 1).coerceIn(1, 4)

    // ─── AI 预设列表 ────────────────────────────────────────────────────────────

    fun loadAiPresets(): List<AiPreset> {
        val names = prefs.getStringSet(KEY_AI_PRESET_IDS, emptySet()) ?: emptySet()
        return names.mapNotNull { id ->
            val json = prefs.getString(aiPresetKey(id), null) ?: return@mapNotNull null
            runCatching { Json.decodeFromString<AiPreset>(json) }.getOrNull()
        }.sortedBy { it.name }
    }

    fun saveAiPreset(preset: AiPreset) {
        val ids = getOrMutablePresetIds()
        ids.add(preset.id)
        prefs.edit()
            .putStringSet(KEY_AI_PRESET_IDS, ids)
            .putString(aiPresetKey(preset.id), Json.encodeToString(preset))
            .apply()
    }

    fun deleteAiPreset(id: String) {
        val ids = getOrMutablePresetIds()
        ids.remove(id)
        prefs.edit()
            .putStringSet(KEY_AI_PRESET_IDS, ids)
            .remove(aiPresetKey(id))
            .apply()
    }

    // ─── 辅助 ─────────────────────────────────────────────────────────────────

    private fun getOrMutablePresetIds(): MutableSet<String> =
        prefs.getStringSet(KEY_AI_PRESET_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()

    private fun aiPresetKey(id: String) = "ai_preset_$id"

    companion object {
        private const val TAG = "AiPreferences"
        private const val PREFS_NAME = "ai_prefs"
        private const val ENCRYPTED_PREFS_NAME = "ai_encrypted_prefs"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_PROVIDER = "api_provider"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_MODEL = "default_model"
        private const val KEY_TOKEN_BUDGET = "max_token_budget"
        private const val KEY_SUBTITLE_LANG = "subtitle_lang"
        private const val KEY_CONCURRENT_CALLS = "concurrent_calls"
        private const val KEY_AI_PRESET_IDS = "ai_preset_ids"
        const val DEFAULT_TOKEN_BUDGET = 2000
    }
}
