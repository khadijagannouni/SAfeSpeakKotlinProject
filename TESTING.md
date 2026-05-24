# SafeSpeak — Test Guide

This document describes every test type in the project and how to run them,
mapped one-to-one with the SafeSpeak report (§7 Testing and Evaluation).

## Test layout

```
app/src/
├── test/                                  ← JVM unit tests
│   └── java/com/safespeak/app/
│       ├── ai/ToxicityClassifierTest.kt        Heuristic scorer behaviour
│       ├── domain/MessageAnalysisEngineTest.kt Moderation rules + timeout
│       └── viewmodel/ChatFlowAnalysisTest.kt   Engine ↔ classifier integration
│
└── androidTest/                           ← Instrumented (device/emulator) tests
    └── java/com/safespeak/app/
        ├── integration/
        │   ├── RoomRepositoryIntegrationTest.kt   Room + repository round-trip
        │   └── ModerationPipelineIntegrationTest.kt  Full pipeline end-to-end
        ├── performance/LatencyPerformanceTest.kt  Latency budget (< 100 ms)
        ├── privacy/NoNetworkPrivacyTest.kt        Zero network bytes during moderation
        └── ui/
            ├── ModerationUiTest.kt        Warning banner + override flow
            └── SendButtonStateTest.kt     Send disabled when input is blank
```

## How to run

| Goal                       | Command                                        |
|----------------------------|------------------------------------------------|
| Unit tests (JVM, fast)     | `./gradlew :app:testDebugUnitTest`             |
| Instrumented tests (device)| `./gradlew :app:connectedDebugAndroidTest`     |
| Specific test class        | `./gradlew :app:testDebugUnitTest --tests "*.ToxicityClassifierTest"` |

> Instrumented tests need an emulator or physical device connected.

## What each test covers

### 1. Unit Testing
- **`ToxicityClassifierTest`** — verifies the heuristic scorer returns
  meaningfully different scores for different inputs, respects word
  boundaries, applies SHOUT/REPEAT amplifiers, and detects each category.
- **`MessageAnalysisEngineTest`** — covers all three sealed-class branches
  (`Safe`, `Toxic`, `SafeWithTimeout`), threshold logic, timeout handling
  via `withTimeout(100ms)`, and exception resilience.
- **`ChatFlowAnalysisTest`** — exercises the engine + real classifier as
  invoked by the ChatViewModel's debounced flow.

### 2. Integration Testing
- **`RoomRepositoryIntegrationTest`** — round-trips every result variant
  through an in-memory Room DB, including the override / justification path.
- **`ModerationPipelineIntegrationTest`** — end-to-end test of
  Classifier → Engine → Middleware → Repository → Room.

### 3. Performance Testing
- **`LatencyPerformanceTest`** — runs 100 analyses, asserts p95 < 80 ms and
  p99 < 100 ms (matches report §7.4 / §8.1 targets).

### 4. Privacy Validation
- **`NoNetworkPrivacyTest`** — uses Android's `TrafficStats` to assert zero
  bytes transmitted during a batch of analyses, plus a static check that
  Retrofit / OkHttp / Firebase classes are not loadable.

### 5. UI Testing
- **`ModerationUiTest`** — verifies the warning banner appears for toxic
  state and the override dialog accepts a justification.
- **`SendButtonStateTest`** — verifies the Send button is disabled when
  the draft is blank and enabled (and clickable) when text is present.
