package com.fitlife.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitlife.app.data.local.DailyStepsEntity
import com.fitlife.app.ui.components.*
import com.fitlife.app.ui.viewmodel.FitnessViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepsScreen(vm: FitnessViewModel) {
    val state by vm.stepsState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val fmt = DateTimeFormatter.ofPattern("EEEE, d MMMM")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Step Tracker", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        Text(today.format(fmt), style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    if (state.healthConnectAvailable) {
                        FilledTonalIconButton(
                            onClick = { vm.syncSteps() },
                            enabled = !state.isSyncing,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            if (state.isSyncing)
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                            else
                                Icon(Icons.Outlined.Sync, "Sync", modifier = Modifier.size(28.dp))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Outlined.Add, null, modifier = Modifier.size(24.dp)) },
                text = { Text("Log Steps", fontSize = 18.sp) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── Main Progress Card ───────────────────────────────────────────
            item {
                val progress = if (state.stepGoal > 0) state.todaySteps.toFloat() / state.stepGoal else 0f
                val animatedProgress by animateFloatAsState(targetValue = progress.coerceIn(0f, 1.2f), label = "progress")

                Card(
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { 1f },
                                modifier = Modifier.size(240.dp),
                                strokeWidth = 18.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            CircularProgressIndicator(
                                progress = { animatedProgress.coerceIn(0f, 1f) },
                                modifier = Modifier.size(240.dp),
                                strokeWidth = 18.dp,
                                color = MaterialTheme.colorScheme.primary,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.DirectionsWalk, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    state.todaySteps.formatNum(),
                                    style = MaterialTheme.typography.displayLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("GOAL: ${state.stepGoal.formatNum()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        
                        Spacer(Modifier.height(32.dp))
                        
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            QuickStat("Calories", "${state.todayCaloriesBurned}", "kcal", Icons.Outlined.LocalFireDepartment, MaterialTheme.colorScheme.error)
                            VerticalDivider(Modifier.height(48.dp).padding(horizontal = 8.dp))
                            QuickStat("Distance", "%.2f".format(state.todayDistanceKm), "km", Icons.Outlined.Map, MaterialTheme.colorScheme.secondary)
                            VerticalDivider(Modifier.height(48.dp).padding(horizontal = 8.dp))
                            QuickStat("Active", "${state.todayActiveMinutes}", "min", Icons.Outlined.Timer, MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }

            item {
                SectionHeader("Activity Overview", "Last 7 days")
                Spacer(Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        val days = buildWeekData(state.weekHistory)
                        val maxVal = maxOf(days.maxOfOrNull { it.second } ?: 0, state.stepGoal).coerceAtLeast(1)
                        BarChart(
                            values = days,
                            maxValue = maxVal,
                            goalValue = state.stepGoal,
                            highlightColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth().height(160.dp)
                        )
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    SectionHeader("Recent History", null)
                    if (state.streakDays > 0) {
                        StreakBadge(state.streakDays)
                    }
                }
            }

            if (state.weekHistory.isEmpty()) {
                item {
                    EmptyState("👣", "No steps logged", "Start walking to see your history here!")
                }
            } else {
                items(state.weekHistory.sortedByDescending { it.date }) { entry ->
                    ModernStepRow(entry, state.stepGoal)
                }
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAddDialog) {
        AddStepsDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { steps, date ->
                vm.addStepsManually(date, steps)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun QuickStat(label: String, value: String, unit: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text(unit, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String?) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ModernStepRow(entry: DailyStepsEntity, goal: Int) {
    val progress = (entry.steps.toFloat() / goal).coerceIn(0f, 1f)
    val isGoalMet = entry.steps >= goal

    Surface(
        onClick = {},
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(Modifier.padding(20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(52.dp),
                    strokeWidth = 5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(52.dp),
                    strokeWidth = 5.dp,
                    color = if (isGoalMet) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                if (isGoalMet) {
                    Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(formatDate(entry.date), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${entry.caloriesBurned} kcal · ${"%.2f".format(entry.distanceMeters/1000)} km",
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(entry.steps.formatNum(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun AddStepsDialog(onDismiss: () -> Unit, onConfirm: (Int, String) -> Unit) {
    var steps by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Steps Manually", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                OutlinedTextField(
                    value = steps,
                    onValueChange = { steps = it.filter(Char::isDigit) },
                    label = { Text("Steps", fontSize = 18.sp) },
                    prefix = { Icon(Icons.Outlined.DirectionsWalk, null, Modifier.size(24.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)", fontSize = 18.sp) },
                    prefix = { Icon(Icons.Outlined.CalendarToday, null, Modifier.size(24.dp)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { 
                val s = steps.toIntOrNull() ?: 0
                if (s > 0) onConfirm(s, date)
            }) { Text("Save Activity", fontSize = 16.sp) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", fontSize = 16.sp) }
        }
    )
}

private fun buildWeekData(history: List<DailyStepsEntity>): List<Pair<String, Int>> {
    val days = listOf("Mo","Tu","We","Th","Fr","Sa","Su")
    return (6 downTo 0).map { i ->
        val d = LocalDate.now().minusDays(i.toLong())
        val entry = history.find { it.date == d.toString() }
        val dayLabel = days[d.dayOfWeek.value - 1]
        dayLabel to (entry?.steps ?: 0)
    }
}

private fun Int.formatNum(): String = "%,d".format(this)

private fun formatDate(s: String): String = try {
    val d = LocalDate.parse(s)
    val today = LocalDate.now()
    when {
        d == today -> "Today"
        d == today.minusDays(1) -> "Yesterday"
        else -> d.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
    }
} catch (e: Exception) { s }
