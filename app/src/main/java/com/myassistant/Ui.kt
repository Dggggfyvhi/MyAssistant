@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.myassistant

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ───────────── 主题：跟随手机的动态取色（Material You），低版本用谷歌蓝 ─────────────

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val cs = when {
        Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme(
            primary = Color(0xFFA8C7FA), onPrimary = Color(0xFF062E6F),
            primaryContainer = Color(0xFF0842A0), onPrimaryContainer = Color(0xFFD3E3FD),
            background = Color(0xFF131314), onBackground = Color(0xFFE3E3E3),
            surface = Color(0xFF131314), onSurface = Color(0xFFE3E3E3),
            surfaceContainer = Color(0xFF1E1F20), surfaceContainerHigh = Color(0xFF282A2C),
            onSurfaceVariant = Color(0xFFC4C7C5), outlineVariant = Color(0xFF444746)
        )
        else -> lightColorScheme(
            primary = Color(0xFF0B57D0), onPrimary = Color.White,
            primaryContainer = Color(0xFFD3E3FD), onPrimaryContainer = Color(0xFF041E49),
            background = Color(0xFFFFFFFF), onBackground = Color(0xFF1F1F1F),
            surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1F1F1F),
            surfaceContainer = Color(0xFFF0F4F9), surfaceContainerHigh = Color(0xFFE9EEF6),
            onSurfaceVariant = Color(0xFF444746), outlineVariant = Color(0xFFC4C7C5)
        )
    }
    val type = Typography(
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 26.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp)
    )
    MaterialTheme(colorScheme = cs, typography = type, content = content)
}

// ───────────── Markdown（精简：标题、粗体、行内代码、列表、代码块） ─────────────

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
    val clip = LocalClipboardManager.current
    val ctx = LocalContext.current
    val parts = text.split("```")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        parts.forEachIndexed { idx, part ->
            if (idx % 2 == 1) {
                val code = part.substringAfter('\n', part).trimEnd()
                Surface(shape = RoundedCornerShape(16.dp), color = cs.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(part.substringBefore('\n', "").trim().ifBlank { "代码" }, Modifier.weight(1f), fontSize = 12.sp, color = cs.onSurfaceVariant)
                            IconButton(onClick = { clip.setText(AnnotatedString(code)); Toast.makeText(ctx, "已复制", Toast.LENGTH_SHORT).show() }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Rounded.ContentCopy, "复制", Modifier.size(16.dp), tint = cs.onSurfaceVariant)
                            }
                        }
                        Text(
                            code, fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 19.sp,
                            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                        )
                    }
                }
            } else if (part.isNotBlank()) {
                Text(mdInline(part.trim('\n'), cs.onSurface.copy(alpha = 0.10f)), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

// ───────────── 消息组件 ─────────────

@Composable
fun Dots() {
    val t = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(vertical = 10.dp)) {
        for (i in 0..2) {
            val a by t.animateFloat(
                initialValue = 0.25f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(600, delayMillis = i * 160), RepeatMode.Reverse), label = "d$i"
            )
            Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = a)))
        }
    }
}

