@file:OptIn(ExperimentalMaterial3Api::class)

package com.mawaqiti.app

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs

// ───────────────────────── Qibla ─────────────────────────

@Composable
fun rememberHeading(lat: Double, lon: Double): State<Float?> {
    val ctx = LocalContext.current
    val heading = remember { mutableStateOf<Float?>(null) }
    DisposableEffect(lat, lon) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val decl = GeomagneticField(lat.toFloat(), lon.toFloat(), 0f, System.currentTimeMillis()).declination
        var smooth: Float? = null
        val rot = FloatArray(9)
        val ori = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rot, e.values)
                SensorManager.getOrientation(rot, ori)
                val az = ((Math.toDegrees(ori[0].toDouble()) + decl + 360.0) % 360.0).toFloat()
                val prev = smooth
                smooth = if (prev == null) az else {
                    val d = ((az - prev + 540f) % 360f) - 180f
                    (prev + 0.2f * d + 360f) % 360f
                }
                heading.value = smooth
            }

            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }
    return heading
}

@Composable
fun CompassDial(heading: Float, qibla: Float, aligned: Boolean, modifier: Modifier = Modifier) {
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val line = MaterialTheme.colorScheme.onSurfaceVariant
    val text = MaterialTheme.colorScheme.onSurface
    val pointer = if (aligned) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary
    val north = Color(0xFFD32F2F)
    val paint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }
    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f * 0.92f
        drawCircle(surface, r, c)
        drawCircle(line.copy(alpha = 0.5f), r, c, style = Stroke(width = 3.dp.toPx()))

        rotate(-heading, c) {
            for (i in 0 until 72) {
                val long = i % 6 == 0
                rotate(i * 5f, c) {
                    drawLine(
                        line,
                        Offset(c.x, c.y - r),
                        Offset(c.x, c.y - r + (if (long) 16.dp.toPx() else 8.dp.toPx())),
                        strokeWidth = if (long) 3.dp.toPx() else 1.5f.dp.toPx()
                    )
                }
            }
            val cv = drawContext.canvas.nativeCanvas
            paint.textSize = 22.sp.toPx()
            listOf("N", "E", "S", "W").forEachIndexed { i, t ->
                cv.save()
                cv.rotate(i * 90f, c.x, c.y)
                paint.color = if (i == 0) north.toArgb() else text.toArgb()
                cv.drawText(t, c.x, c.y - r + 46.dp.toPx(), paint)
                cv.restore()
            }
            // الكعبة على زاوية القبلة
            rotate(qibla, c) {
                val box = 30.dp.toPx()
                val top = c.y - r + 62.dp.toPx()
                drawLine(Gold.copy(alpha = 0.7f), c, Offset(c.x, top + box), strokeWidth = 3.dp.toPx())
                drawRect(Color.Black, Offset(c.x - box / 2, top), Size(box, box))
                drawRect(Gold, Offset(c.x - box / 2, top + box * 0.25f), Size(box, box * 0.18f))
            }
        }

        // مؤشر ثابت أعلى الجهاز
        val p = Path().apply {
            moveTo(c.x, c.y - r + 6.dp.toPx())
            lineTo(c.x - 12.dp.toPx(), c.y - r - 16.dp.toPx())
            lineTo(c.x + 12.dp.toPx(), c.y - r - 16.dp.toPx())
            close()
        }
        drawPath(p, pointer)
        drawCircle(pointer, 6.dp.toPx(), c)
    }
}

