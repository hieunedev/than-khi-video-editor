package com.thankhi.videoeditor

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

private val Bg=Color(0xFF080A0E); private val Panel=Color(0xFF11151B); private val Panel2=Color(0xFF181E26); private val Cyan=Color(0xFF18C7E8); private val Green=Color(0xFF35D07F); private val Muted=Color(0xFF8A94A3)
enum class Page { HOME, EDITOR, EXPORT, SETTINGS }

class MainActivity: ComponentActivity(){ override fun onCreate(savedInstanceState: Bundle?){ super.onCreate(savedInstanceState); setContent{App()} } }

@OptIn(UnstableApi::class)
@Composable fun App(){
    var page by remember{mutableStateOf(Page.HOME)}
    var clips by remember{mutableStateOf(listOf<Clip>())}
    var selected by remember{mutableStateOf<Clip?>(null)}
    var subs by remember{mutableStateOf(listOf<Subtitle>())}
    val context=androidx.compose.ui.platform.LocalContext.current
    val store=remember{ProjectStore(context)}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){uris->
        if(uris.isNotEmpty()){
            val added=uris.mapIndexed{idx,u->Clip(System.nanoTime()+idx,u,"Video ${clips.size+idx+1}")}
            clips=clips+added; selected=added.first(); store.saveClips(clips); page=Page.EDITOR
        }
    }
    MaterialTheme(colorScheme=darkColorScheme(background=Bg,surface=Panel,primary=Cyan,secondary=Green)){
        Scaffold(containerColor=Bg,bottomBar={Bottom(page){page=it}}){pad->
            Box(Modifier.fillMaxSize().padding(pad)){ when(page){
                Page.HOME->Home({picker.launch(arrayOf("video/*"))},clips,{page=Page.EDITOR})
                Page.EDITOR->Editor(clips,selected,{selected=it},{new->clips=new;store.saveClips(new)},{new->subs=new},{page=Page.EXPORT})
                Page.EXPORT->Export(clips)
                Page.SETTINGS->Settings()
            }}
        }
    }
}

@Composable fun Header(title:String, sub:String=""){ Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){ Column(Modifier.weight(1f)){Text("THẦN KHÍ",fontSize=10.sp,color=Cyan,fontWeight=FontWeight.Bold);Text(title,fontSize=22.sp,fontWeight=FontWeight.Bold);if(sub.isNotBlank())Text(sub,color=Muted,fontSize=11.sp)} Icon(Icons.Default.AutoAwesome,null,tint=Cyan)} }

@Composable fun Home(add:()->Unit,clips:List<Clip>,open:()->Unit){ Column(Modifier.fillMaxSize()){ Header("Video Editor","V5 • AI pipeline • xử lý nền • video dài"); Row(Modifier.padding(horizontal=12.dp).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){Chip(Icons.Default.Add,"Thêm video",add);Chip(Icons.Default.Subtitles,"SRT");Chip(Icons.Default.Translate,"Dịch");Chip(Icons.Default.RecordVoiceOver,"Lồng tiếng");Chip(Icons.Default.AutoAwesome,"AI")}; Spacer(Modifier.height(14.dp)); Surface(Modifier.padding(12.dp).fillMaxWidth().clickable{add()},RoundedCornerShape(18.dp),color=Panel2){Column(Modifier.padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.VideoLibrary,null,tint=Cyan,modifier=Modifier.size(42.dp));Text("Tạo dự án mới",fontWeight=FontWeight.Bold,fontSize=18.sp);Text("Chọn nhiều video • xử lý theo chunk • không giữ toàn bộ RAM",color=Muted,fontSize=11.sp);Spacer(Modifier.height(12.dp));Button(add,colors=ButtonDefaults.buttonColors(containerColor=Cyan,contentColor=Color.Black)){Text("Chọn video")}}}; Text("Dự án hiện tại",Modifier.padding(16.dp,8.dp),fontWeight=FontWeight.Bold); if(clips.isEmpty())Text("Chưa có clip nào",Modifier.padding(16.dp),color=Muted) else LazyColumn{items(clips){c->Row(Modifier.fillMaxWidth().clickable{open()}.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Movie,null,tint=Cyan);Spacer(Modifier.width(12.dp));Text(c.name,Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis);Icon(Icons.Default.ChevronRight,null,tint=Muted)}}}} }

