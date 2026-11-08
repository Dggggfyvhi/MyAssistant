package com.myassistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/** 全部使用手机系统自带的语音识别与朗读。想换声音：在系统“文字转语音”里换引擎即可。 */
class Voice(private val ctx: Context) {
    private var sr: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ready = false

    fun init() {
        tts = TextToSpeech(ctx) { ready = it == TextToSpeech.SUCCESS }
    }

    fun listen(
        onPartial: (String) -> Unit, onFinal: (String) -> Unit,
        onError: (Int) -> Unit, onRms: (Float) -> Unit
    ) {
        stopListen()
        val cbPartial = onPartial; val cbFinal = onFinal; val cbError = onError; val cbRms = onRms
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) { cbError(-1); return }
        val r = SpeechRecognizer.createSpeechRecognizer(ctx)
        sr = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) = cbRms(v)
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(e: Int) = cbError(e)
            override fun onResults(b: Bundle?) =
                cbFinal(b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty())
            override fun onPartialResults(b: Bundle?) {
                b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(cbPartial)
            }
            override fun onEvent(t: Int, p: Bundle?) {}
        })
        r.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        )
    }

    fun stopListen() {
        sr?.let { try { it.cancel(); it.destroy() } catch (e: Exception) { } }
        sr = null
    }

    fun speak(text: String, onDone: () -> Unit) {
        val t = tts
        val finish = onDone
        val clean = text.replace(Regex("[*#`>_~|]"), "").take(3500)
        if (t == null || !ready || clean.isBlank()) { finish(); return }
        t.language = if (clean.any { it.code in 0x4E00..0x9FFF }) Locale.SIMPLIFIED_CHINESE else Locale.getDefault()
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) = finish()
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) = finish()
        })
        t.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "u")
    }

    fun stopSpeak() { tts?.stop() }

    fun shutdown() { stopListen(); tts?.shutdown(); tts = null }
}

/** 语音模式的状态机：聆听 → 思考（含 Agent 执行）→ 朗读 → 继续聆听 */
class VoiceSession(private val vm: VM, private val voice: Voice) {
    var phase by mutableStateOf("boot")
    var status by mutableStateOf("")
    var caption by mutableStateOf("")
    var level by mutableFloatStateOf(0f)
    var muted by mutableStateOf(false)

    private var alive = false
    private val h = Handler(Looper.getMainLooper())

    fun start() { alive = true; listen() }

    fun stop() {
        alive = false
        h.removeCallbacksAndMessages(null)
        voice.stopListen(); voice.stopSpeak()
    }

    fun tap() {
        if (phase == "speak") { voice.stopSpeak(); listen() }
        else if (phase == "idle" && !muted) listen()
    }

    fun toggleMute() {
        muted = !muted
        if (muted) {
            voice.stopListen()
            if (phase == "listen") { phase = "idle"; status = "已静音"; caption = "" }
        } else listen()
    }

    private fun listen() {
        if (!alive || muted) return
        phase = "listen"; status = "正在聆听"; caption = ""
        voice.listen(
            onPartial = { caption = it },
            onFinal = { t -> if (t.isBlank()) retry() else ask(t) },
            onError = { c ->
                when (c) {
                    6, 7 -> retry()
                    8 -> retry(700)
                    else -> fail("语音识别不可用（$c），点按圆球重试")
                }
            },
            onRms = { level = ((it + 2f) / 12f).coerceIn(0f, 1f) }
        )
    }

    private fun retry(d: Long = 250) { h.postDelayed({ listen() }, d) }

    private fun ask(t: String) {
        caption = t; phase = "think"; status = "思考中"
        vm.send(t) { r -> h.post { reply(r) } }
    }

    private fun reply(r: String?) {
        if (!alive) return
        if (r == null) { fail(vm.error ?: "出错了，点按圆球重试"); return }
        if (vm.store.speak && r.isNotBlank()) {
            phase = "speak"; status = ""; caption = r.take(120)
            voice.speak(r) { h.post { listen() } }
        } else listen()
    }

    private fun fail(m: String) { phase = "idle"; status = m }
}
