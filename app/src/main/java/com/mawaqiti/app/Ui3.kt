@file:OptIn(ExperimentalMaterial3Api::class)

package com.mawaqiti.app

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings as SettingsIcon
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

// ───────────────────────── مشتركات ─────────────────────────

@Composable
fun SubHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") }
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

// ───────────────────────── المزيد ─────────────────────────

@Composable
fun MoreScreen(
    vm: PrayerViewModel, sub: Int, onSub: (Int) -> Unit,
    onRequestGps: () -> Unit, onRequestNotif: () -> Unit
) {
    if (sub != 0) BackHandler { vm.stopPreview(); onSub(0) }
    when (sub) {
        1 -> SettingsScreen(vm, onRequestGps, onRequestNotif) { vm.stopPreview(); onSub(0) }
        2 -> CalendarScreen(vm) { onSub(0) }
        3 -> TrackerScreen(vm) { onSub(0) }
        4 -> MonthTableScreen(vm) { onSub(0) }
        5 -> NamesScreen { onSub(0) }
        else -> MoreMenu(onSub)
    }
}

private data class MenuItem(val title: String, val subtitle: String, val icon: ImageVector, val sub: Int)

@Composable
private fun MoreMenu(onSub: (Int) -> Unit) {
    val entries = listOf(
        MenuItem("الإعدادات", "الموقع، الحساب، الأذان والمؤذنون، التنبيهات", Icons.Filled.SettingsIcon, 1),
        MenuItem("التقويم الهجري", "التاريخ والمناسبات الإسلامية", Icons.Filled.CalendarMonth, 2),
        MenuItem("متتبع الصلوات", "سجّل صلواتك واحتفظ بسلسلة أيامك", Icons.Filled.TaskAlt, 3),
        MenuItem("جدول الشهر", "مواقيت كل أيام الشهر", Icons.Filled.TableChart, 4),
        MenuItem("أسماء الله الحسنى", "التسعة والتسعون اسماً", Icons.Filled.AutoAwesome, 5)
    )
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("المزيد", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        items(entries) { m ->
            Card(
                onClick = { onSub(m.sub) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(m.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(m.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(m.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ───────────────────────── الإعدادات ─────────────────────────

@Composable
private fun AdhanRow(
    label: String, id: String, selected: Boolean, previewing: Boolean,
    onSelect: () -> Unit, onPreview: (() -> Unit)?, onDelete: (() -> Unit)?
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, Modifier.weight(1f))
        if (onPreview != null) {
            IconButton(onClick = onPreview) {
                Icon(if (previewing) Icons.Filled.Stop else Icons.Filled.PlayArrow, "استماع")
            }
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "حذف", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun AdhanPickerDialog(
    vm: PrayerViewModel, title: String, selected: String, allowSame: Boolean,
    onSelect: (String) -> Unit, onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val builtins = remember { AdhanSources.builtins(ctx) }
    val customs = vm.customAdhans
    AlertDialog(
        onDismissRequest = { vm.stopPreview(); onDismiss() },
        confirmButton = { TextButton(onClick = { vm.stopPreview(); onDismiss() }) { Text("تم") } },
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.heightIn(max = 440.dp)) {
                if (allowSame) {
                    item { AdhanRow("نفس الأذان العام", "", selected == "", false, { onSelect("") }, null, null) }
                }
                item { AdhanRow("تنبيه عادي (صوت الإشعار)", "off", selected == "off", false, { onSelect("off") }, null, null) }
                if (builtins.isEmpty() && customs.isEmpty()) {
                    item {
                        Text(
                            "لا توجد أصوات أذان بعد. اضغط «إضافة أذان من الهاتف» لاختيار ملف صوتي من ذاكرة جهازك.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
                items(builtins) { (id, label) ->
                    AdhanRow(label, id, selected == id, vm.previewId == id, { onSelect(id) }, { vm.preview(id) }, null)
                }
                items(customs) { (id, label) ->
                    AdhanRow(label, id, selected == id, vm.previewId == id, { onSelect(id) }, { vm.preview(id) }, { vm.deleteCustomAdhan(id) })
                }
            }
        }
    )
}

@Composable
fun SettingsScreen(vm: PrayerViewModel, onRequestGps: () -> Unit, onRequestNotif: () -> Unit, onBack: () -> Unit) {
    val s = vm.settings
    val ctx = LocalContext.current
    var dialog by remember { mutableIntStateOf(0) } // 1 مدينة، 2 طريقة، 3 تعديل المواقيت، 4 أذان عام، 5 أذان الفجر

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.addCustomAdhan(uri)
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        SubHeader("الإعدادات", onBack)
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard("الموقع") {
                    Text(s.place, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (s.gps) "حسب موقعك الحالي" else "مدينة مختارة يدوياً",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (vm.locating) LinearProgressIndicator(Modifier.fillMaxWidth())
                    vm.locationMsg?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onRequestGps) {
                            Icon(Icons.Filled.MyLocation, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("تحديد تلقائي")
                        }
                        OutlinedButton(onClick = { dialog = 1 }) { Text("اختيار مدينة") }
                    }
                }
            }

            item {
                SectionCard("طريقة الحساب") {
                    Text(PrayerCalc.methods.getOrElse(s.method) { PrayerCalc.methods[0] }.name)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { dialog = 2 }) { Text("تغيير الطريقة") }
                        OutlinedButton(onClick = { dialog = 3 }) { Text("تعديل يدوي للمواقيت") }
                    }
                    Text("وقت العصر", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !s.hanafi, onClick = { vm.update(s.copy(hanafi = false)) }, label = { Text("الجمهور") })
                        FilterChip(selected = s.hanafi, onClick = { vm.update(s.copy(hanafi = true)) }, label = { Text("الحنفي") })
                    }
                    HorizontalDivider()
                    Text("التاريخ الهجري: ${PrayerCalc.hijriText(LocalDate.now(vm.zone()), s.hijriOffset)}")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("تعديل بالأيام", Modifier.weight(1f))
                        IconButton(onClick = { vm.updateUi(s.copy(hijriOffset = (s.hijriOffset - 1).coerceIn(-2, 2))) }) {
                            Icon(Icons.Filled.Remove, "أقل")
                        }
                        Text(if (s.hijriOffset > 0) "+${s.hijriOffset}" else "${s.hijriOffset}", Modifier.width(36.dp), textAlign = TextAlign.Center)
                        IconButton(onClick = { vm.updateUi(s.copy(hijriOffset = (s.hijriOffset + 1).coerceIn(-2, 2))) }) {
                            Icon(Icons.Filled.Add, "أكثر")
                        }
                    }
                }
            }

            item {
                SectionCard("صوت الأذان والمؤذنون") {
                    Text("الأذان العام: ${AdhanSources.label(ctx, s.adhan)}", fontWeight = FontWeight.Bold)
                    Text("أذان الفجر: ${AdhanSources.label(ctx, s.fajrAdhan)}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { dialog = 4 }) { Text("اختيار المؤذن") }
                        OutlinedButton(onClick = { dialog = 5 }) { Text("أذان الفجر") }
                    }
                    OutlinedButton(onClick = { picker.launch(arrayOf("audio/*")) }) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("إضافة أذان من الهاتف")
                    }
                    vm.adhanMsg?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                    Text(
                        "يُشغَّل الأذان كاملاً عند دخول الوقت مع زر «إيقاف الأذان» في الإشعار. الملفات المضافة تُنسخ داخل التطبيق ولا تحتاج صلاحيات.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                SectionCard("التنبيهات") {
                    Prayer.values().filter { it.notifiable }.forEach { p ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(p.ar, Modifier.weight(1f), fontSize = 17.sp)
                            Switch(
                                checked = p.name in s.notify,
                                onCheckedChange = {
                                    vm.update(s.copy(notify = if (it) s.notify + p.name else s.notify - p.name))
                                    if (it) onRequestNotif()
                                }
                            )
                        }
                    }
                    HorizontalDivider()
                    Text("تنبيه مسبق قبل الصلاة", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0, 5, 10, 15, 30).forEach { m ->
                            FilterChip(
                                selected = s.preMinutes == m,
                                onClick = { vm.update(s.copy(preMinutes = m)) },
                                label = { Text(if (m == 0) "بدون" else "$m د") }
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("تذكير بأذكار الصباح والمساء", Modifier.weight(1f))
                        Switch(checked = s.azkarAlerts, onCheckedChange = {
                            vm.update(s.copy(azkarAlerts = it))
                            if (it) onRequestNotif()
                        })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onRequestNotif) { Text("السماح بالإشعارات") }
                        OutlinedButton(onClick = {
                            onRequestNotif()
                            Notifier.show(ctx, "TEST", "تنبيه تجريبي", "هكذا ستظهر تنبيهات الصلاة")
                        }) { Text("اختبار التنبيه") }
                    }
                    Text(
                        "إن لم يعمل الأذان أو التنبيه في وقته، استثنِ التطبيق من «توفير البطارية» وتأكد من السماح بالتنبيهات الدقيقة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                SectionCard("المظهر") {
                    Text("السمة", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("النظام", "فاتح", "داكن").forEachIndexed { i, t ->
                            FilterChip(selected = s.theme == i, onClick = { vm.updateUi(s.copy(theme = i)) }, label = { Text(t) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("نظام 24 ساعة", Modifier.weight(1f))
                        Switch(checked = s.use24h, onCheckedChange = { vm.updateUi(s.copy(use24h = it)) })
                    }
                    Text("حجم خط الأذكار: ${s.azkarFont}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = s.azkarFont.toFloat(),
                        onValueChange = { vm.updateUi(s.copy(azkarFont = it.roundToInt())) },
                        valueRange = 16f..34f
                    )
                    Text("سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", fontSize = s.azkarFont.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }

            item {
                Text(
                    "تُحسب المواقيت داخل الهاتف دون إنترنت. يُنصح بمطابقتها مع تقويم الجهة الدينية في بلدك.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }
        }
    }

    when (dialog) {
        1 -> {
            var q by remember { mutableStateOf("") }
            val list = remember(q) { Cities.filter { q.isBlank() || it.name.contains(q.trim()) } }
            AlertDialog(
                onDismissRequest = { dialog = 0 },
                confirmButton = { TextButton(onClick = { dialog = 0 }) { Text("إغلاق") } },
                title = { Text("اختر المدينة") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = q, onValueChange = { q = it }, singleLine = true,
                            placeholder = { Text("بحث...") }, modifier = Modifier.fillMaxWidth()
                        )
                        LazyColumn(Modifier.heightIn(max = 380.dp)) {
                            items(list, key = { it.name }) { c ->
                                Text(
                                    c.name,
                                    fontWeight = if (c.name == s.place) FontWeight.Bold else FontWeight.Normal,
                                    color = if (c.name == s.place) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable { vm.selectCity(c); dialog = 0 }
                                        .padding(vertical = 12.dp, horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
            )
        }
        2 -> AlertDialog(
            onDismissRequest = { dialog = 0 },
            confirmButton = { TextButton(onClick = { dialog = 0 }) { Text("إغلاق") } },
            title = { Text("طريقة الحساب") },
            text = {
                Column {
                    PrayerCalc.methods.forEachIndexed { i, m ->
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.update(s.copy(method = i)); dialog = 0 },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = s.method == i, onClick = { vm.update(s.copy(method = i)); dialog = 0 })
                            Text(m.name)
                        }
                    }
                }
            }
        )
        3 -> AlertDialog(
            onDismissRequest = { dialog = 0 },
            confirmButton = { TextButton(onClick = { dialog = 0 }) { Text("تم") } },
            dismissButton = { TextButton(onClick = { vm.update(s.copy(adjust = emptyMap())) }) { Text("إعادة الضبط") } },
            title = { Text("تعديل المواقيت (بالدقائق)") },
            text = {
                Column {
                    Prayer.values().forEach { p ->
                        val cur = s.adjust[p.name] ?: 0
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(p.ar, Modifier.weight(1f))
                            IconButton(onClick = { vm.update(s.copy(adjust = s.adjust + (p.name to (cur - 1).coerceIn(-30, 30)))) }) {
                                Icon(Icons.Filled.Remove, "أقل")
                            }
                            Text(if (cur > 0) "+$cur" else "$cur", Modifier.width(40.dp), textAlign = TextAlign.Center)
                            IconButton(onClick = { vm.update(s.copy(adjust = s.adjust + (p.name to (cur + 1).coerceIn(-30, 30)))) }) {
                                Icon(Icons.Filled.Add, "أكثر")
                            }
                        }
                    }
                }
            }
        )
        4 -> AdhanPickerDialog(vm, "اختر المؤذن", s.adhan, false, { vm.setAdhan(it) }) { dialog = 0 }
        5 -> AdhanPickerDialog(vm, "أذان الفجر", s.fajrAdhan, true, { vm.setFajrAdhan(it) }) { dialog = 0 }
    }
}

// ───────────────────────── التقويم الهجري ─────────────────────────

@Composable
fun CalendarScreen(vm: PrayerViewModel, onBack: () -> Unit) {
    val s = vm.settings
    val zone = remember(s.tz) { vm.zone() }
    val today = remember(zone) { LocalDate.now(zone) }
    val th = PrayerCalc.hijri(today, s.hijriOffset)
    var ym by rememberSaveable { mutableIntStateOf((th?.first ?: 1447) * 100 + (th?.second ?: 1)) }
    val year = ym / 100
    val month = ym % 100
    val days = remember(ym, s.hijriOffset) { PrayerCalc.hijriMonth(year, month, s.hijriOffset) }
    val gShort = remember { DateTimeFormatter.ofPattern("d MMMM", Locale("ar")) }
    val gMonth = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ar")) }
    val weekNames = listOf("سبت", "أحد", "اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة")

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        SubHeader("التقويم الهجري", onBack)
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { ym = if (month == 1) (year - 1) * 100 + 12 else ym - 1 }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "السابق")
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${HijriMonths[(month - 1).coerceIn(0, 11)]} $year هـ", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        if (days != null) {
                            Text(
                                "${days.first().format(gMonth)} – ${days.last().format(gMonth)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { ym = if (month == 12) (year + 1) * 100 + 1 else ym + 1 }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "التالي")
                    }
                }
            }

            item {
                if (days == null) {
                    Text("هذا الشهر خارج نطاق التقويم المدعوم.", color = MaterialTheme.colorScheme.error)
                } else {
                    val lead = (days.first().dayOfWeek.value + 1) % 7
                    val cells: List<Int?> = List(lead) { null } + (1..days.size).toList()
                    Column {
                        Row(Modifier.fillMaxWidth()) {
                            weekNames.forEach {
                                Text(
                                    it, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        cells.chunked(7).forEach { r ->
                            Row(Modifier.fillMaxWidth()) {
                                for (i in 0 until 7) {
                                    val hd = r.getOrNull(i)
                                    Box(
                                        Modifier.weight(1f).aspectRatio(1f).padding(2.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (hd != null && days[hd - 1] == today) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.background
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (hd != null) {
                                            val g = days[hd - 1]
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    "$hd", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                                    color = if (i == 6) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text("${g.dayOfMonth}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                if (PrayerCalc.occasion(month, hd) != null) {
                                                    Box(Modifier.size(5.dp).clip(CircleShape).background(Gold))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val occ = days?.mapIndexedNotNull { i, g ->
                PrayerCalc.occasion(month, i + 1)?.let { Triple(i + 1, g, it) }
            } ?: emptyList()
            item {
                Text(
                    if (occ.isEmpty()) "لا مناسبات في هذا الشهر" else "مناسبات الشهر",
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(occ) { (d, g, name) ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(name, fontWeight = FontWeight.Bold)
                        Text(
                            "$d ${HijriMonths[(month - 1).coerceIn(0, 11)]} • ${g.format(gShort)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Text(
                    "يُستحب صيام الإثنين والخميس والأيام البيض (13 و14 و15 من كل شهر هجري). التواريخ حسابية (أم القرى) وقد تختلف يوماً عن الرؤية في بلدك؛ عدّلها من الإعدادات.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

// ───────────────────────── متتبع الصلوات ─────────────────────────

@Composable
fun TrackerScreen(vm: PrayerViewModel, onBack: () -> Unit) {
    val version = vm.trackerVersion
    val zone = remember(vm.settings.tz) { vm.zone() }
    val today = remember(zone, version) { LocalDate.now(zone) }
    val prayers = listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)
    val days = (0..6).map { today.minusDays(it.toLong()) }
    val done = days.sumOf { Integer.bitCount(vm.trackerMask(it)) }
    val streak = vm.streak()

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        SubHeader("متتبع الصلوات", onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$done / 35", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text("صلوات هذا الأسبوع", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$streak", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text("أيام متتالية كاملة", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            LinearProgressIndicator(progress = { done / 35f }, modifier = Modifier.fillMaxWidth())

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1.4f))
                prayers.forEach {
                    Text(it.ar, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            days.forEachIndexed { idx, d ->
                val label = when (idx) {
                    0 -> "اليوم"
                    1 -> "أمس"
                    else -> d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("ar"))
                }
                val mask = vm.trackerMask(d)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, Modifier.weight(1.4f), fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Normal)
                    prayers.forEachIndexed { bit, _ ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Checkbox(
                                checked = ((mask shr bit) and 1) == 1,
                                onCheckedChange = { vm.toggleTrack(d, bit) }
                            )
                        }
                    }
                }
            }
            Text(
                "علّم كل صلاة أدّيتها. يمكنك تعديل الأيام الماضية أيضاً.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ───────────────────────── جدول الشهر ─────────────────────────

@Composable
fun MonthTableScreen(vm: PrayerViewModel, onBack: () -> Unit) {
    val s = vm.settings
    val zone = remember(s.tz) { vm.zone() }
    var shift by rememberSaveable { mutableIntStateOf(0) }
    val today = remember(zone) { LocalDate.now(zone) }
    val first = remember(shift, today) { today.withDayOfMonth(1).plusMonths(shift.toLong()) }
    val rows = remember(first, s) {
        (0 until first.lengthOfMonth()).map { i ->
            val d = first.plusDays(i.toLong())
            d to PrayerCalc.forSettings(d, s)
        }
    }
    val fmt = remember(s.use24h) { DateTimeFormatter.ofPattern(if (s.use24h) "HH:mm" else "h:mm", Locale.US) }
    val title = remember(first) { first.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ar"))) }
    val cols = listOf(Prayer.FAJR, Prayer.SUNRISE, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        SubHeader("جدول مواقيت الشهر", onBack)
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { shift -= 1 }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "السابق") }
            Text(title, Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            IconButton(onClick = { shift += 1 }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "التالي") }
        }
        Text(
            s.place, Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text("اليوم", Modifier.weight(1.3f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            cols.forEach {
                Text(it.ar, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        HorizontalDivider()
        LazyColumn(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
            items(rows, key = { it.first.toString() }) { (d, times) ->
                val isToday = d == today
                Row(
                    Modifier.fillMaxWidth()
                        .background(if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background)
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${d.dayOfMonth} ${d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("ar"))}",
                        Modifier.weight(1.3f), fontSize = 12.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )
                    cols.forEach { p ->
                        Text(
                            times?.get(p)?.format(fmt) ?: "--", Modifier.weight(1f),
                            textAlign = TextAlign.Center, fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// ───────────────────────── أسماء الله الحسنى ─────────────────────────

@Composable
fun NamesScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        SubHeader("أسماء الله الحسنى", onBack)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            gridItemsIndexed(Names99) { i, n ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}", fontSize = 12.sp, color = Gold, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text(n, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
