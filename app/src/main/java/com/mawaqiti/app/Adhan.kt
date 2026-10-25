package com.mawaqiti.app

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.os.PowerManager
import android.provider.OpenableColumns
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import java.io.File

/**
 * مصادر الأذان:
 *  - "off"          : تنبيه عادي بصوت الإشعار
 *  - "raw:<اسم>"    : مؤذن مدمج داخل التطبيق (ملف في res/raw)
 *  - "file:<ملف>"   : أذان أضافه المستخدم من ذاكرة الهاتف
 */
object AdhanSources {

    /** أسماء ملفات res/raw المدعومة (تظهر في التطبيق فقط إذا وُجد الملف) */
    val builtinNames = listOf(
        "adhan_makkah" to "الحرم المكي",
        "adhan_madinah" to "الحرم المدني",
        "adhan_aqsa" to "المسجد الأقصى",
        "adhan_egypt" to "مؤذن مصري",
        "adhan_fajr" to "أذان الفجر",
        "adhan_1" to "مؤذن إضافي 1",
        "adhan_2" to "مؤذن إضافي 2",
        "adhan_3" to "مؤذن إضافي 3"
    )

    fun builtins(ctx: Context): List<Pair<String, String>> =
        builtinNames
            .filter { ctx.resources.getIdentifier(it.first, "raw", ctx.packageName) != 0 }
            .map { "raw:${it.first}" to it.second }

    fun dir(ctx: Context) = File(ctx.filesDir, "adhan")

    fun customs(ctx: Context): List<Pair<String, String>> =
        Prefs(ctx).customAdhans().filter { File(dir(ctx), it.first.removePrefix("file:")).exists() }

    fun label(ctx: Context, id: String): String = when {
        id == "off" -> "تنبيه عادي (صوت الإشعار)"
        id.isEmpty() -> "نفس الأذان العام"
        else -> (builtins(ctx) + customs(ctx)).firstOrNull { it.first == id }?.second ?: "غير متوفر"
    }

    /** ينسخ ملفاً صوتياً من ذاكرة الهاتف إلى مساحة التطبيق، ويعيد (المعرّف، الاسم) أو null عند الفشل */
    fun importAdhan(ctx: Context, uri: Uri): Pair<String, String>? {
        val cr = ctx.contentResolver
        var name = "أذان مخصص"
        var size = -1L
        try {
            cr.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (ni >= 0) name = c.getString(ni) ?: name
                    val si = c.getColumnIndex(OpenableColumns.SIZE)
                    if (si >= 0 && !c.isNull(si)) size = c.getLong(si)
                }
            }
        } catch (e: Exception) {
            // نتجاهل ونكمل بالاسم الافتراضي
        }
        if (size > 15L * 1024 * 1024) return null

        val ext = name.substringAfterLast('.', "mp3").lowercase().takeIf { it.length in 2..4 } ?: "mp3"
        val label = name.substringBeforeLast('.').replace("|", " ").replace(";", " ").trim()
            .ifBlank { "أذان مخصص" }
        val fileName = "${System.currentTimeMillis()}.$ext"
        val d = dir(ctx)
        d.mkdirs()
        val out = File(d, fileName)
        try {
            val input = cr.openInputStream(uri) ?: return null
            input.use { ins -> out.outputStream().use { os -> ins.copyTo(os) } }
        } catch (e: Exception) {
            out.delete()
            return null
        }

        // التأكد أن الملف صوتي صالح
        val r = MediaMetadataRetriever()
        val valid = try {
            r.setDataSource(out.absolutePath)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"
        } catch (e: Exception) {
            false
        } finally {
            try { r.release() } catch (e: Exception) { }
        }
        if (!valid) {
            out.delete()
            return null
        }

        val id = "file:$fileName"
        val prefs = Prefs(ctx)
        prefs.setCustomAdhans(prefs.customAdhans() + (id to label))
        return id to label
    }

    fun delete(ctx: Context, id: String) {
        val prefs = Prefs(ctx)
        prefs.setCustomAdhans(prefs.customAdhans().filter { it.first != id })
        File(dir(ctx), id.removePrefix("file:")).delete()
    }
}