/** 思考过程小卡片：思考中显示进度和最新一行；完成后显示用时，点开看全文。 */
@Composable
fun ThinkCard(m: Msg) {
    val cs = MaterialTheme.colorScheme
    var open by rememberSaveable(m.id) { mutableStateOf(false) }
    val secs = (m.thinkMs / 1000).toInt()
    Surface(
        shape = RoundedCornerShape(18.dp), color = cs.surfaceContainer,
        modifier = Modifier.fillMaxWidth().animateContentSize().clip(RoundedCornerShape(18.dp)).clickable { open = !open }
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (m.thinking) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Rounded.Psychology, null, Modifier.size(20.dp), tint = cs.primary)
                Spacer(Modifier.width(10.dp))
                Text(
                    if (m.thinking) "正在思考…" else if (secs > 0) "已思考 $secs 秒" else "思考过程",
                    Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant
                )
                Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = cs.onSurfaceVariant)
            }
            if (open) {
                Spacer(Modifier.height(8.dp))
                Text(
                    m.reasoning, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
                    modifier = Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())
                )
            } else if (m.thinking && m.reasoning.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    m.reasoning.takeLast(90).replace('\n', ' '), style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MessageItem(m: Msg, isLast: Boolean, busy: Boolean, onCopy: (String) -> Unit, onRegen: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    if (m.role == "user") {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Surface(
                color = cs.primaryContainer, contentColor = cs.onPrimaryContainer,
                shape = RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp), modifier = Modifier.padding(start = 48.dp)
            ) {
                SelectionContainer { Text(m.content, Modifier.padding(horizontal = 16.dp, vertical = 11.dp), style = MaterialTheme.typography.bodyLarge) }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (m.reasoning.isNotBlank() || m.thinking) ThinkCard(m)
            if (m.error) {
                Surface(shape = RoundedCornerShape(18.dp), color = cs.errorContainer, contentColor = cs.onErrorContainer, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(m.content, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onRegen) { Text("重试") }
                    }
                }
            } else if (m.content.isNotBlank()) {
                SelectionContainer { Markdown(m.content) }
            } else if (busy && isLast && m.reasoning.isBlank()) {
                Dots()
            }
            if (!m.error && m.content.isNotBlank() && !(busy && isLast)) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = { onCopy(m.content) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.ContentCopy, "复制", Modifier.size(18.dp), tint = cs.onSurfaceVariant)
                    }
                    if (isLast) IconButton(onClick = onRegen, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Refresh, "重新生成", Modifier.size(20.dp), tint = cs.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ───────────── 侧边栏：只管 API ─────────────

@Composable
private fun ApiCard(title: String, sub: String, selected: Boolean, onClick: () -> Unit, onEdit: (() -> Unit)? = null, onDelete: (() -> Unit)? = null) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) cs.secondaryContainer else cs.surfaceContainer,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick)
            Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (onEdit != null) IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "编辑", Modifier.size(20.dp)) }
            if (onDelete != null) IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, "删除", Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun ApiEditor(init: Profile, onSave: (Profile) -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(init.name) }
    var base by remember { mutableStateOf(init.base) }
    var key by remember { mutableStateOf(init.key) }
    var model by remember { mutableStateOf(init.model) }
    var models by remember { mutableStateOf(listOf<String>()) }
    var status by remember { mutableStateOf("") }
    var ok by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)

    Column(
        Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(if (init.id.isBlank()) "添加 API" else "编辑 API", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("名称（可不填）") }, singleLine = true, shape = shape)
        OutlinedTextField(base, { base = it }, Modifier.fillMaxWidth(), label = { Text("API 地址") }, placeholder = { Text("https://api.example.com/v1") }, singleLine = true, shape = shape)
        OutlinedTextField(key, { key = it }, Modifier.fillMaxWidth(), label = { Text("API Key") }, singleLine = true, shape = shape, visualTransformation = PasswordVisualTransformation())
        OutlinedTextField(model, { model = it }, Modifier.fillMaxWidth(), label = { Text("模型") }, singleLine = true, shape = shape)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(onClick = {
                if (busy) return@FilledTonalButton
                busy = true; status = "获取中…"; ok = true
                scope.launch {
                    try {
                        val l = withContext(Dispatchers.IO) { Api.models(base, key) }
                        models = l; status = "✓ 获取到 ${l.size} 个模型"
                        if (model.isBlank() && l.isNotEmpty()) model = l.first()
                    } catch (e: Exception) { ok = false; status = "✗ ${e.message}" }
                    busy = false
                }
            }) { Text("获取模型列表") }
            OutlinedButton(onClick = {
                if (busy) return@OutlinedButton
                if (model.isBlank()) { ok = false; status = "✗ 请先填写模型，或点“获取模型列表”"; return@OutlinedButton }
                busy = true; status = "测试中…"; ok = true
                scope.launch {
                    val r = withContext(Dispatchers.IO) { Api.test(Provider("", Api.urlOf(base), key, model)) }
                    ok = r.startsWith("✓"); status = r; busy = false
                }
            }) { Text("测试连接") }
        }
        if (status.isNotEmpty()) Text(status, color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        if (models.isNotEmpty()) {
            val shown = (if (models.contains(model)) models else models.filter { model.isBlank() || it.contains(model, true) }).take(40)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                shown.forEach { m -> FilterChip(selected = m == model, onClick = { model = m }, label = { Text(m) }) }
            }
        }
        Button(
            onClick = {
                val host = base.trim().removePrefix("https://").removePrefix("http://").substringBefore('/')
                onSave(Profile(init.id.ifBlank { System.currentTimeMillis().toString() }, name.trim().ifBlank { host.ifBlank { "我的 API" } }, base.trim(), key.trim(), model.trim()))
            },
            enabled = base.isNotBlank() && model.isNotBlank(), modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("保存并使用") }
    }
}

// ───────────── 主页 ─────────────