@Composable fun Chip(icon:androidx.compose.ui.graphics.vector.ImageVector,label:String,onClick:()->Unit={}){ Surface(Modifier.clickable{onClick()},RoundedCornerShape(13.dp),color=Panel2){Row(Modifier.padding(11.dp),verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=Cyan,modifier=Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text(label,fontSize=11.sp,fontWeight=FontWeight.SemiBold)}} }

@OptIn(UnstableApi::class)
@Composable fun Editor(clips:List<Clip>,sel:Clip?,select:(Clip)->Unit,setClips:(List<Clip>)->Unit,setSubs:(List<Subtitle>)->Unit,export:()->Unit){
    val context=androidx.compose.ui.platform.LocalContext.current
    var start by remember(sel){mutableFloatStateOf((sel?.startMs?:0L)/1000f)}
    var end by remember(sel){mutableFloatStateOf(((sel?.endMs?:60000L)/1000f).coerceAtLeast(start+1f))}
    var showSub by remember{mutableStateOf(false)}; var text by remember{mutableStateOf("")}
    val srtPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri-> if(uri!=null){setSubs(Srt.read(context,uri));Toast.makeText(context,"Đã nhập SRT",Toast.LENGTH_SHORT).show()}}
    Column(Modifier.fillMaxSize()){ Header("Biên tập","${clips.size} clip • timeline • SRT • render nền"); if(sel!=null){ VideoPreview(sel.uri); Spacer(Modifier.height(8.dp)); Text(sel.name,Modifier.padding(horizontal=14.dp),fontWeight=FontWeight.Bold); Text("Trim: ${start.toInt()}s → ${end.toInt()}s",Modifier.padding(horizontal=14.dp),color=Muted,fontSize=11.sp); RangeSlider(value=start..end,onValueChange={r->start=r.start;end=r.end.coerceAtLeast(start+1f)},valueRange=0f..maxOf(61f,end+30f),modifier=Modifier.padding(horizontal=12.dp)); ToolRow({showSub=true},{tts(context,text)},{setClips(clips.filterNot{it.id==sel.id})},{export()},{srtPicker.launch(arrayOf("text/*","application/x-subrip"))}); if(showSub){ Surface(Modifier.padding(12.dp).fillMaxWidth(),RoundedCornerShape(16.dp),color=Panel){Column(Modifier.padding(14.dp)){Text("Phụ đề",fontWeight=FontWeight.Bold);OutlinedTextField(text,{text=it},Modifier.fillMaxWidth(),label={Text("Nội dung")});Row{Button({showSub=false}){Text("Lưu")};Spacer(Modifier.width(8.dp));Button({srtPicker.launch(arrayOf("text/*","application/x-subrip"))}){Text("Nhập SRT")}}}}}; Text("Timeline",Modifier.padding(14.dp,6.dp),fontWeight=FontWeight.Bold); LazyColumn{items(clips){c->Surface(Modifier.padding(horizontal=12.dp,vertical=4.dp).fillMaxWidth().clickable{select(c)},RoundedCornerShape(12.dp),color=if(c.id==sel.id)Cyan.copy(.16f) else Panel){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Movie,null,tint=Cyan);Spacer(Modifier.width(10.dp));Text(c.name,Modifier.weight(1f));Text("${c.startMs/1000}s",color=Muted,fontSize=10.sp)}}}} } else Text("Chọn một clip",Modifier.padding(16.dp),color=Muted) }
}

@OptIn(UnstableApi::class)
@Composable fun VideoPreview(uri:Uri){ AndroidView(factory={ctx->PlayerView(ctx).apply{val p=ExoPlayer.Builder(ctx).build();player=p;p.setMediaItem(MediaItem.fromUri(uri));p.prepare();p.playWhenReady=false;addOnAttachStateChangeListener(object:android.view.View.OnAttachStateChangeListener{override fun onViewAttachedToWindow(v:android.view.View){};override fun onViewDetachedFromWindow(v:android.view.View){p.release()}})}},Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(16.dp))) }

