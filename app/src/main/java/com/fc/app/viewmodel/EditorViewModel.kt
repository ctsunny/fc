package com.fc.app.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fc.app.util.VideoExporter
import com.fc.app.data.PresetTemplates
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
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val exportMessage: String = "",
    val exportedFileUri: Uri? = null
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    fun setVideoUri(uri: Uri) {
        _uiState.update { it.copy(videoUri = uri) }
    }

    fun applyTemplate(template: StyleTemplate) {
        _uiState.update { it.copy(fields = template.fields.map { f -> f.copy() }, selectedTemplate = template) }
    }

    fun applyPresetByCategory(category: TemplateCategory) {
        applyTemplate(PresetTemplates.all.first { it.category == category })
    }

    fun selectField(fieldId: String?) {
        _uiState.update { it.copy(selectedFieldId = fieldId) }
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

    fun clearFields() {
        _uiState.update { it.copy(fields = emptyList(), selectedFieldId = null, exportedFileUri = null, exportMessage = "") }
    }

    @OptIn(UnstableApi::class)
    fun exportVideo() {
        val state = _uiState.value
        val videoUri = state.videoUri ?: return
        if (state.fields.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isExporting = true, exportProgress = 0f, exportMessage = "准备导出...") }
            var inputFile: File? = null
            var outputFile: File? = null
            try {
                val ctx = getApplication<Application>()
                inputFile = copyUriToCache(ctx, videoUri)
                outputFile = File(ctx.cacheDir, "output_${System.currentTimeMillis()}.mp4")

                _uiState.update { it.copy(exportMessage = "合成中，请稍候...") }

                VideoExporter(ctx).export(inputFile, outputFile, state.fields)

                val savedUri = saveToMediaStore(ctx, outputFile)
                _uiState.update {
                    it.copy(isExporting = false, exportProgress = 1f,
                        exportMessage = "导出成功！", exportedFileUri = savedUri)
                }
            } catch (e: ExportException) {
                _uiState.update { it.copy(isExporting = false, exportMessage = "导出失败：${e.message}") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, exportMessage = "错误：${e.message}") }
            } finally {
                inputFile?.delete()
            }
        }
    }

    private fun copyUriToCache(ctx: Context, uri: Uri): File {
        val file = File(ctx.cacheDir, "input_${System.currentTimeMillis()}.mp4")
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        return file
    }

    private fun saveToMediaStore(ctx: Context, file: File): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/FCVideo")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val uri = ctx.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            ctx.contentResolver.openOutputStream(it)?.use { out ->
                file.inputStream().use { inp -> inp.copyTo(out) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                ctx.contentResolver.update(it, values, null, null)
            }
        }
        file.delete()
        return uri
    }
}
