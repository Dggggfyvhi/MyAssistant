package com.myassistant

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.*
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

// ───────────────────────── Theme（跟随系统，默认偏浅色） ─────────────────────────

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val cs = if (dark) darkColorScheme(
        primary = Color(0xFF8AB4F8),
        onPrimary = Color(0xFF0B1C2E),
        secondary = Color(0xFF81C995),
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        surfaceVariant = Color(0xFF2C2C2C),
        onBackground = Color(0xFFE8EAED),
        onSurface = Color(0xFFE8EAED),
        onSurfaceVariant = Color(0xFF9AA0A6),
        outline = Color(0xFF3C4043),
        outlineVariant = Color(0xFF444746),
        error = Color(0xFFF28B82)
    ) else lightColorScheme(
        primary = Color(0xFF1A73E8),
        onPrimary = Color.White,
        secondary = Color(0xFF188038),
        background = Color(0xFFF8F9FA),
        surface = Color.White,
        surfaceVariant = Color(0xFFF1F3F4),
        onBackground = Color(0xFF202124),
        onSurface = Color(0xFF202124),
        onSurfaceVariant = Color(0xFF5F6368),
        outline = Color(0xFFDADCE0),
        outlineVariant = Color(0xFFE8EAED),
        error = Color(0xFFD93025)
    )
    MaterialTheme(colorScheme = cs, content = content)
}

@Composable
fun CircleBtn(
    icon: ImageVector, contentDesc: String, size: Dp = 40.dp,
    solid: Color? = null, fg: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val bg = solid ?: cs.surfaceVariant.copy(alpha = 0.8f)
    Box(
        Modifier.size(size).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDesc, tint = if (solid != null) fg else cs.onSurface, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun PillBtn(text: String, modifier: Modifier = Modifier, filled: Boolean = false, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier
            .clip(shape)
            .background(if (filled) cs.primary else cs.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (filled) cs.onPrimary else cs.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// ───────────────────────── Markdown ─────────────────────────

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
        if (head) addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp), start, length)
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
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(cs.surfaceVariant)
                        .horizontalScroll(rememberScrollState()).padding(12.dp)
                ) { Text(code, fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 19.sp) }
            } else if (part.isNotBlank()) {
                Text(mdInline(part.trim('\n'), cs.surfaceVariant), fontSize = 15.5.sp, lineHeight = 24.sp)
            }
        }
    }
}

@Composable
fun Dots() {
    val t = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(0, 1, 2).forEach { i ->
            val a by t.animateFloat(
                initialValue = 0.3f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(500, delayMillis = i * 160), RepeatMode.Reverse),
                label = "d$i"
            )
            Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = a)))
        }
    }
}

// ───────────────────────── Bubbles ─────────────────────────

