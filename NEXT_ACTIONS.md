# Next Actions

Skeleton in place. Outstanding work:

## 1. Define a minimal `LmstudioConfig` interface

`codex-kotlin` does not publish `Config`/`ModelProviderInfo` as a
separate Maven artifact. To keep this library standalone, define:

```kotlin
package io.github.kotlinmania.codex.lmstudio

interface LmstudioConfig {
    val baseUrl: String?
    val model: String
}
```

Downstream callers (codex-kotlin) implement this against their own
`Config.modelProviders[LMSTUDIO_OSS_PROVIDER_ID]`.

## 2. Port `src/lib.rs` → `Lib.kt`

- `DEFAULT_OSS_MODEL = "openai/gpt-oss-20b"`
- `suspend fun ensureOssReady(config: LmstudioConfig)` — reachability,
  list, download, background-load.

## 3. Port `src/client.rs` → `Client.kt`

- `LMStudioClient` data class wrapping `HttpClient` + `baseUrl`.
- `tryFromProvider`, `checkServer`, `loadModel`, `fetchModels`,
  `downloadModel`, `findLms` (and `findLmsWithHomeDir`).

## 4. Port the test suite to `commonTest`

- Replace `wiremock` with `ktor-client-mock` `MockEngine`.
- Replace `tokio::test` with `runTest`.
- Preserve the `CODEX_SANDBOX_NETWORK_DISABLED` env-var skip semantics
  (read via `platform.posix.getenv` in `nativeTest`, or via a
  cross-platform expect-actual `getEnvOrNull`).

## 5. Wire up downstream

Once published:

```kotlin
// in codex-kotlin/build.gradle.kts
implementation("io.github.kotlinmania:codex-lmstudio-kotlin:0.1.0")
```

Then port `codex-rs/common/src/oss.rs` (currently blocked on this
crate). It dispatches between the lmstudio and ollama OSS providers.
