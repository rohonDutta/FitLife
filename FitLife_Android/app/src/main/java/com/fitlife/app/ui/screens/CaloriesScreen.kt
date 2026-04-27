package com.fitlife.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitlife.app.data.food.FoodItem
import com.fitlife.app.data.food.FoodRepository
import com.fitlife.app.data.local.FoodLogEntity
import com.fitlife.app.ui.components.*
import com.fitlife.app.ui.viewmodel.FitnessViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaloriesScreen(vm: FitnessViewModel, foodRepo: FoodRepository) {
    val state by vm.calorieState.collectAsState()
    var showAddFood by remember { mutableStateOf(false) }
    var defaultMeal by remember { mutableStateOf("Breakfast") }
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Calorie Counter", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(today, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { defaultMeal = "Breakfast"; showAddFood = true }) {
                        Icon(Icons.Outlined.Add, "Log food", modifier = Modifier.size(28.dp))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { defaultMeal = "Breakfast"; showAddFood = true },
                icon = { Icon(Icons.Outlined.Restaurant, null) },
                text = { Text("Log Food", fontSize = 18.sp) },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        val pct = if (state.calorieGoal > 0) state.totalCalories.toFloat() / state.calorieGoal else 0f
                        RingProgress(progress = pct, size = 150.dp, strokeWidth = 16.dp,
                            progressColor = MaterialTheme.colorScheme.secondary) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(state.totalCalories.toString(),
                                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary)
                                Text("kcal", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CalRow("Goal",   state.calorieGoal.toString(),   "kcal", MaterialTheme.colorScheme.onSurface)
                            CalRow("Burned", state.burnedCalories.toString(), "kcal", MaterialTheme.colorScheme.error)
                            HorizontalDivider()
                            CalRow("Net",    state.netCalories.toString(),    "kcal",
                                if (state.netCalories > state.calorieGoal) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
            item {
                SectionCard("Macros Today") {
                    MacroRow("Protein", state.proteinG, 150f, MaterialTheme.colorScheme.primary)
                    MacroRow("Carbs",   state.carbsG,   200f, MaterialTheme.colorScheme.tertiary)
                    MacroRow("Fat",     state.fatG,      65f, MaterialTheme.colorScheme.error)
                }
            }
            listOf("Breakfast","Lunch","Dinner","Snack").forEach { meal ->
                val mealFoods = state.byMeal[meal] ?: emptyList()
                val mealCal   = mealFoods.sumOf { it.calories }
                val icon = when(meal) { "Breakfast"->"☀️"; "Lunch"->"🌤"; "Dinner"->"🌙"; else->"🍎" }
                item {
                    SectionCard(title = "$icon $meal", subtitle = "$mealCal kcal",
                        trailing = {
                            TextButton(onClick = { defaultMeal = meal; showAddFood = true }) {
                                Icon(Icons.Outlined.Add, null, Modifier.size(20.dp))
                                Text("Add", fontSize = 16.sp)
                            }
                        }) {
                        if (mealFoods.isEmpty()) {
                            Text("Nothing logged yet", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            mealFoods.forEach { food -> FoodRow(food, onDelete = { vm.deleteFood(food.id) }) }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAddFood) {
        AddFoodSheet(defaultMeal = defaultMeal, foodRepo = foodRepo,
            onDismiss = { showAddFood = false },
            onSave    = { food -> vm.logFood(food); showAddFood = false })
    }
}

@Composable
private fun CalRow(label: String, value: String, unit: String, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(unit,  style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FoodRow(food: FoodLogEntity, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(food.foodName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text("P:${food.proteinG.toInt()}g · C:${food.carbsG.toInt()}g · F:${food.fatG.toInt()}g · ${food.servingGrams.toInt()}g",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("${food.calories} kcal", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.Close, "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(.4f))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodSheet(
    defaultMeal: String,
    foodRepo: FoodRepository,
    onDismiss: () -> Unit,
    onSave: (FoodLogEntity) -> Unit
) {
    var meal    by remember { mutableStateOf(defaultMeal) }
    var query   by remember { mutableStateOf("") }
    var name    by remember { mutableStateOf("") }
    var cal     by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs   by remember { mutableStateOf("") }
    var fat     by remember { mutableStateOf("") }
    var serving by remember { mutableStateOf("100") }

    var localResults by remember { mutableStateOf(listOf<FoodItem>()) }
    var apiResults   by remember { mutableStateOf(listOf<FoodItem>()) }
    var isSearching  by remember { mutableStateOf(false) }
    var showDropdown by remember { mutableStateOf(false) }

    var selectedFood by remember { mutableStateOf<FoodItem?>(null) }

    val scope = rememberCoroutineScope()
    val allResults = remember(localResults, apiResults) {
        (localResults + apiResults).distinctBy { it.name.lowercase().take(25) }.take(10)
    }

    fun onQueryChange(q: String) {
        query = q; name = q; selectedFood = null
        if (q.length < 2) { localResults = emptyList(); apiResults = emptyList(); showDropdown = false; return }
        localResults = foodRepo.searchLocalInstant(q)
        showDropdown = true
        scope.launch {
            delay(350)
            if (query != q) return@launch
            isSearching = true
            apiResults = foodRepo.searchApi(q)
            isSearching = false
        }
    }

    fun scaleMacros(food: FoodItem, servingStr: String) {
        val g = servingStr.toFloatOrNull() ?: 100f
        val f = g / 100f
        cal     = (food.calories * f).toInt().toString()
        protein = "%.1f".format(food.proteinG * f)
        carbs   = "%.1f".format(food.carbsG * f)
        fat     = "%.1f".format(food.fatG * f)
    }

    fun selectFood(food: FoodItem) {
        selectedFood = food
        query = food.name; name = food.name
        scaleMacros(food, serving)
        showDropdown = false
        localResults = emptyList(); apiResults = emptyList()
    }

    fun onServingChange(s: String) {
        serving = s
        selectedFood?.let { scaleMacros(it, s) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.fillMaxHeight(0.92f)) {
        Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {

            Text("Log Food", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Breakfast","Lunch","Dinner","Snack").forEach { m ->
                    FilterChip(selected = meal == m, onClick = { meal = m }, label = { Text(m, fontSize = 16.sp) })
                }
            }
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { onQueryChange(it) },
                label = { Text("Type food name…", fontSize = 18.sp) },
                trailingIcon = {
                    if (isSearching)
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else
                        Icon(Icons.Outlined.Search, null, modifier = Modifier.size(28.dp))
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            AnimatedVisibility(visible = showDropdown && allResults.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column {
                        allResults.forEachIndexed { idx, food ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .clickable { selectFood(food) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(food.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1)
                                    Text(
                                        "P:${food.proteinG.toInt()}g  C:${food.carbsG.toInt()}g  F:${food.fatG.toInt()}g  /100g",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${food.calories} kcal",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary)
                                    if (food.source == "api")
                                        Text("🌐 online", style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (idx < allResults.lastIndex)
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(.15f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = serving,
                onValueChange = { onServingChange(it.filter(Char::isDigit)) },
                label = { Text("Serving size (g)", fontSize = 18.sp) },
                suffix = { Text("g", fontSize = 18.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(Modifier.height(10.dp))

            Text("Nutritional values (auto-filled · editable)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(cal,     { cal = it.filter(Char::isDigit) },
                    label = { Text("Calories", fontSize = 18.sp) }, modifier = Modifier.weight(1f), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(protein, { protein = it },
                    label = { Text("Protein g", fontSize = 18.sp) }, modifier = Modifier.weight(1f), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(carbs, { carbs = it },
                    label = { Text("Carbs g", fontSize = 18.sp) }, modifier = Modifier.weight(1f), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(fat,   { fat = it },
                    label = { Text("Fat g", fontSize = 18.sp) }, modifier = Modifier.weight(1f), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }

            if (cal.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(.35f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("🔥" to cal, "💪" to "${protein}g", "🌾" to "${carbs}g", "🥑" to "${fat}g").forEach { (icon, v) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(icon, fontSize = 20.sp); Text(v, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, Modifier.weight(1f)) { Text("Cancel", fontSize = 18.sp) }
                Button(
                    onClick = {
                        val calories = cal.toIntOrNull() ?: return@Button
                        onSave(FoodLogEntity(
                            date = LocalDate.now().toString(), mealType = meal,
                            foodName     = name.ifBlank { "Food" },
                            calories     = calories,
                            proteinG     = protein.toFloatOrNull() ?: 0f,
                            carbsG       = carbs.toFloatOrNull()   ?: 0f,
                            fatG         = fat.toFloatOrNull()     ?: 0f,
                            servingGrams = serving.toFloatOrNull() ?: 100f
                        ))
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Log Food", fontSize = 18.sp) }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
