package com.thankhi.videoeditor

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.util.Locale

private val Bg = Color(0xFF080A0E)
private val Panel = Color(0xFF11151B)
private val Panel2 = Color(0xFF181E26)
private val Cyan = Color(0xFF18C7E8)
private val Green = Color(0xFF35D07F)
private val Muted = Color(0xFF8A94A3)

enum class Page { HOME, EDITOR, EXPORT, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

@Composable
fun App() {
    var page by remember { mutableStateOf(Page.HOME) }
    var clips by remember { mutableStateOf(emptyList<Clip>()) }
    var selected by remember { mutableStateOf<Clip?>(null) }
    var subtitles by remember { mutableStateOf(emptyList<Subtitle>()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { ProjectStore(context) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            val added = uris.mapIndexed { index, uri ->
                Clip(System.nanoTime() + index, uri, "Video ${clips.size + index + 1}")
            }
            clips = clips + added
            selected = added.firstOrNull()
            store.saveClips(clips)
            page = Page.EDITOR
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Panel, primary = Cyan, secondary = Green)) {
        Scaffold(
            containerColor = Bg,
            bottomBar = { BottomBar(page) { page = it } }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (page) {
                    Page.HOME -> HomeScreen(clips, { picker.launch(arrayOf("video/*")) }) { page = Page.EDITOR }
                    Page.EDITOR -> EditorScreen(
                        clips = clips,
                        selected = selected,
                        subtitles = subtitles,
                        onSelect = { selected = it },
                        onDelete = { clip ->
                            clips = clips.filterNot { it.id == clip.id }
                            selected = clips.firstOrNull()
                            store.saveClips(clips)
                        },
                        onImportSrt = { uri -> subtitles = Srt.read(context, uri) },
                        onRender = { page = Page.EXPORT }
                    )
                    Page.EXPORT -> ExportScreen(clips)
                    Page.SETTINGS -> SettingsScreen()
                }
            }
        }
    }
}

@Composable
fun Header(title: String, subtitle: String = "") {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("THẦN KHÍ", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (subtitle.isNotBlank()) Text(subtitle, color = Muted, fontSize = 11.sp)
        }
        Icon(Icons.Default.AutoAwesome, null, tint = Cyan)
    }
}

@Composable
fun HomeScreen(clips: List<Clip>, add: () -> Unit, open: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Header("Video Editor", "V6 • mobile-first • xử lý video dài")
        Row(
            Modifier.padding(horizontal = 12.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionChip(Icons.Default.Add, "Thêm video", add)
            ActionChip(Icons.Default.Subtitles, "SRT")
            ActionChip(Icons.Default.Translate, "Dịch")
            ActionChip(Icons.Default.RecordVoiceOver, "Lồng tiếng")
        }
        Surface(
            Modifier.padding(12.dp).fillMaxWidth().clickable { add() },
            RoundedCornerShape(18.dp), color = Panel2
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.VideoLibrary, null, tint = Cyan, modifier = Modifier.size(42.dp))
                Text("Tạo dự án mới", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Chọn nhiều video • xử lý theo hàng đợi", color = Muted, fontSize = 11.sp)
                Spacer(Modifier.height(12.dp))
                Button(add, colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color.Black)) {
                    Text("Chọn video")
                }
            }
        }
        Text("Dự án hiện tại", Modifier.padding(16.dp, 8.dp), fontWeight = FontWeight.Bold)
        if (clips.isEmpty()) {
            Text("Chưa có clip nào", Modifier.padding(16.dp), color = Muted)
        } else {
            LazyColumn {
                items(clips) { clip ->
                    Row(
                        Modifier.fillMaxWidth().clickable { open() }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Movie, null, tint = Cyan)
                        Spacer(Modifier.width(12.dp))
                        Text(clip.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Default.ChevronRight, null, tint = Muted)
                    }
                }
            }
        }
    }
}

