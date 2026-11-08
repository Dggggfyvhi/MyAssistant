package com.myassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.sin

// ───────────────────────── 主题与“液态玻璃” ─────────────────────────

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val cs = if (isSystemInDarkTheme()) darkColorScheme(
        primary = Color(0xFF7DA2FF), onPrimary = Color.White,
        background = Color(0xFF0B0C10), onBackground = Color(0xFFF2F3F7),
        surface = Color(0xFF1B1D25), onSurface = Color(0xFFF2F3F7),
        surfaceVariant = Color(0xFF262932), onSurfaceVariant = Color(0xFFA6AAB8),
        outlineVariant = Color(0xFF383B47), error = Color(0xFFFF7A7A)
    ) else lightColorScheme(
        primary = Color(0xFF3B6FE8), onPrimary = Color.White,
        background = Color(0xFFF3F5FA), onBackground = Color(0xFF0D0E12),
        surface = Color.White, onSurface = Color(0xFF0D0E12),
        surfaceVariant = Color(0xFFE9ECF4), onSurfaceVariant = Color(0xFF666B7A),
        outlineVariant = Color(0xFFDDE1EA), error = Color(0xFFD33A3A)
    )
    MaterialTheme(colorScheme = cs, content = content)
}

val LocalHaze = compositionLocalOf<HazeState?> { null }

/** 玻璃：背后内容的真实模糊 + 半透明着色 + 顶部高光 + 渐变描边 + 柔和投影。 */
@Composable
fun Modifier.glass(shape: Shape): Modifier {
    val haze = LocalHaze.current
    val dark = isSystemInDarkTheme()
    val tint = if (dark) Color(0xFF1E2029).copy(alpha = 0.42f) else Color.White.copy(alpha = 0.42f)
    val edge = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = if (dark) 0.50f else 0.95f),
            Color.White.copy(alpha = 0.05f),
            Color.White.copy(alpha = if (dark) 0.26f else 0.70f)
        )
    )
    val sheen = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = if (dark) 0.10f else 0.34f), Color.Transparent), endY = 200f
    )
    var m = this.shadow(elevation = 10.dp, shape = shape, ambientColor = Color(0x22000000), spotColor = Color(0x30000000)).clip(shape)
    if (haze != null) m = m.hazeEffect(state = haze) { blurRadius = 28.dp; noiseFactor = 0.03f }
    return m.background(tint).background(sheen).border(1.dp, edge, shape)
}

/** 柔和的彩色底，玻璃会折射/模糊它。 */
@Composable
fun Ambient() {
    val dark = isSystemInDarkTheme()
    Canvas(Modifier.fillMaxSize()) {
        drawRect(if (dark) Color(0xFF0B0C10) else Color(0xFFF3F5FA))
        val w = size.width
        val h = size.height
        fun blob(cx: Float, cy: Float, r: Float, c: Color) {
            drawCircle(Brush.radialGradient(listOf(c, Color.Transparent), center = Offset(cx, cy), radius = r), radius = r, center = Offset(cx, cy))
        }
        blob(w * 0.12f, h * 0.10f, w * 0.85f, Color(0xFF7BA7FF).copy(alpha = if (dark) 0.34f else 0.55f))
        blob(w * 0.98f, h * 0.42f, w * 0.75f, Color(0xFFC6A4FF).copy(alpha = if (dark) 0.28f else 0.45f))
        blob(w * 0.15f, h * 0.88f, w * 0.85f, Color(0xFF7FE3D6).copy(alpha = if (dark) 0.24f else 0.42f))
    }
}

@Composable
fun CircleBtn(
    icon: ImageVector, desc: String, size: Dp = 44.dp, solid: Color? = null,
    fg: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit
) {
    val base = Modifier.size(size)
    val m = if (solid == null) base.glass(CircleShape) else base.clip(CircleShape).background(solid)
    Box(m.clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(imageVector = icon, contentDescription = desc, tint = fg, modifier = Modifier.size(size * 0.48f))
    }
}

@Composable
fun PillBtn(text: String, modifier: Modifier = Modifier, filled: Boolean = false, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier.height(40.dp).clip(RoundedCornerShape(20.dp))
            .background(if (filled) cs.onBackground else cs.primary.copy(alpha = 0.14f))
            .clickable(onClick = onClick).padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = if (filled) cs.background else cs.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
}

