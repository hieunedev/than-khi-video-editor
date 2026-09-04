package com.thankhi.videoeditor

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@UnstableApi
object VideoEngine {
    fun mediaItem(uri: Uri, startMs: Long = 0L, endMs: Long? = null): MediaItem {
        val clipping = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(startMs.coerceAtLeast(0L))
            .apply { if (endMs != null && endMs > startMs) setEndPositionMs(endMs) }
            .build()
        return MediaItem.Builder().setUri(uri).setClippingConfiguration(clipping).build()
    }

    suspend fun render(context: Context, clips: List<Clip>, output: File) {
        require(clips.isNotEmpty())
        output.parentFile?.mkdirs()
        val editedItems = clips.map { EditedMediaItem.Builder(mediaItem(it.uri, it.startMs, it.endMs)).build() }
        val videoSequence = EditedMediaItemSequence.withAudioAndVideoFrom(editedItems)
        val composition = Composition.Builder(videoSequence).build()

        suspendCancellableCoroutine<Unit> { continuation ->
            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, result: ExportResult) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                    override fun onError(composition: Composition, result: ExportResult, exception: ExportException) {
                        if (continuation.isActive) continuation.resumeWithException(exception)
                    }
                    override fun onFallbackApplied(composition: Composition, originalTransformationRequest: androidx.media3.transformer.TransformationRequest, fallbackTransformationRequest: androidx.media3.transformer.TransformationRequest) = Unit
                })
                .build()
            continuation.invokeOnCancellation { transformer.cancel() }
            transformer.start(composition, output.absolutePath)
        }
    }
}
