package com.myassistant

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.isSystemInDarkTheme
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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

// ───────────────────────── Theme & Glass ─────────────────────────

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = true // Prefer dark for liquid glass
    val cs = if (dark) darkColorScheme(
        primary = Color(0xFF8AB4F8),
        onPrimary = Color(0xFF0B1C2E),
        secondary = Color(0xFF81C995),
        background = Color(0xFF0D0E12),
        surface = Color(0xFF16181F),
        surfaceVariant = Color(0xFF1E2029),
        onBackground = Color(0xFFE8EAED),
        onSurface = Color(0xFFE8EAED),
        onSurfaceVariant = Color(0xFF9AA0A6),
        outline = Color(0xFF3C4043),
        outlineVariant = Color(0xFF2A2D35),
        error = Color(0xFFF28B82)
    ) else lightColorScheme()
    MaterialTheme(colorScheme = cs, content = content)
}

val LocalHaze = compositionLocalOf<HazeState?> { null }

@Composable
fun Modifier.glass(shape: Shape = RoundedCornerShape(20.dp)): Modifier {
    val haze = LocalHaze.current
    val tint = Color(0xFF1E2029).copy(alpha = 0.55f)
    val edge = Brush.linearGradient(
        listOf(Color.White.copy(0.18f), Color.White.copy(0.04f), Color.White.copy(0.12f))
    )
    var m = this
        .shadow(elevation = 12.dp, shape = shape, ambientColor = Color(0x33000000), spotColor = Color(0x44000000))
        .clip(shape)
    if (haze != null) {
        m = m.hazeEffect(state = haze, style = dev.chrisbanes.haze.HazeStyle(
            backgroundColor = tint,
            tint = null,
            blurRadius = 24.dp,
            noiseFactor = 0.02f
        ))
    } else {
        m = m.background(tint)
    }
    return m.border(0.6.dp, edge, shape)
}

@Composable
fun Ambient() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        fun blob(cx: Float, cy: Float, r: Float, c: Color) {
            drawCircle(brush = Brush.radialGradient(listOf(c, Color.Transparent), center = Offset(cx, cy), radius = r), radius = r, center = Offset(cx, cy))
        }
        blob(w * 0.15f, h * 0.12f, w * 0.55f, Color(0xFF3B6FE8).copy(0.22f))
        blob(w * 0.85f, h * 0.25f, w * 0.48f, Color(0xFF8B5CF6).copy(0.18f))
        blob(w * 0.5f, h * 0.85f, w * 0.6f, Color(0xFF06B6D4).copy(0.12f))
    }
}

@Composable
fun CircleBtn(
    icon: ImageVector, contentDesc: String, size: Dp = 44.dp,
    solid: Color? = null, fg: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit
) {
    val base = Modifier.size(size)
    val m = if (solid == null) base.glass(CircleShape) else base.clip(CircleShape).background(solid)
    Box(m.clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDesc, tint = fg, modifier = Modifier.size(size * 0.48f))
    }
}

@Composable
fun PillBtn(text: String, modifier: Modifier = Modifier, filled: Boolean = false, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier
            .then(if (filled) Modifier.background(cs.primary, shape) else Modifier.glass(shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
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
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(cs.onSurface.copy(alpha = 0.07f))
                        .horizontalScroll(rememberScrollState()).padding(12.dp)
                ) { Text(code, fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 19.sp) }
            } else if (part.isNotBlank()) {
                Text(mdInline(part.trim('\n'), cs.onSurface.copy(alpha = 0.10f)), fontSize = 15.5.sp, lineHeight = 24.sp)
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
            Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = a)))
        }
    }
}

// ───────────────────────── Message Bubbles ─────────────────────────

