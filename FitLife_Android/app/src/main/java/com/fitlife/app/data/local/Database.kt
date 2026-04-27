package com.fitlife.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ═══════════════════════════════════════════════════════════════════════
// ENTITIES
// ═══════════════════════════════════════════════════════════════════════

@Entity(tableName = "daily_steps")
data class DailyStepsEntity(
    @PrimaryKey val date: String,           // "2025-03-20"
    val steps: Int = 0,
    val caloriesBurned: Int = 0,
    val distanceMeters: Float = 0f,
    val activeMinutes: Int = 0,
    val syncedWithFit: Boolean = false
)

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val name: String,
    val category: String,                   // Strength, Cardio, HIIT, Yoga, etc.
    val durationMinutes: Int = 0,
    val caloriesBurned: Int = 0,
    val notes: String = "",
    val completed: Boolean = false,
    val templateId: Long? = null
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val name: String,
    val sets: Int = 3,
    val repsPerSet: Int = 10,
    val weightKg: Float = 0f,
    val durationSeconds: Int = 0,
    val restSeconds: Int = 60,
    val notes: String = ""
)

@Entity(tableName = "food_log")
data class FoodLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val mealType: String,                   // Breakfast, Lunch, Dinner, Snack
    val foodName: String,
    val calories: Int,
    val proteinG: Float = 0f,
    val carbsG: Float = 0f,
    val fatG: Float = 0f,
    val servingGrams: Float = 100f,
    val barcode: String? = null
)

@Entity(tableName = "diet_plans")
data class DietPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val goal: String = "custom",            // fatloss, muscle, maintain, keto, custom
    val dailyCalorieTarget: Int,
    val proteinTargetG: Float,
    val carbsTargetG: Float,
    val fatTargetG: Float,
    val isActive: Boolean = false,
    val createdAt: String = ""
)

@Entity(tableName = "sleep_log")
data class SleepLogEntity(
    @PrimaryKey val date: String,
    val bedtimeHour: Int,
    val bedtimeMinute: Int,
    val wakeHour: Int,
    val wakeMinute: Int,
    val durationMinutes: Int,
    val qualityRating: Int = 3,            // 1–5
    val notes: String = ""
)

@Entity(tableName = "wellbeing_log")
data class WellbeingLogEntity(
    @PrimaryKey val date: String,
    val moodScore: Int = 3,               // 1–5
    val stressLevel: Int = 5,             // 1–10
    val waterGlasses: Int = 0,
    val meditationMinutes: Int = 0,
    val notes: String = ""
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val dateOfBirth: String = "",
    val sex: String = "Male",
    val heightCm: Float = 170f,
    val weightKg: Float = 70f,
    val dailyStepGoal: Int = 10000,
    val dailyCalorieGoal: Int = 2000,
    val dailyWaterGoal: Int = 8,
    val weightGoal: String = "maintain",  // lose, maintain, gain
    val activityLevel: String = "moderate",
    val onboardingComplete: Boolean = false
)

// ═══════════════════════════════════════════════════════════════════════
// DAOs
// ═══════════════════════════════════════════════════════════════════════

@Dao
interface StepsDao {
    @Query("SELECT * FROM daily_steps WHERE date = :date")
    suspend fun getByDate(date: String): DailyStepsEntity?

    @Query("SELECT * FROM daily_steps ORDER BY date DESC LIMIT 30")
    fun observeLast30Days(): Flow<List<DailyStepsEntity>>

