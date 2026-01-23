# Agent Guide for v2ex-simple

This document is for coding agents working in this repository. It captures
how to build, test, and follow existing style conventions.

## Repo Snapshot
- Android app written primarily in Kotlin (AndroidX, Room, OkHttp, Firebase).
- Gradle Kotlin DSL build files (`build.gradle.kts`).
- Main module: `app/` with source sets `main`, `debug`, `testDebug`.
- Additional module: `searchablespinnerlibrary/`.

## Build, Lint, and Test
Use the Gradle wrapper from repo root.

### Build
- Debug APK: `./gradlew :app:assembleDebug`
- Release APK: `./gradlew :app:assembleRelease`
- Install debug on device/emulator: `./gradlew :app:installDebug`

### Lint
- Lint debug: `./gradlew :app:lintDebug`
- Lint release: `./gradlew :app:lintRelease`
- Note: lint is configured with `abortOnError = false` in `app/build.gradle.kts`.

### Unit Tests
- Run all debug unit tests: `./gradlew :app:testDebugUnitTest`
- Run a single test class:
  `./gradlew :app:testDebugUnitTest --tests "im.fdx.v2ex.utils.TimeUtilTest"`
- Run a single test method:
  `./gradlew :app:testDebugUnitTest --tests "im.fdx.v2ex.utils.TimeUtilTest.toLong"`

### Instrumentation Tests (if/when added)
- Connected tests: `./gradlew :app:connectedDebugAndroidTest`

### General Gradle Tips
- List tasks: `./gradlew tasks`
- Clean build artifacts: `./gradlew clean`

## Local Config and Signing
- Release signing reads from `local.properties` (see `app/build.gradle.kts`).
- Expected keys: `keyAlias`, `keyPassword`, `storeFile`, `storePassword`.
- Avoid committing keystore files or secrets.

## Code Style and Conventions
Follow existing Kotlin and Android patterns in the codebase. Prefer
consistency with nearby files over adding new stylistic patterns.

### Kotlin Formatting
- 4-space indentation, no tabs.
- Braces on the same line as statements (`if (...) {`).
- Space before `{` and around operators (`a + b`).
- Avoid trailing semicolons.
- Keep line breaks readable; favor simple expressions over deep nesting.

### Imports
- Keep imports sorted and remove unused.
- No strict rule on grouping, but typical order is:
  Android/Java -> AndroidX -> third-party -> local `im.fdx.v2ex.*`.
- Wildcard imports are rare; existing code uses `okhttp3.*` when needed.

### Types and Nullability
- Prefer `val` over `var` unless mutation is required.
- Be explicit with nullability; avoid `!!` unless the value is truly guaranteed.
- Use safe calls (`?.`) and `let`/`run` to scope nullable handling.

### Naming
- Packages: lowercase (`im.fdx.v2ex...`).
- Classes/objects: PascalCase (`HttpHelper`, `TopicActivity`).
- Functions/variables: camelCase (`loadNodes`, `isSystemFont`).
- Constants: `const val` with uppercase and underscores (`MODE_SYSTEM`).
- Boolean vars prefixed with `is/has/should` when meaningful.
- Tests named `*Test` and placed in `app/src/testDebug`.

### Structure and Patterns
- Use `object` for stateless singletons (e.g., `HttpHelper`).
- Extension functions live under `im.fdx.v2ex.utils.extensions`.
- UI lives under `im.fdx.v2ex.ui.*` by feature (`main`, `topic`, `member`).
- Room database classes live under `im.fdx.v2ex.database`.

### Error Handling and Logging
- Wrap network callbacks with `try/catch` and avoid crashing callbacks.
- Report unexpected exceptions to Crashlytics when appropriate.
- Common logging helpers: `logd`, `logi`, and `Log.*`.
- Avoid swallowing exceptions without logging or reporting.

### Threading and Background Work
- Network is via OkHttp; use async `Call.enqueue` or helper wrappers.
- Background tasks use WorkManager (`GetMsgWorker`, `GetMoreRepliesWorker`).
- UI updates should run on the main thread.

### Android Resources
- Resource names are `snake_case`.
- Keep layouts and drawables organized by feature, following existing files.

### Gradle and Dependencies
- Update dependency versions in `app/build.gradle.kts` as needed.
- Room uses `androidx.room` plugin and KSP; keep schema in `app/schemas`.
- Kotlin/JVM target is 17; keep new code compatible.

## Tests and Fixtures
- Tests use JUnit4 (`org.junit.Test`).
- Keep tests small and deterministic; avoid network calls in unit tests.

## Docs and Metadata
- No Cursor rules (`.cursor/rules/` or `.cursorrules`) detected.
- No Copilot rules (`.github/copilot-instructions.md`) detected.

## When in Doubt
- Match the style of the nearest file in the same package.
- Prefer small, localized changes over large refactors.
- If a change touches UI or network behavior, scan similar classes for
  existing patterns before introducing new abstractions.
