package com.myassistant

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
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
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlin.math.sqrt

/** 录音并做简单的静音检测：说完话停顿约 1.2 秒就结束，返回 16kHz 单声道 wav；一直没人说话返回 null。 */
class Recorder {
    @Volatile private var run = false

    fun start(onLevel: (Float) -> Unit, onDone: (ByteArray?, String?) -> Unit) {
        run = true
        Thread {
            val rate = 16000
            val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val rec = try {
                AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, rate, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, maxOf(min, rate))
            } catch (e: Exception) { null }
            if (rec == null || rec.state != AudioRecord.STATE_INITIALIZED) { onDone(null, "无法打开麦克风"); return@Thread }
            val buf = ShortArray(rate / 50)
            val pcm = ByteArrayOutputStream()
            var floor = 0f; var n = 0; var spoke = false; var silent = 0; var frames = 0
            rec.startRecording()
            while (run) {
                val r = rec.read(buf, 0, buf.size)
                if (r <= 0) continue
                var sum = 0.0
                for (i in 0 until r) sum += buf[i].toDouble() * buf[i]
                val rms = (sqrt(sum / r) / 32768.0).toFloat()
                onLevel((rms * 9f).coerceIn(0f, 1f))
                for (i in 0 until r) { pcm.write(buf[i].toInt() and 0xff); pcm.write((buf[i].toInt() shr 8) and 0xff) }
                frames++
                if (n < 12) { floor += rms / 12f; n++; continue }
                val th = maxOf(0.025f, floor * 3f)
                if (rms > th) { spoke = true; silent = 0 } else if (spoke) silent++
                if (spoke && silent > 60) break
                if (!spoke && frames > 400) break
                if (frames > 1500) break
            }
            try { rec.stop() } catch (e: Exception) { }
            rec.release()
            run = false
            onDone(if (spoke) wav(pcm.toByteArray(), rate) else null, null)
        }.start()
    }

    fun stop() { run = false }

    private fun wav(pcm: ByteArray, rate: Int): ByteArray {
        val o = ByteArrayOutputStream()
        fun i32(v: Int) { for (k in 0..3) o.write((v shr (8 * k)) and 0xff) }
        fun i16(v: Int) { for (k in 0..1) o.write((v shr (8 * k)) and 0xff) }
        o.write("RIFF".toByteArray()); i32(36 + pcm.size); o.write("WAVEfmt ".toByteArray())
        i32(16); i16(1); i16(1); i32(rate); i32(rate * 2); i16(2); i16(16)
        o.write("data".toByteArray()); i32(pcm.size); o.write(pcm)
        return o.toByteArray()
    }
}

/** 语音输入有两种方式：用当前模型听写（默认，更准），或手机系统自带识别。朗读始终用系统 TTS。 */
class Voice(private val ctx: Context, private val store: Store) {
    private var sr: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ready = false
    private var rec: Recorder? = null
    private var gen = 0
    private val main = Handler(Looper.getMainLooper())
    @Volatile var lastError = ""

    fun init() { tts = TextToSpeech(ctx) { ready = it == TextToSpeech.SUCCESS } }

    fun listen(
        onPartial: (String) -> Unit, onFinal: (String) -> Unit,
        onError: (Int) -> Unit, onRms: (Float) -> Unit
    ) {
        stopListen()
        val my = ++gen
        val cbPartial = onPartial; val cbFinal = onFinal; val cbError = onError; val cbRms = onRms
        if (store.stt == "model" && store.configured()) {
            val r = Recorder(); rec = r
            r.start(onLevel = { v -> if (my == gen) main.post { cbRms(v * 12f - 2f) } }) { wav, err ->
                if (my != gen) return@start
                if (err != null) { lastError = err; main.post { cbError(-2) }; return@start }
                if (wav == null) { main.post { cbError(7) }; return@start }
                main.post { if (my == gen) cbPartial("识别中…") }
                try {
                    val t = Api.transcribe(store.baseUrl, store.apiKey, store.model, wav)
                    main.post { if (my == gen) { if (t.isBlank()) cbError(7) else cbFinal(t) } }
                } catch (e: Exception) {
                    lastError = e.message ?: "转写失败"
                    main.post { if (my == gen) cbError(-2) }
                }
            }
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) { lastError = "系统没有可用的语音识别服务"; cbError(-2); return }
        val s = SpeechRecognizer.createSpeechRecognizer(ctx)
        sr = s
        s.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) = cbRms(v)
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(e: Int) { if (e != 5) cbError(e) }
            override fun onResults(b: Bundle?) =
                cbFinal(b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty())
            override fun onPartialResults(b: Bundle?) {
                b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(cbPartial)
            }
            override fun onEvent(t: Int, p: Bundle?) {}
        })
        s.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        )
    }

    fun stopListen() {
        gen++
        rec?.stop(); rec = null
        sr?.let { try { it.cancel(); it.destroy() } catch (e: Exception) { } }
        sr = null
    }

    fun speak(text: String, onDone: () -> Unit) {
        val finish = onDone
        val t = tts
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

/** 语音模式：聆听 → 思考（含 Agent 执行）→ 朗读 → 继续聆听 */
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
                    -2 -> fail(voice.lastError.ifBlank { "语音识别失败" } + "（可在设置里切换识别方式），点按圆球重试")
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
