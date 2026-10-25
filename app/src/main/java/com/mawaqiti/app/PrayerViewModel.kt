package com.mawaqiti.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.math.abs

val TasbeehPhrases = listOf(
    "سُبْحَانَ اللَّهِ",
    "الْحَمْدُ لِلَّهِ",
    "اللَّهُ أَكْبَرُ",
    "لَا إِلَهَ إِلَّا اللَّهُ",
    "أَسْتَغْفِرُ اللَّهَ",
    "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ"
)

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

    // المسبحة
    var tasbeehIndex by mutableIntStateOf(0)
        private set
    val tasbeehCounts = mutableStateListOf<Int>().apply {
        addAll(TasbeehPhrases.indices.map { prefs.tasbeehCount(it) })
    }

    // الأذان والمؤذنون
    var customAdhans by mutableStateOf(AdhanSources.customs(app))
        private set
    var adhanMsg by mutableStateOf<String?>(null)
        private set
    var previewId by mutableStateOf<String?>(null)
        private set
    private val previewPlayer = AdhanPlayer(app)

    // متتبع الصلوات
    var trackerVersion by mutableIntStateOf(0)
        private set

    init {
        if (settings.place.startsWith("موقعي (")) {
            settings = settings.copy(place = "موقعي الحالي")
            prefs.save(settings)
        }
        recompute()
        Notifier.ensureChannel(app)
        PrayerScheduler.scheduleNext(app)
    }

    fun zone(): ZoneId = runCatching { ZoneId.of(settings.tz) }.getOrDefault(ZoneId.systemDefault())

    fun recompute() {
        val s = settings
        val d = LocalDate.now(zone())
        todayDate = d
        today = PrayerCalc.forSettings(d, s)
        tomorrow = PrayerCalc.forSettings(d.plusDays(1), s)
    }

    /** تغيير يؤثر على المواقيت/التنبيهات */
    fun update(new: Settings) {
        settings = new
        prefs.save(new)
        recompute()
        PrayerScheduler.scheduleNext(getApplication())
        NextPrayerWidget.updateAll(getApplication())
    }

    /** تغيير شكلي فقط (مظهر، خط، نظام الساعة) */
    fun updateUi(new: Settings) {
        settings = new
        prefs.save(new)
        NextPrayerWidget.updateAll(getApplication())
    }

    fun setMessage(m: String?) {
        locationMsg = m
    }

    fun selectCity(c: City) {
        update(
            settings.copy(
                lat = c.lat, lon = c.lon, place = c.name, tz = c.tz,
                located = true, gps = false, method = c.method
            )
        )
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
            val signal: android.os.CancellationSignal? = null
            val consumer = androidx.core.util.Consumer<Location?> { loc ->
                locating = false
                if (loc != null) applyLocation(loc)
                else if (last == null) locationMsg = "تعذر تحديد الموقع. حاول مرة أخرى أو اختر مدينة."
            }
            LocationManagerCompat.getCurrentLocation(lm, provider, signal, ContextCompat.getMainExecutor(ctx), consumer)
        } catch (e: Exception) {
            locating = false
            if (last == null) locationMsg = "تعذر تحديد الموقع. اختر مدينة يدوياً."
        }
    }

    private fun applyLocation(loc: Location) {
        val near = Cities.minByOrNull { PrayerCalc.distanceKm(loc.latitude, loc.longitude, it.lat, it.lon) }
        val dist = near?.let { PrayerCalc.distanceKm(loc.latitude, loc.longitude, it.lat, it.lon) } ?: 1e9
        // نحتفظ بالاسم الحالي إن بقي المستخدم في نفس المكان تقريباً
        val keep = settings.gps && settings.place != "موقعي الحالي" &&
            PrayerCalc.distanceKm(settings.lat, settings.lon, loc.latitude, loc.longitude) < 3
        val place = when {
            keep -> settings.place
            near != null && dist < 40 -> near.name
            else -> "موقعي الحالي"
        }
        val method = if (!settings.located && near != null && dist < 400) near.method else settings.method
        update(
            settings.copy(
                lat = loc.latitude, lon = loc.longitude, place = place,
                tz = ZoneId.systemDefault().id, located = true, gps = true, method = method
            )
        )
        resolveName(loc.latitude, loc.longitude)
    }

    /** يحاول جلب اسم المنطقة (مدينة، دولة) بالعربية؛ إن لم تتوفر الخدمة يبقى الاسم الحالي */
    private fun resolveName(lat: Double, lon: Double) {
        val ctx = getApplication<Application>()
        if (!Geocoder.isPresent()) return
        viewModelScope.launch {
            val name: String? = withContext(Dispatchers.IO) {
                try {
                    @Suppress("DEPRECATION")
                    val list = Geocoder(ctx, Locale("ar")).getFromLocation(lat, lon, 1)
                    val a = list?.firstOrNull()
                    if (a == null) {
                        null
                    } else {
                        val primary = a.locality ?: a.subAdminArea ?: a.adminArea
                        val country = a.countryName
                        when {
                            primary.isNullOrBlank() -> country
                            country.isNullOrBlank() || country == primary -> primary
                            else -> "$primary، $country"
                        }
                    }
                } catch (e: Exception) {
                    null
                }
            }
            val s = settings
            if (!name.isNullOrBlank() && s.gps && abs(s.lat - lat) < 1e-6 && abs(s.lon - lon) < 1e-6 && s.place != name) {
                updateUi(s.copy(place = name))
            }
        }
    }

    // ───── الأذان ─────
    fun setAdhan(id: String) {
        updateUi(settings.copy(adhan = id))
    }

    fun setFajrAdhan(id: String) {
        updateUi(settings.copy(fajrAdhan = id))
    }

    fun addCustomAdhan(uri: Uri) {
        adhanMsg = null
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { AdhanSources.importAdhan(getApplication(), uri) }
            if (r == null) {
                adhanMsg = "تعذّرت إضافة الملف. تأكد أنه ملف صوتي سليم وحجمه أقل من 15 ميغابايت."
            } else {
                customAdhans = AdhanSources.customs(getApplication())
                adhanMsg = "تمت إضافة: ${r.second}"
                if (settings.adhan == "off") updateUi(settings.copy(adhan = r.first))
            }
        }
    }

    fun deleteCustomAdhan(id: String) {
        stopPreview()
        AdhanSources.delete(getApplication(), id)
        customAdhans = AdhanSources.customs(getApplication())
        var s = settings
        if (s.adhan == id) s = s.copy(adhan = "off")
        if (s.fajrAdhan == id) s = s.copy(fajrAdhan = "")
        updateUi(s)
    }

    fun preview(id: String) {
        if (previewId == id) {
            stopPreview()
            return
        }
        stopPreview()
        if (id.isEmpty() || id == "off") return
        val ok = previewPlayer.play(id) { previewId = null }
        previewId = if (ok) id else null
        if (!ok) adhanMsg = "تعذّر تشغيل هذا الملف."
    }

    fun stopPreview() {
        previewPlayer.stop()
        previewId = null
    }

    fun clearAdhanMsg() {
        adhanMsg = null
    }

    override fun onCleared() {
        previewPlayer.stop()
        super.onCleared()
    }

    // ───── المسبحة ─────
    fun selectTasbeeh(i: Int) {
        tasbeehIndex = i.coerceIn(0, TasbeehPhrases.lastIndex)
    }

    fun bumpTasbeeh(): Int {
        val n = tasbeehCounts[tasbeehIndex] + 1
        tasbeehCounts[tasbeehIndex] = n
        prefs.setTasbeehCount(tasbeehIndex, n)
        return n
    }

    fun resetTasbeeh() {
        tasbeehCounts[tasbeehIndex] = 0
        prefs.setTasbeehCount(tasbeehIndex, 0)
    }

    // ───── متتبع الصلوات ─────
    fun trackerMask(d: LocalDate): Int = prefs.trackerMask(d)

    fun toggleTrack(d: LocalDate, bit: Int) {
        prefs.setTrackerMask(d, prefs.trackerMask(d) xor (1 shl bit))
        trackerVersion += 1
    }

    fun streak(): Int {
        var d = LocalDate.now(zone())
        if (prefs.trackerMask(d) != 31) d = d.minusDays(1)
        var n = 0
        while (prefs.trackerMask(d) == 31 && n < 3650) {
            n += 1
            d = d.minusDays(1)
        }
        return n
    }
}
