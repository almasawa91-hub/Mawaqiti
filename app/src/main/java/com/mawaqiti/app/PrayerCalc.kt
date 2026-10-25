package com.mawaqiti.app

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.chrono.HijrahChronology
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

enum class Prayer(val ar: String, val notifiable: Boolean) {
    FAJR("الفجر", true),
    SUNRISE("الشروق", false),
    DHUHR("الظهر", true),
    ASR("العصر", true),
    MAGHRIB("المغرب", true),
    ISHA("العشاء", true)
}

data class CalcMethod(
    val name: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaMinutes: Int = 0,
    val ishaRamadanMinutes: Int = 0
)

val HijriMonths = listOf(
    "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
    "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
)

object PrayerCalc {

    const val KAABA_LAT = 21.422487
    const val KAABA_LON = 39.826206

    val methods = listOf(
        CalcMethod("رابطة العالم الإسلامي", 18.0, 17.0),
        CalcMethod("الهيئة المصرية العامة للمساحة", 19.5, 17.5),
        CalcMethod("أم القرى – مكة المكرمة", 18.5, 0.0, 90, 120),
        CalcMethod("جامعة العلوم الإسلامية – كراتشي", 18.0, 18.0),
        CalcMethod("أمريكا الشمالية (ISNA)", 15.0, 15.0),
        CalcMethod("الإمارات (دبي)", 18.2, 18.2)
    )

    // ---------- math helpers (degrees) ----------
    private fun rad(d: Double) = Math.toRadians(d)
    private fun deg(r: Double) = Math.toDegrees(r)
    private fun sinD(d: Double) = sin(rad(d))
    private fun cosD(d: Double) = cos(rad(d))
    private fun tanD(d: Double) = tan(rad(d))
    private fun arcsinD(x: Double) = deg(asin(x))
    private fun arccosD(x: Double) = deg(acos(x))
    private fun arctan2D(y: Double, x: Double) = deg(atan2(y, x))
    private fun arccotD(x: Double) = deg(atan(1.0 / x))
    private fun fixAngle(a: Double) = a - 360.0 * floor(a / 360.0)
    private fun fixHour(a: Double) = a - 24.0 * floor(a / 24.0)

    private fun julian(date: LocalDate): Double {
        var y = date.year
        var m = date.monthValue
        val d = date.dayOfMonth.toDouble()
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + d + b - 1524.5
    }

    /** returns declination (deg) and equation of time (hours) */
    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sinD(g) + 0.020 * sinD(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = arctan2D(cosD(e) * sinD(l), cosD(l)) / 15.0
        val eqt = q / 15.0 - fixHour(ra)
        val decl = arcsinD(sinD(e) * sinD(l))
        return decl to eqt
    }

    private fun midDay(jd: Double, t: Double) = fixHour(12.0 - sunPosition(jd + t).second)

    private fun sunAngleTime(jd: Double, lat: Double, angle: Double, t: Double, ccw: Boolean): Double {
        val decl = sunPosition(jd + t).first
        val noon = midDay(jd, t)
        val x = (-sinD(angle) - sinD(decl) * sinD(lat)) / (cosD(decl) * cosD(lat))
        if (x < -1.0 || x > 1.0 || x.isNaN()) return Double.NaN
        val t2 = arccosD(x) / 15.0
        return noon + if (ccw) -t2 else t2
    }

    private fun asrTime(jd: Double, lat: Double, factor: Double, t: Double): Double {
        val decl = sunPosition(jd + t).first
        val angle = -arccotD(factor + tanD(abs(lat - decl)))
        return sunAngleTime(jd, lat, angle, t, false)
    }

    // ---------- public API ----------
    fun hijri(date: LocalDate, offset: Int = 0): Triple<Int, Int, Int>? = runCatching {
        val h = HijrahChronology.INSTANCE.date(date.plusDays(offset.toLong()))
        Triple(h.get(ChronoField.YEAR), h.get(ChronoField.MONTH_OF_YEAR), h.get(ChronoField.DAY_OF_MONTH))
    }.getOrNull()

    fun hijriText(date: LocalDate, offset: Int = 0): String {
        val h = hijri(date, offset) ?: return ""
        return "${h.third} ${HijriMonths[(h.second - 1).coerceIn(0, 11)]} ${h.first} هـ"
    }

    /** تواريخ (ميلادية) أيام شهر هجري كامل */
    fun hijriMonth(year: Int, month: Int, offset: Int): List<LocalDate>? = runCatching {
        val first = HijrahChronology.INSTANCE.date(year, month, 1)
        val len = first.lengthOfMonth()
        val start = LocalDate.from(first).minusDays(offset.toLong())
        (0 until len).map { start.plusDays(it.toLong()) }
    }.getOrNull()

