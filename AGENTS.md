# Repository Guidelines

## Project Structure & Module Organization
- Root contains Gradle project files: `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, and wrapper scripts.
- App module lives in `app/`.
- Source code is under `app/src/main/java/com/toonflow/game/`, split by responsibility:
  - `api/` for Retrofit interfaces and client setup.
  - `data/` for models, storage, and repository logic.
  - `viewmodel/` for state and UI-facing business logic.
  - top-level package for Compose screens and `MainActivity`.
- Android resources are in `app/src/main/res/` (`values/`, `xml/`).
- Generated outputs are in `app/build/`; do not edit generated files.

## Build, Test, and Development Commands
- `./gradlew assembleDebug`: build debug APK.
- `./gradlew installDebug`: install debug build to a connected device/emulator.
- `./gradlew lint`: run Android lint checks.
- `./gradlew test`: run JVM unit tests.
- `./gradlew connectedAndroidTest`: run instrumentation/Compose tests on device or emulator.
- `./gradlew clean`: clear build outputs when cache/state is stale.

## Coding Style & Naming Conventions
- Kotlin style follows `kotlin.code.style=official` from `gradle.properties`.
- Match existing formatting: 2-space indentation, concise functions, and explicit state names.
- Naming:
  - packages: lowercase (`com.toonflow.game...`)
  - classes/objects: `PascalCase`
  - functions/properties: `camelCase`
  - constants: `UPPER_SNAKE_CASE`
- Compose UI functions should use descriptive names like `HomeScreen`, `HeaderBar`, `SessionCard`.

## Testing Guidelines
- Add tests with every feature/fix; currently there is no committed test suite.
- Place JVM tests in `app/src/test/...` and instrumentation/UI tests in `app/src/androidTest/...`.
- Use `*Test.kt` naming (example: `GameRepositoryTest.kt`).
- Before PR, run at least: `./gradlew test lint`.

## Commit & Pull Request Guidelines
- Git history is unavailable in this workspace, so use Conventional Commits (for example: `feat: add hall world filtering`).
- Keep commits atomic and scoped to one concern.
- PRs should include:
  - concise change summary
  - related issue/task link
  - verification steps and executed commands
  - screenshots or recordings for UI changes

## Security & Configuration Tips
- Never commit secrets or personal tokens; keep machine-specific values in `local.properties`.
- Validate API base URL/token handling in `SettingsStore` before release.
- Keep network security settings explicit in `app/src/main/res/xml/network_security_config.xml`.


# 系统环境配置
[system.yml](system/system.yml)

# web端和安卓端同步修改
web端和安卓端需要同步修改内容
但是也得考虑一下web 是否适用！
例如按住语音输入这个适用安卓，但是不适用web.
同样的web的实现安卓是否适用
# 设计要点
按钮多用奥森字体图标而不是文字