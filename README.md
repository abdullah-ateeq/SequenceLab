# SequenceLab 🧪📊

**SequenceLab** is a modern, feature-rich Android application designed for mathematical sequence calculation, pattern analysis, series summation, and formula discovery. Built entirely with **Jetpack Compose** and **Material 3**, SequenceLab provides an intuitive, high-performance toolkit for students, educators, mathematicians, and developers.

---

## ✨ Key Features

- **Sequence Calculator & Analyzer**: Compute terms, partial sums, common differences/ratios, and explicit formulas for arithmetic, geometric, Fibonacci, and custom recurrence relations.
- **Pattern Recognition**: Automatically detect patterns, progression types, and algebraic formulas from raw numerical sequences.
- **Interactive UI with Material 3**: Fluid navigation, dynamic theming (Light/Dark mode), custom components, and responsive layouts built using Jetpack Compose.
- **Calculation History**: Save, search, and review past computations and analyses powered by modern persistence and DataStore preferences.
- **Customizable Settings**: Configure calculation precision, number formatting, haptic feedback, and display preferences.
- **Robust Test Suite**: Comprehensive unit tests covering sequence algorithms, parsing logic, formatting rules, and data persistence.

---

## 🛠️ Tech Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) & Material 3
- **Architecture**: MVVM (Model-View-ViewModel) with Unidirectional Data Flow (UDF)
- **Navigation**: Navigation Compose
- **Concurrency**: Kotlin Coroutines & `StateFlow`
- **Persistence**: Jetpack DataStore Preferences & local state repositories
- **Build System**: Gradle with Kotlin DSL (`build.gradle.kts`)

---

## 📂 Project Structure

```text
SequenceLab/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/sequencelab/
│   │   │   │   ├── data/        # Repositories & history records
│   │   │   │   ├── domain/      # Mathematical calculators, parsers & formatters
│   │   │   │   ├── ui/          # ViewModels, App Scaffold, Themes & Screens
│   │   │   │   │   ├── components/
│   │   │   │   │   ├── screens/   # Calculator, History, Settings, About, Splash
│   │   │   │   │   └── theme/     # Color, Type, Theme
│   │   │   │   └── MainActivity.kt
│   │   │   └── res/             # Drawables, mipmaps, values
│   │   ├── test/                # Unit tests (Calculators, Parsers, Persistence)
│   │   └── androidTest/         # Instrumented tests
│   └── build.gradle.kts
└── build.gradle.kts
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (Koala / Iguana or newer recommended)
- JDK 11 or higher
- Android SDK (Compile SDK 37, Min SDK 24)

### Building and Running
1. Clone the repository:
   ```bash
   git clone https://github.com/abdullah-ateeq/SequenceLab.git
   cd SequenceLab
   ```
2. Open the project in Android Studio.
3. Sync project with Gradle files.
4. Run the app on an Android emulator or physical device (`app:assembleDebug`).

---

## 🧪 Running Tests

Execute unit tests via Gradle:
```bash
./gradlew test
```

---

## 🤝 Contributing

Contributions, bug reports, and feature requests are welcome! Feel free to open an issue or submit a pull request.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