    @Query("SELECT * FROM daily_steps ORDER BY date DESC LIMIT 7")
    suspend fun getLast7Days(): List<DailyStepsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyStepsEntity)

    @Query("SELECT SUM(steps) FROM daily_steps WHERE date >= :fromDate")
    suspend fun totalStepsSince(fromDate: String): Int?

    @Query("SELECT COUNT(*) FROM daily_steps WHERE steps >= :goal AND date >= :fromDate")
    suspend fun daysGoalMetSince(goal: Int, fromDate: String): Int
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY date DESC")
    fun observeAll(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE date = :date ORDER BY id DESC")
    fun observeByDate(date: String): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE date >= :fromDate ORDER BY date DESC")
    suspend fun getFrom(fromDate: String): List<WorkoutEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workout: WorkoutEntity): Long

    @Update
    suspend fun update(workout: WorkoutEntity)

    @Delete
    suspend fun delete(workout: WorkoutEntity)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM exercises WHERE workoutId = :workoutId ORDER BY id")
    fun observeExercises(workoutId: Long): Flow<List<ExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity)

    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE workoutId = :workoutId")
    suspend fun deleteExercisesForWorkout(workoutId: Long)
}

@Dao
interface FoodLogDao {
    @Query("SELECT * FROM food_log WHERE date = :date ORDER BY mealType, id")
    fun observeByDate(date: String): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_log WHERE date >= :fromDate ORDER BY date DESC")
    fun observeFrom(fromDate: String): Flow<List<FoodLogEntity>>

    @Query("SELECT SUM(calories) FROM food_log WHERE date = :date")
    fun observeTotalCalories(date: String): Flow<Int?>

    @Query("SELECT SUM(proteinG) FROM food_log WHERE date = :date")
    suspend fun totalProtein(date: String): Float?

    @Query("SELECT SUM(carbsG) FROM food_log WHERE date = :date")
    suspend fun totalCarbs(date: String): Float?

    @Query("SELECT SUM(fatG) FROM food_log WHERE date = :date")
    suspend fun totalFat(date: String): Float?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(food: FoodLogEntity): Long

    @Delete
    suspend fun delete(food: FoodLogEntity)

    @Query("DELETE FROM food_log WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface DietPlanDao {
    @Query("SELECT * FROM diet_plans WHERE isActive = 1 LIMIT 1")
    fun observeActivePlan(): Flow<DietPlanEntity?>

    @Query("SELECT * FROM diet_plans ORDER BY id DESC")
    fun observeAll(): Flow<List<DietPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: DietPlanEntity): Long

    @Update
    suspend fun update(plan: DietPlanEntity)

    @Query("UPDATE diet_plans SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE diet_plans SET isActive = 1 WHERE id = :id")
    suspend fun activate(id: Long)

    @Query("DELETE FROM diet_plans WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleep_log WHERE date = :date")
    suspend fun getByDate(date: String): SleepLogEntity?

    @Query("SELECT * FROM sleep_log ORDER BY date DESC LIMIT 14")
    fun observeLast14(): Flow<List<SleepLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: SleepLogEntity)

    @Query("DELETE FROM sleep_log WHERE date = :date")
    suspend fun deleteByDate(date: String)
}

@Dao
interface WellbeingDao {
    @Query("SELECT * FROM wellbeing_log WHERE date = :date")
    suspend fun getByDate(date: String): WellbeingLogEntity?

    @Query("SELECT * FROM wellbeing_log ORDER BY date DESC LIMIT 7")
    fun observeLast7(): Flow<List<WellbeingLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WellbeingLogEntity)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observe(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun get(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)
}

// ═══════════════════════════════════════════════════════════════════════
// DATABASE
// ═══════════════════════════════════════════════════════════════════════

@Database(
    entities = [
        DailyStepsEntity::class,
        WorkoutEntity::class,
        ExerciseEntity::class,
        FoodLogEntity::class,
        DietPlanEntity::class,
        SleepLogEntity::class,
        WellbeingLogEntity::class,
        UserProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FitLifeDatabase : RoomDatabase() {
    abstract fun stepsDao(): StepsDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun foodLogDao(): FoodLogDao
    abstract fun dietPlanDao(): DietPlanDao
    abstract fun sleepDao(): SleepDao
    abstract fun wellbeingDao(): WellbeingDao
    abstract fun profileDao(): ProfileDao
}
