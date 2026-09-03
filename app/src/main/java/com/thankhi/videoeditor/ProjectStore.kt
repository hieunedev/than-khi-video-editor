package com.thankhi.videoeditor

import android.content.Context

class ProjectStore(context: Context) {
    private val prefs = context.getSharedPreferences("than_khi_project", Context.MODE_PRIVATE)
    fun saveClips(clips: List<Clip>) {
        val encoded = clips.joinToString("\\n") { listOf(it.id, it.uri.toString(), it.name, it.durationMs, it.startMs, it.endMs ?: -1).joinToString("|") }
        prefs.edit().putString("clips", encoded).apply()
    }
}
