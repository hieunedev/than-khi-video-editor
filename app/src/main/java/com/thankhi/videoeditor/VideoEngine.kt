package com.thankhi.videoeditor

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.Transformer.ExportException
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

@UnstableApi
object VideoEngine {
    fun mediaItem(uri: Uri, startMs: Long = 0, endMs: Long? = null): MediaItem {
        val clip = MediaItem.ClippingConfiguration.Builder().setStartPositionMs(startMs)
        if (endMs != null && endMs > startMs) clip.setEndPositionMs(endMs)
        return MediaItem.Builder().setUri(uri).setClippingConfiguration(clip.build()).build()
    }

    suspend fun render(context: Context, clips: List<Clip>, output: File, onProgress: (Int) -> Unit = {}) {
        require(clips.isNotEmpty())
        output.parentFile?.mkdirs()
        val edited = clips.map { EditedMediaItem.Builder(mediaItem(it.uri, it.startMs, it.endMs)).build() }
        val sequence = EditedMediaItemSequence.withAudioAndVideoFrom(edited)
        val composition = Composition.Builder(sequence).build()
        suspendCancellableCoroutine<Unit> { cont ->
            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        onProgress(100)
                        if (cont.isActive) cont.resume(Unit)
                    }
                    override fun onError(composition: Composition, exportResult: ExportResult?, exportException: ExportException) {
                        if (cont.isActive) cont.resumeWithException(exportException)
                    }
                    override fun onFallbackApplied(
                        composition: Composition,
                        originalTransformationRequest: androidx.media3.transformer.TransformationRequest,
                        fallbackTransformationRequest: androidx.media3.transformer.TransformationRequest
                    ) { }
                }).build()
            cont.invokeOnCancellation { transformer.cancel() }
            transformer.start(composition, output.absolutePath)
        }
    }
}
