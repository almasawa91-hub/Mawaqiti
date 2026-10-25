package com.mawaqiti.app

import android.content.Context
import java.time.LocalDate

data class City(val name: String, val lat: Double, val lon: Double, val tz: String, val method: Int)

/** method: 0 رابطة العالم، 1 مصرية، 2 أم القرى، 3 كراتشي، 4 ISNA، 5 دبي */
val Cities = listOf(
    City("مكة المكرمة", 21.4225, 39.8262, "Asia/Riyadh", 2),
    City("المدينة المنورة", 24.4672, 39.6024, "Asia/Riyadh", 2),
    City("الرياض", 24.7136, 46.6753, "Asia/Riyadh", 2),
    City("جدة", 21.4858, 39.1925, "Asia/Riyadh", 2),
    City("الدمام", 26.4207, 50.0888, "Asia/Riyadh", 2),
    City("أبها", 18.2164, 42.5053, "Asia/Riyadh", 2),
    City("تبوك", 28.3835, 36.5662, "Asia/Riyadh", 2),
    City("القاهرة", 30.0444, 31.2357, "Africa/Cairo", 1),
    City("الإسكندرية", 31.2001, 29.9187, "Africa/Cairo", 1),
    City("بغداد", 33.3152, 44.3661, "Asia/Baghdad", 1),
    City("البصرة", 30.5085, 47.7804, "Asia/Baghdad", 1),
    City("الموصل", 36.3400, 43.1300, "Asia/Baghdad", 1),
    City("أربيل", 36.1911, 44.0092, "Asia/Baghdad", 1),
    City("دمشق", 33.5138, 36.2765, "Asia/Damascus", 1),
    City("حلب", 36.2021, 37.1343, "Asia/Damascus", 1),
    City("عمّان", 31.9454, 35.9284, "Asia/Amman", 1),
    City("القدس", 31.7683, 35.2137, "Asia/Jerusalem", 1),
    City("رام الله", 31.9038, 35.2034, "Asia/Jerusalem", 1),
    City("غزة", 31.5017, 34.4668, "Asia/Gaza", 1),
    City("بيروت", 33.8938, 35.5018, "Asia/Beirut", 1),
    City("الكويت", 29.3759, 47.9774, "Asia/Kuwait", 2),
    City("الدوحة", 25.2854, 51.5310, "Asia/Qatar", 2),
    City("المنامة", 26.2285, 50.5860, "Asia/Bahrain", 2),
    City("أبوظبي", 24.4539, 54.3773, "Asia/Dubai", 5),
    City("دبي", 25.2048, 55.2708, "Asia/Dubai", 5),
    City("مسقط", 23.5880, 58.3829, "Asia/Muscat", 2),
    City("صنعاء", 15.3694, 44.1910, "Asia/Aden", 2),
    City("عدن", 12.7855, 45.0187, "Asia/Aden", 2),
    City("الخرطوم", 15.5007, 32.5599, "Africa/Khartoum", 1),
    City("طرابلس", 32.8872, 13.1913, "Africa/Tripoli", 1),
    City("بنغازي", 32.1167, 20.0667, "Africa/Tripoli", 1),
    City("تونس", 36.8065, 10.1815, "Africa/Tunis", 0),
    City("الجزائر", 36.7538, 3.0588, "Africa/Algiers", 0),
    City("وهران", 35.6971, -0.6308, "Africa/Algiers", 0),
    City("الرباط", 34.0209, -6.8416, "Africa/Casablanca", 0),
    City("الدار البيضاء", 33.5731, -7.5898, "Africa/Casablanca", 0),
    City("فاس", 34.0181, -5.0078, "Africa/Casablanca", 0),
    City("مراكش", 31.6295, -7.9811, "Africa/Casablanca", 0),
    City("نواكشوط", 18.0735, -15.9582, "Africa/Nouakchott", 0),
    City("مقديشو", 2.0469, 45.3182, "Africa/Mogadishu", 0),
    City("إسطنبول", 41.0082, 28.9784, "Europe/Istanbul", 0),
    City("أنقرة", 39.9334, 32.8597, "Europe/Istanbul", 0),
    City("إسلام آباد", 33.6844, 73.0479, "Asia/Karachi", 3),
    City("كراتشي", 24.8607, 67.0011, "Asia/Karachi", 3),
    City("لاهور", 31.5204, 74.3587, "Asia/Karachi", 3),
    City("دلهي", 28.6139, 77.2090, "Asia/Kolkata", 3),
    City("دكا", 23.8103, 90.4125, "Asia/Dhaka", 3),
    City("جاكرتا", -6.2088, 106.8456, "Asia/Jakarta", 0),
    City("كوالالمبور", 3.1390, 101.6869, "Asia/Kuala_Lumpur", 0),
    City("سنغافورة", 1.3521, 103.8198, "Asia/Singapore", 0),
    City("لندن", 51.5072, -0.1276, "Europe/London", 0),
    City("باريس", 48.8566, 2.3522, "Europe/Paris", 0),
    City("برلين", 52.5200, 13.4050, "Europe/Berlin", 0),
    City("مدريد", 40.4168, -3.7038, "Europe/Madrid", 0),
    City("روما", 41.9028, 12.4964, "Europe/Rome", 0),
    City("أمستردام", 52.3676, 4.9041, "Europe/Amsterdam", 0),
    City("موسكو", 55.7558, 37.6173, "Europe/Moscow", 0),
    City("نيويورك", 40.7128, -74.0060, "America/New_York", 4),
    City("شيكاغو", 41.8781, -87.6298, "America/Chicago", 4),
    City("لوس أنجلوس", 34.0522, -118.2437, "America/Los_Angeles", 4),
    City("تورونتو", 43.6532, -79.3832, "America/Toronto", 4),
    City("سيدني", -33.8688, 151.2093, "Australia/Sydney", 0)
)

