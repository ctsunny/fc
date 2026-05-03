package com.fc.app.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File

private const val SAVE_TAG = "MediaStoreSave"

/**
 * Copies [file] to the public Movies/果旺角视频 folder via MediaStore.
 * Returns the URI of the saved file, or null if the save failed.
 */
fun saveVideoFileToMediaStore(context: Context, file: File, subFolder: String = "果旺角视频"): Uri? {
    val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$subFolder")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
    }
    val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        ?: return null
    return try {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            file.inputStream().use { it.copyTo(out) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        }
        uri
    } catch (e: Exception) {
        Log.w(SAVE_TAG, "Failed to save video to MediaStore", e)
        context.contentResolver.delete(uri, null, null)
        null
    }
}