@Composable
fun rememberMicGate(): (() -> Unit) -> Unit {
    val ctx = LocalContext.current
    var pend by remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) pend?.invoke()
        pend = null
    }
    return { action ->
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) action()
        else { pend = action; launcher.launch(Manifest.permission.RECORD_AUDIO) }
    }
}

// ───────────────────────── 消息 ─────────────────────────

private fun mdInline(src: String, codeBg: Color): AnnotatedString = buildAnnotatedString {
    val re = Regex("""\*\*(.+?)\*\*|`([^`]+)`""")
    val lines = src.split('\n')
    lines.forEachIndexed { idx, line ->
        var l = line
        var head = false
        if (l.startsWith("### ")) { l = l.removePrefix("### "); head = true }
        else if (l.startsWith("## ")) { l = l.removePrefix("## "); head = true }
        else if (l.startsWith("# ")) { l = l.removePrefix("# "); head = true }
        else if (l.startsWith("- ") || l.startsWith("* ")) { l = "•  " + l.drop(2) }
        val start = length
        var last = 0
        for (m in re.findAll(l)) {
            append(l.substring(last, m.range.first))
            if (m.groupValues[1].isNotEmpty()) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(m.groupValues[1]) }
            else withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg)) { append(m.groupValues[2]) }
            last = m.range.last + 1
        }
        append(l.substring(last))
        if (head) addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp), start, length)
        if (idx < lines.lastIndex) append("\n")
    }
}

@Composable
fun Markdown(text: String) {
    val cs = MaterialTheme.colorScheme
    val parts = text.split("```")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parts.forEachIndexed { idx, part ->
            if (idx % 2 == 1) {
                val code = part.substringAfter('\n', part).trimEnd()
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(cs.onSurface.copy(alpha = 0.07f))
                        .horizontalScroll(rememberScrollState()).padding(12.dp)
                ) { Text(code, fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 19.sp) }
            } else if (part.isNotBlank()) {
                Text(mdInline(part.trim('\n'), cs.onSurface.copy(alpha = 0.10f)), fontSize = 16.sp, lineHeight = 26.sp)
            }
        }
    }
}

@Composable
fun Dots() {
    val t = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(vertical = 8.dp)) {
        for (i in 0..2) {
            val a by t.animateFloat(
                initialValue = 0.25f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(600, delayMillis = i * 160), RepeatMode.Reverse), label = "d$i"
            )
            Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = a)))
        }
    }
}

@Composable
fun Bubble(m: JSONObject) {
    val cs = MaterialTheme.colorScheme
    val text = m.optString("content")
    when (m.optString("role")) {
        "user" -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            val shape = RoundedCornerShape(22.dp)
            Box(
                Modifier.padding(start = 56.dp).clip(shape)
                    .background(Brush.linearGradient(listOf(Color(0xFF6C97FF), Color(0xFF3B6FE8))))
                    .border(0.5.dp, Color.White.copy(alpha = 0.45f), shape)
            ) { Text(text, Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = Color.White, fontSize = 16.sp, lineHeight = 24.sp) }
        }
        "assistant" -> if (text.isNotBlank()) SelectionContainer { Markdown(text) }
        "tool" -> Row(
            Modifier.clip(RoundedCornerShape(14.dp)).background(cs.surface.copy(alpha = 0.65f))
                .border(0.5.dp, cs.outlineVariant, RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(cs.primary))
            Spacer(Modifier.width(8.dp))
            Text(
                Tools.label(m.optString("name")) + " · " + text.replace('\n', ' ').take(48),
                fontSize = 12.sp, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DrawerItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(cs.surfaceVariant.copy(alpha = 0.6f))
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 16.sp)
    }
}

// ───────────────────────── 主页 ─────────────────────────