class AdhanPlayer(private val ctx: Context) {
    private var mp: MediaPlayer? = null

    fun play(id: String, onDone: () -> Unit): Boolean {
        stop()
        val player = MediaPlayer()
        try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            player.setWakeMode(ctx, PowerManager.PARTIAL_WAKE_LOCK)
            when {
                id.startsWith("raw:") -> {
                    val resId = ctx.resources.getIdentifier(id.removePrefix("raw:"), "raw", ctx.packageName)
                    if (resId == 0) throw IllegalStateException("missing raw")
                    ctx.resources.openRawResourceFd(resId).use {
                        player.setDataSource(it.fileDescriptor, it.startOffset, it.length)
                    }
                }
                id.startsWith("file:") -> {
                    val f = File(AdhanSources.dir(ctx), id.removePrefix("file:"))
                    if (!f.exists()) throw IllegalStateException("missing file")
                    player.setDataSource(f.absolutePath)
                }
                else -> throw IllegalStateException("bad source")
            }
            player.setOnCompletionListener {
                stop()
                onDone()
            }
            player.setOnErrorListener { _, _, _ ->
                stop()
                onDone()
                true
            }
            player.prepare()
            player.start()
            mp = player
            return true
        } catch (e: Exception) {
            try { player.release() } catch (x: Exception) { }
            return false
        }
    }

    fun stop() {
        val p = mp
        mp = null
        if (p != null) {
            try { p.stop() } catch (e: Exception) { }
            try { p.release() } catch (e: Exception) { }
        }
    }
}

/** خدمة أمامية تشغّل الأذان وتعرض إشعاراً فيه زر «إيقاف الأذان» */
class AdhanService : Service() {
    private var player: AdhanPlayer? = null
    private var focusReq: AudioFocusRequest? = null
    private var title = "حان وقت الصلاة"
    private var text = "حيّ على الصلاة"
    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            finish(false)
            return START_NOT_STICKY
        }
        title = intent?.getStringExtra("title") ?: title
        text = intent?.getStringExtra("text") ?: text
        val src = intent?.getStringExtra("src") ?: ""

        Notifier.ensureChannel(this)
        ServiceCompat.startForeground(
            this, NOTIF_ID, buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
        started = true

        player?.stop()
        val p = AdhanPlayer(this)
        player = p
        requestFocus()
        val ok = p.play(src) { finish(false) }
        if (!ok) finish(true)
        return START_NOT_STICKY
    }

    private fun buildNotification() = run {
        val stop = PendingIntent.getService(
            this, 5, Intent(this, AdhanService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        NotificationCompat.Builder(this, Notifier.CH_ADHAN)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(Notifier.contentIntent(this))
            .addAction(0, "إيقاف الأذان", stop)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
    }

    private fun requestFocus() {
        val am = getSystemService(AudioManager::class.java)
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .build()
        focusReq = req
        am.requestAudioFocus(req)
    }

    private fun abandonFocus() {
        focusReq?.let { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
        focusReq = null
    }

    private fun finish(failed: Boolean) {
        player?.stop()
        player = null
        abandonFocus()
        if (started) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            started = false
        }
        // إن فشل تشغيل الملف نُنبّه بصوت الإشعار الافتراضي بدل الصمت
        Notifier.show(this, "ADHAN_DONE", title, text, silent = !failed)
        stopSelf()
    }

    override fun onDestroy() {
        player?.stop()
        abandonFocus()
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 4001
        const val ACTION_STOP = "com.mawaqiti.app.STOP_ADHAN"

        fun start(ctx: Context, title: String, text: String, src: String): Boolean = try {
            ContextCompat.startForegroundService(
                ctx,
                Intent(ctx, AdhanService::class.java)
                    .putExtra("title", title).putExtra("text", text).putExtra("src", src)
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}
