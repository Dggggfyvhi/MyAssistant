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
import java.util.concurrent.TimeUnit

object Api {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()
    private val JSON = "application/json".toMediaType()

    @Volatile var call: Call? = null

    fun urlOf(base: String) = base.trim().trimEnd('/') + "/chat/completions"

    private fun JSONObject.str(k: String): String = if (isNull(k)) "" else optString(k, "")

    private fun build(p: Provider, body: JSONObject): Request =
        Request.Builder().url(p.url)
            .apply { if (p.key.isNotBlank()) header("Authorization", "Bearer ${p.key.trim()}") }
            .post(body.toString().toRequestBody(JSON)).build()

    fun models(base: String, key: String): List<String> {
        val req = Request.Builder().url(base.trim().trimEnd('/') + "/models")
            .apply { if (key.isNotBlank()) header("Authorization", "Bearer ${key.trim()}") }.build()
        return http.newCall(req).execute().use { r ->
            val b = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw IOException("${r.code} ${b.take(160)}")
            val arr = JSONObject(b).optJSONArray("data") ?: throw IOException("返回内容不是 OpenAI 格式的模型列表")
            (0 until arr.length()).map { arr.getJSONObject(it).optString("id") }.filter { it.isNotBlank() }.sorted()
        }
    }

    fun test(p: Provider): String {
        val t0 = System.currentTimeMillis()
        val body = JSONObject().put("model", p.model)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", "hi")))
        return try {
            http.newCall(build(p, body)).execute().use { r ->
                val s = r.body?.string().orEmpty()
                if (r.isSuccessful) "✓ 连接正常 · ${System.currentTimeMillis() - t0} ms" else "✗ ${r.code} ${s.take(140)}"
            }
        } catch (e: Exception) { "✗ ${e.message}" }
    }

    /** 流式对话。onDelta 收到的是累计的（正文，思考过程）。返回最终的（正文，思考过程）。 */
    fun stream(p: Provider, history: List<Pair<String, String>>, onDelta: (String, String) -> Unit): Pair<String, String> {
        val arr = JSONArray()
        history.forEach { arr.put(JSONObject().put("role", it.first).put("content", it.second)) }
        val body = JSONObject().put("model", p.model).put("stream", true).put("messages", arr)
        val c = http.newCall(build(p, body))
        call = c
        return c.execute().use { r ->
            if (!r.isSuccessful) throw IOException("${r.code} ${r.body?.string()?.take(160).orEmpty()}")
            val ct = r.header("Content-Type").orEmpty()
            val src = r.body!!.source()
            val content = StringBuilder()
            val reason = StringBuilder()
            if (ct.contains("json") && !ct.contains("event-stream")) {
                val m = JSONObject(src.readUtf8()).getJSONArray("choices").getJSONObject(0).getJSONObject("message")
                content.append(m.str("content"))
                reason.append(m.str("reasoning_content").ifEmpty { m.str("reasoning") })
                onDelta(content.toString(), reason.toString())
            } else {
                while (true) {
                    val line = src.readUtf8Line() ?: break
                    if (!line.startsWith("data:")) continue
                    val d = line.substring(5).trim()
                    if (d == "[DONE]") break
                    if (d.isEmpty()) continue
                    val j = try { JSONObject(d) } catch (e: JSONException) { continue }
                    val delta = j.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta") ?: continue
                    val dc = delta.str("content")
                    val dr = delta.str("reasoning_content").ifEmpty { delta.str("reasoning") }
                    if (dc.isEmpty() && dr.isEmpty()) continue
                    content.append(dc)
                    reason.append(dr)
                    onDelta(content.toString(), reason.toString())
                }
            }
            if (content.isEmpty() && reason.isEmpty()) throw IOException("空响应")
            content.toString() to reason.toString()
        }
    }
}
