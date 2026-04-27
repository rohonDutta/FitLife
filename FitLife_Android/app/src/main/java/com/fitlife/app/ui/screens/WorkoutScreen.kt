package com.fitlife.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.fitlife.app.data.local.ExerciseEntity
import com.fitlife.app.data.local.WorkoutEntity
import com.fitlife.app.ui.components.*
import com.fitlife.app.ui.viewmodel.FitnessViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val CATEGORIES = listOf("Strength","Cardio","HIIT","Yoga","Flexibility","Custom")
private val TEMPLATES = listOf(
    Triple("Push Day A", "Strength", listOf("Bench Press 3×8","Overhead Press 3×10","Tricep Dips 3×12","Lateral Raises 3×15")),
    Triple("Pull Day A", "Strength", listOf("Deadlift 3×5","Barbell Row 3×8","Bicep Curls 3×12","Face Pulls 3×15")),
    Triple("Leg Day",    "Strength", listOf("Squat 4×6","Leg Press 3×10","Romanian Deadlift 3×10","Calf Raises 4×15")),
    Triple("HIIT Blast", "HIIT",     listOf("Burpees 3×12","Jump Rope 3×60s","Mountain Climbers 3×20","Box Jumps 3×10")),
    Triple("Morning Yoga","Yoga",    listOf("Sun Salutation","Warrior I & II","Downward Dog","Child's Pose")),
    Triple("5K Run",    "Cardio",    listOf("5 min warm-up walk","5 km steady run","5 km steady run")),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(vm: FitnessViewModel) {
    val state by vm.workoutState.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var prefillTemplate by remember { mutableStateOf<Triple<String,String,List<String>>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Planner", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { prefillTemplate = null; showAdd = true },
                icon = { Icon(Icons.Outlined.FitnessCenter, null) },
                text = { Text("New Workout") },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Tab row
            TabRow(selectedTabIndex = tab) {
                listOf("Today","History","Templates").forEachIndexed { i, title ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
                }
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Summary chips
                if (tab == 0 || tab == 1) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatCard("This Week", state.weeklyCount.toString(), "workouts",
                                MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                            StatCard("Total Time", "${state.totalMinutes}m", "all time",
                                MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                        }
                    }
                }

                when (tab) {
                    // ── TODAY ────────────────────────────────────────────────
                    0 -> {
                        if (state.todayWorkouts.isEmpty()) {
                            item { EmptyState("💪","No workouts today","Tap + to log your first session") }
                        } else {
                            items(state.todayWorkouts) { w ->
                                WorkoutCard(w, onDelete = { vm.deleteWorkout(w.id) },
                                    onComplete = { vm.completeWorkout(w) })
                            }
                        }
                    }
                    // ── HISTORY ──────────────────────────────────────────────
                    1 -> {
                        if (state.allWorkouts.isEmpty()) {
                            item { EmptyState("📋","No workouts yet","Start logging to see your history") }
                        } else {
                            items(state.allWorkouts.sortedByDescending { it.date }) { w ->
                                WorkoutCard(w, onDelete = { vm.deleteWorkout(w.id) },
                                    onComplete = { vm.completeWorkout(w) })
                            }
                        }
                    }
                    // ── TEMPLATES ────────────────────────────────────────────
                    2 -> {
                        items(TEMPLATES) { t ->
                            TemplateCard(t, onClick = { prefillTemplate = t; showAdd = true })
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showAdd) {
        AddWorkoutSheet(
            prefill = prefillTemplate,
            onDismiss = { showAdd = false },
            onSave = { workout, exercises ->
                vm.saveWorkout(workout, exercises)
                showAdd = false
            }
        )
    }
}

@Composable
private fun WorkoutCard(w: WorkoutEntity, onDelete: () -> Unit, onComplete: () -> Unit) {
    val catColor = when (w.category) {
        "Cardio" -> MaterialTheme.colorScheme.secondary
        "HIIT"   -> MaterialTheme.colorScheme.error
        "Yoga"   -> MaterialTheme.colorScheme.tertiary
        else     -> MaterialTheme.colorScheme.primary
    }
    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(10.dp), color = catColor.copy(.12f), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.FitnessCenter, null, tint = catColor, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(w.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(6.dp))
                    AssistChip(onClick = {}, label = { Text(w.category) }, modifier = Modifier.height(22.dp))
                }
                Text(
                    "${formatDateLocal(w.date)} · ${w.durationMinutes} min · ${w.caloriesBurned} kcal" +
                    if (w.notes.isNotEmpty()) " · ${w.notes}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (!w.completed) {
                    IconButton(onClick = onComplete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.CheckCircle, "Complete", tint = MaterialTheme.colorScheme.tertiary)
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error.copy(.7f))
                }
            }
        }
    }
}

private fun formatDateLocal(s: String): String = try {
    val d = LocalDate.parse(s)
    val today = LocalDate.now()
    when {
        d == today -> "Today"
        d == today.minusDays(1) -> "Yesterday"
        else -> d.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
    }
} catch (e: Exception) { s }

@Composable
private fun TemplateCard(t: Triple<String, String, List<String>>, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(t.first, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(t.second, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(t.third.take(3).joinToString(" · ") + if (t.third.size > 3) "…" else "",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onClick, modifier = Modifier.padding(start = 8.dp)) { Text("Use") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWorkoutSheet(
    prefill: Triple<String, String, List<String>>?,
    onDismiss: () -> Unit,
    onSave: (WorkoutEntity, List<ExerciseEntity>) -> Unit
) {
    var name by remember { mutableStateOf(prefill?.first ?: "") }
    var category by remember { mutableStateOf(prefill?.second ?: "Strength") }
    var duration by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var exercises by remember {
        mutableStateOf(
            prefill?.third?.map { it to "" }?.toMutableList() ?: mutableListOf()
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.fillMaxHeight(0.92f)) {
        Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
            Text("New Workout", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(name, { name = it }, label = { Text("Workout name") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))

            // Category chips
            Text("Category", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CATEGORIES.take(4).forEach { cat ->
                    FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                }
            }
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(duration, { duration = it.filter(Char::isDigit) },
                    label = { Text("Duration (min)") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(calories, { calories = it.filter(Char::isDigit) },
                    label = { Text("Calories") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Exercises", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { exercises = (exercises + ("" to "")).toMutableList() }) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(16.dp))
                    Text("Add")
                }
            }
            exercises.forEachIndexed { i, (exName, _) ->
                var exVal by remember { mutableStateOf(exName) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${i+1}.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(24.dp))
                    OutlinedTextField(exVal, { v -> exVal = v; exercises = exercises.toMutableList().also { it[i] = v to "" } },
                        label = { Text("Exercise") }, modifier = Modifier.weight(1f),
                        singleLine = true)
                    IconButton(onClick = { exercises = exercises.toMutableList().also { it.removeAt(i) } }) {
                        Icon(Icons.Outlined.Close, null, tint = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = {
                        val workout = WorkoutEntity(
                            date = LocalDate.now().toString(),
                            name = name.ifBlank { "Workout" },
                            category = category,
                            durationMinutes = duration.toIntOrNull() ?: 0,
                            caloriesBurned = calories.toIntOrNull() ?: 0,
                            notes = notes, completed = true
                        )
                        val exList = exercises.mapIndexed { i, (n, _) ->
                            ExerciseEntity(workoutId = 0, name = n.ifBlank { "Exercise ${i+1}" })
                        }
                        onSave(workout, exList)
                    },
                    Modifier.weight(1f)
                ) { Text("Save Workout") }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