@Composable
fun HomeScreen(vm: VM) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val scroll = TopAppBarDefaults.pinnedScrollBehavior()
    val listState = rememberLazyListState()
    var editing by remember { mutableStateOf<Profile?>(null) }
    var text by remember { mutableStateOf("") }
    var follow by remember { mutableStateOf(true) }
    val msgs = vm.msgs
    val tail = msgs.lastOrNull()

    BackHandler(drawer.isOpen) { scope.launch { drawer.close() } }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress to !listState.canScrollForward }.collect { (dragging, bottom) -> if (dragging) follow = bottom }
    }
    LaunchedEffect(msgs.size) { follow = true; if (msgs.isNotEmpty()) listState.animateScrollToItem(msgs.size) }
    LaunchedEffect(tail?.content?.length, tail?.reasoning?.length) {
        if (follow && vm.busy && msgs.isNotEmpty()) listState.scrollToItem(msgs.size)
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp).padding(bottom = 16.dp)) {
                    Text("API", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp, 20.dp, 16.dp, 12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ApiCard("免费体验", "匿名公共接口 · 无需配置", vm.active == FREE, onClick = { vm.select(FREE); scope.launch { drawer.close() } })
                        vm.profiles.forEach { p ->
                            ApiCard(
                                p.name, p.model + " · " + p.base.removePrefix("https://").removePrefix("http://").substringBefore('/'),
                                vm.active == p.id,
                                onClick = { vm.select(p.id); scope.launch { drawer.close() } },
                                onEdit = { editing = p }, onDelete = { vm.deleteProfile(p) }
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    FilledTonalButton(onClick = { editing = Profile("", "", "", "", "") }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("添加 API")
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scroll.nestedScrollConnection).imePadding(),
            containerColor = cs.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("我的助手", style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = { IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Rounded.Menu, "菜单") } },
                    actions = { IconButton(onClick = { vm.newChat() }) { Icon(Icons.Rounded.Edit, "新聊天") } },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = cs.background, scrolledContainerColor = cs.surfaceContainer),
                    scrollBehavior = scroll
                )
            },
            bottomBar = {
                Surface(color = cs.background, modifier = Modifier.navigationBarsPadding()) {
                    Surface(
                        shape = RoundedCornerShape(28.dp), color = cs.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(Modifier.padding(start = 20.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
                            BasicTextField(
                                value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f).padding(bottom = 2.dp),
                                textStyle = TextStyle(color = cs.onSurface, fontSize = 16.sp, lineHeight = 24.sp),
                                cursorBrush = SolidColor(cs.primary), maxLines = 6,
                                decorationBox = { inner ->
                                    Box(Modifier.heightIn(min = 44.dp), contentAlignment = Alignment.CenterStart) {
                                        if (text.isEmpty()) Text("发消息", color = cs.onSurfaceVariant, fontSize = 16.sp)
                                        inner()
                                    }
                                }
                            )
                            FilledIconButton(
                                onClick = { if (vm.busy) vm.stop() else { vm.send(text); text = "" } },
                                enabled = vm.busy || text.isNotBlank(), modifier = Modifier.size(44.dp)
                            ) { Icon(if (vm.busy) Icons.Rounded.Stop else Icons.Rounded.ArrowUpward, if (vm.busy) "停止" else "发送") }
                        }
                    }
                }
            }
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                if (msgs.isEmpty()) {
                    Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(64.dp).clip(CircleShape).background(cs.primaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(32.dp), tint = cs.onPrimaryContainer)
                        }
                        Spacer(Modifier.height(18.dp))
                        Text("我的助手", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(12.dp))
                        AssistChip(
                            onClick = { scope.launch { drawer.open() } },
                            label = { Text(if (vm.activeProfile() == null) "免费体验 · 点此添加你的 API" else vm.activeProfile()!!.name) }
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        items(msgs, key = { it.id }) { m ->
                            Box(Modifier.animateItem()) {
                                MessageItem(
                                    m, isLast = m.id == tail?.id, busy = vm.busy,
                                    onCopy = { clip.setText(AnnotatedString(it)); Toast.makeText(ctx, "已复制", Toast.LENGTH_SHORT).show() },
                                    onRegen = { vm.regenerate() }
                                )
                            }
                        }
                        item(key = "end") { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }

    editing?.let { p ->
        ModalBottomSheet(onDismissRequest = { editing = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            ApiEditor(p) { vm.saveProfile(it); editing = null; scope.launch { drawer.close() } }
        }
    }
}
