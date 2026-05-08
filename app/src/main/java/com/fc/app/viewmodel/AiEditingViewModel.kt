package com.fc.app.viewmodel

import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.fc.app.data.AiPreferences
import com.fc.app.data.model.AiPreset
import com.fc.app.data.model.AiProvider
import com.fc.app.data.model.AiWorkflow
import com.fc.app.data.model.CoverText
import com.fc.app.data.model.SubtitleLang
import com.fc.app.data.model.SubtitleSegment
import com.fc.app.service.AiEditingPipeline
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

// ─── UI 状态 ──────────────────────────────────────────────────────────────────

enum class AiPipelineStage {
    IDLE, EXTRACTING_AUDIO, TRANSCRIBING, REFINING, COMPOSITING, DONE, ERROR
}

data class AiPipelineState(
    val stage: AiPipelineStage = AiPipelineStage.IDLE,
    val stageLabel: String = "",
    val progress: Float = 0f,
    val errorMessage: String = "",
)

data class AiSettingsState(
    val provider: AiProvider = AiProvider.QWEN,
    val customBaseUrl: String = "",
    val defaultModel: String = "",
    val apiKey: String = "",
    val maxTokenBudget: Int = AiPreferences.DEFAULT_TOKEN_BUDGET,
    val subtitleLang: SubtitleLang = SubtitleLang.ZH,
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

@OptIn(UnstableApi::class)
class AiEditingViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val TAG = "AiEditingViewModel"
    }

    private val aiPrefs = AiPreferences(application)
    private val pipeline = AiEditingPipeline(application)

    // AI 预设列表
    private val _aiPresetsFlow = MutableStateFlow<List<AiPreset>>(emptyList())
    val aiPresetsFlow: StateFlow<List<AiPreset>> = _aiPresetsFlow.asStateFlow()

    // 流水线状态
    private val _pipelineState = MutableStateFlow(AiPipelineState())
    val pipelineState: StateFlow<AiPipelineState> = _pipelineState.asStateFlow()

    // 字幕片段（可编辑）
    private val _subtitleSegments = MutableStateFlow<List<SubtitleSegment>>(emptyList())
    val subtitleSegments: StateFlow<List<SubtitleSegment>> = _subtitleSegments.asStateFlow()

    // 封面文案（可编辑）
    private val _coverText = MutableStateFlow(CoverText())
    val coverText: StateFlow<CoverText> = _coverText.asStateFlow()

    // 当前正在编辑的 AI 预设（新建或编辑时使用）
    private val _editingPreset = MutableStateFlow<AiPreset?>(null)
    val editingPreset: StateFlow<AiPreset?> = _editingPreset.asStateFlow()

    // 合成完成后的输出文件
    private val _outputFile = MutableStateFlow<File?>(null)
    val outputFile: StateFlow<File?> = _outputFile.asStateFlow()

    // 当前选中的视频 Uri（用于开始流水线）
    private val _pendingVideoUri = MutableStateFlow<Uri?>(null)
    val pendingVideoUri: StateFlow<Uri?> = _pendingVideoUri.asStateFlow()

    // AI 设置状态
    private val _settingsState = MutableStateFlow(AiSettingsState())
    val settingsState: StateFlow<AiSettingsState> = _settingsState.asStateFlow()

    // 已保存到 MediaStore 的最终 Uri
    private val _savedUri = MutableStateFlow<Uri?>(null)
    val savedUri: StateFlow<Uri?> = _savedUri.asStateFlow()

    private var pipelineJob: Job? = null

    init {
        refreshAiPresets()
        loadSettings()
    }

    // ─── 预设管理 ──────────────────────────────────────────────────────────────

    fun refreshAiPresets() {
        _aiPresetsFlow.value = aiPrefs.loadAiPresets()
    }

    fun startNewPreset() {
        _editingPreset.value = AiPreset(
            id = UUID.randomUUID().toString(),
            name = "新预设",
            workflow = AiWorkflow.COVER_AND_SUBTITLE,
        )
    }

    fun startEditPreset(preset: AiPreset) {
        _editingPreset.value = preset.copy()
    }

    fun updateEditingPreset(preset: AiPreset) {
        _editingPreset.value = preset
    }

    fun saveEditingPreset() {
        val preset = _editingPreset.value ?: return
        aiPrefs.saveAiPreset(preset)
        refreshAiPresets()
        _editingPreset.value = null
    }

    fun cancelPresetEdit() {
        _editingPreset.value = null
    }

    fun deleteAiPreset(id: String) {
        aiPrefs.deleteAiPreset(id)
        refreshAiPresets()
    }

    // ─── 视频 Uri 设置 ─────────────────────────────────────────────────────────

    fun setPendingVideoUri(uri: Uri) {
        _pendingVideoUri.value = uri
    }

    // ─── 流水线控制 ───────────────────────────────────────────────────────────

    fun startPipeline(videoUri: Uri, presetId: String) {
        if (_pipelineState.value.stage != AiPipelineStage.IDLE &&
            _pipelineState.value.stage != AiPipelineStage.ERROR &&
            _pipelineState.value.stage != AiPipelineStage.DONE
        ) {
            Log.w(TAG, "Pipeline already running, ignoring start request")
            return
        }

        val preset = _aiPresetsFlow.value.firstOrNull { it.id == presetId } ?: run {
            _pipelineState.update { it.copy(stage = AiPipelineStage.ERROR, errorMessage = "预设不存在") }
            return
        }

        val settings = _settingsState.value
        val apiKey = aiPrefs.loadApiKey()
        val effectiveBaseUrl = resolveBaseUrl(preset, settings)
        val effectiveModel = preset.modelId.ifBlank { settings.defaultModel }
        val tokenBudget = if (preset.maxTokenBudget > 0) preset.maxTokenBudget else settings.maxTokenBudget

        val adjustedPreset = preset.copy(modelId = effectiveModel)

        _pipelineState.update { AiPipelineState(stage = AiPipelineStage.EXTRACTING_AUDIO, stageLabel = "准备中", progress = 0f) }
        _subtitleSegments.value = emptyList()
        _coverText.value = CoverText()
        _outputFile.value = null
        _savedUri.value = null

        pipelineJob = viewModelScope.launch {
            try {
                val result = pipeline.run(
                    videoUri = videoUri,
                    preset = adjustedPreset,
                    apiKey = apiKey,
                    effectiveBaseUrl = effectiveBaseUrl,
                    maxTokenBudget = tokenBudget,
                ) { stageName, progress ->
                    val stage = when {
                        progress < 0.2f -> AiPipelineStage.EXTRACTING_AUDIO
                        progress < 0.5f -> AiPipelineStage.TRANSCRIBING
                        progress < 0.7f -> AiPipelineStage.REFINING
                        progress < 1.0f -> AiPipelineStage.COMPOSITING
                        else -> AiPipelineStage.DONE
                    }
                    _pipelineState.update {
                        it.copy(stage = stage, stageLabel = stageName, progress = progress)
                    }
                }
                _subtitleSegments.value = result.subtitles
                _coverText.value = result.coverText
                _outputFile.value = result.outputFile
                _pipelineState.update {
                    it.copy(stage = AiPipelineStage.DONE, stageLabel = "完成", progress = 1f)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Pipeline error", e)
                _pipelineState.update {
                    it.copy(
                        stage = AiPipelineStage.ERROR,
                        stageLabel = "出错",
                        errorMessage = e.localizedMessage ?: "未知错误",
                    )
                }
            }
        }
    }

    fun cancelPipeline() {
        pipelineJob?.cancel()
        _pipelineState.update { AiPipelineState(stage = AiPipelineStage.IDLE, stageLabel = "已取消") }
    }

    fun resetPipeline() {
        pipelineJob?.cancel()
        _pipelineState.value = AiPipelineState()
        _subtitleSegments.value = emptyList()
        _coverText.value = CoverText()
        _outputFile.value = null
        _savedUri.value = null
    }

    // ─── 字幕 / 封面编辑 ──────────────────────────────────────────────────────

    fun updateSubtitleText(id: String, text: String) {
        _subtitleSegments.update { list ->
            list.map { if (it.id == id) it.copy(text = text) else it }
        }
    }

    fun updateCoverTitle(title: String) {
        _coverText.update { it.copy(title = title) }
    }

    fun updateCoverSubtitle(subtitle: String) {
        _coverText.update { it.copy(subtitle = subtitle) }
    }

    // ─── 导出最终视频到 MediaStore ────────────────────────────────────────────

    fun exportFinal() {
        val file = _outputFile.value ?: return
        viewModelScope.launch {
            try {
                val uri = saveToMediaStore(file)
                _savedUri.value = uri
            } catch (e: Exception) {
                Log.e(TAG, "Export to gallery failed", e)
            }
        }
    }

    private fun saveToMediaStore(file: File): Uri? {
        val context = getApplication<Application>()
        val fileName = "AI_${System.currentTimeMillis()}.mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/FcAi")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val itemUri = resolver.insert(collection, contentValues) ?: return null
        resolver.openOutputStream(itemUri)?.use { out ->
            file.inputStream().use { it.copyTo(out) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(itemUri, contentValues, null, null)
        }
        return itemUri
    }

    // ─── AI 设置 ──────────────────────────────────────────────────────────────

    private fun loadSettings() {
        _settingsState.value = AiSettingsState(
            provider = aiPrefs.loadApiProvider(),
            customBaseUrl = aiPrefs.loadCustomBaseUrl(),
            defaultModel = aiPrefs.loadDefaultModel(),
            apiKey = aiPrefs.loadApiKey(),
            maxTokenBudget = aiPrefs.loadMaxTokenBudget(),
            subtitleLang = aiPrefs.loadSubtitleLang(),
        )
    }

    fun saveSettings(state: AiSettingsState) {
        aiPrefs.saveApiProvider(state.provider)
        aiPrefs.saveCustomBaseUrl(state.customBaseUrl)
        aiPrefs.saveDefaultModel(state.defaultModel)
        aiPrefs.saveApiKey(state.apiKey)
        aiPrefs.saveMaxTokenBudget(state.maxTokenBudget)
        aiPrefs.saveSubtitleLang(state.subtitleLang)
        _settingsState.value = state
    }

    // ─── 辅助 ─────────────────────────────────────────────────────────────────

    private fun resolveBaseUrl(preset: AiPreset, settings: AiSettingsState): String {
        // 预设可以覆盖服务商；若服务商是 CUSTOM，用全局 customBaseUrl
        val provider = preset.apiProvider
        return when {
            provider == AiProvider.CUSTOM -> settings.customBaseUrl.ifBlank { settings.provider.defaultBaseUrl }
            else -> provider.defaultBaseUrl
        }
    }
}
