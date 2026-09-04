package com.thankhi.videoeditor

import android.content.Context
import android.net.Uri
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import org.json.JSONArray
import java.io.File
import java.util.concurrent.TimeUnit

object ProcessingQueue {
    const val KEY_PROJECT = "project"

    fun enqueueProject(context: Context, clips: List<Clip>) = run {
        require(clips.isNotEmpty())
        val array = JSONArray()
        clips.forEach { clip ->
            array.put(JSONArray().apply {
                put(clip.uri.toString())
                put(clip.name)
                put(clip.durationMs)
                put(clip.startMs)
                put(clip.endMs ?: -1L)
            })
        }
        val request = OneTimeWorkRequestBuilder<RenderWorker>()
            .setInputData(workDataOf(KEY_PROJECT to array.toString()))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueue(request)
        request.id
    }
}

class RenderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val raw = inputData.getString(ProcessingQueue.KEY_PROJECT) ?: return Result.failure()
        val array = JSONArray(raw)
        val clips = buildList {
            for (i in 0 until array.length()) {
                val row = array.getJSONArray(i)
                add(Clip(id = i.toLong(), uri = Uri.parse(row.getString(0)), name = row.getString(1), durationMs = row.getLong(2), startMs = row.getLong(3), endMs = row.getLong(4).takeIf { it >= 0L }))
            }
        }
        if (clips.isEmpty()) return Result.failure()
        val outputDir = applicationContext.getExternalFilesDir("exports") ?: return Result.failure()
        val output = File(outputDir, "ThanKhi_${System.currentTimeMillis()}.mp4")
        setProgress(workDataOf("stage" to "Đang render", "percent" to 5))
        return try {
            VideoEngine.render(applicationContext, clips, output)
            setProgress(workDataOf("stage" to "Hoàn tất", "percent" to 100))
            Result.success(workDataOf("output" to output.absolutePath, "percent" to 100))
        } catch (error: Exception) {
            Result.failure(workDataOf("error" to (error.message ?: "Render failed")))
        }
    }
}

object ChunkPlanner {
    fun plan(durationMs: Long, chunkMs: Long = 5 * 60_000L): List<LongRange> {
        if (durationMs <= 0L || chunkMs <= 0L) return emptyList()
        val result = mutableListOf<LongRange>()
        var start = 0L
        while (start < durationMs) {
            val endExclusive = minOf(durationMs, start + chunkMs)
            result += start until endExclusive
            start = endExclusive
        }
        return result
    }
}
