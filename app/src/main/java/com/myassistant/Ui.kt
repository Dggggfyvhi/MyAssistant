package com.myassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val cs = if (isSystemInDarkTheme()) darkColorScheme(
        primary = Color(0xFF6F96F5), onPrimary = Color.White,
        background = Color(0xFF141414), onBackground = Color(0xFFF2F2F2),
        surface = Color(0xFF232323), onSurface = Color(0xFFF2F2F2),
        surfaceVariant = Color(0xFF2B2B2B), onSurfaceVariant = Color(0xFFA3A3A3),
        outlineVariant = Color(0xFF3A3A3A), error = Color(0xFFFF7A7A)
    ) else lightColorScheme(
        primary = Color(0xFF3B6FE8), onPrimary = Color.White,
        background = Color(0xFFFAFAFA), onBackground = Color(0xFF0D0D0D),
        surface = Color.White, onSurface = Color(0xFF0D0D0D),
        surfaceVariant = Color(0xFFF0F0F0), onSurfaceVariant = Color(0xFF6B6B6B),
        outlineVariant = Color(0xFFE4E4E4), error = Color(0xFFD33A3A)
    )
    MaterialTheme(colorScheme = cs, content = content)
}

@Composable
fun CircleBtn(
    icon: ImageVector, desc: String,
    bg: Color = MaterialTheme.colorScheme.surface,
    fg: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 44.dp, border: Boolean = true, onClick: () -> Unit
) {
    Box(
        Modifier.size(size).clip(CircleShape).background(bg)
            .then(if (border) Modifier.border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(imageVector = icon, contentDescription = desc, tint = fg, modifier = Modifier.size(size * 0.5f)) }
}

@Composable
fun PillBtn(text: String, modifier: Modifier = Modifier, filled: Boolean = false, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier.height(44.dp).clip(RoundedCornerShape(22.dp))
            .background(if (filled) cs.onBackground else cs.surfaceVariant)
            .clickable(onClick = onClick).padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = if (filled) cs.background else cs.onSurface, fontSize = 15.sp) }
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

@Composable
fun DrawerItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(cs.surface)
            .border(0.5.dp, cs.outlineVariant, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 16.sp)
    }
}