@Composable
fun Bubble(m: JSONObject) {
    val cs = MaterialTheme.colorScheme
    val text = m.optString("content")
    when (m.optString("role")) {
        "user" -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            val shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
            Box(
                Modifier
                    .padding(start = 48.dp)
                    .clip(shape)
                    .background(cs.primary)
            ) {
                Text(
                    text,
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = cs.onPrimary,
                    fontSize = 15.5.sp,
                    lineHeight = 22.sp
                )
            }
        }
        "assistant" -> if (text.isNotBlank()) {
            SelectionContainer { Markdown(text) }
        }
        "tool" -> Row(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(cs.surfaceVariant)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(cs.secondary))
            Spacer(Modifier.width(8.dp))
            Text(
                Tools.label(m.optString("name")) + " · " + text.replace('\n', ' ').take(48),
                fontSize = 12.sp, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DrawerItem(icon: ImageVector, label: String, selected: Boolean = false, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val bg = if (selected) cs.primary.copy(0.12f) else Color.Transparent
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (selected) cs.primary else cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 15.sp, color = if (selected) cs.primary else cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ───────────────────────── Home（贴近原版） ─────────────────────────

@Composable
fun Home(vm: VM, openSettings: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = cs.surface,
                drawerContentColor = cs.onSurface,
                modifier = Modifier.fillMaxHeight().widthIn(max = 300.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(16.dp)
                ) {
                    Text("对话", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                    PillBtn("＋ 新对话", filled = true, modifier = Modifier.fillMaxWidth()) {
                        vm.newChat()
                        scope.launch { drawer.close() }
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(vm.chats, key = { it.id }) { c ->
                            DrawerItem(
                                Icons.Rounded.ChatBubbleOutline,
                                c.title.ifBlank { "新对话" },
                                selected = c === vm.cur
                            ) {
                                vm.open(c)
                                scope.launch { drawer.close() }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    DrawerItem(Icons.Rounded.Settings, "设置") {
                        openSettings()
                        scope.launch { drawer.close() }
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = cs.background,
            topBar = {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleBtn(Icons.Rounded.Menu, "菜单") { scope.launch { drawer.open() } }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            vm.cur.title.ifBlank { "我的助手" },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (vm.modelLabel.isNotBlank()) {
                            Text(vm.modelLabel, fontSize = 12.sp, color = cs.onSurfaceVariant, maxLines = 1)
                        }
                    }
                    if (vm.busy) {
                        CircleBtn(Icons.Rounded.Stop, "停止", solid = cs.error, fg = Color.White) { vm.stop() }
                    } else {
                        CircleBtn(Icons.Rounded.Settings, "设置") { openSettings() }
                    }
                }
            },
            bottomBar = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .background(cs.surface)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    vm.error?.let {
                        Text(it, color = cs.error, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    // 原生风格输入框：白底 + 细边框 + 圆角，贴底
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                            .background(cs.surface)
                            .border(1.dp, cs.outline.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(color = cs.onSurface, fontSize = 16.sp),
                            cursorBrush = SolidColor(cs.primary),
                            maxLines = 5,
                            decorationBox = { inner ->
                                Box(Modifier.heightIn(min = 40.dp), contentAlignment = Alignment.CenterStart) {
                                    if (text.isEmpty()) Text("发送消息…", color = cs.onSurfaceVariant, fontSize = 16.sp)
                                    inner()
                                }
                            }
                        )
                        val canSend = text.isNotBlank() && !vm.busy
                        IconButton(
                            onClick = {
                                val t = text.trim()
                                if (t.isNotEmpty()) {
                                    text = ""
                                    vm.send(t)
                                }
                            },
                            enabled = canSend,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Send,
                                "发送",
                                tint = if (canSend) cs.primary else cs.onSurfaceVariant.copy(0.4f)
                            )
                        }
                    }
                }
            }
        ) { padding ->
            val msgs = vm.cur.msgs
            val ls = rememberLazyListState()
            val last = msgs.lastOrNull()
            val thinking = vm.busy && (last == null || last.optString("role") != "assistant" || last.optString("content").isBlank())

            LaunchedEffect(msgs.size, vm.busy) {
                if (msgs.isNotEmpty() || thinking) {
                    ls.animateScrollToItem(maxOf(0, msgs.size))
                }
            }

            LazyColumn(
                state = ls,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (msgs.isEmpty() && !vm.busy) {
                    item {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 72.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 简单干净的 Logo 圆
                            Box(
                                Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(cs.primary.copy(0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(cs.primary)
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            Text("我的助手", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (vm.store.configured()) "有什么可以帮你的？"
                                else "先去设置里配置 API 和模型",
                                color = cs.onSurfaceVariant,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
                items(msgs, key = { it.toString().hashCode() }) { m ->
                    Bubble(m)
                }
                if (thinking) {
                    item {
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(cs.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Dots()
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (vm.status.isNotBlank()) vm.status else "思考中…",
                                fontSize = 13.sp,
                                color = cs.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── Settings ─────────────────────────

@Composable
private fun Group(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(0.5f), RoundedCornerShape(14.dp))
            .padding(4.dp),
        content = content
    )
}

@Composable
private fun SectionTitle(t: String) {
    Text(
        t,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun Line() = HorizontalDivider(
    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
    thickness = 0.5.dp
)

@Composable
private fun Field(
    label: String, value: String, hint: String = "",
    password: Boolean = false, single: Boolean = true, onChange: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(label, fontSize = 12.sp, color = cs.onSurfaceVariant)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = single,
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            textStyle = TextStyle(color = cs.onSurface, fontSize = 15.sp),
            cursorBrush = SolidColor(cs.primary),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(hint, color = cs.onSurfaceVariant.copy(0.6f), fontSize = 15.sp)
                    inner()
                }
            }
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), fontSize = 15.sp)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun Hint(t: String) {
    Text(
        t,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
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
    var agent by remember { mutableStateOf(st.agent) }
    var a11y by remember { mutableStateOf(AgentService.inst != null) }
    var status by remember { mutableStateOf("") }
    var ok by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf(listOf<String>()) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { a11y = AgentService.inst != null }

    fun go(intent: Intent) {
        try { ctx.startActivity(intent) } catch (e: Exception) {
            try { ctx.startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (_: Exception) { }
        }
    }

    Scaffold(
        containerColor = cs.background,
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp, 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleBtn(Icons.Rounded.ArrowBack, "返回") { onBack() }
                Spacer(Modifier.width(12.dp))
                Text("设置", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
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
                                if (model.isBlank() && l.isNotEmpty()) {
                                    model = l.first(); st.model = model; vm.modelLabel = model
                                }
                            } catch (e: Exception) { ok = false; status = "✗ ${e.message}" }
                            busy = false
                        }
                    }
                }
                if (status.isNotEmpty()) {
                    Text(
                        status,
                        color = if (ok) cs.primary else cs.error,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                    )
                }
                if (models.isNotEmpty()) {
                    val shown = (if (models.contains(model)) models else models.filter {
                        model.isBlank() || it.contains(model, true)
                    }).take(30)
                    FlowRow(
                        Modifier.padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        shown.forEach { m ->
                            val sel = m == model
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (sel) cs.primary else cs.surfaceVariant)
                                    .clickable { model = m; st.model = m; vm.modelLabel = m }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(m, fontSize = 12.sp, color = if (sel) cs.onPrimary else cs.onSurface, maxLines = 1)
                            }
                        }
                    }
                }
            }

            item {
                SectionTitle("系统提示词")
                Group {
                    Field("System Prompt", sys, single = false) { sys = it; st.system = it }
                }
                Hint("控制助手人设与行为。Agent 模式下会自动追加工具使用规则。")
            }

            item {
                SectionTitle("Agent 能力")
                Group {
                    SwitchRow("启用 Agent（工具调用）", agent) { agent = it; st.agent = it }
                }
                Hint("开启后可调用读屏、点击、打开应用/网页等。需要先开启无障碍服务。")
            }

            item {
                SectionTitle("无障碍服务")
                Group {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(if (a11y) "已开启" else "未开启", fontSize = 15.sp)
                            Text("用于读屏与手势操作", fontSize = 12.sp, color = cs.onSurfaceVariant)
                        }
                        PillBtn(if (a11y) "已开启" else "去开启") {
                            go(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    }
                }
            }

            item {
                SectionTitle("关于")
                Group {
                    Column(Modifier.padding(14.dp)) {
                        Text("我的助手", fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "已移除语音与无线调试。开启无障碍即可使用 Agent。",
                            fontSize = 12.5.sp,
                            color = cs.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
