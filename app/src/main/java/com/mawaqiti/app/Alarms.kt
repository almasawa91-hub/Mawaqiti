package com.mawaqiti.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.time.ZoneId
import java.time.ZonedDateTime

object Notifier {
    private const val CHANNEL = "prayer_v1"

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) != null) return
        val ch = NotificationChannel(CHANNEL, "تنبيهات الصلاة", NotificationManager.IMPORTANCE_HIGH)
        ch.description = "تنبيه عند دخول وقت كل صلاة"
        ch.enableVibration(true)
        // لإضافة صوت الأذان: ضع الملف adhan.mp3 داخل app/src/main/res/raw ثم أعد البناء
        val resId = ctx.resources.getIdentifier("adhan", "raw", ctx.packageName)
        if (resId != 0) {
            ch.setSound(
                Uri.parse("android.resource://${ctx.packageName}/$resId"),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
        nm.createNotificationChannel(ch)
    }

    fun show(ctx: Context, prayer: Prayer) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel(ctx)
        val open = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("حان الآن موعد صلاة ${prayer.ar}")
            .setContentText("حيّ على الصلاة")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(100 + prayer.ordinal, n)
    }
}

object PrayerScheduler {
    private const val REQ = 77

    fun scheduleNext(ctx: Context) {
        val s = Prefs(ctx).load()
        val am = ctx.getSystemService(AlarmManager::class.java)
        val zone = runCatching { ZoneId.of(s.tz) }.getOrDefault(ZoneId.systemDefault())
        val now = ZonedDateTime.now(zone)

        var next: Pair<Prayer, ZonedDateTime>? = null
        for (offset in 0..2) {
            val day = PrayerCalc.forSettings(now.toLocalDate().plusDays(offset.toLong()), s) ?: continue
            val cand = day.entries
                .filter { it.key.name in s.notify && it.value.isAfter(now.plusSeconds(1)) }
                .minByOrNull { it.value }
            if (cand != null) { next = cand.key to cand.value; break }
        }

        val intent = Intent(ctx, AlarmReceiver::class.java)
        val cancelPi = PendingIntent.getBroadcast(
            ctx, REQ, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (cancelPi != null) am.cancel(cancelPi)

        val target = next ?: return
        val pi = PendingIntent.getBroadcast(
            ctx, REQ, intent.putExtra("p", target.first.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val at = target.second.toInstant().toEpochMilli()
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val p = runCatching { Prayer.valueOf(intent.getStringExtra("p") ?: "") }.getOrNull()
        if (p != null) Notifier.show(context, p)
        PrayerScheduler.scheduleNext(context)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        PrayerScheduler.scheduleNext(context)
    }
}
