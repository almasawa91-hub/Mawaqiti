@file:OptIn(ExperimentalMaterial3Api::class)

package com.mawaqiti.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ───────────────────────── Theme ─────────────────────────

val Gold = Color(0xFFC8A24A)

/** mode: 0 النظام، 1 فاتح، 2 داكن */
@Composable
fun AppTheme(mode: Int, content: @Composable () -> Unit) {
    val dark = when (mode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val scheme = if (dark) {
        darkColorScheme(
            primary = Color(0xFF6FD3A3), onPrimary = Color(0xFF003822),
            primaryContainer = Color(0xFF14532D), onPrimaryContainer = Color(0xFFD1F5E0),
            secondary = Gold, background = Color(0xFF0C1511), surface = Color(0xFF0C1511),
            surfaceVariant = Color(0xFF1B2B24), onSurface = Color(0xFFE5F0EA),
            onSurfaceVariant = Color(0xFFA7BDB3), onBackground = Color(0xFFE5F0EA)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF14532D), onPrimary = Color.White,
            primaryContainer = Color(0xFFD1F5E0), onPrimaryContainer = Color(0xFF052E16),
            secondary = Gold, background = Color(0xFFF7FAF8), surface = Color(0xFFF7FAF8),
            surfaceVariant = Color(0xFFE3EEE7), onSurface = Color(0xFF101C16),
            onSurfaceVariant = Color(0xFF475A50), onBackground = Color(0xFF101C16)
        )
    }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
        }
    }
}

// ───────────────────────── Root ─────────────────────────

@Composable
fun MawaqitiApp(vm: PrayerViewModel) {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var moreSub by rememberSaveable { mutableIntStateOf(0) }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val requestNotif: () -> Unit = {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res.values.any { it }) vm.fetchLocation()
        else vm.setMessage("لم تُمنح صلاحية الموقع. اختر مدينة يدوياً من الإعدادات.")
        requestNotif()
    }
    val requestGps: () -> Unit = {
        locLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
    }

    LaunchedEffect(Unit) {
        if (!vm.settings.located) requestGps()
        else {
            requestNotif()
            if (vm.settings.gps) vm.fetchLocation()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(tab == 0, { tab = 0 }, { Icon(Icons.Filled.Schedule, null) }, label = { Text("المواقيت") })
                NavigationBarItem(tab == 1, { tab = 1 }, { Icon(Icons.Filled.Explore, null) }, label = { Text("القبلة") })
                NavigationBarItem(tab == 2, { tab = 2 }, { Icon(Icons.Filled.MenuBook, null) }, label = { Text("الأذكار") })
                NavigationBarItem(tab == 3, { tab = 3 }, { Icon(Icons.Filled.TouchApp, null) }, label = { Text("المسبحة") })
                NavigationBarItem(tab == 4, { tab = 4 }, { Icon(Icons.Filled.MoreHoriz, null) }, label = { Text("المزيد") })
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> TimesScreen(vm, requestGps, requestNotif) { moreSub = 1; tab = 4 }
                1 -> QiblaScreen(vm)
                2 -> AzkarScreen(vm)
                3 -> TasbeehScreen(vm)
                else -> MoreScreen(vm, moreSub, { moreSub = it }, requestGps, requestNotif)
            }
        }
    }
}

// ───────────────────────── Times ─────────────────────────

private fun prayerIcon(p: Prayer): ImageVector = when (p) {
    Prayer.FAJR -> Icons.Filled.NightsStay
    Prayer.SUNRISE -> Icons.Filled.WbTwilight
    Prayer.DHUHR -> Icons.Filled.LightMode
    Prayer.ASR -> Icons.Filled.WbSunny
    Prayer.MAGHRIB -> Icons.Filled.WbTwilight
    Prayer.ISHA -> Icons.Filled.Bedtime
}