@Composable
fun ThinkingCard(status: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cs.surfaceVariant.copy(alpha = 0.7f))
            .border(0.5.dp, cs.outlineVariant.copy(0.6f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dots()
        Spacer(Modifier.width(12.dp))
        Text(
            if (status.isNotBlank()) status else "思考中…",
            fontSize = 13.5.sp,
            color = cs.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun Bubble(m: JSONObject) {
    val cs = MaterialTheme.colorScheme
    val text = m.optString("content")
    val role = m.optString("role")
    when (role) {
        "user" -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            val shape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)
            Box(
                Modifier
                    .padding(start = 48.dp)
                    .clip(shape)
                    .background(Brush.linearGradient(listOf(Color(0xFF5B8DEF), Color(0xFF3B6FE8))))
                    .border(0.5.dp, Color.White.copy(alpha = 0.25f), shape)
            ) {
                Text(
                    text,
                    Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                    color = Color.White,
                    fontSize = 15.5.sp,
                    lineHeight = 23.sp
                )
            }
        }
        "assistant" -> if (text.isNotBlank()) {
            Column(Modifier.fillMaxWidth().padding(end = 24.dp)) {
                // Optional reasoning / thinking block if model put it in content with markers
                val thinkMatch = Regex("""(?s)<think>(.*?)</think>|<reasoning>(.*?)</reasoning>""").find(text)
                val mainText = if (thinkMatch != null) {
                    text.replace(thinkMatch.value, "").trim()
                } else text
                if (thinkMatch != null) {
                    val thinkBody = (thinkMatch.groupValues.getOrNull(1) ?: thinkMatch.groupValues.getOrNull(2) ?: "").trim()
                    if (thinkBody.isNotBlank()) {
                        var expanded by remember { mutableStateOf(false) }
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(cs.surfaceVariant.copy(0.5f))
                                .border(0.5.dp, cs.outlineVariant.copy(0.5f), RoundedCornerShape(14.dp))
                                .clickable { expanded = !expanded }
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Psychology, null, tint = cs.primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("思考过程", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = cs.primary)
                                Spacer(Modifier.weight(1f))
                                Icon(
                                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp)
                                )
                            }
                            if (expanded) {
                                Spacer(Modifier.height(8.dp))
                                Text(thinkBody, fontSize = 13.sp, color = cs.onSurfaceVariant, lineHeight = 20.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                SelectionContainer { Markdown(mainText) }
            }
        }
        "tool" -> Row(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(cs.surface.copy(alpha = 0.75f))
                .border(0.5.dp, cs.outlineVariant, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(cs.secondary))
            Spacer(Modifier.width(8.dp))
            Text(
                Tools.label(m.optString("name")) + " · " + text.replace('\n', ' ').take(56),
                fontSize = 12.sp, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DrawerItem(icon: ImageVector, label: String, selected: Boolean = false, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val bg = if (selected) cs.primary.copy(0.15f) else Color.Transparent
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = if (selected) cs.primary else cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 15.sp, color = if (selected) cs.primary else cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ───────────────────────── Home (ChatGPT-style) ─────────────────────────

@Composable
fun Home(vm: VM, openSettings: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val haze = remember { HazeState() }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    val topPad = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val botPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    CompositionLocalProvider(LocalHaze provides haze) {
        ModalNavigationDrawer(
            drawerState = drawer,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = Color.Transparent,
                    drawerContentColor = cs.onBackground,
                    modifier = Modifier.fillMaxHeight().widthIn(max = 300.dp)
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize().hazeSource(haze)) { Ambient() }
                        Column(
                            Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(16.dp)
                        ) {
                            Text("对话", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                            PillBtn("＋ 新对话", filled = true, modifier = Modifier.fillMaxWidth()) {
                                vm.newChat()
                                scope.launch { drawer.close() }
                            }
                            Spacer(Modifier.height(16.dp))
                            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                            Spacer(Modifier.height(12.dp))
                            DrawerItem(Icons.Rounded.Settings, "设置") {
                                openSettings()
                                scope.launch { drawer.close() }
                            }
                        }
                    }
                }
            }
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().hazeSource(haze)) { Ambient() }
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    // Top bar
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
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

                    // Messages
                    val msgs = vm.cur.msgs
                    val ls = rememberLazyListState()
                    val last = msgs.lastOrNull()
                    val thinking = vm.busy && (last == null || last.optString("role") != "assistant" || last.optString("content").isBlank())

                    LaunchedEffect(msgs.size, vm.busy, vm.status) {
                        if (msgs.isNotEmpty() || thinking) {
                            ls.animateScrollToItem(maxOf(0, msgs.size + if (thinking) 0 else -1))
                        }
                    }

                    LazyColumn(
                        state = ls,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (msgs.isEmpty() && !vm.busy) {
                            item {
                                Column(
                                    Modifier.fillMaxWidth().padding(top = 48.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("我的助手", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "配置好模型后，可以让我打开网页、搜索信息，\n或用 Agent 操控手机完成任务。",
                                        textAlign = TextAlign.Center,
                                        color = cs.onSurfaceVariant,
                                        fontSize = 14.sp,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }
                        items(msgs, key = { it.toString().hashCode() }) { m ->
                            Bubble(m)
                        }
                        if (thinking) {
                            item { ThinkingCard(vm.status) }
                        }
                    }

                    // Status / error
                    vm.error?.let {
                        Text(it, color = cs.error, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    }

                    // Input bar — GPT style glass
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .glass(RoundedCornerShape(28.dp))
                            .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(color = cs.onSurface, fontSize = 16.sp),
                            cursorBrush = SolidColor(cs.primary),
                            maxLines = 6,
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
                            modifier = Modifier.size(42.dp)
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
        }
    }
}

// ───────────────────────── Settings helpers ─────────────────────────

@Composable
private fun Group(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.55f))
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.5f), RoundedCornerShape(18.dp))
            .padding(4.dp),
        content = content
    )
}

@Composable
private fun SectionTitle(t: String) {
    Text(t, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp))
}

@Composable
private fun Line() = HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 0.5.dp)

@Composable
private fun Field(label: String, value: String, hint: String = "", password: Boolean = false, single: Boolean = true, onChange: (String) -> Unit) {
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
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 15.sp)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun Hint(t: String) {
    Text(t, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.5.sp, lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
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
    var agent by remember { mutableStateOf(st.agent) }
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
                Row(Modifier.fillMaxWidth().padding(16.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircleBtn(Icons.Rounded.ArrowBack, "返回") { onBack() }
                    Spacer(Modifier.width(12.dp))
                    Text("设置", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
                LazyColumn(
                    Modifier.weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
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
                        Hint("开启后模型可调用读屏、点击、打开应用/网页、滚动等工具。无障碍或无线调试任一可用即可。")
                    }

                    item {
                        SectionTitle("无障碍服务（推荐）")
                        Group {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
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
                        SectionTitle("无线调试（ADB）")
                        Group {
                            Column(Modifier.padding(14.dp)) {
                                Text(if (adbOn) "已连接" else "未连接", fontSize = 15.sp)
                                Spacer(Modifier.height(8.dp))
                                Field("配对端口", pairPort, "从开发者选项查看") { pairPort = it }
                                Field("配对码", pairCode) { pairCode = it }
                                Field("连接端口", connPort) { connPort = it }
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PillBtn(if (adbBusy) "…" else "配对") {
                                        if (adbBusy) return@PillBtn
                                        adbBusy = true; adbMsg = "配对中…"
                                        scope.launch {
                                            adbMsg = withContext(Dispatchers.IO) {
                                                try {
                                                    AdbMgr.pair(ctx, "127.0.0.1", pairPort.toIntOrNull() ?: 0, pairCode)
                                                    "配对成功"
                                                } catch (e: Exception) { "配对失败: ${e.message}" }
                                            }
                                            adbBusy = false
                                        }
                                    }
                                    PillBtn(if (adbBusy) "…" else "连接") {
                                        if (adbBusy) return@PillBtn
                                        adbBusy = true; adbMsg = "连接中…"
                                        scope.launch {
                                            adbMsg = withContext(Dispatchers.IO) {
                                                try {
                                                    AdbMgr.connect(ctx, "127.0.0.1", connPort.toIntOrNull() ?: 0)
                                                    adbOn = AdbMgr.isUp()
                                                    if (adbOn) "已连接" else "连接失败"
                                                } catch (e: Exception) { "连接失败: ${e.message}" }
                                            }
                                            adbBusy = false
                                        }
                                    }
                                }
                                if (adbMsg.isNotEmpty()) Text(adbMsg, fontSize = 13.sp, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                        Hint("开发者选项 → 无线调试 → 使用配对码配对设备。分屏打开本应用可自动发现端口。")
                    }

                    item {
                        SectionTitle("关于")
                        Group {
                            Column(Modifier.padding(14.dp)) {
                                Text("我的助手 · 纯 AI 聊天 + 伪 Agent", fontSize = 15.sp)
                                Spacer(Modifier.height(4.dp))
                                Text("已移除不稳定的语音识别。界面采用液态玻璃 + 深色 GPT 风格。", fontSize = 12.5.sp, color = cs.onSurfaceVariant, lineHeight = 18.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("桌面/Linux 更强 Agent 可参考 DeepSeek Harness (DSH)。手机端继续用无障碍或 ADB。", fontSize = 12.5.sp, color = cs.onSurfaceVariant, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
