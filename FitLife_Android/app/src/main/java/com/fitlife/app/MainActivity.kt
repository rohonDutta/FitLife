package com.fitlife.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitlife.app.data.food.FoodRepository
import com.fitlife.app.service.StepCounterService
import com.fitlife.app.ui.screens.*
import com.fitlife.app.ui.theme.FitLifeTheme
import com.fitlife.app.ui.viewmodel.FitnessViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Steps     : Screen("steps",     "Steps",    Icons.Outlined.DirectionsWalk)
    object Workout   : Screen("workout",   "Workout",  Icons.Outlined.FitnessCenter)
    object Calories  : Screen("calories",  "Calories", Icons.Outlined.LocalFireDepartment)
    object Diet      : Screen("diet",      "Diet",     Icons.Outlined.Restaurant)
    object Wellbeing : Screen("wellbeing", "Wellbeing",Icons.Outlined.FavoriteBorder)
}

val navItems = listOf(Screen.Steps, Screen.Workout, Screen.Calories, Screen.Diet, Screen.Wellbeing)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var foodRepository: FoodRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitLifeTheme {
                FitLifeRoot(foodRepository)
            }
        }
    }
}

@Composable
fun FitLifeRoot(foodRepo: FoodRepository) {
    val vm: FitnessViewModel = hiltViewModel()
    val profileState by vm.profileState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current  // ← captured here, safe inside @Composable

    var permissionsDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.message.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    when {
        !profileState.onboardingComplete -> {
            OnboardingScreen(vm)
        }
        !permissionsDone -> {
            PermissionScreen(
                onAllGranted = {
                    permissionsDone = true
                    StepCounterService.start(context) // ← context passed, not called inside lambda
                }
            )
        }
        else -> {
            MainApp(vm, foodRepo, snackbarHostState, context)
        }
    }
}

@Composable
private fun MainApp(
    vm: FitnessViewModel,
    foodRepo: FoodRepository,
    snackbarHostState: SnackbarHostState,
    context: Context
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Steps) }

    LaunchedEffect(Unit) {
        StepCounterService.start(context)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(tonalElevation = 4.dp) {
                navItems.forEach { screen ->
                    NavigationBarItem(
                        icon     = { Icon(screen.icon, contentDescription = screen.label) },
                        label    = { Text(screen.label, fontWeight = FontWeight.Medium) },
                        selected = currentScreen == screen,
                        onClick  = { currentScreen = screen }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding).fillMaxSize()) {
            when (currentScreen) {
                Screen.Steps     -> StepsScreen(vm)
                Screen.Workout   -> WorkoutScreen(vm)
                Screen.Calories  -> CaloriesScreen(vm, foodRepo)
                Screen.Diet      -> DietScreen(vm)
                Screen.Wellbeing -> WellbeingScreen(vm)
            }
        }
    }
}
