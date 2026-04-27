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
import com.fitlife.app.data.local.DietPlanEntity
import com.fitlife.app.ui.components.*
import com.fitlife.app.ui.viewmodel.FitnessViewModel

private val PLAN_TEMPLATES = mapOf(
    "fatloss"  to listOf(1500, 180, 120, 50),
    "muscle"   to listOf(2800, 220, 280, 80),
    "maintain" to listOf(2200, 160, 230, 70),
    "keto"     to listOf(1800, 160, 25, 140),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietScreen(vm: FitnessViewModel) {
    val state by vm.dietState.collectAsState()
    var showCreate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diet Planner", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showCreate = true }) {
                        Icon(Icons.Outlined.Add, "Create plan")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Outlined.EditNote, null) },
                text = { Text("Create Plan") },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Active plan ───────────────────────────────────────────────────
            item {
                val plan = state.activePlan
                SectionCard(
                    title = "Active Plan",
                    subtitle = plan?.name ?: "No active plan",
                    trailing = {
                        if (plan != null) AssistChip(onClick = {}, label = { Text(plan.goal.replaceFirstChar { it.uppercase() }) })
                    }
                ) {
                    if (plan != null) {
                        // Target chips
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCard("Calories", plan.dailyCalorieTarget.toString(), "kcal",
                                MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                            StatCard("Protein", "${plan.proteinTargetG.toInt()}g", "",
                                MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                            StatCard("Carbs", "${plan.carbsTargetG.toInt()}g", "",
                                MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                            StatCard("Fat", "${plan.fatTargetG.toInt()}g", "",
                                MaterialTheme.colorScheme.error, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(14.dp))
                        // Progress vs today
                        Text("Today's Progress", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        MacroRow("Calories", state.todayCalories.toFloat(), plan.dailyCalorieTarget.toFloat(), MaterialTheme.colorScheme.secondary)
                        MacroRow("Protein", state.todayProtein, plan.proteinTargetG, MaterialTheme.colorScheme.primary)
                        MacroRow("Carbs", state.todayCarbs, plan.carbsTargetG, MaterialTheme.colorScheme.tertiary)
                        MacroRow("Fat", state.todayFat, plan.fatTargetG, MaterialTheme.colorScheme.error)
                    } else {
                        EmptyState("🥗","No plan active","Create a diet plan to track your macro targets")
                    }
                }
            }

            // ── Net vs goal summary ───────────────────────────────────────────
            if (state.activePlan != null) {
                item {
                    val diff = state.todayCalories - (state.activePlan?.dailyCalorieTarget ?: 2000)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (diff > 100) MaterialTheme.colorScheme.errorContainer
                            else if (diff < -300) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (diff > 100) Icons.Outlined.TrendingUp else Icons.Outlined.TrendingDown,
                                null,
                                tint = if (diff > 100) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    when {
                                        diff > 100 -> "You're $diff kcal over today's target"
                                        diff < -300 -> "You're ${-diff} kcal under today's target"
                                        else -> "On track! ${diff.let { if (it >= 0) "+$it" else "$it" }} kcal vs target"
                                    },
                                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold
                                )
                                Text("Keep it up! 💪", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // ── All plans ─────────────────────────────────────────────────────
            item {
                Text("Your Plans", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }

            if (state.allPlans.isEmpty()) {
                item { EmptyState("📋","No plans yet","Tap + to create your first diet plan") }
            } else {
                items(state.allPlans) { plan ->
                    PlanCard(
                        plan = plan,
                        onActivate = { vm.activatePlan(plan.id) },
                        onDelete = { vm.deletePlan(plan.id) }
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showCreate) {
        CreatePlanSheet(
            onDismiss = { showCreate = false },
            onSave = { plan -> vm.saveDietPlan(plan); showCreate = false }
        )
    }
}

@Composable
private fun PlanCard(plan: DietPlanEntity, onActivate: () -> Unit, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        border = if (plan.isActive) ButtonDefaults.outlinedButtonBorder else null,
        colors = CardDefaults.cardColors(
            containerColor = if (plan.isActive) MaterialTheme.colorScheme.primaryContainer.copy(.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(plan.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    if (plan.isActive) {
                        Spacer(Modifier.width(6.dp))
                        AssistChip(onClick = {}, label = { Text("Active") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer))
                    }
                }
                Text("${plan.dailyCalorieTarget} kcal · P:${plan.proteinTargetG.toInt()}g · C:${plan.carbsTargetG.toInt()}g · F:${plan.fatTargetG.toInt()}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!plan.isActive) {
                TextButton(onClick = onActivate) { Text("Activate") }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error.copy(.7f),
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePlanSheet(onDismiss: () -> Unit, onSave: (DietPlanEntity) -> Unit) {
    var planName by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("2000") }
    var protein by remember { mutableStateOf("150") }
    var carbs by remember { mutableStateOf("200") }
    var fat by remember { mutableStateOf("65") }
    var selectedTemplate by remember { mutableStateOf("custom") }

    fun applyTemplate(key: String) {
        selectedTemplate = key
        PLAN_TEMPLATES[key]?.let { (cal, p, c, f) ->
            calories = cal.toString(); protein = p.toString(); carbs = c.toString(); fat = f.toString()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
            Text("Create Diet Plan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(planName, { planName = it },
                label = { Text("Plan name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(12.dp))

            Text("Template", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("custom" to "Custom","fatloss" to "Fat Loss","muscle" to "Muscle","keto" to "Keto","maintain" to "Maintain")
                    .forEach { (key, label) ->
                        FilterChip(selected = selectedTemplate == key, onClick = { applyTemplate(key) },
                            label = { Text(label) })
                    }
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(calories, { calories = it.filter(Char::isDigit) },
                label = { Text("Daily calories") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(protein, { protein = it.filter(Char::isDigit) },
                    label = { Text("Protein (g)") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(carbs, { carbs = it.filter(Char::isDigit) },
                    label = { Text("Carbs (g)") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(fat, { fat = it.filter(Char::isDigit) },
                    label = { Text("Fat (g)") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = {
                        onSave(DietPlanEntity(
                            name = planName.ifBlank { "My Plan" },
                            goal = selectedTemplate,
                            dailyCalorieTarget = calories.toIntOrNull() ?: 2000,
                            proteinTargetG = protein.toFloatOrNull() ?: 150f,
                            carbsTargetG = carbs.toFloatOrNull() ?: 200f,
                            fatTargetG = fat.toFloatOrNull() ?: 65f,
                            isActive = true
                        ))
                    },
                    Modifier.weight(1f)
                ) { Text("Save & Activate") }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