    fun occasion(month: Int, day: Int): String? = when {
        month == 1 && day == 1 -> "رأس السنة الهجرية"
        month == 1 && day == 9 -> "تاسوعاء (يُستحب صيامه)"
        month == 1 && day == 10 -> "يوم عاشوراء (يُستحب صيامه)"
        month == 3 && day == 12 -> "ذكرى المولد النبوي"
        month == 7 && day == 27 -> "ذكرى الإسراء والمعراج"
        month == 9 && day == 1 -> "أول أيام رمضان"
        month == 9 && day == 21 -> "بداية العشر الأواخر من رمضان"
        month == 10 && day == 1 -> "عيد الفطر"
        month == 12 && day == 1 -> "بداية العشر الأوائل من ذي الحجة"
        month == 12 && day == 9 -> "يوم عرفة (يُستحب صيامه لغير الحاج)"
        month == 12 && day == 10 -> "عيد الأضحى"
        month == 12 && day in 11..13 -> "أيام التشريق"
        month != 9 && month != 12 && day == 13 -> "بداية الأيام البيض (13–15) يُستحب صيامها"
        else -> null
    }

    fun label(p: Prayer, date: LocalDate): String =
        if (p == Prayer.DHUHR && date.dayOfWeek == DayOfWeek.FRIDAY) "الجمعة" else p.ar

    fun nextPrayer(s: Settings, now: ZonedDateTime): Pair<Prayer, ZonedDateTime>? {
        for (off in 0..1) {
            val day = forSettings(now.toLocalDate().plusDays(off.toLong()), s) ?: continue
            val c = day.entries.firstOrNull { it.key.notifiable && it.value.isAfter(now) }
            if (c != null) return c.key to c.value
        }
        return null
    }

    fun forSettings(date: LocalDate, s: Settings): Map<Prayer, ZonedDateTime>? {
        val zone = runCatching { ZoneId.of(s.tz) }.getOrDefault(ZoneId.systemDefault())
        val method = methods.getOrElse(s.method) { methods[0] }
        val base = compute(date, s.lat, s.lon, zone, method, s.hanafi) ?: return null
        if (s.adjust.isEmpty()) return base
        val out = LinkedHashMap<Prayer, ZonedDateTime>()
        for ((p, t) in base) out[p] = t.plusMinutes((s.adjust[p.name] ?: 0).toLong())
        return out
    }

    fun compute(
        date: LocalDate, lat: Double, lon: Double, zone: ZoneId,
        method: CalcMethod, hanafi: Boolean
    ): Map<Prayer, ZonedDateTime>? {
        val jd = julian(date) - lon / (15.0 * 24.0)
        val init = listOf(5.0, 6.0, 12.0, 13.0, 18.0, 18.0).map { it / 24.0 }
        var guess = init
        var r = DoubleArray(6)
        repeat(2) {
            r = doubleArrayOf(
                sunAngleTime(jd, lat, method.fajrAngle, guess[0], true),
                sunAngleTime(jd, lat, 0.833, guess[1], true),
                midDay(jd, guess[2]),
                asrTime(jd, lat, if (hanafi) 2.0 else 1.0, guess[3]),
                sunAngleTime(jd, lat, 0.833, guess[4], false),
                if (method.ishaMinutes > 0) Double.NaN
                else sunAngleTime(jd, lat, method.ishaAngle, guess[5], false)
            )
            guess = r.mapIndexed { i, v -> if (v.isNaN()) init[i] else v / 24.0 }
        }

        val tz = zone.rules.getOffset(date.atTime(12, 0)).totalSeconds / 3600.0
        val off = tz - lon / 15.0
        var fajr = r[0] + off
        val sunrise = r[1] + off
        val dhuhr = r[2] + off + 1.0 / 60.0
        val asr = r[3] + off
        val sunset = r[4] + off
        var isha = r[5] + off

        if (sunrise.isNaN() || sunset.isNaN() || dhuhr.isNaN() || asr.isNaN()) return null

        val night = 24.0 - (sunset - sunrise)
        if (fajr.isNaN()) fajr = sunrise - night * (method.fajrAngle / 60.0)

        val ishaMin = if (method.ishaMinutes > 0) {
            val ramadan = hijri(date)?.second == 9
            if (ramadan && method.ishaRamadanMinutes > 0) method.ishaRamadanMinutes else method.ishaMinutes
        } else 0
        isha = when {
            ishaMin > 0 -> sunset + ishaMin / 60.0
            isha.isNaN() -> sunset + night * (method.ishaAngle / 60.0)
            else -> isha
        }

        fun at(h: Double): ZonedDateTime =
            date.atStartOfDay()
                .plusSeconds((h * 3600.0).roundToLong())
                .plusSeconds(30)
                .truncatedTo(ChronoUnit.MINUTES)
                .atZone(zone)

        return linkedMapOf(
            Prayer.FAJR to at(fajr),
            Prayer.SUNRISE to at(sunrise),
            Prayer.DHUHR to at(dhuhr),
            Prayer.ASR to at(asr),
            Prayer.MAGHRIB to at(sunset),
            Prayer.ISHA to at(isha)
        )
    }

    /** Bearing (degrees from true north) towards the Kaaba. */
    fun qiblaBearing(lat: Double, lon: Double): Double {
        val p1 = rad(lat)
        val p2 = rad(KAABA_LAT)
        val dl = rad(KAABA_LON - lon)
        val y = sin(dl)
        val x = cos(p1) * tan(p2) - sin(p1) * cos(dl)
        return (deg(atan2(y, x)) + 360.0) % 360.0
    }

    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = rad(lat2 - lat1)
        val dLon = rad(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(rad(lat1)) * cos(rad(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * r * asin(sqrt(a))
    }
}
