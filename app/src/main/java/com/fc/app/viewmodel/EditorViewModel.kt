package com.fc.app.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fc.app.data.UserPreferences
import com.fc.app.data.UserPreset
import com.fc.app.util.VideoExporter
import com.fc.app.util.AspectRatioOption
import com.fc.app.data.PresetTemplates
import com.fc.app.data.model.FontFamilyOption
import com.fc.app.data.model.OverlayTextField
import com.fc.app.data.model.StyleTemplate
import com.fc.app.data.model.TemplateCategory
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.ExportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class EditorUiState(
    val videoUri: Uri? = null,
    val fields: List<OverlayTextField> = emptyList(),
    val selectedFieldId: String? = null,
    val selectedTemplate: StyleTemplate? = null,
    val aspectRatioOption: AspectRatioOption = AspectRatioOption.ORIGINAL,
    val fadeDurationSecs: Int = UserPreferences.DEFAULT_FADE_SECS,
    val previewCanvasWidth: Int = 0,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val exportMessage: String = "",
    val exportedFileUri: Uri? = null,
)

/** Separate UI-state for the preset-editing screen (no video). */
data class PresetEditUiState(
    val presetName: String = "",
    val fields: List<OverlayTextField> = emptyList(),
    val selectedFieldId: String? = null,
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private companion object {
        private const val TAG = "EditorViewModel"
    }

    private val userPrefs = UserPreferences(application)

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _presetEditState = MutableStateFlow(PresetEditUiState())
    val presetEditState: StateFlow<PresetEditUiState> = _presetEditState.asStateFlow()

    fun setVideoUri(uri: Uri) {
        val lastFields = userPrefs.loadLastFields()
        val lastRatio = userPrefs.loadLastAspectRatio()
        val lastFade = userPrefs.loadFadeDurationSecs()
        _uiState.update {
            it.copy(
                videoUri = uri,
                fields = (lastFields ?: PresetTemplates.promotionFields).map { field -> field.copy() },
                selectedFieldId = null,
                selectedTemplate = null,
                aspectRatioOption = lastRatio,
                fadeDurationSecs = lastFade,
                isExporting = false,
                exportProgress = 0f,
                exportMessage = "",
                exportedFileUri = null,
            )
        }
    }

    fun applyTemplate(template: StyleTemplate) {
        _uiState.update {
            it.copy(
                fields = template.fields.map { field -> field.copy() },
                selectedFieldId = null,
                selectedTemplate = template,
                exportProgress = 0f,
                exportMessage = "",
                exportedFileUri = null
            )
        }
    }

    fun applyPresetByCategory(category: TemplateCategory) {
        val template = PresetTemplates.all.firstOrNull { it.category == category } ?: return
        applyTemplate(template)
    }

    fun applyUserPreset(preset: UserPreset) {
        _uiState.update {
            it.copy(
                fields = preset.fields.map { field -> field.copy() },
                selectedFieldId = null,
                selectedTemplate = null,
                exportProgress = 0f,
                exportMessage = "",
                exportedFileUri = null
            )
        }
    }

    fun loadUserPresets(): List<UserPreset> = userPrefs.loadPresets()

    fun saveCurrentAsPreset(name: String) {
        userPrefs.savePreset(name, _uiState.value.fields)
    }

    fun deleteUserPreset(name: String) {
        userPrefs.deletePreset(name)
    }

    fun updatePreviewCanvasSize(width: Int, height: Int) {
        if (width > 0) {
            _uiState.update { it.copy(previewCanvasWidth = width) }
        }
    }

    fun saveDraft() {
        val state = _uiState.value
        val uri = state.videoUri ?: return
        userPrefs.saveDraft(
            videoUriString = uri.toString(),
            fields = state.fields,
            aspectRatio = state.aspectRatioOption,
            fadeSecs = state.fadeDurationSecs,
        )
    }

    fun hasDraft(): Boolean = userPrefs.hasDraft()

    fun restoreDraft(): Boolean {
        val draft = userPrefs.loadDraft() ?: return false
        _uiState.update {
            it.copy(
                videoUri = Uri.parse(draft.videoUriString),
                fields = draft.fields.map { f -> f.copy() },
                selectedFieldId = null,
                selectedTemplate = null,
                aspectRatioOption = draft.aspectRatioOption(),
                fadeDurationSecs = draft.fadeSecs,
                isExporting = false,
                exportProgress = 0f,
                exportMessage = "",
                exportedFileUri = null,
            )
        }
        return true
    }

    fun clearDraft() {
        userPrefs.clearDraft()
    }

    fun savePreferredCaptureRatio(option: AspectRatioOption) {
        userPrefs.savePreferredCaptureRatio(option)
    }

    fun loadPreferredCaptureRatio(): AspectRatioOption =
        userPrefs.loadPreferredCaptureRatio()

    fun setVideoUriWithRatio(uri: Uri, preferredRatio: AspectRatioOption) {
        val lastFields = userPrefs.loadLastFields()
        val lastFade = userPrefs.loadFadeDurationSecs()
        _uiState.update {
            it.copy(
                videoUri = uri,
                fields = (lastFields ?: PresetTemplates.promotionFields).map { field -> field.copy() },
                selectedFieldId = null,
                selectedTemplate = null,
                aspectRatioOption = preferredRatio,
                fadeDurationSecs = lastFade,
                isExporting = false,
                exportProgress = 0f,
                exportMessage = "",
                exportedFileUri = null,
            )
        }
    }


    fun selectField(fieldId: String?) {
        _uiState.update { it.copy(selectedFieldId = fieldId) }
    }

    fun updateAspectRatioOption(option: AspectRatioOption) {
        _uiState.update {
            it.copy(
                aspectRatioOption = option,
                exportMessage = "",
                exportedFileUri = null
            )
        }
    }

    fun updateFadeDurationSecs(secs: Int) {
        _uiState.update { it.copy(fadeDurationSecs = secs.coerceIn(0, 10)) }
    }

    fun updateFieldText(fieldId: String, text: String) {
        _uiState.update { s -> s.copy(fields = s.fields.map { if (it.id == fieldId) it.copy(text = text) else it }) }
    }

    fun updateFieldPosition(fieldId: String, xFraction: Float, yFraction: Float) {
        _uiState.update { s ->
            s.copy(fields = s.fields.map {
                if (it.id == fieldId) it.copy(
                    xFraction = xFraction.coerceIn(0f, 1f),
                    yFraction = yFraction.coerceIn(0f, 1f)
                ) else it
            })
        }
    }

    fun updateFieldFontSize(fieldId: String, size: Float) {
        _uiState.update { s -> s.copy(fields = s.fields.map { if (it.id == fieldId) it.copy(fontSize = size.coerceIn(10f, 80f)) else it }) }
    }

    fun updateFieldColor(fieldId: String, hex: String) {
        _uiState.update { s -> s.copy(fields = s.fields.map { if (it.id == fieldId) it.copy(colorHex = hex) else it }) }
    }

    fun updateFieldBold(fieldId: String, bold: Boolean) {
        _uiState.update { s -> s.copy(fields = s.fields.map { if (it.id == fieldId) it.copy(isBold = bold) else it }) }
    }

    fun updateFieldVisibility(fieldId: String, visible: Boolean) {
        _uiState.update { s -> s.copy(fields = s.fields.map { if (it.id == fieldId) it.copy(isVisible = visible) else it }) }
    }

    fun updateFieldFontFamily(fieldId: String, fontFamily: FontFamilyOption) {
        _uiState.update { s -> s.copy(fields = s.fields.map { if (it.id == fieldId) it.copy(fontFamily = fontFamily) else it }) }
    }

    fun clearFields() {
        _uiState.update {
            it.copy(
                fields = emptyList(),
                selectedFieldId = null,
                selectedTemplate = null,
                isExporting = false,
                exportProgress = 0f,
                exportMessage = "",
                exportedFileUri = null
            )
        }
    }

    fun clearProject() {
        _uiState.value = EditorUiState()
    }

    // ─── Preset editing ───────────────────────────────────────────────────────

    fun startPresetEdit(preset: UserPreset) {
        _presetEditState.value = PresetEditUiState(
            presetName = preset.name,
            fields = preset.fields.map { it.copy() },
        )
    }

    fun selectPresetEditField(fieldId: String?) {
        _presetEditState.update { it.copy(selectedFieldId = fieldId) }
    }

    fun updatePresetEditFieldText(fieldId: String, text: String) {
        _presetEditState.update { s -> s.copy(fields = s.fields.map { if (it.id == fieldId) it.copy(text = text) else it }) }
    }

    fun updatePresetEditFieldFontSize(fieldId: String, size: Float) {
        _presetEditState.update { s -> s.copy(fields = s.fields.map { if (it.id == fieldId) it.copy(fontSize = size.coerceIn(10f, 80f)) else it }) }
    }

    fun updatePresetEditFieldColor(fieldId: String, hex: String) {
        _presetEditState.update { s -> s.copy(fields = s.fields.map { if (it.id == fieldId) it.copy(colorHex = hex) else it }) }
    }

    fun updatePresetEditFieldBold(fieldId: String, bold: Boolean) {
        _presetEditState.update { s -> s.copy(fields = s.fields.map { if (it.id == fieldId) it.copy(isBold = bold) else it }) }
    }

    fun updatePresetEditFieldFontFamily(fieldId: String, fontFamily: FontFamilyOption) {
        _presetEditState.update { s -> s.copy(fields = s.fields.map { if (it.id == fieldId) it.copy(fontFamily = fontFamily) else it }) }
    }

    fun updatePresetEditFieldVisibility(fieldId: String, visible: Boolean) {
        _presetEditState.update { s -> s.copy(fields = s.fields.map { if (it.id == fieldId) it.copy(isVisible = visible) else it }) }
    }

    /** Persists the currently edited preset and clears the edit state. */
    fun savePresetEdit() {
        val state = _presetEditState.value
        if (state.presetName.isNotEmpty()) {
            userPrefs.savePreset(state.presetName, state.fields)
        }
        _presetEditState.value = PresetEditUiState()
    }

    fun cancelPresetEdit() {
        _presetEditState.value = PresetEditUiState()
    }

    // ─── Export ───────────────────────────────────────────────────────────────

    @OptIn(UnstableApi::class)
    fun exportVideo() {
        val state = _uiState.value
        if (state.isExporting) return   // guard against concurrent exports
        val videoUri = state.videoUri ?: return
        if (state.fields.all { !it.isVisible || it.text.isBlank() }) {
            _uiState.update {
                it.copy(
                    isExporting = false,
                    exportProgress = 0f,
                    exportedFileUri = null,
                    exportMessage = "请至少保留一个可见文字后再导出"
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isExporting = true, exportProgress = 0f, exportMessage = "准备导出...") }
            var inputFile: File? = null
            var outputFile: File? = null
            try {
                val ctx = getApplication<Application>()
                _uiState.update { it.copy(exportProgress = 0.15f, exportMessage = "正在读取视频...") }
                inputFile = copyUriToCache(ctx, videoUri)
                outputFile = File(ctx.cacheDir, "output_${System.currentTimeMillis()}.mp4")

                _uiState.update { it.copy(exportProgress = 0.45f, exportMessage = "合成中，请稍候...") }

                VideoExporter(ctx).export(
                    inputFile = inputFile,
                    outputFile = outputFile,
                    overlays = state.fields,
                    aspectRatioOption = state.aspectRatioOption,
                    fadeDurationSecs = state.fadeDurationSecs,
                    previewCanvasWidth = state.previewCanvasWidth,
                )

                _uiState.update { it.copy(exportProgress = 0.85f, exportMessage = "正在保存到相册...") }
                val savedUri = saveToMediaStore(ctx, outputFile)
                outputFile.delete()
                outputFile = null

                // Auto-save all current settings as defaults for next time
                saveCurrentDefaults(state)

                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportProgress = 1f,
                        exportMessage = "导出成功！",
                        exportedFileUri = savedUri
                    )
                }
            } catch (e: ExportException) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportProgress = 0f,
                        exportedFileUri = null,
                        exportMessage = "导出失败：${e.message ?: "请稍后重试"}"
                    )
                }
                outputFile?.delete()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportProgress = 0f,
                        exportedFileUri = null,
                        exportMessage = "错误：${e.message ?: "请稍后重试"}"
                    )
                }
                outputFile?.delete()
            } finally {
                inputFile?.delete()
            }
        }
    }

    private fun saveCurrentDefaults(state: EditorUiState) {
        userPrefs.saveLastFields(state.fields)
        userPrefs.saveLastAspectRatio(state.aspectRatioOption)
        userPrefs.saveFadeDurationSecs(state.fadeDurationSecs)
    }

    private fun copyUriToCache(ctx: Context, uri: Uri): File {
        val file = File(ctx.cacheDir, "input_${System.currentTimeMillis()}.mp4")
        val inputStream = ctx.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("无法打开所选视频输入流")
        inputStream.use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        return file
    }

    private fun saveToMediaStore(ctx: Context, file: File): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/FCVideo")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val uri = ctx.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("无法创建系统相册文件")

        try {
            val outputStream = ctx.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("无法写入系统相册文件")
            outputStream.use { out ->
                file.inputStream().use { inp ->
                    inp.copyTo(out)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                ctx.contentResolver.update(uri, values, null, null)
            }
            return uri
        } catch (e: Exception) {
            Log.w(TAG, "Cleaning up failed MediaStore export entry: $uri", e)
            ctx.contentResolver.delete(uri, null, null)
            throw e
        }
    }
}

