package com.fitlife.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitlife.app.data.local.SleepLogEntity
import com.fitlife.app.ui.components.*
import com.fitlife.app.ui.viewmodel.FitnessViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellbeingScreen(vm: FitnessViewModel) {
    val state by vm.wellbeingState.collectAsState()
    val today = LocalDate.now().toString()
    var showSleep by remember { mutableStateOf(false) }
    var moodSelected by remember { mutableIntStateOf(state.todayWellbeing?.moodScore ?: 3) }
    var stressLevel by remember { mutableFloatStateOf((state.todayWellbeing?.stressLevel ?: 5).toFloat()) }

    // Meditation timer
    var medActive by remember { mutableStateOf(false) }
    var medSeconds by remember { mutableIntStateOf(0) }
    var medTotal by remember { mutableIntStateOf(0) }
    LaunchedEffect(medActive) {
        if (medActive) {
            while (medActive && medSeconds > 0) {
                delay(1000L); medSeconds--
            }
            if (medSeconds == 0 && medActive) {
                medActive = false
                val mins = medTotal / 60
                vm.addMeditationMinutes(today, mins)
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Wellbeing", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Sleep ─────────────────────────────────────────────────────────
            item {
                SectionCard(
                    title = "Sleep",
                    subtitle = state.todaySleep?.let { "${it.durationMinutes / 60}h ${it.durationMinutes % 60}m last night" }
                        ?: "No sleep logged today",
                    trailing = {
                        Button(onClick = { showSleep = true },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)) {
                            Icon(Icons.Outlined.Add, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Log", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Last Night",
                            state.recentSleep.firstOrNull()?.let { "${it.durationMinutes / 60}h" } ?: "—",
                            "", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                        StatCard("7-Day Avg",
                            if (state.avgSleepHours > 0) "${"%.1f".format(state.avgSleepHours)}h" else "—",
                            "", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        StatCard("Entries", state.recentSleep.size.toString(), "logged",
                            MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                    }

                    if (state.recentSleep.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("Last 7 nights", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(6.dp))
                        val sleepData = state.recentSleep.take(7).reversed().map { s ->
                            val day = java.time.LocalDate.parse(s.date)
                            listOf("Mo","Tu","We","Th","Fr","Sa","Su")[day.dayOfWeek.value - 1] to (s.durationMinutes / 60)
                        }
                        BarChart(
                            values = sleepData,
                            maxValue = 10,
                            goalValue = 8,
                            highlightColor = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.fillMaxWidth().height(80.dp)
                        )
                    }
                }
            }

            // ── Mood check-in ─────────────────────────────────────────────────
            item {
                SectionCard("Mood Check-in") {
                    MoodSelector(selected = moodSelected, onSelect = { moodSelected = it })
                    Spacer(Modifier.height(14.dp))
                    Text("Stress level: ${stressLevel.toInt()}/10",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = stressLevel, onValueChange = { stressLevel = it },
                        valueRange = 1f..10f, steps = 8, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { vm.logMood(today, moodSelected, stressLevel.toInt()) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Save, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save Check-in")
                    }
                }
            }

            // ── Hydration ─────────────────────────────────────────────────────
            item {
                val waterGlasses = state.todayWellbeing?.waterGlasses ?: 0
                val waterGoal = 8
                SectionCard(
                    title = "Hydration",
                    subtitle = "$waterGlasses / $waterGoal glasses",
                    trailing = {
                        FilledTonalButton(
                            onClick = { vm.addWater(today) },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("+ Glass", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                ) {
                    AnimatedProgressBar(
                        progress = waterGlasses.toFloat() / waterGoal,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        height = 10.dp
                    )
                    Spacer(Modifier.height(10.dp))
                    // Cup icons
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (i in 1..waterGoal) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (i <= waterGlasses) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f).aspectRatio(1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("💧", fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }

            // ── Meditation ────────────────────────────────────────────────────
            item {
                SectionCard(
                    title = "Meditation",
                    subtitle = "Today: ${state.meditationMinutes} min"
                ) {
                    if (!medActive) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10, 15, 20).forEach { min ->
                                OutlinedButton(
                                    onClick = { medTotal = min * 60; medSeconds = min * 60; medActive = true },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("${min}m") }
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "%02d:%02d".format(medSeconds / 60, medSeconds % 60),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { if (medTotal > 0) 1f - (medSeconds.toFloat() / medTotal) else 0f },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(onClick = { medActive = false }) { Text("Stop") }
                        }
                    }
                }
            }

            // ── Wellbeing Insights ────────────────────────────────────────────
            item {
                SectionCard("Weekly Insights") {
                    val avgMood = state.weekWellbeing.map { it.moodScore }.average()
                    val avgStress = state.weekWellbeing.map { it.stressLevel }.average()
                    val avgWater = state.weekWellbeing.map { it.waterGlasses }.average()

                    InsightRow("😊 Avg mood", if (avgMood.isNaN()) "—" else "${"%.1f".format(avgMood)}/5")
                    InsightRow("😤 Avg stress", if (avgStress.isNaN()) "—" else "${"%.1f".format(avgStress)}/10")
                    InsightRow("💧 Avg water", if (avgWater.isNaN()) "—" else "${"%.1f".format(avgWater)} glasses/day")
                    InsightRow("🧘 Meditation", "${state.weekWellbeing.sumOf { it.meditationMinutes }} min this week")
                    InsightRow("🌙 Sleep entries", "${state.recentSleep.size} nights logged")
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showSleep) {
        LogSleepSheet(onDismiss = { showSleep = false }, onSave = { vm.logSleep(it); showSleep = false })
    }
}

@Composable
private fun InsightRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(.5f))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogSleepSheet(onDismiss: () -> Unit, onSave: (SleepLogEntity) -> Unit) {
    var bedH by remember { mutableStateOf("23") }
    var bedM by remember { mutableStateOf("00") }
    var wakeH by remember { mutableStateOf("07") }
    var wakeM by remember { mutableStateOf("00") }
    var quality by remember { mutableIntStateOf(4) }
    var notes by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
            Text("Log Sleep", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Bedtime", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(bedH, { bedH = it.filter(Char::isDigit).take(2) },
                            label = { Text("HH") }, modifier = Modifier.weight(1f), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(bedM, { bedM = it.filter(Char::isDigit).take(2) },
                            label = { Text("MM") }, modifier = Modifier.weight(1f), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("Wake up", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(wakeH, { wakeH = it.filter(Char::isDigit).take(2) },
                            label = { Text("HH") }, modifier = Modifier.weight(1f), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(wakeM, { wakeM = it.filter(Char::isDigit).take(2) },
                            label = { Text("MM") }, modifier = Modifier.weight(1f), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            Text("Quality", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "😴",2 to "😪",3 to "😐",4 to "😊",5 to "😁").forEach { (v, emoji) ->
                    FilterChip(selected = quality == v, onClick = { quality = v },
                        label = { Text("$emoji $v") })
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(), maxLines = 2)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = {
                        val bH = bedH.toIntOrNull()?.coerceIn(0,23) ?: 23
                        val bM = bedM.toIntOrNull()?.coerceIn(0,59) ?: 0
                        val wH = wakeH.toIntOrNull()?.coerceIn(0,23) ?: 7
                        val wM = wakeM.toIntOrNull()?.coerceIn(0,59) ?: 0
                        var mins = (wH * 60 + wM) - (bH * 60 + bM)
                        if (mins < 0) mins += 1440
                        onSave(SleepLogEntity(
                            date = LocalDate.now().toString(),
                            bedtimeHour = bH, bedtimeMinute = bM,
                            wakeHour = wH, wakeMinute = wM,
                            durationMinutes = mins, qualityRating = quality, notes = notes
                        ))
                    },
                    Modifier.weight(1f)
                ) { Text("Save Sleep") }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
