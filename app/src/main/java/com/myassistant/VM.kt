package com.myassistant

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.coroutineContext

fun msg(role: String, content: String): JSONObject = JSONObject().put("role", role).put("content", content)

class VM(app: Application) : AndroidViewModel(app) {
    val store = Store(app)
    val chats = mutableStateListOf<Chat>().also { it.addAll(store.loadChats()) }
    var cur by mutableStateOf(Chat(newId(), ""))
    var modelLabel by mutableStateOf(store.model)
    var busy by mutableStateOf(false)
    var status by mutableStateOf("")
    var error by mutableStateOf<String?>(null)
    private var job: Job? = null

    private fun newId() = System.currentTimeMillis().toString() + (0..999).random()

    fun newChat() { if (!busy) { cur = Chat(newId(), ""); error = null } }
    fun open(c: Chat) { if (!busy) { cur = c; error = null } }
    fun delete(c: Chat) {
        if (busy) return
        chats.remove(c)
        if (cur === c) cur = Chat(newId(), "")
        store.saveChats(chats)
    }

    fun stop() { job?.cancel(); Api.call?.cancel() }

    /** done 总会被调用：成功时返回最终回复，失败或停止时返回 null。 */
    fun send(text: String, done: (String?) -> Unit = {}) {
        val q = text.trim()
        if (busy || q.isEmpty()) { done(null); return }
        if (!store.configured()) {
            error = "请先在设置里填写 API 地址、Key 和模型"
            done(null); return
        }
        val c = cur
        if (c.msgs.isEmpty()) {
            c.title = q.take(24)
            if (c !in chats) chats.add(0, c)
        }
        c.msgs.add(msg("user", q))
        busy = true; error = null
        job = viewModelScope.launch(Dispatchers.IO) {
            var out: String? = null
            try {
                val tools = if (store.agent) Tools.defs() else null
                var step = 0
                while (true) {
                    coroutineContext.ensureActive()
                    val i = c.msgs.size
                    status = "思考中"
                    c.msgs.add(msg("assistant", ""))
                    val r = Api.stream(store.baseUrl, store.apiKey, store.model, request(c), tools) {
                        c.msgs[i] = msg("assistant", it)
                    }
                    if (r.calls.isEmpty()) {
                        c.msgs[i] = msg("assistant", r.text)
                        out = r.text
                        break
                    }
                    val arr = JSONArray()
                    r.calls.forEach {
                        val o = JSONObject().put("id", it.id).put("type", "function")
                            .put("function", JSONObject().put("name", it.name).put("arguments", it.args))
                        if (it.extra != null) o.put("extra_content", it.extra)
                        arr.put(o)
                    }
                    c.msgs[i] = msg("assistant", r.text).put("tool_calls", arr)
                    AgentService.inst?.showOverlay { stop() }
                    for (call in r.calls) {
                        coroutineContext.ensureActive()
                        status = Tools.label(call.name)
                        val res = Tools.run(call.name, call.args)
                        c.msgs.add(JSONObject().put("role", "tool").put("tool_call_id", call.id)
                            .put("name", call.name).put("content", res))
                    }
                    if (++step >= 30) {
                        c.msgs.add(msg("assistant", "已达到单次任务的步数上限，先停在这里。"))
                        out = "已达到单次任务的步数上限"
                        break
                    }
                }
            } catch (e: Exception) {
                out = null
                val m = e.message.orEmpty()
                error = if (e is CancellationException || m.contains("Canceled", true) || m.contains("closed", true)) "已停止"
                else m.ifBlank { "请求失败" }
            } finally {
                AgentService.inst?.hideOverlay()
                val last = c.msgs.lastOrNull()
                if (last != null && last.optString("role") == "assistant" &&
                    last.optString("content").isBlank() && !last.has("tool_calls")
                ) c.msgs.removeAt(c.msgs.lastIndex)
                busy = false
                status = ""
                store.saveChats(chats)
                withContext(NonCancellable + Dispatchers.Main) { done(out) }
            }
        }
    }

    /** 组装发给模型的消息：补上系统提示、丢掉不完整的工具调用、压缩较早的工具结果。 */
    private fun request(c: Chat): List<JSONObject> {
        val src = c.msgs.toList()
        val have = src.filter { it.optString("role") == "tool" }.map { it.optString("tool_call_id") }.toSet()
        val valid = mutableSetOf<String>()
        val lastTool = src.indexOfLast { it.optString("role") == "tool" }
        val out = mutableListOf(msg("system", store.system))
        src.forEachIndexed { i, m ->
            when (m.optString("role")) {
                "assistant" -> {
                    val tc = m.optJSONArray("tool_calls")
                    if (tc != null) {
                        val ids = (0 until tc.length()).map { tc.getJSONObject(it).getString("id") }
                        if (have.containsAll(ids)) { valid.addAll(ids); out.add(m) }
                    } else if (m.optString("content").isNotBlank()) out.add(m)
                }
                "tool" -> if (m.optString("tool_call_id") in valid) {
                    val body = m.optString("content")
                    out.add(if (i < lastTool - 2 && body.length > 300)
                        JSONObject(m.toString()).put("content", body.take(200) + "…（已省略）") else m)
                }
                else -> out.add(m)
            }
        }
        return out
    }
}
