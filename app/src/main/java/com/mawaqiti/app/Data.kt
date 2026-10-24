package com.mawaqiti.app

import android.content.Context

data class City(val name: String, val lat: Double, val lon: Double, val tz: String, val method: Int)

/** method: 0 رابطة العالم، 1 مصرية، 2 أم القرى، 3 كراتشي، 4 ISNA، 5 دبي */
val Cities = listOf(
    City("مكة المكرمة", 21.4225, 39.8262, "Asia/Riyadh", 2),
    City("المدينة المنورة", 24.4672, 39.6024, "Asia/Riyadh", 2),
    City("الرياض", 24.7136, 46.6753, "Asia/Riyadh", 2),
    City("جدة", 21.4858, 39.1925, "Asia/Riyadh", 2),
    City("الدمام", 26.4207, 50.0888, "Asia/Riyadh", 2),
    City("القاهرة", 30.0444, 31.2357, "Africa/Cairo", 1),
    City("الإسكندرية", 31.2001, 29.9187, "Africa/Cairo", 1),
    City("بغداد", 33.3152, 44.3661, "Asia/Baghdad", 1),
    City("دمشق", 33.5138, 36.2765, "Asia/Damascus", 1),
    City("عمّان", 31.9454, 35.9284, "Asia/Amman", 1),
    City("القدس", 31.7683, 35.2137, "Asia/Jerusalem", 1),
    City("غزة", 31.5017, 34.4668, "Asia/Gaza", 1),
    City("بيروت", 33.8938, 35.5018, "Asia/Beirut", 1),
    City("الكويت", 29.3759, 47.9774, "Asia/Kuwait", 2),
    City("الدوحة", 25.2854, 51.5310, "Asia/Qatar", 2),
    City("المنامة", 26.2285, 50.5860, "Asia/Bahrain", 2),
    City("أبوظبي", 24.4539, 54.3773, "Asia/Dubai", 5),
    City("دبي", 25.2048, 55.2708, "Asia/Dubai", 5),
    City("مسقط", 23.5880, 58.3829, "Asia/Muscat", 2),
    City("صنعاء", 15.3694, 44.1910, "Asia/Aden", 2),
    City("الخرطوم", 15.5007, 32.5599, "Africa/Khartoum", 1),
    City("طرابلس", 32.8872, 13.1913, "Africa/Tripoli", 1),
    City("تونس", 36.8065, 10.1815, "Africa/Tunis", 0),
    City("الجزائر", 36.7538, 3.0588, "Africa/Algiers", 0),
    City("الرباط", 34.0209, -6.8416, "Africa/Casablanca", 0),
    City("الدار البيضاء", 33.5731, -7.5898, "Africa/Casablanca", 0),
    City("نواكشوط", 18.0735, -15.9582, "Africa/Nouakchott", 0),
    City("مقديشو", 2.0469, 45.3182, "Africa/Mogadishu", 0),
    City("إسطنبول", 41.0082, 28.9784, "Europe/Istanbul", 0),
    City("إسلام آباد", 33.6844, 73.0479, "Asia/Karachi", 3),
    City("جاكرتا", -6.2088, 106.8456, "Asia/Jakarta", 0),
    City("كوالالمبور", 3.1390, 101.6869, "Asia/Kuala_Lumpur", 0),
    City("لندن", 51.5072, -0.1276, "Europe/London", 0),
    City("باريس", 48.8566, 2.3522, "Europe/Paris", 0),
    City("برلين", 52.5200, 13.4050, "Europe/Berlin", 0),
    City("نيويورك", 40.7128, -74.0060, "America/New_York", 4),
    City("تورونتو", 43.6532, -79.3832, "America/Toronto", 4)
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
    val notify: Set<String> = setOf("FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA")
)

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("mawaqiti", Context.MODE_PRIVATE)

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
            notify = sp.getStringSet("notify", null)?.toSet() ?: d.notify
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
            .apply()
    }

    var tasbeeh: Int
        get() = sp.getInt("tasbeeh", 0)
        set(v) { sp.edit().putInt("tasbeeh", v).apply() }
}
