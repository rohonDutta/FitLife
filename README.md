# 🏋️ FitLife — All-in-One Fitness Tracker for Android

**FitLife** is a modern, feature-rich Android fitness application built entirely with **Kotlin** and **Jetpack Compose**. It brings step tracking, workout management, calorie logging, diet planning, and wellbeing monitoring into a single, beautifully designed Material 3 interface.

---

## ✨ Key Features

### 🚶 Step Tracking
- **Real-time step counting** via a hardware step-counter foreground service
- Automatic calorie burn, distance, and active-minutes estimation
- Daily step goal with animated circular progress ring
- 30-day step history with interactive bar charts
- Streak tracking (consecutive days of goal achievement)
- Manual step entry with date selection
- Syncs bi-directionally with **Google Health Connect**

### 💪 Workout Management
- Create custom workouts with multiple exercises (sets × reps)
- Pre-built workout templates: Push Day, Pull Day, Leg Day, Morning Yoga, 5K Run
- Workout categories: Strength, Cardio, HIIT, Yoga, Flexibility, Custom
- Track workout completion and duration
- Workout sessions auto-sync to Health Connect

### 🔥 Calorie & Macro Tracking
- Log food by meal (Breakfast, Lunch, Dinner, Snack)
- **100+ built-in food database** covering proteins, dairy, grains, fruits, vegetables, legumes, nuts, oils, snacks, drinks, and fast food
- Live search with **Open Food Facts API** integration for thousands of additional foods
- Real-time macro dashboard: protein, carbs, fat with progress bars
- Adjustable serving sizes with auto-scaled macros
- TDEE (Total Daily Energy Expenditure) auto-calculated from profile

### 🥗 Diet Planning
- Create and manage multiple diet plans with calorie and macro targets
- Quick-start templates: Fat Loss, Muscle Gain, Maintenance, Keto
- Activate/deactivate plans — active plan drives daily calorie goals
- Visual adherence tracking against the active plan

### 🧘 Wellbeing Dashboard
- **Sleep tracking**: log bed time, wake time, and quality rating; 14-day history
- **Water intake**: glass-by-glass counter with hydration reminders every 2 hours
- **Mood & stress logging**: emoji-based mood selector (1–5 scale)
- **Meditation minutes**: track daily mindfulness sessions
- AI-generated wellbeing insights based on logged data

### 📋 Onboarding & Personalization
- 4-step guided onboarding wizard (Welcome → Profile → Goals → Step Goal)
- Collects: name, age, sex, height, weight, weight goal, activity level
- Calculates personalized TDEE using Mifflin-St Jeor equation
- Daily step goal slider (1,000–25,000 steps)

### 🔔 Smart Notifications
- **Step reminder** at 7 PM if you're below 80% of your daily goal
- **Water reminder** every 2 hours if you've logged fewer than 4 glasses
- Silent foreground notification showing live step count

---

## 🏗️ Architecture

FitLife follows the **MVVM (Model-View-ViewModel)** architecture pattern with clean separation of concerns:

```
┌──────────────────────────────────────────────────────────┐
│                     UI Layer                             │
│  Jetpack Compose Screens + Reusable Components           │
│  (StepsScreen, WorkoutScreen, CaloriesScreen, etc.)      │
└────────────────────────┬─────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────┐
│                  ViewModel Layer                         │
│  FitnessViewModel (single shared ViewModel)              │
│  UI state models: StepsUiState, WorkoutUiState, etc.     │
└────────────────────────┬─────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────┐
│                 Repository Layer                         │
│  FitnessRepository  ←→  HealthConnectManager             │
│  FoodRepository (local DB + Open Food Facts API)         │
└────────────────────────┬─────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────┐
│                  Data Layer                              │
│  Room Database (fitlife_v1.db)                           │
│  8 Entities · 7 DAOs · DataStore Preferences             │
└──────────────────────────────────────────────────────────┘
```

### Dependency Injection

All dependencies are wired through **Dagger Hilt**:
- `AppModule` provides the Room database and all 7 DAOs as singletons
- `FoodRepository` and `FitnessRepository` use `@Inject constructor()`
- Workers use `@HiltWorker` with `@AssistedInject`

---

