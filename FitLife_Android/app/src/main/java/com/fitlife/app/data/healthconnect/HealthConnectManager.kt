package com.fitlife.app.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class HealthSummary(
    val steps: Int = 0,
    val activeCalories: Int = 0,
    val sleepMinutes: Int = 0,
    val heartRateAvg: Int = 0
)

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    val sdkStatus: Int get() = HealthConnectClient.sdkStatus(context)
    val isAvailable: Boolean get() = sdkStatus == HealthConnectClient.SDK_AVAILABLE

    val requiredPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class),
        HealthPermission.getWritePermission(NutritionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
    )

    suspend fun hasAllPermissions(): Boolean {
        if (!isAvailable) return false
        return client.permissionController.getGrantedPermissions()
            .containsAll(requiredPermissions)
    }

    // ── Steps ────────────────────────────────────────────────────────────────

    suspend fun readStepsForDate(date: LocalDate): Int {
        if (!isAvailable) return 0
        return try {
            val zone = ZoneId.systemDefault()
            val start = date.atStartOfDay(zone).toInstant()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant()
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            response.records.sumOf { it.count }.toInt()
        } catch (e: Exception) { 0 }
    }

    suspend fun writeSteps(date: LocalDate, count: Long) {
        if (!isAvailable || !hasAllPermissions()) return
        try {
            val zone = ZoneId.systemDefault()
            val start = date.atStartOfDay(zone).toInstant()
            val end = start.plusSeconds(86399)
            client.insertRecords(listOf(
                StepsRecord(
                    count = count,
                    startTime = start, endTime = end,
                    startZoneOffset = zone.rules.getOffset(start),
                    endZoneOffset = zone.rules.getOffset(end)
                )
            ))
        } catch (e: Exception) { /* silent fail */ }
    }

    // ── Calories ─────────────────────────────────────────────────────────────

    suspend fun readActiveCaloriesForDate(date: LocalDate): Int {
        if (!isAvailable) return 0
        return try {
            val zone = ZoneId.systemDefault()
            val start = date.atStartOfDay(zone).toInstant()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant()
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            response.records.sumOf { it.energy.inKilocalories }.toInt()
        } catch (e: Exception) { 0 }
    }

    // ── Sleep ────────────────────────────────────────────────────────────────

    suspend fun readSleepForDate(date: LocalDate): Int {
        if (!isAvailable) return 0
        return try {
            val zone = ZoneId.systemDefault()
            val start = date.minusDays(1).atTime(12, 0).atZone(zone).toInstant()
            val end = date.atTime(12, 0).atZone(zone).toInstant()
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            response.records.sumOf {
                (it.endTime.epochSecond - it.startTime.epochSecond) / 60
            }.toInt()
        } catch (e: Exception) { 0 }
    }

    // ── Exercise ─────────────────────────────────────────────────────────────

    suspend fun writeExerciseSession(
        name: String,
        start: Instant,
        end: Instant,
        type: Int = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
    ) {
        if (!isAvailable || !hasAllPermissions()) return
        try {
            val zone = ZoneId.systemDefault()
            client.insertRecords(listOf(
                ExerciseSessionRecord(
                    startTime = start, endTime = end,
                    exerciseType = type, title = name,
                    startZoneOffset = zone.rules.getOffset(start),
                    endZoneOffset = zone.rules.getOffset(end)
                )
            ))
        } catch (e: Exception) { /* silent fail */ }
    }

    // ── Nutrition ─────────────────────────────────────────────────────────────

    suspend fun writeNutrition(
        name: String,
        time: Instant,
        calories: Double,
        proteinG: Double,
        carbsG: Double,
        fatG: Double
    ) {
        if (!isAvailable || !hasAllPermissions()) return
        try {
            val zone = ZoneId.systemDefault()
            client.insertRecords(listOf(
                NutritionRecord(
                    startTime = time, endTime = time.plusSeconds(1),
                    energy = Energy.kilocalories(calories),
                    protein = Mass.grams(proteinG),
                    totalCarbohydrate = Mass.grams(carbsG),
                    totalFat = Mass.grams(fatG),
                    name = name,
                    startZoneOffset = zone.rules.getOffset(time),
                    endZoneOffset = zone.rules.getOffset(time)
                )
            ))
        } catch (e: Exception) { /* silent fail */ }
    }

    // ── Heart Rate ────────────────────────────────────────────────────────────

    suspend fun readAvgHeartRateForDate(date: LocalDate): Int {
        if (!isAvailable) return 0
        return try {
            val zone = ZoneId.systemDefault()
            val start = date.atStartOfDay(zone).toInstant()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant()
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            val samples = response.records.flatMap { it.samples }
            if (samples.isEmpty()) 0 else (samples.sumOf { it.beatsPerMinute } / samples.size).toInt()
        } catch (e: Exception) { 0 }
    }

    // ── Daily summary ─────────────────────────────────────────────────────────

    suspend fun getDailySummary(date: LocalDate): HealthSummary = try {
        HealthSummary(
            steps = readStepsForDate(date),
            activeCalories = readActiveCaloriesForDate(date),
            sleepMinutes = readSleepForDate(date),
            heartRateAvg = readAvgHeartRateForDate(date)
        )
    } catch (e: Exception) { HealthSummary() }
}