@Composable
fun TimesScreen(vm: PrayerViewModel, onRequestGps: () -> Unit, onRequestNotif: () -> Unit, onOpenSettings: () -> Unit) {
    val s = vm.settings
    val ctx = LocalContext.current
    val zone = remember(s.tz) { vm.zone() }
    var now by remember(zone) { mutableStateOf(ZonedDateTime.now(zone)) }
    LaunchedEffect(zone) {
        while (true) {
            now = ZonedDateTime.now(zone)
            if (now.toLocalDate() != vm.todayDate) vm.recompute()
            delay(1000)
        }
    }

    val today = vm.today
    val tomorrow = vm.tomorrow
    val next: Pair<Prayer, ZonedDateTime>? =
        today?.entries?.firstOrNull { it.key.notifiable && it.value.isAfter(now) }?.let { it.key to it.value }
            ?: tomorrow?.get(Prayer.FAJR)?.let { Prayer.FAJR to it }
    val fmt = remember(s.use24h) {
        DateTimeFormatter.ofPattern(if (s.use24h) "HH:mm" else "hh:mm a", Locale("ar"))
    }
    val dateText = remember(now.toLocalDate()) {
        now.format(DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", Locale("ar")))
    }
    val hijriText = PrayerCalc.hijriText(now.toLocalDate(), s.hijriOffset)

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("مواقيتي", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(dateText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (hijriText.isNotBlank()) Text(hijriText, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    Row(
                        Modifier.padding(top = 4.dp).clickable { onOpenSettings() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.LocationOn, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text(s.place, color = MaterialTheme.colorScheme.primary)
                    }
                }
                if (today != null) {
                    IconButton(onClick = {
                        val body = today.entries.joinToString("\n") {
                            "${PrayerCalc.label(it.key, it.value.toLocalDate())}: ${it.value.format(fmt)}"
                        }
                        val text = "مواقيت الصلاة – ${s.place}\n$dateText\n$hijriText\n\n$body"
                        val i = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        ctx.startActivity(Intent.createChooser(i, null))
                    }) { Icon(Icons.Filled.Share, "مشاركة المواقيت") }
                }
            }
        }

        if (!s.located) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("حدّد موقعك لضبط المواقيت بدقة", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onRequestGps) { Text("تحديد تلقائي") }
                            OutlinedButton(onClick = onOpenSettings) { Text("اختيار مدينة") }
                        }
                        vm.locationMsg?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp)) }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (next == null) {
                        Text("تعذر حساب المواقيت لهذا الموقع", color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        val secs = Duration.between(now, next.second).seconds.coerceAtLeast(0)
                        Text("الصلاة القادمة", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                        Text(
                            PrayerCalc.label(next.first, next.second.toLocalDate()),
                            fontSize = 34.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(next.second.format(fmt), fontSize = 20.sp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            String.format(Locale.US, "%02d:%02d:%02d", secs / 3600, (secs % 3600) / 60, secs % 60),
                            fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Gold
                        )
                        Text("متبقي", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    }
                }
            }
        }

        if (now.dayOfWeek == DayOfWeek.FRIDAY) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(
                        "يوم الجمعة: لا تنسَ قراءة سورة الكهف، والإكثار من الصلاة على النبي ﷺ، والاغتسال والتبكير إلى الجمعة.",
                        Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (today != null) {
            items(today.entries.toList(), key = { it.key.name }) { (p, time) ->
                val isNext = next != null && next.first == p && next.second == time
                val passed = time.isBefore(now)
                val alpha = if (passed) 0.55f else 1f
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isNext) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            prayerIcon(p), null,
                            tint = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            PrayerCalc.label(p, time.toLocalDate()), Modifier.weight(1f), fontSize = 20.sp,
                            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                        )
                        Text(
                            time.format(fmt), fontSize = 20.sp,
                            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                        )
                        if (p.notifiable) {
                            val on = p.name in s.notify
                            IconButton(onClick = {
                                vm.update(s.copy(notify = if (on) s.notify - p.name else s.notify + p.name))
                                if (!on) onRequestNotif()
                            }) {
                                Icon(
                                    if (on) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                                    "تنبيه",
                                    tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Spacer(Modifier.width(48.dp))
                        }
                    }
                }
            }
        }
    }
}
