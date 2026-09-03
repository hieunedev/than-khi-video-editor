package com.thankhi.videoeditor

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

object Srt {
    private val time = Regex("(\\d{2}):(\\d{2}):(\\d{2})[,.:](\\d{3})")

    fun parse(text: String): List<Subtitle> {
        val blocks = text.replace("\\r", "").trim().split(Regex("\\n\\s*\\n"))
        return blocks.mapNotNull { block ->
            val lines = block.lines()
            val timing = lines.firstOrNull { it.contains(" --> ") } ?: return@mapNotNull null
            val parts = timing.split(" --> ")
            if (parts.size != 2) return@mapNotNull null
            val start = parseTime(parts[0].trim()) ?: return@mapNotNull null
            val end = parseTime(parts[1].trim()) ?: return@mapNotNull null
            val textStart = lines.indexOfFirst { it.contains(" --> ") } + 1
            val body = lines.drop(textStart).joinToString("\\n").trim()
            if (body.isBlank()) return@mapNotNull null
            Subtitle(System.nanoTime(), start, end, body)
        }
    }

    fun stringify(items: List<Subtitle>): String = buildString {
        items.sortedBy { it.startMs }.forEachIndexed { i, s ->
            append(i + 1).append('\n')
            append(formatTime(s.startMs)).append(" --> ").append(formatTime(s.endMs)).append('\n')
            append(s.text).append("\n\n")
        }
    }

    fun read(context: Context, uri: Uri): List<Subtitle> = context.contentResolver.openInputStream(uri)?.use { input ->
        parse(BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText())
    } ?: emptyList()

    private fun parseTime(s: String): Long? {
        val m = time.find(s) ?: return null
        val h = m.groupValues[1].toLong(); val min = m.groupValues[2].toLong(); val sec = m.groupValues[3].toLong(); val ms = m.groupValues[4].toLong()
        return (((h * 60) + min) * 60 + sec) * 1000 + ms
    }
    private fun formatTime(ms: Long): String {
        val total = ms.coerceAtLeast(0)
        val h = total / 3600000; val m = (total / 60000) % 60; val s = (total / 1000) % 60; val x = total % 1000
        return "%02d:%02d:%02d,%03d".format(h, m, s, x)
    }
}