@Composable
fun Bubble(m: JSONObject) {
    val cs = MaterialTheme.colorScheme
    val text = m.optString("content")
    when (m.optString("role")) {
        "user" -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Surface(color = cs.surfaceVariant, shape = RoundedCornerShape(22.dp), modifier = Modifier.padding(start = 48.dp)) {
                Text(text, Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontSize = 16.sp, lineHeight = 24.sp)
            }
        }
        "assistant" -> if (text.isNotBlank()) SelectionContainer {
            Text(text, fontSize = 16.sp, lineHeight = 26.sp)
        }
        "tool" -> Surface(color = cs.surfaceVariant.copy(alpha = 0.6f), shape = RoundedCornerShape(14.dp)) {
            Text(
                m.optString("name") + " · " + text.replace('\n', ' ').take(60),
                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                fontSize = 12.sp, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun Home(vm: VM, voice: Voice, openSettings: () -> Unit, openVoice: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val gate = rememberMicGate()
    var text by remember { mutableStateOf("") }
    var dictating by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
                drawerContainerColor = cs.background, drawerContentColor = cs.onBackground
            ) {
                Column(Modifier.fillMaxHeight().statusBarsPadding().navigationBarsPadding().padding(12.dp)) {
                    DrawerItem(Icons.Rounded.Edit, "新聊天") { vm.newChat(); scope.launch { drawer.close() } }
                    Text("最近", color = cs.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(12.dp, 18.dp, 12.dp, 6.dp))
                    LazyColumn(Modifier.weight(1f)) {
                        items(vm.chats.toList(), key = { it.id }) { c ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                    .clickable { vm.open(c); scope.launch { drawer.close() } }
                                    .padding(start = 12.dp),
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
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircleBtn(Icons.Rounded.Menu, "菜单") { scope.launch { drawer.open() } }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(cs.primary.copy(alpha = 0.12f))
                            .clickable(onClick = openSettings).padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(vm.modelLabel.ifBlank { "配置模型" }, color = cs.primary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                CircleBtn(Icons.Rounded.Edit, "新聊天") { vm.newChat() }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                val msgs = vm.cur.msgs
                if (msgs.isEmpty()) {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("我的助手", fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                        if (vm.modelLabel.isBlank()) {
                            Spacer(Modifier.height(16.dp))
                            PillBtn("先配置 API", onClick = openSettings)
                        }
                    }
                } else {
                    val ls = rememberLazyListState()
                    LaunchedEffect(msgs.size, msgs.lastOrNull()?.optString("content")?.length) {
                        if (msgs.isNotEmpty()) ls.scrollToItem(msgs.size - 1)
                    }
                    LazyColumn(
                        state = ls, contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) { items(msgs.size) { i -> Bubble(msgs[i]) } }
                }
            }

            vm.error?.let {
                Text(it, color = cs.error, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            }
            Surface(
                Modifier.fillMaxWidth().padding(12.dp, 8.dp, 12.dp, 12.dp),
                shape = RoundedCornerShape(30.dp), color = cs.surface,
                border = BorderStroke(0.5.dp, cs.outlineVariant), shadowElevation = 2.dp
            ) {
                Row(Modifier.padding(start = 20.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f),
                        textStyle = TextStyle(color = cs.onSurface, fontSize = 16.sp),
                        cursorBrush = SolidColor(cs.primary), maxLines = 5,
                        decorationBox = { inner ->
                            Box(Modifier.heightIn(min = 40.dp), contentAlignment = Alignment.CenterStart) {
                                if (text.isEmpty()) Text("询问我的助手", color = cs.onSurfaceVariant, fontSize = 16.sp)
                                inner()
                            }
                        }
                    )
                    CircleBtn(
                        if (dictating) Icons.Rounded.MicOff else Icons.Rounded.Mic, "语音转文字",
                        bg = Color.Transparent, fg = if (dictating) cs.error else cs.onSurface, size = 40.dp, border = false
                    ) {
                        gate {
                            if (dictating) { voice.stopListen(); dictating = false }
                            else {
                                dictating = true
                                voice.listen(
                                    onPartial = { text = it },
                                    onFinal = { if (it.isNotBlank()) text = it; dictating = false },
                                    onError = { dictating = false },
                                    onRms = {}
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    when {
                        vm.busy -> CircleBtn(Icons.Rounded.Stop, "停止", bg = cs.onBackground, fg = cs.background, size = 40.dp, border = false) { vm.stop() }
                        text.isNotBlank() -> CircleBtn(Icons.Rounded.ArrowUpward, "发送", bg = cs.onBackground, fg = cs.background, size = 40.dp, border = false) {
                            val t = text
                            if (vm.store.configured()) text = ""
                            vm.send(t)
                        }
                        else -> CircleBtn(Icons.Rounded.GraphicEq, "语音模式", bg = cs.primary, fg = Color.White, size = 40.dp, border = false) { openVoice() }
                    }
                }
            }
        }
    }
}

@Composable
private fun Group(content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = cs.surface, border = BorderStroke(0.5.dp, cs.outlineVariant)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), content = content)
    }
}

@Composable
private fun SectionTitle(t: String) {
    Text(t, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp, top = 20.dp, bottom = 8.dp))
}

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: VM, onBack: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val st = vm.store
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var base by remember { mutableStateOf(st.baseUrl) }
    var key by remember { mutableStateOf(st.apiKey) }
    var model by remember { mutableStateOf(st.model) }
    var sys by remember { mutableStateOf(st.system) }
    var speak by remember { mutableStateOf(st.speak) }
    var agent by remember { mutableStateOf(st.agent) }
    var a11y by remember { mutableStateOf(AgentService.inst != null) }
    var status by remember { mutableStateOf("") }
    var ok by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf(listOf<String>()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { a11y = AgentService.inst != null }

    fun go(intent: Intent) {
        try { ctx.startActivity(intent) } catch (e: Exception) {
            try { ctx.startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (e2: Exception) { }
        }
    }

    Column(Modifier.fillMaxSize().background(cs.background).statusBarsPadding().navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().padding(16.dp, 12.dp), contentAlignment = Alignment.CenterStart) {
            CircleBtn(Icons.Rounded.ArrowBack, "返回") { onBack() }
            Text("设置", Modifier.align(Alignment.Center), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).imePadding()) {
            SectionTitle("模型接口（OpenAI 兼容）")
            Group {
                Field("API 地址", base, "https://api.example.com/v1") { base = it; st.baseUrl = it }
                HorizontalDivider(color = cs.outlineVariant, thickness = 0.5.dp)
                Field("API Key", key, "sk-", password = true) { key = it; st.apiKey = it }
                HorizontalDivider(color = cs.outlineVariant, thickness = 0.5.dp)
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
                                .background(if (m == model) cs.primary.copy(alpha = 0.15f) else cs.surfaceVariant)
                                .clickable { model = m; st.model = m; vm.modelLabel = m }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) { Text(m, fontSize = 13.sp, color = if (m == model) cs.primary else cs.onSurface) }
                    }
                }
            }

            SectionTitle("Agent（让助手操作手机）")
            Group {
                SwitchRow("允许助手操作手机", agent) { agent = it; st.agent = it }
                HorizontalDivider(color = cs.outlineVariant, thickness = 0.5.dp)
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("无障碍服务", fontSize = 16.sp)
                        Text(if (a11y) "已开启" else "未开启", fontSize = 13.sp, color = if (a11y) cs.primary else cs.onSurfaceVariant)
                    }
                    PillBtn("开启无障碍") { go(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                }
                HorizontalDivider(color = cs.outlineVariant, thickness = 0.5.dp)
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("无线调试", Modifier.weight(1f), fontSize = 16.sp)
                    PillBtn("开启无线调试") { go(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
                }
            }
            Text(
                "开启无障碍后，助手才能读取屏幕并代你点击、输入、滑动。若选项是灰色，请到系统“应用信息”里允许“受限制的设置”。无线调试目前只是跳转，留给后续的高级操作。",
                color = cs.onSurfaceVariant, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(8.dp)
            )

            SectionTitle("声音")
            Group {
                SwitchRow("语音模式下朗读回复", speak) { speak = it; st.speak = it }
                HorizontalDivider(color = cs.outlineVariant, thickness = 0.5.dp)
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("系统语音设置", Modifier.weight(1f), fontSize = 16.sp)
                    PillBtn("打开") { go(Intent("com.android.settings.TTS_SETTINGS")) }
                }
            }
            Text(
                "语音识别和朗读都用手机系统自带的。想要更真实的女声：安装 sherpa-onnx 的 TTS 引擎 APK，在系统“文字转语音”里把它设为首选引擎，这里会自动使用。",
                color = cs.onSurfaceVariant, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(8.dp)
            )

            SectionTitle("高级")
            Group {
                Field("系统提示词", sys, single = false) { sys = it; st.system = it }
            }
            Row(Modifier.padding(vertical = 12.dp)) { PillBtn("恢复默认提示词") { sys = DEFAULT_SYSTEM; st.system = sys } }
            Spacer(Modifier.height(24.dp))
        }
    }
}

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
    val ses = remember { VoiceSession(vm, voice) }
    val gate = rememberMicGate()
    LaunchedEffect(Unit) { gate { ses.start() } }
    DisposableEffect(Unit) { onDispose { ses.stop(); vm.stop() } }

    Column(Modifier.fillMaxSize().background(cs.background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(16.dp, 12.dp)) {
            CircleBtn(Icons.Rounded.KeyboardArrowDown, "收起") { onClose() }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { ses.tap() }) {
                    Orb(ses.phase, ses.level)
                }
                Spacer(Modifier.height(28.dp))
                Text(ses.status, color = cs.onSurfaceVariant, fontSize = 17.sp, modifier = Modifier.heightIn(min = 26.dp), textAlign = TextAlign.Center)
            }
            Text(
                ses.caption, Modifier.align(Alignment.BottomCenter).padding(horizontal = 28.dp, vertical = 12.dp).heightIn(min = 60.dp),
                textAlign = TextAlign.Center, fontSize = 17.sp, lineHeight = 26.sp
            )
        }
        Row(Modifier.fillMaxWidth().padding(12.dp, 8.dp, 12.dp, 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(26.dp)).background(cs.surface)
                    .border(0.5.dp, cs.outlineVariant, RoundedCornerShape(26.dp))
                    .clickable { onClose() }.padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) { Text("询问我的助手", color = cs.onSurfaceVariant, fontSize = 16.sp) }
            Spacer(Modifier.width(10.dp))
            CircleBtn(if (ses.muted) Icons.Rounded.MicOff else Icons.Rounded.Mic, "静音", bg = cs.surfaceVariant, size = 52.dp, border = false) { ses.toggleMute() }
            Spacer(Modifier.width(10.dp))
            CircleBtn(Icons.Rounded.Close, "结束", bg = cs.onBackground, fg = cs.background, size = 52.dp, border = false) { onClose() }
        }
    }
}