data class Settings(
    val lat: Double = 21.4225,
    val lon: Double = 39.8262,
    val place: String = "مكة المكرمة",
    val tz: String = "Asia/Riyadh",
    val located: Boolean = false,
    val gps: Boolean = false,
    val method: Int = 2,
    val hanafi: Boolean = false,
    val use24h: Boolean = false,
    val notify: Set<String> = setOf("FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA"),
    /** تعديل يدوي بالدقائق لكل صلاة (مفتاحه اسم Prayer) */
    val adjust: Map<String, Int> = emptyMap(),
    val hijriOffset: Int = 0,
    /** تنبيه مسبق قبل الصلاة بعدد دقائق (0 = بدون) */
    val preMinutes: Int = 0,
    val azkarAlerts: Boolean = false,
    /** 0 النظام، 1 فاتح، 2 داكن */
    val theme: Int = 0,
    val azkarFont: Int = 20,
    /** مصدر الأذان العام: off | raw:<اسم> | file:<ملف> */
    val adhan: String = "off",
    /** أذان الفجر: فارغ = نفس الأذان العام */
    val fajrAdhan: String = ""
)

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("mawaqiti", Context.MODE_PRIVATE)

    private fun encodeAdjust(m: Map<String, Int>): String =
        m.entries.joinToString(";") { "${it.key}=${it.value}" }

    private fun decodeAdjust(s: String?): Map<String, Int> {
        if (s.isNullOrBlank()) return emptyMap()
        val out = HashMap<String, Int>()
        for (part in s.split(";")) {
            val kv = part.split("=")
            if (kv.size == 2) kv[1].toIntOrNull()?.let { out[kv[0]] = it }
        }
        return out
    }

    fun load(): Settings {
        val d = Settings()
        return Settings(
            lat = sp.getString("lat", null)?.toDoubleOrNull() ?: d.lat,
            lon = sp.getString("lon", null)?.toDoubleOrNull() ?: d.lon,
            place = sp.getString("place", null) ?: d.place,
            tz = sp.getString("tz", null) ?: d.tz,
            located = sp.getBoolean("located", false),
            gps = sp.getBoolean("gps", false),
            method = sp.getInt("method", d.method),
            hanafi = sp.getBoolean("hanafi", false),
            use24h = sp.getBoolean("use24h", false),
            notify = sp.getStringSet("notify", null)?.toSet() ?: d.notify,
            adjust = decodeAdjust(sp.getString("adjust", null)),
            hijriOffset = sp.getInt("hijriOffset", 0),
            preMinutes = sp.getInt("preMinutes", 0),
            azkarAlerts = sp.getBoolean("azkarAlerts", false),
            theme = sp.getInt("theme", 0),
            azkarFont = sp.getInt("azkarFont", 20),
            adhan = sp.getString("adhan", null) ?: "off",
            fajrAdhan = sp.getString("fajrAdhan", null) ?: ""
        )
    }

    fun save(s: Settings) {
        sp.edit()
            .putString("lat", s.lat.toString())
            .putString("lon", s.lon.toString())
            .putString("place", s.place)
            .putString("tz", s.tz)
            .putBoolean("located", s.located)
            .putBoolean("gps", s.gps)
            .putInt("method", s.method)
            .putBoolean("hanafi", s.hanafi)
            .putBoolean("use24h", s.use24h)
            .putStringSet("notify", HashSet(s.notify))
            .putString("adjust", encodeAdjust(s.adjust))
            .putInt("hijriOffset", s.hijriOffset)
            .putInt("preMinutes", s.preMinutes)
            .putBoolean("azkarAlerts", s.azkarAlerts)
            .putInt("theme", s.theme)
            .putInt("azkarFont", s.azkarFont)
            .putString("adhan", s.adhan)
            .putString("fajrAdhan", s.fajrAdhan)
            .apply()
    }

    // ----- الأذان المضاف من الذاكرة: "file:a.mp3|الاسم;file:b.ogg|الاسم" -----
    fun customAdhans(): List<Pair<String, String>> {
        val raw = sp.getString("customAdhans", null)
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(";").mapNotNull {
            val kv = it.split("|")
            if (kv.size == 2 && kv[0].isNotBlank()) kv[0] to kv[1] else null
        }
    }

    fun setCustomAdhans(list: List<Pair<String, String>>) {
        sp.edit().putString("customAdhans", list.joinToString(";") { "${it.first}|${it.second}" }).apply()
    }

    // ----- المسبحة -----
    fun tasbeehCount(i: Int): Int = sp.getInt("tc_$i", 0)
    fun setTasbeehCount(i: Int, v: Int) {
        sp.edit().putInt("tc_$i", v).apply()
    }

    // ----- متتبع الصلوات (قناع بتات: 5 صلوات) -----
    fun trackerMask(d: LocalDate): Int = sp.getInt("t_$d", 0)
    fun setTrackerMask(d: LocalDate, m: Int) {
        sp.edit().putInt("t_$d", m).apply()
    }
}
