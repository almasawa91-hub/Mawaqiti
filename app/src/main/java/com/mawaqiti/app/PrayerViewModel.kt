package com.mawaqiti.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.AndroidViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class PrayerViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)

    var settings by mutableStateOf(prefs.load())
        private set
    var today by mutableStateOf<Map<Prayer, ZonedDateTime>?>(null)
        private set
    var tomorrow by mutableStateOf<Map<Prayer, ZonedDateTime>?>(null)
        private set
    var todayDate by mutableStateOf<LocalDate?>(null)
        private set
    var locating by mutableStateOf(false)
        private set
    var locationMsg by mutableStateOf<String?>(null)
        private set
    var tasbeeh by mutableIntStateOf(prefs.tasbeeh)
        private set

    init {
        recompute()
        Notifier.ensureChannel(app)
        PrayerScheduler.scheduleNext(app)
    }

    fun recompute() {
        val s = settings
        val zone = runCatching { ZoneId.of(s.tz) }.getOrDefault(ZoneId.systemDefault())
        val d = LocalDate.now(zone)
        todayDate = d
        today = PrayerCalc.forSettings(d, s)
        tomorrow = PrayerCalc.forSettings(d.plusDays(1), s)
    }

    fun update(new: Settings) {
        settings = new
        prefs.save(new)
        recompute()
        PrayerScheduler.scheduleNext(getApplication())
    }

    fun setMessage(m: String?) {
        locationMsg = m
    }

    fun selectCity(c: City) {
        update(settings.copy(lat = c.lat, lon = c.lon, place = c.name, tz = c.tz, located = true, gps = false, method = c.method))
    }

    @SuppressLint("MissingPermission")
    fun fetchLocation() {
        val ctx = getApplication<Application>()
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            locationMsg = "لم تُمنح صلاحية الموقع. اختر مدينة يدوياً أو اسمح بالموقع من إعدادات الهاتف."
            return
        }
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val all = lm.getProviders(true)
        val last = all.mapNotNull { p ->
            try { lm.getLastKnownLocation(p) } catch (e: Exception) { null }
        }.maxByOrNull { it.time }
        if (last != null) applyLocation(last)

        val usable = all.filter { it != LocationManager.PASSIVE_PROVIDER }
        if (usable.isEmpty()) {
            if (last == null) locationMsg = "فعّل خدمة الموقع (GPS) من إعدادات الهاتف ثم أعد المحاولة."
            return
        }
        val provider = if (LocationManager.NETWORK_PROVIDER in usable) LocationManager.NETWORK_PROVIDER else usable.first()
        locating = true
        locationMsg = null
        try {
            LocationManagerCompat.getCurrentLocation(lm, provider, null, ContextCompat.getMainExecutor(ctx)) { loc ->
                locating = false
                if (loc != null) applyLocation(loc)
                else if (last == null) locationMsg = "تعذر تحديد الموقع. حاول مرة أخرى أو اختر مدينة."
            }
        } catch (e: Exception) {
            locating = false
            if (last == null) locationMsg = "تعذر تحديد الموقع. اختر مدينة يدوياً."
        }
    }

    private fun applyLocation(loc: Location) {
        val near = Cities.minByOrNull { PrayerCalc.distanceKm(loc.latitude, loc.longitude, it.lat, it.lon) }
        val dist = near?.let { PrayerCalc.distanceKm(loc.latitude, loc.longitude, it.lat, it.lon) } ?: 1e9
        val place = if (near != null && dist < 60) near.name
        else String.format(Locale.US, "موقعي (%.2f, %.2f)", loc.latitude, loc.longitude)
        val method = if (!settings.located && near != null && dist < 400) near.method else settings.method
        update(
            settings.copy(
                lat = loc.latitude, lon = loc.longitude, place = place,
                tz = ZoneId.systemDefault().id, located = true, gps = true, method = method
            )
        )
    }

    fun bumpTasbeeh(): Int {
        tasbeeh += 1
        prefs.tasbeeh = tasbeeh
        return tasbeeh
    }

    fun resetTasbeeh() {
        tasbeeh = 0
        prefs.tasbeeh = 0
    }
}
