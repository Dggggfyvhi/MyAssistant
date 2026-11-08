package com.myassistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object Tools {
    private fun p(type: String, desc: String) = JSONObject().put("type", type).put("description", desc)
    private fun choice(desc: String, vararg v: String) =
        JSONObject().put("type", "string").put("description", desc).put("enum", JSONArray(v.toList()))

    private fun t(name: String, desc: String, props: JSONObject = JSONObject(), req: List<String> = emptyList()) =
        JSONObject().put("type", "function").put(
            "function", JSONObject().put("name", name).put("description", desc).put(
                "parameters", JSONObject().put("type", "object").put("properties", props).put("required", JSONArray(req))
            )
        )

    val defs: JSONArray = JSONArray().apply {
        put(t("read_screen", "读取当前屏幕上的可见元素，返回带编号 [id] 的列表（文字、描述、是否可点击或可输入、中心坐标）。每次操作之后都要调用它确认结果。"))
        put(t("tap", "点击 read_screen 列表里编号对应的元素", JSONObject().put("id", p("integer", "元素编号")), listOf("id")))
        put(t("tap_xy", "点击屏幕坐标（元素列表里没有时使用）",
            JSONObject().put("x", p("integer", "x")).put("y", p("integer", "y")), listOf("x", "y")))
        put(t("input_text", "向编号对应的输入框填入文字，会替换原有内容",
            JSONObject().put("id", p("integer", "输入框编号")).put("text", p("string", "要输入的文字")), listOf("id", "text")))
        put(t("scroll", "滚动屏幕", JSONObject().put("direction", choice("down 表示查看下方内容", "down", "up", "left", "right")), listOf("direction")))
        put(t("press", "按系统按键", JSONObject().put("key", choice("按键", "back", "home", "recents", "notifications")), listOf("key")))
        put(t("open_app", "按名称打开手机上已安装的应用", JSONObject().put("name", p("string", "应用名称")), listOf("name")))
        put(t("open_url", "用浏览器打开网址", JSONObject().put("url", p("string", "完整网址，含 https://")), listOf("url")))
        put(t("wait", "等待若干秒让页面加载", JSONObject().put("seconds", p("integer", "1 到 10")), listOf("seconds")))
    }

    suspend fun run(name: String, argsJson: String): String {
        val svc = AgentService.inst ?: return "无障碍服务未开启，无法操作手机。请让用户先在设置里开启。"
        val a = try { JSONObject(argsJson) } catch (e: Exception) { JSONObject() }
        return try {
            when (name) {
                "read_screen" -> svc.readScreen()
                "tap" -> { val r = svc.tap(a.optInt("id", -1)); delay(900); r }
                "tap_xy" -> { val r = svc.tapXY(a.optInt("x"), a.optInt("y")); delay(900); r }
                "input_text" -> { val r = svc.input(a.optInt("id", -1), a.optString("text")); delay(500); r }
                "scroll" -> { val r = svc.scroll(a.optString("direction", "down")); delay(700); r }
                "press" -> { val r = svc.press(a.optString("key")); delay(900); r }
                "open_app" -> { val r = svc.openApp(a.optString("name")); delay(1800); r }
                "open_url" -> { val r = svc.openUrl(a.optString("url")); delay(2000); r }
                "wait" -> { delay(a.optInt("seconds", 2).coerceIn(1, 10) * 1000L); "已等待" }
                else -> "未知工具：$name"
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { "执行出错：${e.message}" }
    }
}

class AgentService : AccessibilityService() {
    companion object { @Volatile var inst: AgentService? = null }

    private var nodes: List<AccessibilityNodeInfo> = emptyList()
    private val main = Handler(Looper.getMainLooper())
    private var overlay: TextView? = null

    override fun onServiceConnected() { inst = this }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onUnbind(intent: Intent?): Boolean { inst = null; hideOverlay(); return super.onUnbind(intent) }

    fun readScreen(): String {
        val root = rootInActiveWindow ?: return "无法读取当前屏幕（可能是受保护的页面或界面还在切换），稍后再试。"
        val list = ArrayList<AccessibilityNodeInfo>()
        val sb = StringBuilder("当前应用：${root.packageName}\n")
        fun walk(n: AccessibilityNodeInfo) {
            if (list.size >= 120) return
            val t = n.text?.toString()?.replace('\n', ' ')?.take(80).orEmpty()
            val d = n.contentDescription?.toString()?.replace('\n', ' ')?.take(60).orEmpty()
            if (n.isVisibleToUser && (n.isClickable || n.isEditable || n.isScrollable || t.isNotBlank() || d.isNotBlank())) {
                val r = Rect(); n.getBoundsInScreen(r)
                sb.append('[').append(list.size).append("] ").append(n.className?.toString()?.substringAfterLast('.') ?: "")
                if (t.isNotBlank()) sb.append(" \"").append(t).append('"')
                if (d.isNotBlank() && d != t) sb.append(" desc=\"").append(d).append('"')
                if (n.isClickable) sb.append(" 可点击")
                if (n.isEditable) sb.append(" 可输入")
                if (n.isScrollable) sb.append(" 可滚动")
                if (n.isChecked) sb.append(" 已选中")
                sb.append(" @").append(r.centerX()).append(',').append(r.centerY()).append('\n')
                list.add(n)
            }
            for (i in 0 until n.childCount) n.getChild(i)?.let { walk(it) }
        }
        walk(root)
        nodes = list
        if (list.isEmpty()) sb.append("（没有读取到可见元素，可能是自绘界面，可尝试 tap_xy）\n")
        return sb.toString()
    }

    private fun gesture(x1: Float, y1: Float, x2: Float, y2: Float, dur: Long): Boolean {
        val p = Path().apply { moveTo(x1, y1); if (x1 != x2 || y1 != y2) lineTo(x2, y2) }
        val g = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p, 0, dur)).build()
        val latch = CountDownLatch(1)
        var ok = false
        dispatchGesture(g, object : GestureResultCallback() {
            override fun onCompleted(d: GestureDescription?) { ok = true; latch.countDown() }
            override fun onCancelled(d: GestureDescription?) { latch.countDown() }
        }, null)
        latch.await(4, TimeUnit.SECONDS)
        return ok
    }

    fun tap(id: Int): String {
        val n = nodes.getOrNull(id) ?: return "编号 $id 无效，请先调用 read_screen 获取最新列表。"
        var c: AccessibilityNodeInfo? = n
        while (c != null && !c.isClickable) c = c.parent
        if (c != null && c.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return "已点击 [$id]"
        val r = Rect(); n.getBoundsInScreen(r)
        return tapXY(r.centerX(), r.centerY())
    }

    fun tapXY(x: Int, y: Int): String =
        if (gesture(x.toFloat(), y.toFloat(), x.toFloat(), y.toFloat(), 60)) "已点击坐标 ($x,$y)" else "点击失败 ($x,$y)"

    fun input(id: Int, text: String): String {
        val n = nodes.getOrNull(id) ?: return "编号 $id 无效，请先调用 read_screen。"
        n.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val b = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
        return if (n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b)) "已输入文字" else "输入失败，该元素可能不可编辑"
    }

    fun scroll(dir: String): String {
        val m = resources.displayMetrics
        val w = m.widthPixels.toFloat(); val h = m.heightPixels.toFloat()
        val ok = when (dir) {
            "up" -> gesture(w / 2, h * .28f, w / 2, h * .72f, 350)
            "left" -> gesture(w * .2f, h / 2, w * .8f, h / 2, 350)
            "right" -> gesture(w * .8f, h / 2, w * .2f, h / 2, 350)
            else -> gesture(w / 2, h * .72f, w / 2, h * .28f, 350)
        }
        return if (ok) "已向 $dir 滚动" else "滚动失败"
    }

    fun press(key: String): String {
        val a = when (key) {
            "back" -> GLOBAL_ACTION_BACK
            "home" -> GLOBAL_ACTION_HOME
            "recents" -> GLOBAL_ACTION_RECENTS
            "notifications" -> GLOBAL_ACTION_NOTIFICATIONS
            else -> return "未知按键：$key"
        }
        return if (performGlobalAction(a)) "已按下 $key" else "按键失败"
    }

    fun openApp(name: String): String {
        val pm = packageManager
        val list = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        val hit = list.firstOrNull { it.loadLabel(pm).toString().contains(name, true) }
            ?: list.firstOrNull { it.activityInfo.packageName.contains(name, true) }
            ?: return "没有找到应用“$name”。可以改用 open_url 在浏览器里打开对应网址。"
        val i = pm.getLaunchIntentForPackage(hit.activityInfo.packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?: return "无法启动该应用"
        startActivity(i)
        return "已打开 ${hit.loadLabel(pm)}"
    }

    fun openUrl(url: String): String {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return "已用浏览器打开 $url"
    }

    fun showOverlay(onStop: () -> Unit) = main.post {
        if (overlay != null) return@post
        val tv = TextView(this).apply {
            text = "●  助手执行中，点按停止"
            setTextColor(Color.WHITE); textSize = 13f
            setPadding(44, 24, 44, 24)
            background = GradientDrawable().apply { cornerRadius = 100f; setColor(0xE6111111.toInt()) }
            setOnClickListener { onStop() }
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = 140 }
        try { (getSystemService(WINDOW_SERVICE) as WindowManager).addView(tv, lp); overlay = tv } catch (e: Exception) { }
    }

    fun hideOverlay() = main.post {
        overlay?.let { try { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it) } catch (e: Exception) { } }
        overlay = null
    }
}
