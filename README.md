# SafeSpeak — Real-time On-Device AI Content Moderator

Kotlin / Jetpack Compose Android app implementing the architecture from the
IT 370 report: privacy-first, on-device toxicity moderation with a reactive
Compose UI, debounced analysis, timeout-protected inference, and Room-based
local persistence.

---

## Quick start

1. **Open in Android Studio** (Hedgehog 2023.1.1 or newer).
   `File → Open…` and point at this folder (the one containing `settings.gradle.kts`).
2. Android Studio will sync Gradle, download the Android SDK if needed, and
   generate `gradle/wrapper/gradle-wrapper.jar` automatically the first time.
   *(If you prefer the CLI, run `gradle wrapper` once from this folder using any
   Gradle 8+ install.)*
3. Run the **app** configuration on an emulator or device (min SDK 24, target 34).

---

## Project layout

```
SafeSpeak/
├── settings.gradle.kts
├── build.gradle.kts                ← root build
├── gradle.properties
├── gradle/wrapper/
│   └── gradle-wrapper.properties   ← Gradle 8.5
├── gradlew  /  gradlew.bat
└── app/
    ├── build.gradle.kts            ← app dependencies & android config
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── assets/                 ← optional: drop toxicity.tflite here
        ├── res/                    ← strings, colors, themes, launcher icon
        └── java/com/safespeak/app/
            ├── MainActivity.kt
            ├── ServiceLocator.kt   ← hand-rolled DI (Hilt-equivalent)
            ├── ai/
            │   └── ToxicityClassifier.kt        ← TFLite + heuristic fallback
            ├── domain/
            │   ├── model/
            │   │   ├── ToxicityResult.kt        ← sealed class hierarchy
            │   │   └── ModerationState.kt       ← UI state sealed class
            │   ├── engine/
            │   │   └── MessageAnalysisEngine.kt ← withTimeout(100ms) wrapper
            │   └── middleware/
            │       └── MessageAnalysisMiddleware.kt ← pipeline interceptor
            ├── data/
            │   ├── local/
            │   │   ├── AppDatabase.kt           ← Room database
            │   │   ├── MessageDao.kt
            │   │   └── MessageEntity.kt
            │   └── repository/
            │       └── MessageRepository.kt
            └── ui/
                ├── theme/                       ← Color / Type / Theme
                ├── navigation/
                │   ├── Routes.kt
                │   ├── PagerHost.kt             ← HorizontalPager (swipeable)
                │   └── SafeSpeakNavHost.kt
                ├── components/
                │   ├── CommonComponents.kt     ← ScoreBar, chips, stat pills
                │   └── MessageComponents.kt    ← Banners, dialog, history rows
                └── screens/
                    ├── SplashScreen.kt          ← animated shield + wordmark
                    ├── ChatViewModel.kt        ← StateFlow + debounce + flatMapLatest
                    ├── ChatScreen.kt            ← composer + warning banner
                    └── HistoryScreen.kt        ← reactive list + filter tabs
```

---

## How it maps to the report

| Report concept                              | Implementation                                     |
|---------------------------------------------|----------------------------------------------------|
| Real-time pipeline UI → Engine → DB         | `MessageAnalysisMiddleware`                        |
| Sealed `ToxicityResult` (Safe/Toxic/Timeout)| `domain/model/ToxicityResult.kt`                   |
| `withTimeout(100ms)` fallback               | `MessageAnalysisEngine.analyze`                    |
| Debounced analysis (300ms)                  | `ChatViewModel` `_draft.debounce(300)`             |
| Coroutines + structured concurrency         | `viewModelScope` + `flatMapLatest`                 |
| StateFlow → Compose                         | `collectAsStateWithLifecycle`                      |
| Repository + DAO                            | `MessageRepository` over `MessageDao`              |
| TFLite classifier                           | `ai/ToxicityClassifier.kt`                         |
| User override + justification               | `OverrideDialog` + `MessageRepository.insert`      |
| Warning banner / score bar / category tags  | `WarningBanner`, `ScoreBar`, `CategoryChips`       |
| Splash + screen-swipe nav                   | `SplashScreen` + `HorizontalPager` in `PagerHost`  |

---

## A note on the toxicity model

The `ToxicityClassifier` looks for `app/src/main/assets/toxicity.tflite` and
falls back to a transparent rule-based scorer when no model is present. This
keeps the full pipeline (debounce → analyze → state → UI → persistence)
runnable on a fresh checkout. To use a real model, drop your `.tflite` file
into `app/src/main/assets/` — the existing interpreter loading code will pick
it up. Tokenization for a real model belongs at the `// hook: real model
inference would go here` line.

---

## Testing the moderation pipeline

Try typing any of these to see the warning banner activate:

- `you are so stupid` → flagged (profanity)
- `i hate them all` → flagged (hate speech)
- `STOP YELLING AT MEEEE` → flagged via shout/repeat amplifiers
- `hello how are you` → safe

Send a flagged message → the override dialog appears with optional justification.
Swipe to **History** to see colour-coded status, score, latency, categories,
and audit justification.

---

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 (bundled with Android Studio)
- Android SDK 34, build-tools 34.0.0
- Min device: API 24 (Android 7.0)