@Composable
fun QiblaScreen(vm: PrayerViewModel) {
    val s = vm.settings
    val bearing = remember(s.lat, s.lon) { PrayerCalc.qiblaBearing(s.lat, s.lon).toFloat() }
    val dist = remember(s.lat, s.lon) { PrayerCalc.distanceKm(s.lat, s.lon, PrayerCalc.KAABA_LAT, PrayerCalc.KAABA_LON) }
    val heading by rememberHeading(s.lat, s.lon)
    val h = heading
    val rel = h?.let { ((bearing - it + 540f) % 360f) - 180f }
    val aligned = rel != null && abs(rel) < 3f
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(aligned) { if (aligned) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("اتجاه القبلة", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(s.place, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        CompassDial(h ?: 0f, bearing, aligned, Modifier.size(320.dp))
        Spacer(Modifier.height(20.dp))
        if (h == null) {
            Text(
                "لا يتوفر حساس البوصلة في هذا الجهاز، لذلك يظهر الاتجاه ثابتاً بالنسبة للشمال.",
                color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center
            )
        } else {
            Text(
                if (aligned) "أنت الآن باتجاه القبلة ✓" else "أدر الهاتف حتى تتطابق الكعبة مع المؤشر الأخضر",
                fontWeight = FontWeight.Bold,
                color = if (aligned) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            String.format(Locale.US, "زاوية القبلة: %.1f° من الشمال", bearing),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            String.format(Locale.US, "المسافة إلى الكعبة: %,.0f كم", dist),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "ضع الهاتف أفقياً وبعيداً عن المعادن والمغناطيس. للمعايرة حرّكه على شكل رقم 8.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ───────────────────────── Azkar ─────────────────────────

private fun azkarIcon(i: Int): ImageVector = when (i) {
    0 -> Icons.Filled.WbSunny
    1 -> Icons.Filled.NightsStay
    2 -> Icons.Filled.Done
    3 -> Icons.Filled.Bedtime
    4 -> Icons.Filled.WbTwilight
    else -> Icons.Filled.MenuBook
}

@Composable
fun AzkarScreen(vm: PrayerViewModel) {
    var sel by rememberSaveable { mutableIntStateOf(-1) }
    if (sel < 0) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("الأذكار", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            AzkarData.categories.forEachIndexed { i, c ->
                Card(
                    onClick = { sel = i },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(azkarIcon(i), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(c.title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Text(c.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    } else {
        BackHandler { sel = -1 }
        AzkarDetail(
            AzkarData.categories[sel], vm.settings.azkarFont,
            onFont = { vm.updateUi(vm.settings.copy(azkarFont = it.coerceIn(16, 34))) }
        ) { sel = -1 }
    }
}

@Composable
fun AzkarDetail(cat: AzkarCategory, fontSize: Int, onFont: (Int) -> Unit, onBack: () -> Unit) {
    val remaining = remember(cat.title) { mutableStateListOf<Int>().apply { addAll(cat.items.map { it.count }) } }
    val haptic = LocalHapticFeedback.current
    val done = remaining.count { it == 0 }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") }
            Column(Modifier.weight(1f)) {
                Text(cat.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("$done من ${cat.items.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { onFont(fontSize - 2) }) { Text("A-") }
            TextButton(onClick = { onFont(fontSize + 2) }) { Text("A+") }
            IconButton(onClick = { cat.items.forEachIndexed { i, d -> remaining[i] = d.count } }) {
                Icon(Icons.Filled.Refresh, "إعادة")
            }
        }
        LinearProgressIndicator(
            progress = { if (cat.items.isEmpty()) 0f else done.toFloat() / cat.items.size },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(cat.items) { i, d ->
                val left = remaining[i]
                Card(
                    onClick = {
                        if (left > 0) {
                            remaining[i] = left - 1
                            haptic.performHapticFeedback(if (left - 1 == 0) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
                        }
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (left == 0) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(d.text, fontSize = fontSize.sp, lineHeight = (fontSize * 1.7f).sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(10.dp))
                        Box(
                            Modifier.size(46.dp).clip(CircleShape)
                                .background(if (left == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (left == 0) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary)
                            else Text("$left", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        if (d.count > 1) {
                            Text(
                                "التكرار: ${d.count}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── Tasbeeh ─────────────────────────

@Composable
fun TasbeehScreen(vm: PrayerViewModel) {
    var target by rememberSaveable { mutableIntStateOf(33) }
    val haptic = LocalHapticFeedback.current
    val idx = vm.tasbeehIndex
    val count = vm.tasbeehCounts[idx]
    val progress = if (target > 0) (count % target).toFloat() / target else 0f

    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("المسبحة", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(TasbeehPhrases) { i, t ->
                FilterChip(selected = idx == i, onClick = { vm.selectTasbeeh(i) }, label = { Text(t) })
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(33, 99, 100, 0).forEach { t ->
                FilterChip(
                    selected = target == t,
                    onClick = { target = t },
                    label = { Text(if (t == 0) "مفتوح" else "$t") }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            TasbeehPhrases[idx], fontSize = 26.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(280.dp),
                strokeWidth = 8.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Box(
                Modifier.size(240.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        val n = vm.bumpTasbeeh()
                        haptic.performHapticFeedback(
                            if (target > 0 && n % target == 0) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$count", fontSize = 64.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    if (target > 0) {
                        Text("الدورة ${count / target + 1} • $target", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    } else {
                        Text("اضغط للتسبيح", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = { vm.resetTasbeeh() }) {
            Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("تصفير هذا الذكر")
        }
    }
}