## 🛠️ Tech Stack

| Category | Technology |
|---|---|
| **Language** | Kotlin 1.9.24 |
| **UI Framework** | Jetpack Compose (BOM 2024.09) |
| **Design System** | Material 3 (Material You) with dynamic color |
| **Navigation** | Navigation Compose 2.8.1 |
| **DI** | Dagger Hilt 2.48 |
| **Local Database** | Room 2.6.1 |
| **Health Data** | Health Connect Client 1.0.0-alpha11 |
| **Background Work** | WorkManager 2.9.1 |
| **Preferences** | DataStore Preferences 1.1.1 |
| **Charts** | Vico (Compose M3) 1.15.0 |
| **Image Loading** | Coil Compose 2.7.0 |
| **Permissions** | Accompanist Permissions 0.36.0 |
| **Async** | Kotlin Coroutines 1.8.1 |
| **Food API** | Open Food Facts (free, no API key needed) |
| **Min SDK** | 26 (Android 8.0 Oreo) |
| **Target SDK** | 35 (Android 15) |
| **Java Target** | JVM 17 |
| **Build System** | Gradle KTS (AGP 8.13.2) |

---

## 📁 Project Structure

```
FitLife_Android/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/fitlife/app/
│       │   ├── FitLifeApp.kt              # Application class (Hilt + WorkManager init)
│       │   ├── MainActivity.kt            # Single activity, navigation host, bottom bar
│       │   ├── di/
│       │   │   └── AppModule.kt           # Hilt module — DB + DAO providers
│       │   ├── data/
│       │   │   ├── local/
│       │   │   │   └── Database.kt        # Room DB, 8 entities, 7 DAOs
│       │   │   ├── healthconnect/
│       │   │   │   └── HealthConnectManager.kt  # Read/write Health Connect data
│       │   │   ├── food/
│       │   │   │   └── FoodRepository.kt  # 100-item local DB + Open Food Facts API
│       │   │   └── repository/
│       │   │       └── FitnessRepository.kt  # Central data coordinator
│       │   ├── service/
│       │   │   └── StepCounterService.kt  # Foreground service (hardware pedometer)
│       │   ├── worker/
│       │   │   └── Workers.kt             # DailySyncWorker, StepReminder, WaterReminder
│       │   └── ui/
│       │       ├── theme/
│       │       │   └── Theme.kt           # Material 3 color schemes (light + dark)
│       │       ├── components/
│       │       │   └── Components.kt      # Reusable composables (RingProgress, BarChart, etc.)
│       │       ├── viewmodel/
│       │       │   └── FitnessViewModel.kt # Shared ViewModel for all screens
│       │       └── screens/
│       │           ├── OnboardingScreen.kt   # 4-step setup wizard
│       │           ├── PermissionScreen.kt   # Runtime permission requests
│       │           ├── StepsScreen.kt        # Step counter + history
│       │           ├── WorkoutScreen.kt      # Workout builder + templates
│       │           ├── CaloriesScreen.kt     # Food logging + macro tracking
│       │           ├── DietScreen.kt         # Diet plan management
│       │           └── WellbeingScreen.kt    # Sleep, water, mood, meditation
│       └── res/                              # Drawables, mipmaps, themes, strings
├── build.gradle.kts                          # Root build config (plugin versions)
├── settings.gradle.kts                       # Project settings
├── gradle.properties                         # Gradle & Android build flags
└── keystores/                                # Signing keystore (release builds)
```

---

## 📊 Database Schema

The Room database (`fitlife_v1.db`) contains **8 entities**:

| Entity | Description | Key Fields |
|---|---|---|
| `DailyStepsEntity` | Daily step records | date, steps, caloriesBurned, distanceMeters, activeMinutes |
| `WorkoutEntity` | Workout sessions | name, category, date, durationMin, completed |
| `ExerciseEntity` | Individual exercises within a workout | workoutId, name, sets, reps, weightKg |
| `FoodLogEntity` | Individual food log entries | date, meal, name, calories, protein, carbs, fat, servingG |
| `DietPlanEntity` | Diet plan definitions | name, dailyCalories, proteinG, carbsG, fatG, isActive |
| `SleepLogEntity` | Sleep records | date, bedTime, wakeTime, hours, quality |
| `WellbeingLogEntity` | Daily wellbeing metrics | date, waterGlasses, mood, stressLevel, meditationMinutes |
| `UserProfileEntity` | User profile & goals | name, age, sex, heightCm, weightKg, goal, activityLevel, dailyStepGoal, tdee |

