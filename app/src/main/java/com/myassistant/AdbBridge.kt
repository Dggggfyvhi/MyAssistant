package com.myassistant

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import java.math.BigInteger
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.ByteArrayInputStream
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date

/** 应用内的无线调试桥：配对码配对 → 连接 → 执行 shell。密钥保存在应用私有目录，配对一次即可。 */
class AdbMgr private constructor(ctx: Context) : AbsAdbConnectionManager() {
    private val priv: PrivateKey
    private val cert: Certificate

    init {
        setApi(Build.VERSION.SDK_INT)
        val sp = ctx.getSharedPreferences("adb_keys", Context.MODE_PRIVATE)
        var p: PrivateKey? = null
        var c: Certificate? = null
        try {
            val pk = sp.getString("priv", null)
            val ce = sp.getString("cert", null)
            if (pk != null && ce != null) {
                p = KeyFactory.getInstance("RSA").generatePrivate(
                    PKCS8EncodedKeySpec(android.util.Base64.decode(pk, android.util.Base64.NO_WRAP)))
                c = CertificateFactory.getInstance("X.509").generateCertificate(
                    ByteArrayInputStream(android.util.Base64.decode(ce, android.util.Base64.NO_WRAP)))
            }
        } catch (e: Exception) { p = null; c = null }
        if (p == null || c == null) {
            val gen = KeyPairGenerator.getInstance("RSA")
            gen.initialize(2048, SecureRandom.getInstance("SHA1PRNG"))
            val kp = gen.generateKeyPair()
            val privateKey = kp.private
            p = privateKey
            val from = Date(System.currentTimeMillis() - 86400000)
            val to = Date(System.currentTimeMillis() + 25L * 365 * 86400000)
            val subject = X500Name("CN=MyAssistant")
            val serial = BigInteger.valueOf(System.currentTimeMillis())
            val certBuilder = JcaX509v3CertificateBuilder(
                subject, serial, from, to, subject, kp.public
            )
            val signer = JcaContentSignerBuilder("SHA256withRSA").build(privateKey)
            val holder = certBuilder.build(signer)
            val certificate = JcaX509CertificateConverter().getCertificate(holder)
            c = certificate
            sp.edit()
                .putString("priv", android.util.Base64.encodeToString(privateKey.encoded, android.util.Base64.NO_WRAP))
                .putString("cert", android.util.Base64.encodeToString(certificate.encoded, android.util.Base64.NO_WRAP))
                .apply()
        }
        priv = p!!
        cert = c!!
    }

    override fun getPrivateKey(): PrivateKey = priv
    override fun getCertificate(): Certificate = cert
    override fun getDeviceName(): String = "MyAssistant"

    companion object {
        @Volatile private var inst: AdbMgr? = null

        fun get(ctx: Context): AdbMgr =
            inst ?: synchronized(this) { inst ?: AdbMgr(ctx.applicationContext).also { inst = it } }

        fun isUp(): Boolean = try { inst?.isConnected == true } catch (e: Exception) { false }

        fun pair(ctx: Context, host: String, port: Int, code: String): String = try {
            get(ctx).pair(host, port, code.trim())
            "✓ 配对成功，接着点“连接”"
        } catch (e: Throwable) { "✗ 配对失败：${e.message ?: e.javaClass.simpleName}" }

        fun connect(ctx: Context, host: String, port: Int): String = try {
            get(ctx).connect(host, port)
            if (isUp()) "✓ 已连接" else "✗ 连接失败，请确认“无线调试”开关是打开的，并核对连接端口"
        } catch (e: Throwable) { "✗ 连接失败：${e.message ?: e.javaClass.simpleName}" }

        fun disconnect() { try { inst?.close() } catch (e: Throwable) { } }

        fun shell(ctx: Context, cmd: String): String {
            val s = get(ctx).openStream("shell:$cmd")
            try {
                val t = s.openInputStream().bufferedReader().readText()
                return if (t.length > 6000) t.take(6000) + "\n…（输出已截断）" else t
            } finally { try { s.close() } catch (e: Throwable) { } }
        }
    }
}

/** 通过局域网服务发现，自动拿到“配对端口”和“连接端口”，用户只需要输入配对码。 */
class AdbDiscover(ctx: Context, private val onPair: (Int) -> Unit, private val onConnect: (Int) -> Unit) {
    private val nsd = ctx.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val ls = mutableListOf<NsdManager.DiscoveryListener>()

    fun start() {
        watch("_adb-tls-pairing._tcp", onPair)
        watch("_adb-tls-connect._tcp", onConnect)
    }

    private fun watch(type: String, cb: (Int) -> Unit) {
        val l = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(t: String?) {}
            override fun onDiscoveryStopped(t: String?) {}
            override fun onStartDiscoveryFailed(t: String?, e: Int) {}
            override fun onStopDiscoveryFailed(t: String?, e: Int) {}
            override fun onServiceLost(s: NsdServiceInfo?) {}
            @Suppress("DEPRECATION")
            override fun onServiceFound(s: NsdServiceInfo) {
                try {
                    nsd.resolveService(s, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(i: NsdServiceInfo?, e: Int) {}
                        override fun onServiceResolved(i: NsdServiceInfo) { cb(i.port) }
                    })
                } catch (e: Exception) { }
            }
        }
        ls.add(l)
        try { nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, l) } catch (e: Exception) { }
    }

    fun stop() {
        ls.forEach { try { nsd.stopServiceDiscovery(it) } catch (e: Exception) { } }
        ls.clear()
    }
}
