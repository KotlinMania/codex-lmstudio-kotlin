# codex-lmstudio-kotlin

Kotlin Multiplatform port of the [`codex-rs/lmstudio/`](https://github.com/openai/codex/tree/main/codex-rs/lmstudio)
crate from the openai/codex repository — a small async client for the
LM Studio OSS provider used by `codex-cli`.

## Status

Skeleton. The upstream Rust source is the oracle; see
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
./gradlew compileKotlinMacosArm64
./gradlew macosArm64Test
```

CI builds for macOS arm64, Linux x64, and Windows mingwX64
(see `.github/workflows/ci.yml`). LM Studio itself is desktop-only,
so iOS, JS, and WasmJs targets are intentionally not included.

## Targets

- macosArm64
- linuxX64
- mingwX64

## Coordinates

```kotlin
implementation("io.github.kotlinmania:codex-lmstudio-kotlin:0.1.0")
```

## License

Dual-licensed Apache-2.0 OR MIT, mirroring upstream `codex-rs/lmstudio`.
See [LICENSE-APACHE](./LICENSE-APACHE) and [LICENSE-MIT](./LICENSE-MIT).
