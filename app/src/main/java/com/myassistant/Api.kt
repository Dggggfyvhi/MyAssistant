package com.myassistant

import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class ToolCall(val id: String, val name: String, val args: String)
class Reply(val text: String, val calls: List<ToolCall>)

object Api {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()
    private val JSON = "application/json".toMediaType()

    @Volatile var call: Call? = null

    private fun req(base: String, key: String, path: String) =
        Request.Builder()
            .url(base.trim().trimEnd('/') + path)
            .header("Authorization", "Bearer ${key.trim()}")

    fun models(base: String, key: String): List<String> {
        return http.newCall(req(base, key, "/models").build()).execute().use { r ->
            val b = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw IOException("${r.code} ${b.take(200)}")
            val arr = JSONObject(b).optJSONArray("data") ?: throw IOException("返回内容不是 OpenAI 格式的模型列表")
            (0 until arr.length()).map { arr.getJSONObject(it).optString("id") }
                .filter { it.isNotBlank() }.sorted()
        }
    }

    fun test(base: String, key: String, model: String): String {
        val t0 = System.currentTimeMillis()
        val body = JSONObject().put("model", model)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", "hi")))
        return try {
            http.newCall(req(base, key, "/chat/completions").post(body.toString().toRequestBody(JSON)).build())
                .execute().use { r ->
                    val s = r.body?.string().orEmpty()
                    if (r.isSuccessful) "✓ 连接正常 · ${System.currentTimeMillis() - t0} ms"
                    else "✗ ${r.code} ${s.take(160)}"
                }
        } catch (e: Exception) { "✗ ${e.message}" }
    }

    private class Acc { val id = StringBuilder(); val name = StringBuilder(); val args = StringBuilder() }

    fun stream(
        base: String, key: String, model: String,
        msgs: List<JSONObject>, tools: JSONArray?, onText: (String) -> Unit
    ): Reply {
        val body = JSONObject().put("model", model).put("stream", true).put("messages", JSONArray(msgs))
        if (tools != null) body.put("tools", tools)
        val c = http.newCall(req(base, key, "/chat/completions").post(body.toString().toRequestBody(JSON)).build())
        call = c
        return c.execute().use { r ->
            if (!r.isSuccessful) throw IOException("${r.code} ${r.body?.string()?.take(300)}")
            val src = r.body!!.source()
            val sb = StringBuilder()
            val acc = sortedMapOf<Int, Acc>()
            while (true) {
                val line = src.readUtf8Line() ?: break
                if (!line.startsWith("data:")) continue
                val d = line.substring(5).trim()
                if (d == "[DONE]") break
                if (d.isEmpty()) continue
                val j = try { JSONObject(d) } catch (e: JSONException) { continue }
                val delta = j.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta") ?: continue
                if (!delta.isNull("content")) {
                    sb.append(delta.getString("content"))
                    onText(sb.toString())
                }
                val tcs = delta.optJSONArray("tool_calls")
                if (tcs != null) for (k in 0 until tcs.length()) {
                    val t = tcs.getJSONObject(k)
                    val a = acc.getOrPut(t.optInt("index", k)) { Acc() }
                    if (!t.isNull("id") && a.id.isEmpty()) a.id.append(t.getString("id"))
                    val f = t.optJSONObject("function")
                    if (f != null) {
                        if (!f.isNull("name") && a.name.isEmpty()) a.name.append(f.getString("name"))
                        if (!f.isNull("arguments")) a.args.append(f.getString("arguments"))
                    }
                }
            }
            Reply(sb.toString(), acc.values.filter { it.name.isNotEmpty() }.map {
                ToolCall(
                    it.id.toString().ifEmpty { "call_" + UUID.randomUUID().toString().take(8) },
                    it.name.toString(), it.args.toString().ifEmpty { "{}" }
                )
            })
        }
    }
}