@Composable
fun Home(vm: VM, voice: Voice, openSettings: () -> Unit, openVoice: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val haze = remember { HazeState() }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val gate = rememberMicGate()
    var text by remember { mutableStateOf("") }
    var dictating by remember { mutableStateOf(false) }
    val topPad = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val botPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    CompositionLocalProvider(LocalHaze provides haze) {
        ModalNavigationDrawer(
            drawerState = drawer,
            drawerContent = {
                ModalDrawerSheet(
                    drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp),
                    drawerContainerColor = cs.surface.copy(alpha = 0.95f), drawerContentColor = cs.onSurface
                ) {
                    Column(Modifier.fillMaxHeight().statusBarsPadding().navigationBarsPadding().padding(14.dp)) {
                        DrawerItem(Icons.Rounded.Edit, "新聊天") { vm.newChat(); scope.launch { drawer.close() } }
                        Text("最近", color = cs.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(12.dp, 18.dp, 12.dp, 6.dp))
                        LazyColumn(Modifier.weight(1f)) {
                            items(vm.chats.toList(), key = { it.id }) { c ->
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                        .clickable { vm.open(c); scope.launch { drawer.close() } }.padding(start = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(c.title.ifBlank { "新聊天" }, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 16.sp)
                                    IconButton(onClick = { vm.delete(c) }) {
                                        Icon(imageVector = Icons.Rounded.Delete, contentDescription = "删除", tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        DrawerItem(Icons.Rounded.Tune, "设置") { scope.launch { drawer.close() }; openSettings() }
                    }
                }
            }
        ) {
            Box(Modifier.fillMaxSize().imePadding()) {
                Box(Modifier.fillMaxSize().hazeSource(haze)) {
                    Ambient()
                    val msgs = vm.cur.msgs
                    if (msgs.isEmpty()) {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("我的助手", fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                            if (vm.modelLabel.isBlank()) {
                                Spacer(Modifier.height(16.dp))
                                PillBtn("先配置 API", onClick = openSettings)
                            }
                        }
                    } else {
                        val ls = rememberLazyListState()
                        val last = msgs.lastOrNull()
                        val thinking = vm.busy && (last == null || last.optString("role") != "assistant" || last.optString("content").isBlank())
                        LaunchedEffect(msgs.size, last?.optString("content")?.length, thinking) {
                            ls.scrollToItem(if (thinking) msgs.size else msgs.size - 1)
                        }
                        LazyColumn(
                            state = ls,
                            contentPadding = PaddingValues(16.dp, topPad + 76.dp, 16.dp, botPad + 104.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(msgs.size) { i -> Bubble(msgs[i]) }
                            if (thinking) item { Dots() }
                        }
                    }
                }

                Row(
                    Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = topPad + 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CircleBtn(Icons.Rounded.Menu, "菜单") { scope.launch { drawer.open() } }
                    Box(
                        Modifier.widthIn(max = 200.dp).height(44.dp).glass(RoundedCornerShape(22.dp))
                            .clickable(onClick = openSettings).padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(vm.modelLabel.ifBlank { "配置模型" }, color = cs.primary, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    CircleBtn(Icons.Rounded.Edit, "新聊天") { vm.newChat() }
                }

                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = botPad + 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (vm.busy && vm.status.isNotBlank()) {
                        Row(
                            Modifier.padding(bottom = 8.dp).clip(RoundedCornerShape(16.dp)).background(cs.surface.copy(alpha = 0.75f))
                                .border(0.5.dp, cs.outlineVariant, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(cs.primary))
                            Spacer(Modifier.width(8.dp))
                            Text(vm.status, fontSize = 13.sp, color = cs.onSurfaceVariant)
                        }
                    }
                    vm.error?.let {
                        Text(it, color = cs.error, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                    }
                    Row(
                        Modifier.fillMaxWidth().glass(RoundedCornerShape(32.dp)).padding(start = 20.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f),
                            textStyle = TextStyle(color = cs.onSurface, fontSize = 16.sp),
                            cursorBrush = SolidColor(cs.primary), maxLines = 5,
                            decorationBox = { inner ->
                                Box(Modifier.heightIn(min = 40.dp), contentAlignment = Alignment.CenterStart) {
                                    if (text.isEmpty()) Text(if (dictating) "正在听…" else "询问我的助手", color = cs.onSurfaceVariant, fontSize = 16.sp)
                                    inner()
                                }
                            }
                        )
                        IconButton(onClick = {
                            gate {
                                if (dictating) { voice.stopListen(); dictating = false }
                                else {
                                    dictating = true
                                    voice.listen(
                                        onPartial = { if (it != "识别中…") text = it },
                                        onFinal = { if (it.isNotBlank()) text = it; dictating = false },
                                        onError = { dictating = false },
                                        onRms = {}
                                    )
                                }
                            }
                        }, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = if (dictating) Icons.Rounded.MicOff else Icons.Rounded.Mic, contentDescription = "语音转文字",
                                tint = if (dictating) cs.error else cs.onSurface
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        when {
                            vm.busy -> CircleBtn(Icons.Rounded.Stop, "停止", size = 40.dp, solid = cs.onBackground, fg = cs.background) { vm.stop() }
                            text.isNotBlank() -> CircleBtn(Icons.Rounded.ArrowUpward, "发送", size = 40.dp, solid = cs.onBackground, fg = cs.background) {
                                val t = text
                                if (vm.store.configured()) text = ""
                                vm.send(t)
                            }
                            else -> CircleBtn(Icons.Rounded.GraphicEq, "语音模式", size = 40.dp, solid = cs.primary, fg = Color.White) { openVoice() }
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── 设置 ─────────────────────────

@Composable
private fun Group(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp)).padding(horizontal = 18.dp, vertical = 6.dp), content = content)
}

@Composable
private fun SectionTitle(t: String) {
    Text(t, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp, top = 22.dp, bottom = 8.dp))
}

@Composable
private fun Line() = HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 0.5.dp)

@Composable
private fun Field(label: String, value: String, hint: String = "", password: Boolean = false, single: Boolean = true, onChange: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.padding(vertical = 10.dp)) {
        Text(label, color = cs.onSurfaceVariant, fontSize = 13.sp)
        BasicTextField(
            value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            textStyle = TextStyle(color = cs.onSurface, fontSize = 16.sp), singleLine = single,
            cursorBrush = SolidColor(cs.primary),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            decorationBox = { inner ->
                Box { if (value.isEmpty()) Text(hint, color = cs.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 16.sp); inner() }
            }
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 16.sp)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun Hint(t: String) {
    Text(t, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(8.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: VM, onBack: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val st = vm.store
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val haze = remember { HazeState() }
    var base by remember { mutableStateOf(st.baseUrl) }
    var key by remember { mutableStateOf(st.apiKey) }
    var model by remember { mutableStateOf(st.model) }
    var sys by remember { mutableStateOf(st.system) }
    var speak by remember { mutableStateOf(st.speak) }
    var agent by remember { mutableStateOf(st.agent) }
    var stt by remember { mutableStateOf(st.stt) }
    var a11y by remember { mutableStateOf(AgentService.inst != null) }
    var adbOn by remember { mutableStateOf(AdbMgr.isUp()) }
    var status by remember { mutableStateOf("") }
    var ok by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf(listOf<String>()) }
    var pairPort by remember { mutableStateOf("") }
    var pairCode by remember { mutableStateOf("") }
    var connPort by remember { mutableStateOf("") }
    var adbMsg by remember { mutableStateOf("") }
    var adbBusy by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { a11y = AgentService.inst != null; adbOn = AdbMgr.isUp() }
    DisposableEffect(Unit) {
        val d = AdbDiscover(ctx, onPair = { pairPort = it.toString() }, onConnect = { connPort = it.toString() })
        d.start()
        onDispose { d.stop() }
    }

    fun go(intent: Intent) {
        try { ctx.startActivity(intent) } catch (e: Exception) {
            try { ctx.startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (e2: Exception) { }
        }
    }

    CompositionLocalProvider(LocalHaze provides haze) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().hazeSource(haze)) { Ambient() }
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Box(Modifier.fillMaxWidth().padding(16.dp, 12.dp), contentAlignment = Alignment.CenterStart) {
                    CircleBtn(Icons.Rounded.ArrowBack, "返回") { onBack() }
                    Text("设置", Modifier.align(Alignment.Center), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).imePadding()) {
                    SectionTitle("模型接口（OpenAI 兼容）")
                    Group {
                        Field("API 地址", base, "https://api.example.com/v1") { base = it; st.baseUrl = it }
                        Line()
                        Field("API Key", key, "sk-", password = true) { key = it; st.apiKey = it }
                        Line()
                        Field("模型名称", model, "点下面“获取模型列表”自动填入") { model = it; st.model = it; vm.modelLabel = it }
                    }
                    Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PillBtn(if (busy) "请稍候…" else "测试连接") {
                            if (busy) return@PillBtn
                            if (model.isBlank()) { ok = false; status = "✗ 请先填写模型名称，或点“获取模型列表”"; return@PillBtn }
                            busy = true; status = "测试中…"
                            scope.launch {
                                val r = withContext(Dispatchers.IO) { Api.test(base, key, model) }
                                ok = r.startsWith("✓"); status = r; busy = false
                            }
                        }
                        PillBtn("获取模型列表") {
                            if (busy) return@PillBtn
                            busy = true; status = "获取中…"
                            scope.launch {
                                try {
                                    val l = withContext(Dispatchers.IO) { Api.models(base, key) }
                                    models = l; ok = true; status = "✓ 获取到 ${l.size} 个模型"
                                    if (model.isBlank() && l.isNotEmpty()) { model = l.first(); st.model = model; vm.modelLabel = model }
                                } catch (e: Exception) { ok = false; status = "✗ ${e.message}" }
                                busy = false
                            }
                        }
                    }
                    if (status.isNotEmpty()) Text(status, color = if (ok) cs.primary else cs.error, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                    if (models.isNotEmpty()) {
                        val shown = (if (models.contains(model)) models else models.filter { model.isBlank() || it.contains(model, true) }).take(30)
                        FlowRow(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            shown.forEach { m ->
                                Box(
                                    Modifier.clip(RoundedCornerShape(16.dp))
                                        .background(if (m == model) cs.primary.copy(alpha = 0.18f) else cs.surface.copy(alpha = 0.7f))
                                        .clickable { model = m; st.model = m; vm.modelLabel = m }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) { Text(m, fontSize = 13.sp, color = if (m == model) cs.primary else cs.onSurface) }
                            }
                        }
                    }

                    SectionTitle("Agent（让助手操作手机）")
                    Group {
                        SwitchRow("允许助手操作手机", agent) { agent = it; st.agent = it }
                        Line()
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("无障碍服务", fontSize = 16.sp)
                                Text(if (a11y) "已开启" else "未开启", fontSize = 13.sp, color = if (a11y) cs.primary else cs.onSurfaceVariant)
                            }
                            PillBtn("开启") { go(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                        }
                    }
                    Hint("无障碍和无线调试开一个就能用，两个都开时优先用无障碍。Android 13+ 如果无障碍是灰色，到系统“应用信息”里允许“受限制的设置”。")

                    SectionTitle("无线调试（原生桥接）")
                    Group {
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("连接状态", fontSize = 16.sp)
                                Text(if (adbOn) "已连接" else "未连接", fontSize = 13.sp, color = if (adbOn) cs.primary else cs.onSurfaceVariant)
                            }
                            PillBtn("开发者选项") { go(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
                        }
                        Line()
                        Field("配对端口（自动识别，也可手填）", pairPort, "例如 37215") { pairPort = it.filter { c -> c.isDigit() } }
                        Line()
                        Field("配对码（6 位）", pairCode, "在“使用配对码配对设备”里看到的数字") { pairCode = it.filter { c -> c.isDigit() }.take(6) }
                        Line()
                        Field("连接端口（自动识别，也可手填）", connPort, "无线调试页面显示的端口") { connPort = it.filter { c -> c.isDigit() } }
                        Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PillBtn(if (adbBusy) "请稍候…" else "配对") {
                                val p = pairPort.toIntOrNull()
                                if (adbBusy) return@PillBtn
                                if (p == null || pairCode.length < 6) { adbMsg = "✗ 请填写配对端口和 6 位配对码"; return@PillBtn }
                                adbBusy = true; adbMsg = "配对中…"
                                scope.launch { adbMsg = withContext(Dispatchers.IO) { AdbMgr.pair(ctx, "127.0.0.1", p, pairCode) }; adbBusy = false }
                            }
                            PillBtn("连接") {
                                val p = connPort.toIntOrNull()
                                if (adbBusy) return@PillBtn
                                if (p == null) { adbMsg = "✗ 请填写连接端口"; return@PillBtn }
                                adbBusy = true; adbMsg = "连接中…"
                                scope.launch {
                                    adbMsg = withContext(Dispatchers.IO) { AdbMgr.connect(ctx, "127.0.0.1", p) }
                                    adbOn = AdbMgr.isUp(); adbBusy = false
                                }
                            }
                            PillBtn("断开") { AdbMgr.disconnect(); adbOn = AdbMgr.isUp(); adbMsg = "已断开" }
                        }
                        if (adbMsg.isNotEmpty()) Text(adbMsg, color = if (adbMsg.startsWith("✗")) cs.error else cs.primary, fontSize = 14.sp, modifier = Modifier.padding(bottom = 10.dp))
                    }
                    Hint("步骤：1) 连上 Wi‑Fi，在开发者选项里打开“无线调试”；2) 点进“使用配对码配对设备”，此时用分屏或小窗同时打开本应用，端口会自动出现；3) 输入配对码点“配对”，再点“连接”。配对只需一次，之后只点“连接”。")

                    SectionTitle("声音")
                    Group {
                        SwitchRow("语音模式下朗读回复", speak) { speak = it; st.speak = it }
                        Line()
                        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("听写方式", Modifier.weight(1f), fontSize = 16.sp)
                            PillBtn("模型", filled = stt == "model") { stt = "model"; st.stt = "model" }
                            PillBtn("系统", filled = stt == "system") { stt = "system"; st.stt = "system" }
                        }
                    }
                    Hint("“模型”用当前模型听写，更准，需要模型支持音频输入（如 Gemini）；不支持或报错时改用“系统”。朗读用手机系统自带的语音。")

                    SectionTitle("高级")
                    Group { Field("系统提示词", sys, single = false) { sys = it; st.system = it } }
                    Row(Modifier.padding(vertical = 12.dp)) { PillBtn("恢复默认提示词") { sys = DEFAULT_SYSTEM; st.system = sys } }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

// ───────────────────────── 语音模式 ─────────────────────────

@Composable
fun Orb(phase: String, level: Float) {
    val inf = rememberInfiniteTransition(label = "orb")
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "t")
    val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "spin")
    val pulse by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val target = when (phase) {
        "listen" -> 1f + level * 0.14f
        "speak" -> 1f + pulse * 0.08f
        "boot" -> 0.45f
        "think" -> 0.92f
        else -> 1f
    }
    val sc by animateFloatAsState(target, tween(140), label = "sc")
    Canvas(Modifier.size(230.dp).graphicsLayer { scaleX = sc; scaleY = sc; rotationZ = if (phase == "think") spin else 0f }) {
        val r = size.minDimension / 2
        val c = center
        clipPath(Path().apply { addOval(Rect(c, r)) }) {
            drawRect(
                Brush.verticalGradient(
                    0f to Color(0xFF3F6FE6), 0.3f to Color(0xFF5B86EE),
                    0.58f to Color(0xFFE6EDFF), 1f to Color(0xFFB4C8FA)
                )
            )
            val a = sin(t * 2 * PI).toFloat()
            val c1 = Offset(c.x + a * r * 0.25f, c.y + r * 0.05f)
            val c2 = Offset(c.x - a * r * 0.25f, c.y + r * 0.3f)
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.9f), Color.Transparent), center = c1, radius = r * 0.75f), radius = r * 0.75f, center = c1)
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.7f), Color.Transparent), center = c2, radius = r * 0.65f), radius = r * 0.65f, center = c2)
        }
    }
}

@Composable
fun VoiceScreen(vm: VM, voice: Voice, onClose: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val haze = remember { HazeState() }
    val ses = remember { VoiceSession(vm, voice) }
    val gate = rememberMicGate()
    LaunchedEffect(Unit) { gate { ses.start() } }
    DisposableEffect(Unit) { onDispose { ses.stop(); vm.stop() } }

    CompositionLocalProvider(LocalHaze provides haze) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().hazeSource(haze)) { Ambient() }
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(16.dp, 12.dp)) {
                    CircleBtn(Icons.Rounded.KeyboardArrowDown, "收起") { onClose() }
                }
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { ses.tap() }) {
                            Orb(ses.phase, ses.level)
                        }
                        Spacer(Modifier.height(28.dp))
                        Text(
                            if (ses.phase == "think" && vm.status.isNotBlank()) vm.status else ses.status,
                            color = cs.onSurfaceVariant, fontSize = 17.sp,
                            modifier = Modifier.heightIn(min = 26.dp).padding(horizontal = 32.dp), textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        ses.caption, Modifier.align(Alignment.BottomCenter).padding(horizontal = 28.dp, vertical = 12.dp).heightIn(min = 60.dp),
                        textAlign = TextAlign.Center, fontSize = 17.sp, lineHeight = 26.sp
                    )
                }
                Row(Modifier.fillMaxWidth().padding(12.dp, 8.dp, 12.dp, 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.weight(1f).height(52.dp).glass(RoundedCornerShape(26.dp)).clickable { onClose() }.padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) { Text("询问我的助手", color = cs.onSurfaceVariant, fontSize = 16.sp) }
                    Spacer(Modifier.width(10.dp))
                    CircleBtn(if (ses.muted) Icons.Rounded.MicOff else Icons.Rounded.Mic, "静音", size = 52.dp) { ses.toggleMute() }
                    Spacer(Modifier.width(10.dp))
                    CircleBtn(Icons.Rounded.Close, "结束", size = 52.dp, solid = cs.onBackground, fg = cs.background) { onClose() }
                }
            }
        }
    }
}
