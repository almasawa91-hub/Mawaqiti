package com.mawaqiti.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object Notifier {
    const val CH_ALERT = "prayer_v1"
    const val CH_SILENT = "prayer_silent_v1"
    const val CH_ADHAN = "adhan_playing_v1"

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CH_ALERT) == null) {
            val ch = NotificationChannel(CH_ALERT, "تنبيهات الصلاة والأذكار", NotificationManager.IMPORTANCE_HIGH)
            ch.description = "تنبيه عند دخول وقت كل صلاة وتذكير بالأذكار"
            ch.enableVibration(true)
            nm.createNotificationChannel(ch)
        }
        if (nm.getNotificationChannel(CH_SILENT) == null) {
            val ch = NotificationChannel(CH_SILENT, "إشعارات الصلاة (صامتة)", NotificationManager.IMPORTANCE_DEFAULT)
            ch.setSound(null, null)
            ch.enableVibration(false)
            nm.createNotificationChannel(ch)
        }
        if (nm.getNotificationChannel(CH_ADHAN) == null) {
            val ch = NotificationChannel(CH_ADHAN, "تشغيل الأذان", NotificationManager.IMPORTANCE_LOW)
            ch.setSound(null, null)
            nm.createNotificationChannel(ch)
        }
    }

    fun contentIntent(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 0,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    fun show(ctx: Context, id: String, title: String, text: String, silent: Boolean = false) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel(ctx)
        val n = NotificationCompat.Builder(ctx, if (silent) CH_SILENT else CH_ALERT)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(if (silent) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(ctx))
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(id.hashCode(), n)
    }
}

data class AlarmEvent(val id: String, val title: String, val text: String, val time: ZonedDateTime)

object PrayerScheduler {
    private const val REQ_EVENT = 77
    private const val REQ_WIDGET = 78

    /** كل الأحداث القادمة (صلوات، تنبيه مسبق، تذكير أذكار) مرتبة زمنياً */
    fun events(s: Settings, now: ZonedDateTime): List<AlarmEvent> {
        val out = ArrayList<AlarmEvent>()
        for (off in 0..2) {
            val d = now.toLocalDate().plusDays(off.toLong())
            val day = PrayerCalc.forSettings(d, s) ?: continue
            for ((p, t) in day) {
                if (p.name !in s.notify) continue
                val name = PrayerCalc.label(p, d)
                out += AlarmEvent(p.name, "حان الآن موعد صلاة $name", "حيّ على الصلاة", t)
                if (s.preMinutes > 0) {
                    out += AlarmEvent(
                        "PRE_${p.name}", "بقي ${s.preMinutes} دقيقة على صلاة $name", "استعدّ للصلاة",
                        t.minusMinutes(s.preMinutes.toLong())
                    )
                }
            }
            if (s.azkarAlerts) {
                day[Prayer.SUNRISE]?.let {
                    out += AlarmEvent("AZKAR_AM", "أذكار الصباح", "لا تنسَ أذكار الصباح", it)
                }
                day[Prayer.ASR]?.let {
                    out += AlarmEvent("AZKAR_PM", "أذكار المساء", "حان وقت أذكار المساء", it.plusMinutes(60))
                }
            }
        }
        return out.filter { it.time.isAfter(now.plusSeconds(1)) }.sortedBy { it.time }
    }

    private fun nextAny(s: Settings, now: ZonedDateTime): AlarmEvent? {
        for (off in 0..1) {
            val day = PrayerCalc.forSettings(now.toLocalDate().plusDays(off.toLong()), s) ?: continue
            val c = day.entries.filter { it.value.isAfter(now.plusSeconds(1)) }.minByOrNull { it.value }
            if (c != null) return AlarmEvent("WIDGET", "", "", c.value)
        }
        return null
    }

    fun scheduleNext(ctx: Context) {
        val s = Prefs(ctx).load()
        val am = ctx.getSystemService(AlarmManager::class.java)
        val zone = runCatching { ZoneId.of(s.tz) }.getOrDefault(ZoneId.systemDefault())
        val now = ZonedDateTime.now(zone)
        setAlarm(ctx, am, REQ_EVENT, events(s, now).firstOrNull())
        setAlarm(ctx, am, REQ_WIDGET, nextAny(s, now))
    }

    private fun setAlarm(ctx: Context, am: AlarmManager, req: Int, ev: AlarmEvent?) {
        val old = PendingIntent.getBroadcast(
            ctx, req, Intent(ctx, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (old != null) am.cancel(old)
        if (ev == null) return
        val intent = Intent(ctx, AlarmReceiver::class.java)
            .putExtra("id", ev.id).putExtra("title", ev.title).putExtra("text", ev.text)
        val pi = PendingIntent.getBroadcast(
            ctx, req, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val at = ev.time.toInstant().toEpochMilli()
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("id") ?: ""
        val title = intent.getStringExtra("title") ?: ""
        val text = intent.getStringExtra("text") ?: ""
        if (id.isNotEmpty() && id != "WIDGET" && title.isNotEmpty()) {
            val isPrayer = id == "FAJR" || id == "DHUHR" || id == "ASR" || id == "MAGHRIB" || id == "ISHA"
            var handled = false
            if (isPrayer) {
                val s = Prefs(context).load()
                val src = if (id == "FAJR" && s.fajrAdhan.isNotEmpty()) s.fajrAdhan else s.adhan
                if (src.isNotEmpty() && src != "off") handled = AdhanService.start(context, title, text, src)
            }
            if (!handled) Notifier.show(context, id, title, text)
        }
        NextPrayerWidget.updateAll(context)
        PrayerScheduler.scheduleNext(context)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NextPrayerWidget.updateAll(context)
        PrayerScheduler.scheduleNext(context)
    }
}

/** ودجت الشاشة الرئيسية: الصلاة القادمة ووقتها */
class NextPrayerWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = build(context)
        for (id in appWidgetIds) appWidgetManager.updateAppWidget(id, views)
    }

    override fun onEnabled(context: Context) {
        PrayerScheduler.scheduleNext(context)
    }

    companion object {
        fun build(ctx: Context): RemoteViews {
            val s = Prefs(ctx).load()
            val zone = runCatching { ZoneId.of(s.tz) }.getOrDefault(ZoneId.systemDefault())
            val now = ZonedDateTime.now(zone)
            val next = PrayerCalc.nextPrayer(s, now)
            val fmt = DateTimeFormatter.ofPattern(if (s.use24h) "HH:mm" else "hh:mm a", Locale("ar"))
            val v = RemoteViews(ctx.packageName, R.layout.widget_next)
            v.setTextViewText(R.id.w_name, next?.let { PrayerCalc.label(it.first, it.second.toLocalDate()) } ?: "--")
            v.setTextViewText(R.id.w_time, next?.second?.format(fmt) ?: "--:--")
            v.setTextViewText(R.id.w_place, s.place)
            val open = PendingIntent.getActivity(
                ctx, 1, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            v.setOnClickPendingIntent(R.id.w_root, open)
            return v
        }

        fun updateAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, NextPrayerWidget::class.java))
            if (ids.isNotEmpty()) mgr.updateAppWidget(ids, build(ctx))
        }
    }
}
