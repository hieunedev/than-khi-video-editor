package com.thankhi.videoeditor

import android.net.Uri

data class Clip(val id: Long, val uri: Uri, val name: String, val durationMs: Long = 0, val startMs: Long = 0, val endMs: Long? = null)
data class Subtitle(val id: Long, val startMs: Long, val endMs: Long, val text: String)
data class ProjectState(val clips: List<Clip> = emptyList(), val subtitles: List<Subtitle> = emptyList())

data class PipelineJob(
    val id: String,
    val stage: String,
    val percent: Int,
    val message: String,
    val output: String? = null
)