@Composable
fun ActionChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit = {}) {
    Surface(Modifier.clickable { onClick() }, RoundedCornerShape(13.dp), color = Panel2) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Cyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun EditorScreen(
    clips: List<Clip>, selected: Clip?, subtitles: List<Subtitle>,
    onSelect: (Clip) -> Unit, onDelete: (Clip) -> Unit,
    onImportSrt: (Uri) -> Unit, onRender: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var subtitleText by remember { mutableStateOf("") }
    val srtPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            onImportSrt(uri)
            Toast.makeText(context, "Đã nhập SRT", Toast.LENGTH_SHORT).show()
        }
    }
    Column(Modifier.fillMaxSize()) {
        Header("Biên tập", "${clips.size} clip • preview • SRT • render")
        if (selected != null) {
            VideoPreview(selected.uri)
            Text(selected.name, Modifier.padding(14.dp, 10.dp), fontWeight = FontWeight.Bold)
            Row(Modifier.padding(horizontal = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ActionChip(Icons.Default.Subtitles, "Subtitle")
                ActionChip(Icons.Default.FileOpen, "Import SRT") { srtPicker.launch(arrayOf("text/*", "application/x-subrip")) }
                ActionChip(Icons.Default.RecordVoiceOver, "TTS") { tts(context, subtitleText) }
                ActionChip(Icons.Default.Delete, "Xóa") { onDelete(selected) }
                ActionChip(Icons.Default.Download, "Render", onRender)
            }
            Spacer(Modifier.height(8.dp))
            Surface(Modifier.padding(12.dp).fillMaxWidth(), RoundedCornerShape(16.dp), color = Panel) {
                Column(Modifier.padding(14.dp)) {
                    Text("Phụ đề", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = subtitleText,
                        onValueChange = { subtitleText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nội dung") }
                    )
                    Text("${subtitles.size} dòng SRT", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
            Text("Timeline", Modifier.padding(14.dp, 6.dp), fontWeight = FontWeight.Bold)
            LazyColumn {
                items(clips) { clip ->
                    Surface(
                        Modifier.padding(horizontal = 12.dp, vertical = 4.dp).fillMaxWidth().clickable { onSelect(clip) },
                        RoundedCornerShape(12.dp),
                        color = if (clip.id == selected.id) Cyan.copy(alpha = .16f) else Panel
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Movie, null, tint = Cyan)
                            Spacer(Modifier.width(10.dp))
                            Text(clip.name, Modifier.weight(1f))
                            Text("${clip.startMs / 1000}s", color = Muted, fontSize = 10.sp)
                        }
                    }
                }
            }
        } else {
            Text("Hãy thêm một video để bắt đầu", Modifier.padding(16.dp), color = Muted)
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPreview(uri: Uri) {
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                val player = ExoPlayer.Builder(ctx).build()
                this.player = player
                player.setMediaItem(MediaItem.fromUri(uri))
                player.prepare()
                player.playWhenReady = false
                addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: android.view.View) = Unit
                    override fun onViewDetachedFromWindow(v: android.view.View) { player.release() }
                })
            }
        },
        modifier = Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(16.dp))
    )
}

@Composable
fun ExportScreen(clips: List<Clip>) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var queued by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Header("Render / Xuất", "WorkManager • multi-clip • MP4")
        Surface(Modifier.padding(14.dp).fillMaxWidth(), RoundedCornerShape(18.dp), color = Panel) {
            Column(Modifier.padding(16.dp)) {
                Text("Pipeline", fontWeight = FontWeight.Bold)
                listOf("Chuẩn bị clip", "Trim / ghép", "ASR → subtitle", "Dịch subtitle", "TTS", "Render MP4").forEach {
                    Text("✓  $it", color = if (queued) Green else Color.White, modifier = Modifier.padding(vertical = 5.dp))
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (clips.isNotEmpty()) {
                            ProcessingQueue.enqueueProject(context, clips)
                            queued = true
                            Toast.makeText(context, "Đã xếp hàng render nền", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color.Black)
                ) { Text(if (queued) "Đã xếp hàng" else "Render MP4") }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    Column(Modifier.fillMaxSize()) {
        Header("Thiết lập", "Thần Khí Video Editor")
        listOf(
            "AI / ASR" to "Adapter để nối engine nhận dạng giọng nói",
            "Dịch" to "Adapter dịch phụ đề",
            "Voice" to "Android TTS preview; neural voice nối ở service",
            "Lưu trữ" to "SAF + metadata project"
        ).forEach { (title, desc) ->
            Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, null, tint = Cyan)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, fontWeight = FontWeight.SemiBold)
                    Text(desc, color = Muted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun BottomBar(page: Page, setPage: (Page) -> Unit) {
    NavigationBar(containerColor = Panel) {
        listOf(
            Page.HOME to (Icons.Default.Home to "Trang chủ"),
            Page.EDITOR to (Icons.Default.Edit to "Editor"),
            Page.EXPORT to (Icons.Default.Download to "Render"),
            Page.SETTINGS to (Icons.Default.Settings to "Cài đặt")
        ).forEach { (p, item) ->
            NavigationBarItem(
                selected = page == p,
                onClick = { setPage(p) },
                icon = { Icon(item.first, null) },
                label = { Text(item.second, fontSize = 9.sp) }
            )
        }
    }
}

private fun tts(context: Context, text: String) {
    if (text.isBlank()) {
        Toast.makeText(context, "Nhập nội dung trước", Toast.LENGTH_SHORT).show()
        return
    }
    var engine: TextToSpeech? = null
    engine = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            engine?.language = Locale("vi", "VN")
            engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "than-khi-preview")
        }
    }
}
