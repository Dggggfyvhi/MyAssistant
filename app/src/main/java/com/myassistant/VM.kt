package com.myassistant

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class VM(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    val msgs = mutableStateListOf<Msg>().also { it.addAll(store.loadChat()) }
    val profiles = mutableStateListOf<Profile>().also { it.addAll(store.loadProfiles()) }
    var active by mutableStateOf(store.active)
    var busy by mutableStateOf(false)
    private var job: Job? = null
    @Volatile private var stopped = false
    private var seq = System.currentTimeMillis()
    private fun nid() = ++seq

    fun activeProfile(): Profile? = profiles.firstOrNull { it.id == active }
    private fun activeProvider(): Provider? = activeProfile()?.let { Provider(it.name, Api.urlOf(it.base), it.key, it.model) }

    fun select(id: String) { active = id; store.active = id }

    fun saveProfile(p: Profile) {
        val i = profiles.indexOfFirst { it.id == p.id }
        if (i >= 0) profiles[i] = p else profiles.add(p)
        store.saveProfiles(profiles.toList())
        select(p.id)
    }

    fun deleteProfile(p: Profile) {
        profiles.remove(p)
        store.saveProfiles(profiles.toList())
        if (active == p.id) select(FREE)
    }

    fun newChat() { stop(); msgs.clear(); store.saveChat(emptyList()) }

    fun stop() { stopped = true; job?.cancel(); Api.call?.cancel() }

    fun send(text: String) {
        val q = text.trim()
        if (q.isEmpty() || busy) return
        msgs.add(Msg(nid(), "user", q))
        run()
    }

    fun regenerate() {
        if (busy) return
        if (msgs.lastOrNull()?.role == "assistant") msgs.removeAt(msgs.lastIndex)
        if (msgs.lastOrNull()?.role == "user") run()
    }

    private fun upd(id: Long, f: (Msg) -> Msg) {
        val i = msgs.indexOfLast { it.id == id }
        if (i >= 0) msgs[i] = f(msgs[i])
    }

    /** 兼容把思考过程写在 <think>…</think> 里的模型。返回（思考，正文）。 */
    private fun split(raw: String): Pair<String, String> {
        val s = raw.indexOf("<think>")
        if (s < 0) return "" to raw
        val e = raw.indexOf("</think>", s)
        return if (e < 0) raw.substring(s + 7) to raw.substring(0, s)
        else raw.substring(s + 7, e) to (raw.substring(0, s) + raw.substring(e + 8))
    }

    private fun run() {
        busy = true; stopped = false
        val id = nid()
        val history = msgs.filter { !it.error && it.content.isNotBlank() && (it.role == "user" || it.role == "assistant") }
            .map { it.role to it.content }
        msgs.add(Msg(id, "assistant", ""))
        val custom = activeProvider()
        val list = if (custom != null) listOf(custom) else Free.list
        job = viewModelScope.launch(Dispatchers.IO) {
            var thinkStart = 0L
            var thinkMs = 0L
            var lastUi = 0L
            var got = false
            var ok = false
            var err: Throwable? = null

            fun push(c: String, r: String, force: Boolean) {
                val now = System.currentTimeMillis()
                val (r2, a2) = split(c)
                val reasoning = (r + r2).trim()
                val answer = a2.trimStart()
                if (reasoning.isNotEmpty() && thinkStart == 0L) thinkStart = now
                if (answer.isNotEmpty() && thinkStart != 0L && thinkMs == 0L) thinkMs = now - thinkStart
                if (!force && now - lastUi < 45) return
                lastUi = now
                upd(id) { it.copy(content = answer, reasoning = reasoning, thinking = reasoning.isNotEmpty() && answer.isEmpty(), thinkMs = thinkMs) }
            }

            for (p in list) {
                try {
                    val (c, r) = Api.stream(p, history) { c, r -> got = true; push(c, r, false) }
                    push(c, r, true)
                    ok = true
                    break
                } catch (e: Throwable) {
                    err = e
                    if (got || stopped) break
                }
            }

            if (!ok && !got && !stopped) {
                val why = err?.message.orEmpty().take(120)
                val text = if (custom != null) "请求失败：$why" else "免费通道暂时不可用（$why）。点左上角菜单，添加你自己的 API。"
                upd(id) { it.copy(error = true, content = text, thinking = false) }
            } else {
                if (thinkStart != 0L && thinkMs == 0L) thinkMs = System.currentTimeMillis() - thinkStart
                upd(id) { it.copy(thinking = false, thinkMs = thinkMs) }
                val m = msgs.firstOrNull { it.id == id }
                if (m != null && m.content.isBlank() && m.reasoning.isBlank()) msgs.remove(m)
            }
            busy = false
            store.saveChat(msgs.toList())
        }
    }
}