---

## 🔌 Health Connect Integration

FitLife integrates with [Health Connect](https://developer.android.com/health-and-fitness/guides/health-connect) to read and write:

| Data Type | Read | Write |
|---|:---:|:---:|
| Steps | ✅ | ✅ |
| Active Calories Burned | ✅ | — |
| Sleep Sessions | ✅ | — |
| Exercise Sessions | — | ✅ |
| Nutrition | — | ✅ |
| Heart Rate | ✅ | — |
| Weight | ✅ | ✅ |

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Ladybug (2024.2.1) or newer
- **JDK 17** or higher
- **Android SDK** with API level 35 installed
- A physical device or emulator running **Android 8.0+ (API 26+)**
- [Health Connect app](https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata) installed on the device (for health data sync)

### Build & Run

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/fitlife.git
   cd fitlife/FitLife_Android
   ```

2. **Open in Android Studio**
   - File → Open → select the `FitLife_Android` directory

3. **Sync Gradle**
   - Android Studio will automatically download all dependencies

4. **Run on device**
   - Select a connected device or emulator
   - Click **Run ▶** or press `Shift + F10`

5. **Build APK (release)**
   ```bash
   ./gradlew assembleRelease
   ```
   The signed APK will be at `app/release/FitLife-release.apk`

### Permissions

On first launch, the app will ask for:
- **Activity Recognition** — for step counting
- **Health Connect** — for syncing health data
- **Notifications** — for step and water reminders
- **Camera** (optional) — reserved for future barcode scanning

---

## 🎨 Theming

FitLife uses **Material 3 / Material You** with:
- **Dynamic Color** support on Android 12+ (colors adapt to your wallpaper)
- **Light & Dark mode** that follows system settings
- Custom fallback color schemes with a fitness-inspired blue/teal palette
- Brand accent colors: Blue, Teal, Orange, Green, Purple, Red

---

## 🔧 Background Services

| Service / Worker | Schedule | Purpose |
|---|---|---|
| `StepCounterService` | Always-on (foreground) | Reads hardware step sensor, persists to Room DB |
| `DailySyncWorker` | Every 1 hour | Syncs steps from Health Connect to local DB |
| `StepReminderWorker` | Daily at 7:00 PM | Notifies if below 80% of step goal |
| `WaterReminderWorker` | Every 2 hours | Notifies if fewer than 4 glasses logged |

---

## 🧩 Reusable UI Components

The app includes a library of reusable composables in `Components.kt`:

| Component | Description |
|---|---|
| `RingProgress` | Animated circular progress ring with customizable stroke and colors |
| `AnimatedProgressBar` | Smooth animated horizontal progress bar |
| `StatCard` | Compact stat display card with label, value, and unit |
| `SectionCard` | Section container with title, subtitle, and trailing action |
| `MacroRow` | Macro nutrient progress bar (protein/carbs/fat) |
| `BarChart` | Custom bar chart for step/calorie history |
| `EmptyState` | Placeholder for empty lists with emoji, title, and subtitle |
| `StreakBadge` | Animated streak counter badge |
| `MoodSelector` | Emoji-based mood picker (1–5 scale) |

---

## 📋 App Flow

```
Launch
  │
  ├─ First Time? → Onboarding (4 steps) → Permission Screen → Main App
  │
  └─ Returning User → Permission Check → Main App
                                              │
                                              ▼
                                    ┌─────────────────┐
                                    │   Bottom Nav    │
                                    ├─────┬─────┬─────┤
                                    │Steps│Work │Cals │
                                    │     │ out │     │
                                    ├─────┼─────┼─────┤
                                    │Diet │Well │     │
                                    │     │being│     │
                                    └─────┴─────┴─────┘
```

---

## 📄 License

This project is developed as a personal/educational fitness application.

---

## 🤝 Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

<p align="center">
  Built with ❤️ using Kotlin & Jetpack Compose
</p>
