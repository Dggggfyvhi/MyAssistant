package com.myassistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

const val FREE = "free"

data class Profile(val id: String, val name: String, val base: String, val key: String, val model: String)

data class Msg(
    val id: Long,
    val role: String,
    val content: String,
    val reasoning: String = "",
    val thinkMs: Long = 0L,
    val thinking: Boolean = false,
    val error: Boolean = false
)

class Provider(val name: String, val url: String, val key: String, val model: String)

/** 没配置 API 时的临时兜底：匿名、无需密钥的公共 OpenAI 兼容接口，按顺序尝试。 */
object Free {
    val list = listOf(
        Provider("Pollinations", "https://text.pollinations.ai/openai", "", "openai-fast"),
        Provider("Pollinations", "https://text.pollinations.ai/openai", "", "openai"),
        Provider("Airforce", "https://api.airforce/v1/chat/completions", "", "grok-4.1-mini:free"),
        Provider("Airforce", "https://api.airforce/v1/chat/completions", "", "step-3.5-flash:free")
    )
}

class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("my_assistant", Context.MODE_PRIVATE)

    var active: String
        get() = sp.getString("active", FREE) ?: FREE
        set(v) { sp.edit().putString("active", v).apply() }

    fun loadProfiles(): List<Profile> = try {
        val a = JSONArray(sp.getString("profiles", "[]"))
        (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Profile(o.getString("id"), o.optString("name"), o.optString("base"), o.optString("key"), o.optString("model"))
        }
    } catch (e: Exception) { emptyList() }

    fun saveProfiles(l: List<Profile>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("base", it.base).put("key", it.key).put("model", it.model)) }
        sp.edit().putString("profiles", a.toString()).apply()
    }

    fun loadChat(): List<Msg> = try {
        val a = JSONArray(sp.getString("chat", "[]"))
        (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Msg(o.getLong("id"), o.getString("role"), o.optString("content"), o.optString("reasoning"), o.optLong("thinkMs"))
        }
    } catch (e: Exception) { emptyList() }

    fun saveChat(l: List<Msg>) {
        val a = JSONArray()
        l.filter { !it.error && (it.content.isNotBlank() || it.role == "user") }.takeLast(200).forEach {
            a.put(JSONObject().put("id", it.id).put("role", it.role).put("content", it.content)
                .put("reasoning", it.reasoning).put("thinkMs", it.thinkMs))
        }
        sp.edit().putString("chat", a.toString()).apply()
    }
}
