package com.fitlife.app.data.repository

import androidx.health.connect.client.records.ExerciseSessionRecord
import com.fitlife.app.data.healthconnect.HealthConnectManager
import com.fitlife.app.data.local.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FitnessRepository @Inject constructor(
    private val db: FitLifeDatabase,
    private val hc: HealthConnectManager
) {

    // ── Profile ───────────────────────────────────────────────────────────────

    fun observeProfile(): Flow<UserProfileEntity?> = db.profileDao().observe()
    suspend fun getProfile(): UserProfileEntity? = db.profileDao().get()
    suspend fun saveProfile(profile: UserProfileEntity) = db.profileDao().upsert(profile)

    // ── Steps ─────────────────────────────────────────────────────────────────

    fun observeLast30DaysSteps(): Flow<List<DailyStepsEntity>> = db.stepsDao().observeLast30Days()

    suspend fun getTodaySteps(): DailyStepsEntity? =
        db.stepsDao().getByDate(LocalDate.now().toString())

    suspend fun addStepsManually(date: String, count: Int) {
        val existing = db.stepsDao().getByDate(date)
            ?: DailyStepsEntity(date = date)
        val updated = existing.copy(
            steps = existing.steps + count,
            caloriesBurned = ((existing.steps + count) * 0.04).toInt(),
            distanceMeters = (existing.steps + count) * 0.762f,
            activeMinutes = (existing.steps + count) / 100
        )
        db.stepsDao().upsert(updated)
    }

    suspend fun syncStepsFromHealthConnect(): Int {
        if (!hc.isAvailable || !hc.hasAllPermissions()) return 0
        val today = LocalDate.now()
        val summary = hc.getDailySummary(today)
        if (summary.steps == 0) return 0
        val existing = db.stepsDao().getByDate(today.toString())
            ?: DailyStepsEntity(date = today.toString())
        // Use whichever is higher — HC or local
        val best = maxOf(existing.steps, summary.steps)
        db.stepsDao().upsert(
            existing.copy(
                steps = best,
                caloriesBurned = (best * 0.04).toInt(),
                distanceMeters = best * 0.762f,
                activeMinutes = best / 100,
                syncedWithFit = true
            )
        )
        return best
    }

    suspend fun computeStreak(): Int {
        val profile = getProfile() ?: return 0
        val goal = profile.dailyStepGoal
        var streak = 0
        var checkDate = LocalDate.now()
        for (i in 0..60) {
            val s = db.stepsDao().getByDate(checkDate.toString())
            if (s != null && s.steps >= goal) {
                streak++
                checkDate = checkDate.minusDays(1)
            } else break
        }
        return streak
    }

    // ── Workouts ──────────────────────────────────────────────────────────────

    fun observeAllWorkouts(): Flow<List<WorkoutEntity>> = db.workoutDao().observeAll()
    fun observeTodayWorkouts(date: String): Flow<List<WorkoutEntity>> = db.workoutDao().observeByDate(date)
    fun observeExercises(workoutId: Long): Flow<List<ExerciseEntity>> = db.workoutDao().observeExercises(workoutId)

    suspend fun saveWorkout(workout: WorkoutEntity, exercises: List<ExerciseEntity>): Long {
        val id = db.workoutDao().insert(workout)
        exercises.forEach { db.workoutDao().insertExercise(it.copy(workoutId = id)) }
        return id
    }

    suspend fun completeWorkout(workout: WorkoutEntity) {
        db.workoutDao().update(workout.copy(completed = true))
        // Write to Health Connect
        if (hc.isAvailable && hc.hasAllPermissions()) {
            val now = Instant.now()
            val start = now.minusSeconds(workout.durationMinutes * 60L)
            val type = when (workout.category) {
                "Cardio" -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
                "HIIT" -> ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
                "Yoga" -> ExerciseSessionRecord.EXERCISE_TYPE_YOGA
                else -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
            }
            hc.writeExerciseSession(workout.name, start, now, type)
        }
    }

    suspend fun deleteWorkout(id: Long) {
        db.workoutDao().deleteExercisesForWorkout(id)
        db.workoutDao().deleteById(id)
    }

    // ── Food Log ──────────────────────────────────────────────────────────────

    fun observeFoodByDate(date: String): Flow<List<FoodLogEntity>> = db.foodLogDao().observeByDate(date)
    fun observeTotalCalories(date: String): Flow<Int?> = db.foodLogDao().observeTotalCalories(date)

    suspend fun logFood(food: FoodLogEntity): Long {
        val id = db.foodLogDao().insert(food)
        // Mirror to Health Connect
        if (hc.isAvailable && hc.hasAllPermissions()) {
            hc.writeNutrition(
                name = food.foodName,
                time = Instant.now(),
                calories = food.calories.toDouble(),
                proteinG = food.proteinG.toDouble(),
                carbsG = food.carbsG.toDouble(),
                fatG = food.fatG.toDouble()
            )
        }
        return id
    }

    suspend fun deleteFood(id: Long) = db.foodLogDao().deleteById(id)

    suspend fun getMacroTotals(date: String): Triple<Float, Float, Float> {
        val p = db.foodLogDao().totalProtein(date) ?: 0f
        val c = db.foodLogDao().totalCarbs(date) ?: 0f
        val f = db.foodLogDao().totalFat(date) ?: 0f
        return Triple(p, c, f)
    }

    // ── Diet Plans ────────────────────────────────────────────────────────────

    fun observeActivePlan(): Flow<DietPlanEntity?> = db.dietPlanDao().observeActivePlan()
    fun observeAllPlans(): Flow<List<DietPlanEntity>> = db.dietPlanDao().observeAll()

    suspend fun savePlan(plan: DietPlanEntity): Long {
        db.dietPlanDao().deactivateAll()
        return db.dietPlanDao().insert(plan.copy(isActive = true))
    }

    suspend fun activatePlan(id: Long) {
        db.dietPlanDao().deactivateAll()
        db.dietPlanDao().activate(id)
    }

    suspend fun deletePlan(id: Long) = db.dietPlanDao().deleteById(id)

    // ── Sleep ─────────────────────────────────────────────────────────────────

    fun observeSleep(): Flow<List<SleepLogEntity>> = db.sleepDao().observeLast14()
    suspend fun getSleepByDate(date: String) = db.sleepDao().getByDate(date)
    suspend fun logSleep(entry: SleepLogEntity) = db.sleepDao().upsert(entry)

    // ── Wellbeing ─────────────────────────────────────────────────────────────

    fun observeWellbeing(): Flow<List<WellbeingLogEntity>> = db.wellbeingDao().observeLast7()
    suspend fun getWellbeingByDate(date: String) = db.wellbeingDao().getByDate(date)
    suspend fun saveWellbeing(entry: WellbeingLogEntity) = db.wellbeingDao().upsert(entry)

    // ── Health Connect helpers ────────────────────────────────────────────────

    val isHealthConnectAvailable get() = hc.isAvailable
    suspend fun hasHealthPermissions() = hc.hasAllPermissions()
    val healthPermissions get() = hc.requiredPermissions
}