@Composable fun ToolRow(sub:()->Unit,voice:()->Unit,delete:()->Unit,export:()->Unit,srt:()->Unit){Row(Modifier.padding(10.dp).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)){Chip(Icons.Default.Subtitles,"Subtitle",sub);Chip(Icons.Default.FileOpen,"Import SRT",srt);Chip(Icons.Default.RecordVoiceOver,"TTS",voice);Chip(Icons.Default.Delete,"Xóa",delete);Chip(Icons.Default.Download,"Render",export)}}

@Composable fun Export(clips:List<Clip>){ val ctx=androidx.compose.ui.platform.LocalContext.current; var queued by remember{mutableStateOf(false)}; Column(Modifier.fillMaxSize()){Header("Render / Xuất","WorkManager • queue • multi-clip • MP4"); Surface(Modifier.padding(14.dp).fillMaxWidth(),RoundedCornerShape(18.dp),color=Panel){Column(Modifier.padding(16.dp)){Text("Pipeline",fontWeight=FontWeight.Bold);listOf("Phân tích thời lượng & chia chunk","Trim / ghép clip","ASR → timestamp subtitle","Dịch subtitle","TTS → thay audio","Render MP4 + theo dõi job").forEach{Text("✓  $it",color=if(queued)Green else Color.White,modifier=Modifier.padding(vertical=5.dp))};Spacer(Modifier.height(10.dp));Button({clips.firstOrNull()?.let{ProcessingQueue.enqueueProject(ctx,clips);queued=true;Toast.makeText(ctx,"Đã xếp hàng render nền",Toast.LENGTH_SHORT).show()}},Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Cyan,contentColor=Color.Black)){Text(if(queued)"Đã xếp hàng" else "Render MP4")}}};Text("Job chạy nền và có thể tiếp tục khi UI bị đóng. AI provider được tách khỏi UI để không nhúng secret vào APK.",Modifier.padding(14.dp),color=Muted,fontSize=11.sp)} }

@Composable fun Settings(){ Column(Modifier.fillMaxSize()){Header("Thiết lập","V4"); listOf("AI / ASR" to "Adapter để nối Whisper hoặc provider tương thích","Dịch" to "Adapter dịch, có thể chạy local hoặc server","Voice" to "Android TTS để preview; provider neural nối ở service","Lưu trữ" to "SAF cho file nguồn/đầu ra; project metadata riêng").forEach{(a,b)->Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Settings,null,tint=Cyan);Spacer(Modifier.width(12.dp));Column{Text(a,fontWeight=FontWeight.SemiBold);Text(b,color=Muted,fontSize=11.sp)}}}} }

@Composable fun Bottom(page:Page,set:(Page)->Unit){ NavigationBar(containerColor=Panel){listOf(Page.HOME to (Icons.Default.Home to "Trang chủ"),Page.EDITOR to (Icons.Default.Edit to "Editor"),Page.EXPORT to (Icons.Default.Download to "Render"),Page.SETTINGS to (Icons.Default.Settings to "Cài đặt")).forEach{(p,x)->NavigationBarItem(selected=page==p,onClick={set(p)},icon={Icon(x.first,null)},label={Text(x.second,fontSize=9.sp)})}} }

private fun tts(context:Context,text:String){ if(text.isBlank()){Toast.makeText(context,"Nhập nội dung trước",Toast.LENGTH_SHORT).show();return}; lateinit var engine: TextToSpeech; engine=TextToSpeech(context){status->if(status==TextToSpeech.SUCCESS){engine.language=Locale("vi","VN");engine.speak(text,TextToSpeech.QUEUE_FLUSH,null,"than-khi-preview")}}; Toast.makeText(context,"Đang phát thử TTS",Toast.LENGTH_SHORT).show() }
