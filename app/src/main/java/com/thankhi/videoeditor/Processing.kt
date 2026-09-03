package com.thankhi.videoeditor

import android.content.Context
import android.net.Uri
import androidx.work.*
import java.io.File
import java.util.concurrent.TimeUnit
import org.json.JSONArray

object ProcessingQueue {
    const val KEY_PROJECT = "project"

    fun enqueueProject(context: Context, clips: List<Clip>): java.util.UUID {
        require(clips.isNotEmpty())
        val arr = JSONArray()
        clips.forEach { c ->
            arr.put(JSONArray().apply {
                put(c.uri.toString()); put(c.name); put(c.startMs); put(c.endMs ?: -1L)
            })
        }
        val data = workDataOf(KEY_PROJECT to arr.toString())
        val request = OneTimeWorkRequestBuilder<RenderWorker>()
            .setInputData(data)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueue(request)
        return request.id
    }
}

class RenderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val raw = inputData.getString(ProcessingQueue.KEY_PROJECT) ?: return Result.failure()
        val arr = JSONArray(raw)
        val clips = buildList {
            for (i in 0 until arr.length()) {
                val row = arr.getJSONArray(i)
                add(Clip(i.toLong(), Uri.parse(row.getString(0)), row.getString(1), 0L, row.getLong(2), row.getLong(3).takeIf { it >= 0 }))
            }
        }
        if (clips.isEmpty()) return Result.failure()
        val outDir = applicationContext.getExternalFilesDir("exports") ?: return Result.failure()
        val out = File(outDir, "ThanKhi_${System.currentTimeMillis()}.mp4")
        setProgress(workDataOf("stage" to "Đang render", "percent" to 5))
        return try {
            VideoEngine.render(applicationContext, clips, out) { p ->
                setProgress(workDataOf("stage" to "Đang render", "percent" to p.coerceIn(5, 100)))
            }
            Result.success(workDataOf("output" to out.absolutePath, "percent" to 100))
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to (e.message ?: "Render failed")))
        }
    }
}

object ChunkPlanner {
    fun plan(durationMs: Long, chunkMs: Long = 5 * 60_000L): List<LongRange> {
        if (durationMs <= 0 || chunkMs <= 0) return emptyList()
        val out = mutableListOf<LongRange>()
        var s = 0L
        while (s < durationMs) {
            val e = minOf(durationMs, s + chunkMs)
            out += s until e
            s = e
        }
        return out
    }
}
