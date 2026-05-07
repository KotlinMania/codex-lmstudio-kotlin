# codex-lmstudio-kotlin

Kotlin Multiplatform port of the [`codex-rs/lmstudio/`](https://github.com/openai/codex/tree/main/codex-rs/lmstudio)
crate from the openai/codex repository — a small async client for the
LM Studio OSS provider used by `codex-cli`.

## Status

Initial Kotlin Multiplatform port. The upstream Rust source is the oracle; see
[`AGENTS.md`](./AGENTS.md) and [`CLAUDE.md`](./CLAUDE.md) for porting
rules.

## Setup

The upstream Rust source is not tracked here. Fetch it once after
cloning:

```bash
./tools/fetch-rust-source.sh
```

That populates `tmp/codex-lmstudio/`.

## Build

```bash
./gradlew jsBrowserTest jsNodeTest linuxX64Test wasmJsBrowserTest wasmJsNodeTest
./gradlew compileKotlinIosArm64 compileKotlinIosSimulatorArm64 compileKotlinMacosArm64 iosSimulatorArm64Test macosArm64Test assembleCodexLMStudioXCFramework -PenableIosSimulatorTests=true
./gradlew mingwX64Test
```

CI follows the same target shape as `starlark-kotlin`: Linux/Android/JS/WASM,
macOS arm64/iOS simulator arm64/Swift XCFramework, and Windows.

## Targets

- macosArm64
- linuxX64
- mingwX64
- android
- iosArm64
- iosSimulatorArm64
- js
- wasmJs

## Coordinates

```kotlin
implementation("io.github.kotlinmania:codex-lmstudio-kotlin:0.1.0")
```

## License

Dual-licensed Apache-2.0 OR MIT, mirroring upstream `codex-rs/lmstudio`.
See [LICENSE-APACHE](./LICENSE-APACHE) and [LICENSE-MIT](./LICENSE-MIT).
