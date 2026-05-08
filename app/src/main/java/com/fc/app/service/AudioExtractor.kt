package com.fc.app.service

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 从视频 Uri 提取音轨，输出为 AAC/MP4 文件，供 Whisper API 上传。
 *
 * 若视频没有音轨（或音轨提取失败），返回 null，调用方应跳过 ASR 阶段。
 */
object AudioExtractor {

    private const val TAG = "AudioExtractor"

    /**
     * @param context Android Context（用于打开 ContentResolver Uri）
     * @param videoUri 视频源 Uri
     * @param outputFile 输出的临时音频文件（调用方负责删除）
     * @return 音频文件（即 [outputFile]），若无音轨则返回 null
     */
    suspend fun extract(context: Context, videoUri: Uri, outputFile: File): File? =
        withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(context, videoUri, null)

                // 找到第一条音轨
                val audioTrackIndex = (0 until extractor.trackCount).firstOrNull { i ->
                    extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
                        ?.startsWith("audio/") == true
                }
                if (audioTrackIndex == null) {
                    Log.d(TAG, "No audio track found in $videoUri")
                    return@withContext null
                }

                val format = extractor.getTrackFormat(audioTrackIndex)
                extractor.selectTrack(audioTrackIndex)

                val muxer = MediaMuxer(
                    outputFile.absolutePath,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
                )
                val muxerTrackIndex = muxer.addTrack(format)
                muxer.start()

                val buffer = java.nio.ByteBuffer.allocate(256 * 1024)
                val bufferInfo = android.media.MediaCodec.BufferInfo()

                while (true) {
                    bufferInfo.offset = 0
                    bufferInfo.size = extractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) break
                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    bufferInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                    extractor.advance()
                }

                muxer.stop()
                muxer.release()
                outputFile
            } catch (e: Exception) {
                Log.e(TAG, "Audio extraction failed", e)
                outputFile.delete()
                null
            } finally {
                extractor.release()
            }
        }
}
