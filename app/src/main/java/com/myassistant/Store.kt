package com.myassistant

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

const val DEFAULT_SYSTEM = "你是“我的助手”，运行在用户的 Android 手机上，回答简洁、直接，使用中文。" +
    "当任务需要操作手机时：先在心里把任务拆解成步骤，然后一步一步调用工具；每次操作之后调用 read_screen 确认结果，不要凭空猜测界面内容。" +
    "遇到登录、验证码、支付、输入密码等敏感步骤，停下来请用户自己完成。任务完成后用一两句话汇报结果。"

class Chat(val id: String, title: String) {
    var title by mutableStateOf(title)
    val msgs = mutableStateListOf<JSONObject>()
}

class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("my_assistant", Context.MODE_PRIVATE)
    private fun s(k: String, d: String = "") = sp.getString(k, d) ?: d

    var baseUrl: String
        get() = s("base")
        set(v) { sp.edit().putString("base", v).apply() }
    var apiKey: String
        get() = s("key")
        set(v) { sp.edit().putString("key", v).apply() }
    var model: String
        get() = s("model")
        set(v) { sp.edit().putString("model", v).apply() }
    var system: String
        get() = s("sys", DEFAULT_SYSTEM)
        set(v) { sp.edit().putString("sys", v).apply() }
    var speak: Boolean
        get() = sp.getBoolean("speak", true)
        set(v) { sp.edit().putBoolean("speak", v).apply() }
    var agent: Boolean
        get() = sp.getBoolean("agent", true)
        set(v) { sp.edit().putBoolean("agent", v).apply() }

    fun configured() = baseUrl.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()

    fun loadChats(): List<Chat> = try {
        val arr = JSONArray(s("chats", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Chat(o.getString("id"), o.optString("title")).also { c ->
                val m = o.getJSONArray("msgs")
                for (k in 0 until m.length()) c.msgs.add(m.getJSONObject(k))
            }
        }
    } catch (e: Exception) { emptyList() }

    fun saveChats(list: List<Chat>) {
        try {
            val arr = JSONArray()
            list.take(50).forEach { c ->
                val m = JSONArray()
                c.msgs.toList().forEach { msg ->
                    if (msg.optString("role") == "tool" && msg.optString("content").length > 500) {
                        m.put(JSONObject(msg.toString()).put("content", msg.optString("content").take(500) + "…"))
                    } else m.put(msg)
                }
                arr.put(JSONObject().put("id", c.id).put("title", c.title).put("msgs", m))
            }
            sp.edit().putString("chats", arr.toString()).apply()
        } catch (e: Exception) { }
    }
}
